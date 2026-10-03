// Reviewer-owned reproduction; run only after smoke-health-notification.mjs on the scratch backend.
import assert from 'node:assert/strict'
import { execFileSync, spawn } from 'node:child_process'
const DB = process.env.DB
const BASE = process.env.BASE
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
const petId = Number(sql("SELECT p.id FROM pets p JOIN users u ON p.user_id=u.id WHERE u.email='hn-user@daipetto.test';"))
const create = async () => (await api('POST', `/api/v1/pets/${petId}/health-records`, { weight: 4.7, symptom: 'cough', memo: 'original', recordedDate: '2026-05-20' })).data.healthRecordId
const partialId = await create()
const partial = await api('PATCH', `/api/v1/health-records/${partialId}`, { memo: 'memo-only' })
const partialState = sql(`SELECT coalesce(weight::text,'NULL') || '/' || coalesce(symptom,'NULL') || '/' || memo FROM health_records WHERE id=${partialId};`)
console.log('R-01 memo-only PATCH:', partial.status, partialState)
assert.equal(partial.status, 200)
assert.equal(partialState, 'NULL/NULL/memo-only', 'Expected current defect: omitted fields lost')

const raceId = await create()
// Hold the exact soft-delete mutation uncommitted; PATCH reads the previous MVCC row,
// then waits on its write. Commit the deletion before allowing PATCH to finish.
const holder = spawn('docker', args, { stdio: ['pipe', 'pipe', 'pipe'] })
let output = ''
holder.stdout.on('data', chunk => { output += chunk })
holder.stderr.on('data', chunk => process.stderr.write(chunk))
const closed = new Promise(resolve => holder.on('close', resolve))
holder.stdin.write(`BEGIN; UPDATE health_records SET deleted_at=CURRENT_TIMESTAMP WHERE id=${raceId}; SELECT 'DELETE_READY';\n`)
async function waitFor(predicate) {
  for (let i = 0; i < 80; i++) { if (predicate()) return; await new Promise(r => setTimeout(r, 100)) }
  throw new Error('Timed out waiting for deterministic interleaving')
}
let race
try {
  await waitFor(() => output.includes('DELETE_READY'))
  race = api('PATCH', `/api/v1/health-records/${raceId}`, { weight: 5.2, symptom: 'better', memo: 'racing' })
  await waitFor(() => Number(sql("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE 'update health_records%';")) > 0)
  console.log('R-02 PATCH reached UPDATE and is waiting behind soft-delete transaction')
} finally {
  holder.stdin.end('COMMIT;\n')
  await closed
}
const raceResponse = await race
const state = sql(`SELECT (deleted_at IS NULL)::text || '/' || memo FROM health_records WHERE id=${raceId};`)
const listed = (await api('GET', `/api/v1/pets/${petId}/health-records`)).data.some(r => r.healthRecordId === raceId)
console.log('R-02 PATCH after deletion commit:', raceResponse.status, state, 'visible:', listed)
assert.equal(raceResponse.status, 200)
assert.equal(state, 'true/racing', 'Expected current defect: deleted_at cleared')
assert.equal(listed, true)
console.log('Both reported defects reproduced (assertions deliberately describe defective behavior).')

// Wider review: the existing hospital update uses the same stale full-entity save pattern.
token = (await api('POST', '/api/v1/auth/login', { email: 'hn-admin@daipetto.test', password: 'Password123!' })).data.accessToken
const hospitalId = (await api('POST', '/api/v1/admin/hospitals', { name: 'Race hospital', address: 'Tokyo', phoneNumber: '03-0000-0000' })).data.hospitalId
const suspendHolder = spawn('docker', args, { stdio: ['pipe', 'pipe', 'pipe'] })
let suspendOutput = ''
suspendHolder.stdout.on('data', chunk => { suspendOutput += chunk })
const suspendClosed = new Promise(resolve => suspendHolder.on('close', resolve))
suspendHolder.stdin.write(`BEGIN; UPDATE hospitals SET status='SUSPENDED' WHERE id=${hospitalId}; SELECT 'SUSPEND_READY';\n`)
let hospitalPatch
try {
  await waitFor(() => suspendOutput.includes('SUSPEND_READY'))
  hospitalPatch = api('PATCH', `/api/v1/admin/hospitals/${hospitalId}`, { name: 'Updated hospital', address: 'Tokyo', phoneNumber: '03-0000-0001' })
  await waitFor(() => Number(sql("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE 'update hospitals%';")) > 0)
} finally {
  suspendHolder.stdin.end('COMMIT;\n')
  await suspendClosed
}
const hospitalResponse = await hospitalPatch
const hospitalState = sql(`SELECT status FROM hospitals WHERE id=${hospitalId};`)
console.log('R-03 existing hospital info PATCH undoes suspension:', hospitalResponse.status, hospitalState)
assert.equal(hospitalResponse.status, 200)
assert.equal(hospitalState, 'ACTIVE')
