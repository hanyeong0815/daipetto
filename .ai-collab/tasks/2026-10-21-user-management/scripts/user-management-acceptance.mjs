// Acceptance checks for the SC-015 user-management APIs and the per-request user check in the JWT
// filter, through the real Spring stack + PostgreSQL. Needs a backend on a fresh throwaway database
// (see README.md). Fixture credentials exist only in that database.
import { execFileSync, spawn } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18081'
const DB = process.env.DB ?? 'daipetto_um_1021'
const PASSWORD = 'Password123!'
const psqlArgs = ['exec', '-i', 'daipetto-postgres', 'psql', '-U', 'daipetto_user', '-d', DB, '-t', '-A', '-v', 'ON_ERROR_STOP=1']
const sql = (q) => execFileSync('docker', psqlArgs, { input: q, encoding: 'utf8' }).trim()

const results = []
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? `  -- ${detail}` : ''}`)
}

async function api(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
  })
  const json = await res.json().catch(() => null)
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null }
}

const signUp = (email, nickname) => api('POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname } })
const login = async (email) => (await api('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } }))
const idOf = (email) => Number(sql(`SELECT id FROM users WHERE email='${email}';`))

async function waitFor(predicate, label) {
  for (let i = 0; i < 100; i++) {
    if (predicate()) return
    await new Promise((r) => setTimeout(r, 100))
  }
  throw new Error(`timed out waiting for ${label}`)
}

await signUp('um-admin@daipetto.test', 'Admin')
await signUp('um-tanaka@daipetto.test', 'Tanaka')
await signUp('um-sato@daipetto.test', 'SATO')
await signUp('um-vet@daipetto.test', 'HappyVet')
sql("UPDATE users SET role='ROLE_SYSTEM_ADMIN' WHERE email='um-admin@daipetto.test'; UPDATE users SET role='ROLE_HOSPITAL_ADMIN' WHERE email='um-vet@daipetto.test';")
sql("INSERT INTO users(email,password,nickname,role,status,deleted_at) VALUES ('um-gone@daipetto.test','x','Gone','ROLE_USER','ACTIVE',now());")
const adminId = idOf('um-admin@daipetto.test')
const tanakaId = idOf('um-tanaka@daipetto.test')
const satoId = idOf('um-sato@daipetto.test')
const admin = (await login('um-admin@daipetto.test')).data.accessToken
const vet = (await login('um-vet@daipetto.test')).data.accessToken

// ------------------------------------------------------------- authorization
{
  const tanaka = (await login('um-tanaka@daipetto.test')).data.accessToken
  const r1 = await api('GET', '/api/v1/admin/users', { token: tanaka })
  const r2 = await api('PATCH', `/api/v1/admin/users/${satoId}/suspend`, { token: vet })
  check('ROLE-T007 ROLE_USER and ROLE_HOSPITAL_ADMIN get 403 on user management', r1.status === 403 && r1.code === 'AUTH-005' && r2.status === 403,
    `${r1.status} ${r1.code} / ${r2.status} ${r2.code}`)
}

// ------------------------------------------------------------------ search
{
  const all = await api('GET', '/api/v1/admin/users', { token: admin })
  const emails = all.data?.content?.map((u) => u.email)
  check('ADMIN-T001 no filters on PostgreSQL (null params) -> all non-deleted users, id order',
    all.status === 200 && JSON.stringify(emails) === JSON.stringify(['um-admin@daipetto.test', 'um-tanaka@daipetto.test', 'um-sato@daipetto.test', 'um-vet@daipetto.test']) && all.data.totalElements === 4,
    `${all.status} ${JSON.stringify(emails)}`)
  check('response carries no password and createdAt has +09:00', all.data?.content?.every((u) => !('password' in u) && /\+09:00$/.test(u.createdAt)), all.data?.content?.[0]?.createdAt)

  const kw = await api('GET', '/api/v1/admin/users?keyword=sato', { token: admin })
  check('keyword matches nickname case-insensitively (SATO)', kw.data?.content?.length === 1 && kw.data.content[0].userId === satoId, JSON.stringify(kw.data?.content?.map((u) => u.nickname)))

  const role = await api('GET', '/api/v1/admin/users?role=ROLE_HOSPITAL_ADMIN', { token: admin })
  check('role filter', role.data?.content?.map((u) => u.email).join() === 'um-vet@daipetto.test', JSON.stringify(role.data?.content?.map((u) => u.email)))

  const paged = await api('GET', '/api/v1/admin/users?page=1&size=3', { token: admin })
  check('paging page=1,size=3 -> 1 item, totalPages 2', paged.data?.content?.length === 1 && paged.data.totalPages === 2 && paged.data.number === 1,
    JSON.stringify({ n: paged.data?.content?.length, totalPages: paged.data?.totalPages, number: paged.data?.number }))

  const bad = await api('GET', '/api/v1/admin/users?role=ROLE_ROOT', { token: admin })
  const badBody = await api('PATCH', `/api/v1/admin/users/${satoId}/role`, { token: admin, body: '{"role":"ROLE_ROOT"}' })
  check('ADMIN-T006 unknown enum in query and JSON -> 400 VALIDATION-001', bad.status === 400 && bad.code === 'VALIDATION-001' && badBody.status === 400 && badBody.code === 'VALIDATION-001',
    `${bad.status} ${bad.code} / ${badBody.status} ${badBody.code}`)
}

// ----------------------------------------------------------------- self / missing
{
  const self = await api('PATCH', `/api/v1/admin/users/${adminId}/suspend`, { token: admin })
  const selfRole = await api('PATCH', `/api/v1/admin/users/${adminId}/role`, { token: admin, body: { role: 'ROLE_USER' } })
  check('ADMIN-T005 self suspend / role change -> 400 USER-005', self.status === 400 && self.code === 'USER-005' && selfRole.code === 'USER-005', `${self.code} / ${selfRole.code}`)
  const missing = await api('PATCH', '/api/v1/admin/users/999999/suspend', { token: admin })
  const deleted = await api('PATCH', `/api/v1/admin/users/${idOf('um-gone@daipetto.test')}/suspend`, { token: admin })
  check('missing or deleted user -> 404 USER-003', missing.status === 404 && missing.code === 'USER-003' && deleted.code === 'USER-003', `${missing.code} / ${deleted.code}`)
}

// ------------------------------------------------------------------ suspend / activate
{
  const session = (await login('um-tanaka@daipetto.test')).data
  const before = await api('GET', '/api/v1/users/me', { token: session.accessToken })
  const suspend = await api('PATCH', `/api/v1/admin/users/${tanakaId}/suspend`, { token: admin })
  const after = await api('GET', '/api/v1/users/me', { token: session.accessToken })
  const activeTokens = Number(sql(`SELECT count(*) FROM refresh_tokens WHERE user_id=${tanakaId} AND revoked=false;`))
  const refresh = await api('POST', '/api/v1/auth/refresh', { body: { refreshToken: session.refreshToken } })
  const relogin = await login('um-tanaka@daipetto.test')
  check('ADMIN-T002 suspend: same access token 200 -> 401 on the next request; refresh tokens revoked',
    before.status === 200 && suspend.status === 200 && after.status === 401 && activeTokens === 0,
    `${before.status} -> ${after.status}, active refresh=${activeTokens}`)
  check('suspended user cannot refresh and login returns AUTH-002', refresh.status >= 400 && relogin.status === 403 && relogin.code === 'AUTH-002',
    `refresh ${refresh.status} ${refresh.code}, login ${relogin.status} ${relogin.code}`)

  const again = await api('PATCH', `/api/v1/admin/users/${tanakaId}/suspend`, { token: admin })
  const activate = await api('PATCH', `/api/v1/admin/users/${tanakaId}/activate`, { token: admin })
  const back = await login('um-tanaka@daipetto.test')
  check('ADMIN-T003 repeated suspend is 200; activate lets the user log in again', again.status === 200 && activate.status === 200 && back.status === 200,
    `${again.status} ${activate.status} ${back.status}`)
}

// ----------------------------------------------------------------- role change
{
  const satoToken = (await login('um-sato@daipetto.test')).data.accessToken
  const denied = await api('GET', '/api/v1/admin/users', { token: satoToken })
  const promote = await api('PATCH', `/api/v1/admin/users/${satoId}/role`, { token: admin, body: { role: 'ROLE_SYSTEM_ADMIN' } })
  const allowed = await api('GET', '/api/v1/admin/users', { token: satoToken })
  const demote = await api('PATCH', `/api/v1/admin/users/${satoId}/role`, { token: admin, body: { role: 'ROLE_USER' } })
  const deniedAgain = await api('GET', '/api/v1/admin/users', { token: satoToken })
  check('ADMIN-T004 role change applies to the same access token on the next request (403 -> 200 -> 403)',
    denied.status === 403 && promote.status === 200 && allowed.status === 200 && demote.status === 200 && deniedAgain.status === 403,
    `${denied.status} -> ${allowed.status} -> ${deniedAgain.status}`)
}

// ------------------------------------------- race: suspension vs an in-flight refresh
// A psql session plays a refresh that holds the user row lock and inserts a successor token. The
// admin suspension must wait for it (session lock) and then revoke that successor too.
{
  await login('um-tanaka@daipetto.test')
  const holder = spawn('docker', psqlArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
  let output = ''
  holder.stdout.on('data', (c) => { output += c })
  const closed = new Promise((resolve) => holder.on('close', resolve))
  holder.stdin.write(`BEGIN; SELECT id FROM users WHERE id=${tanakaId} FOR UPDATE; UPDATE refresh_tokens SET revoked=true WHERE user_id=${tanakaId}; INSERT INTO refresh_tokens(user_id, token, expires_at, revoked) VALUES (${tanakaId}, 'successor-from-inflight-refresh', now() + interval '14 days', false); SELECT 'HOLDING';\n`)
  let pending
  try {
    await waitFor(() => output.includes('HOLDING'), 'holder')
    pending = api('PATCH', `/api/v1/admin/users/${tanakaId}/suspend`, { token: admin })
    await waitFor(() => Number(sql("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query ILIKE 'select%from users%for%';")) > 0,
      'suspension blocked on the user row lock')
  } finally {
    holder.stdin.end('COMMIT;\n')
    await closed
  }
  const r = await pending
  const successor = sql("SELECT revoked::text FROM refresh_tokens WHERE token='successor-from-inflight-refresh';")
  check('race: suspension waits for an in-flight refresh and revokes its successor token', r.status === 200 && successor === 'true', `${r.status} successor revoked=${successor}`)
  await api('PATCH', `/api/v1/admin/users/${tanakaId}/activate`, { token: admin })
}

// --------------------------------------- race: login in flight vs suspension (REVIEW-001 R-01)
// A psql session plays the suspension: it holds the user row lock, sets SUSPENDED and revokes tokens.
// The login has already read ACTIVE (committed state) and must block on the session lock; after the
// suspension commits it must re-read the status and fail with AUTH-002 without issuing a token.
{
  const holder = spawn('docker', psqlArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
  let output = ''
  holder.stdout.on('data', (c) => { output += c })
  const closed = new Promise((resolve) => holder.on('close', resolve))
  holder.stdin.write(`BEGIN; SELECT id FROM users WHERE id=${tanakaId} FOR UPDATE; UPDATE users SET status='SUSPENDED' WHERE id=${tanakaId}; UPDATE refresh_tokens SET revoked=true WHERE user_id=${tanakaId}; SELECT 'HOLDING';\n`)
  let pending
  try {
    await waitFor(() => output.includes('HOLDING'), 'holder')
    pending = login('um-tanaka@daipetto.test')
    await waitFor(() => Number(sql("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query ILIKE 'select%from users%for%';")) > 0,
      'login blocked on the user row lock')
  } finally {
    holder.stdin.end('COMMIT;\n')
    await closed
  }
  const r = await pending
  const active = Number(sql(`SELECT count(*) FROM refresh_tokens WHERE user_id=${tanakaId} AND revoked=false;`))
  check('ADMIN-T007 race: login that read ACTIVE before a suspension commits -> 403 AUTH-002, no active refresh token',
    r.status === 403 && r.code === 'AUTH-002' && active === 0, `${r.status} ${r.code} active=${active}`)
  await api('PATCH', `/api/v1/admin/users/${tanakaId}/activate`, { token: admin })
}

const failed = results.filter((x) => !x.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
