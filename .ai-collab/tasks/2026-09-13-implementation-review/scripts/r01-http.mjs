// R-01 re-verification through the real Spring stack (isolated backend on :18080, DB daipetto_r01).
// A psql session holds the user row so the HTTP operations queue in a known order:
// refresh acquires the lock first and is still in-flight when logout / login arrive.
import { execSync, spawn } from 'node:child_process'

const BASE = 'http://localhost:18080'
const DB = 'daipetto_r01'
const PASSWORD = 'Password123!'
const USER = 'r01@daipetto.test'

const results = []
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? `  -- ${detail}` : ''}`)
}

async function api(method, path, { token, body } = {}) {
  const started = Date.now()
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let json = null
  try { json = text ? JSON.parse(text) : null } catch { json = null }
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null, ms: Date.now() - started }
}

const sql = (q) => execSync(`docker exec daipetto-postgres psql -U daipetto_user -d ${DB} -t -A -c "${q}"`, { encoding: 'utf8' }).trim()
const activeTokens = (uid) => sql(`SELECT coalesce(string_agg(token,','),'') FROM refresh_tokens WHERE user_id=${uid} AND revoked=false`)
const activeCount = (uid) => Number(sql(`SELECT count(*) FROM refresh_tokens WHERE user_id=${uid} AND revoked=false`))
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// Holds `SELECT ... FOR UPDATE` on the user row for `seconds`, so later requests queue behind it.
function holdUserRow(uid, seconds) {
  const p = spawn('docker', ['exec', '-i', 'daipetto-postgres', 'psql', '-U', 'daipetto_user', '-d', DB, '-q'], { stdio: ['pipe', 'ignore', 'ignore'] })
  p.stdin.end(`BEGIN;\nSELECT id FROM users WHERE id=${uid} FOR UPDATE;\nSELECT pg_sleep(${seconds});\nCOMMIT;\n`)
  return new Promise((resolve) => p.on('close', resolve))
}

const login = async () => (await api('POST', '/api/v1/auth/login', { body: { email: USER, password: PASSWORD } })).data

await api('POST', '/api/v1/users', { body: { email: USER, password: PASSWORD, nickname: 'r01user' } })
const userId = Number(sql(`SELECT id FROM users WHERE email='${USER}'`))

// --------------------------------------------------- E1: refresh first, then logout
{
  const s = await login()
  const holder = holdUserRow(userId, 6)
  await sleep(800)
  const refreshCall = api('POST', '/api/v1/auth/refresh', { body: { refreshToken: s.refreshToken } })
  await sleep(800)
  const logoutCall = api('POST', '/api/v1/auth/logout', { token: s.accessToken, body: { refreshToken: s.refreshToken } })
  const [refresh, logout] = await Promise.all([refreshCall, logoutCall])
  await holder

  const remaining = activeTokens(userId)
  const successorReuse = refresh.status === 200
    ? await api('POST', '/api/v1/auth/refresh', { body: { refreshToken: refresh.data.refreshToken } })
    : { status: null, code: null }

  check('E1 refresh holds the lock first, logout waits: no session survives logout',
    refresh.status === 200 && logout.status === 200 && remaining === '',
    `refresh=${refresh.status}(${refresh.ms}ms) logout=${logout.status}(${logout.ms}ms) active="${remaining}"`)
  check('E1 the successor issued by the racing refresh is already revoked',
    successorReuse.status === 401 && successorReuse.code === 'AUTH-003',
    `${successorReuse.status} ${successorReuse.code}`)
}

// --------------------------------------------------- E2: refresh first, then login
{
  const s = await login()
  const holder = holdUserRow(userId, 6)
  await sleep(800)
  const refreshCall = api('POST', '/api/v1/auth/refresh', { body: { refreshToken: s.refreshToken } })
  await sleep(800)
  const loginCall = api('POST', '/api/v1/auth/login', { body: { email: USER, password: PASSWORD } })
  const [refresh, second] = await Promise.all([refreshCall, loginCall])
  await holder

  const remaining = activeTokens(userId)
  check('E2 refresh racing with login leaves exactly one active session (the login token)',
    refresh.status === 200 && second.status === 200 && remaining === second.data.refreshToken,
    `refresh=${refresh.status} login=${second.status} activeCount=${activeCount(userId)} isLoginToken=${remaining === second.data.refreshToken}`)
}

// --------------------------------------------------- E3: concurrent refresh (retained coverage)
{
  const s = await login()
  const racers = await Promise.all(Array.from({ length: 8 }, () => api('POST', '/api/v1/auth/refresh', { body: { refreshToken: s.refreshToken } })))
  const ok = racers.filter((r) => r.status === 200).length
  const rejected = racers.filter((r) => r.status === 401 && r.code === 'AUTH-003').length
  check('E3 8 concurrent refreshes of one token: one succeeds, rest AUTH-003, one active token',
    ok === 1 && rejected === 7 && activeCount(userId) === 1, `ok=${ok} rejected=${rejected} active=${activeCount(userId)}`)
}

// --------------------------------------------------- E4: concurrent logins (previous residual)
{
  const racers = await Promise.all(Array.from({ length: 8 }, () => api('POST', '/api/v1/auth/login', { body: { email: USER, password: PASSWORD } })))
  const ok = racers.filter((r) => r.status === 200).length
  const active = activeCount(userId)
  check('E4 8 concurrent logins serialize into exactly one active session',
    ok === 8 && active === 1, `ok=${ok} active=${active}`)
}

// --------------------------------------------------- E5: the lock is actually taken by each operation
{
  const s = await login()
  const holder = holdUserRow(userId, 4)
  await sleep(800)
  const logout = await api('POST', '/api/v1/auth/logout', { token: s.accessToken, body: { refreshToken: s.refreshToken } })
  await holder
  check('E5 logout waits for the held user row (proves it takes the same lock)',
    logout.status === 200 && logout.ms > 2000, `logout=${logout.status} waited=${logout.ms}ms`)
}

const failed = results.filter((r) => !r.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
