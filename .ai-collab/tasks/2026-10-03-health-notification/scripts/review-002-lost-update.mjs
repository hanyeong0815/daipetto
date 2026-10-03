// Reviewer reproduction after smoke/fix-acceptance; scratch database only.
import assert from 'node:assert/strict'
import { execFileSync, spawn } from 'node:child_process'
const DB = process.env.DB, BASE = process.env.BASE
assert.equal(DB, 'daipetto_review_1003')
assert.equal(BASE, 'http://localhost:18081')
const args = ['exec', '-i', 'daipetto-postgres', 'psql', '-U', 'daipetto_user', '-d', DB, '-t', '-A', '-v', 'ON_ERROR_STOP=1']
const sql = q => execFileSync('docker', args, { input: q, encoding: 'utf8' }).trim()
let token
async function api(method, path, body) {
  const res = await fetch(BASE + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined })
  return { status: res.status, ...await res.json() }
}
token = (await api('POST', '/api/v1/auth/login', { email: 'hn-user@daipetto.test', password: 'Password123!' })).data.accessToken
const petId = Number(sql("SELECT p.id FROM pets p JOIN users u ON p.user_id=u.id WHERE u.email='hn-user@daipetto.test' AND p.deleted_at IS NULL ORDER BY p.id LIMIT 1;"))
const id = (await api('POST', `/api/v1/pets/${petId}/health-records`, { weight: 4.7, memo: 'original', symptom: 'cough' })).data.healthRecordId
const holder = spawn('docker', args, { stdio: ['pipe', 'pipe', 'pipe'] })
let output = ''
holder.stdout.on('data', chunk => { output += chunk })
holder.stderr.on('data', chunk => process.stderr.write(chunk))
const closed = new Promise(resolve => holder.on('close', resolve))
holder.stdin.write(`BEGIN; UPDATE health_records SET memo='concurrent-memo' WHERE id=${id}; SELECT 'READY';\n`)
async function waitFor(predicate) {
  for (let i = 0; i < 80; i++) { if (predicate()) return; await new Promise(r => setTimeout(r, 100)) }
  throw new Error('Expected interleaving not reached')
}
let pending
try {
  await waitFor(() => output.includes('READY'))
  pending = api('PATCH', `/api/v1/health-records/${id}`, { weight: 5.3 })
  await waitFor(() => Number(sql("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE 'update health_records%';")) > 0)
} finally { holder.stdin.end('COMMIT;\n'); await closed }
const result = await pending
const state = sql(`SELECT weight::text || '/' || memo FROM health_records WHERE id=${id};`)
console.log('Weight-only PATCH after concurrent memo commit:', result.status, state)
assert.equal(result.status, 200)
assert.equal(state, '5.30/original', 'Reproduces defect: unsent memo overwritten with stale value')
console.log('R-04 reproduced: concurrent-memo lost despite PATCH not supplying memo.')
