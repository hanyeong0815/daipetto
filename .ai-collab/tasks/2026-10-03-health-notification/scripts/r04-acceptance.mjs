// Acceptance for REVIEW-002 R-04: a partial PATCH must not write back stale values of fields it did
// not receive when another change to the same record commits while the PATCH is in flight.
// Same deterministic method as fix-acceptance.mjs: a psql session holds a row lock uncommitted, the
// HTTP request(s) are sent, the script waits until pg_stat_activity shows them blocked on
// health_records (it throws otherwise), then the holder commits. Asserts the FIXED behaviour.
// Run after smoke-health-notification.mjs against the same isolated backend (fixtures reused).
import { execFileSync, spawn } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18080'
const DB = process.env.DB ?? 'daipetto_hn'
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
    body: body ? JSON.stringify(body) : undefined,
  })
  const json = await res.json().catch(() => null)
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null }
}

async function waitFor(predicate, label) {
  for (let i = 0; i < 100; i++) {
    if (predicate()) return
    await new Promise((r) => setTimeout(r, 100))
  }
  throw new Error(`timed out waiting for ${label}`)
}

const blockedOnHealthRecords = () => Number(sql(
  "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND (query LIKE 'update health_records%' OR query LIKE 'select % from health_records % for %');",
))

// Holds `statement` uncommitted, fires `requests`, waits until all of them are blocked, then commits.
async function interleave(statement, requests) {
  const holder = spawn('docker', psqlArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
  let output = ''
  holder.stdout.on('data', (chunk) => { output += chunk })
  const closed = new Promise((resolve) => holder.on('close', resolve))
  holder.stdin.write(`BEGIN; ${statement}; SELECT 'HOLDING';\n`)
  let pending
  try {
    await waitFor(() => output.includes('HOLDING'), 'holder')
    pending = requests.map((request) => request())
    await waitFor(() => blockedOnHealthRecords() >= requests.length, `${requests.length} request(s) blocked on health_records`)
  } finally {
    holder.stdin.end('COMMIT;\n')
    await closed
  }
  return Promise.all(pending)
}

const token = (await api('POST', '/api/v1/auth/login', { body: { email: 'hn-user@daipetto.test', password: 'Password123!' } })).data.accessToken
const petId = Number(sql("SELECT p.id FROM pets p JOIN users u ON p.user_id=u.id WHERE u.email='hn-user@daipetto.test' AND p.deleted_at IS NULL ORDER BY p.id LIMIT 1;"))
const createRecord = async () => (await api('POST', `/api/v1/pets/${petId}/health-records`, {
  token, body: { weight: 4.7, symptom: 'cough', memo: 'original', recordedDate: '2026-05-20' },
})).data.healthRecordId
const patch = (id, body) => () => api('PATCH', `/api/v1/health-records/${id}`, { token, body })
const recordState = (id) => sql(`SELECT coalesce(weight::text,'NULL') || '/' || coalesce(symptom,'NULL') || '/' || coalesce(memo,'NULL') || '/' || recorded_date FROM health_records WHERE id=${id};`)

// The reviewer's reproduction (review-002-lost-update.mjs), asserting the fixed outcome.
{
  const id = await createRecord()
  const [r] = await interleave(`UPDATE health_records SET memo='concurrent-memo' WHERE id=${id}`, [patch(id, { weight: 5.3 })])
  check('weight-only PATCH behind a committed memo update keeps the concurrent memo',
    r.status === 200 && recordState(id) === '5.30/cough/concurrent-memo/2026-05-20', `${r.status} ${recordState(id)}`)
}

{
  const id = await createRecord()
  const [r] = await interleave(`UPDATE health_records SET memo='concurrent-memo' WHERE id=${id}`, [patch(id, {})])
  check('empty PATCH behind a committed memo update does not undo it',
    r.status === 200 && recordState(id) === '4.70/cough/concurrent-memo/2026-05-20', `${r.status} ${recordState(id)}`)
}

// Two real HTTP PATCHes to different fields, both in flight at once behind a held row lock.
{
  const id = await createRecord()
  const [memoOnly, weightOnly] = await interleave(
    `SELECT id FROM health_records WHERE id=${id} FOR UPDATE`,
    [patch(id, { memo: 'http-memo' }), patch(id, { weight: 5.3 })],
  )
  check('two concurrent PATCHes to different fields both survive',
    memoOnly.status === 200 && weightOnly.status === 200 && recordState(id) === '5.30/cough/http-memo/2026-05-20',
    `${memoOnly.status}/${weightOnly.status} ${recordState(id)}`)
}

const failed = results.filter((r) => !r.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
