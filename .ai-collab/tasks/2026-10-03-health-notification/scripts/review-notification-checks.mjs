// Reviewer-owned integration checks. Requires the original smoke fixtures, scratch DB only.
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const DB = process.env.DB, BASE = process.env.BASE
assert.equal(DB, 'daipetto_review_1003')
assert.equal(BASE, 'http://localhost:18081')
const sql = query => execFileSync('docker', ['exec', '-i', 'daipetto-postgres', 'psql', '-U', 'daipetto_user', '-d', DB, '-t', '-A', '-v', 'ON_ERROR_STOP=1'], { input: query, encoding: 'utf8' }).trim()
async function api(method, path, token, body) {
  const res = await fetch(BASE + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined })
  return { status: res.status, ...await res.json() }
}
const login = async email => (await api('POST', '/api/v1/auth/login', null, { email, password: 'Password123!' })).data.accessToken
const user = await login('hn-user@daipetto.test')
const admin = await login('hn-admin@daipetto.test')
const other = await login('hn-other@daipetto.test')
const userId = Number(sql("SELECT id FROM users WHERE email='hn-user@daipetto.test';"))
const petId = Number(sql(`SELECT id FROM pets WHERE user_id=${userId};`))
assert.equal((await api('GET', '/api/v1/notifications')).status, 401)
assert.equal((await api('GET', `/api/v1/pets/${petId}/health-records`)).status, 401)
assert.deepEqual((await api('GET', '/api/v1/notifications', other)).data, [])
const recordId = Number(sql(`SELECT id FROM health_records WHERE pet_id=${petId} AND deleted_at IS NULL ORDER BY id LIMIT 1;`))
for (const [method, path, body] of [
  ['GET', `/api/v1/pets/${petId}/health-records`],
  ['PATCH', `/api/v1/health-records/${recordId}`, { memo: 'foreign' }],
  ['DELETE', `/api/v1/health-records/${recordId}`],
]) {
  const result = await api(method, path, other, body)
  assert.equal(result.status, 403)
  assert.equal(result.code, 'HEALTH-002')
}
console.log('PASS anonymous requests and cross-user list/update/delete boundaries')
const notificationId = (await api('GET', '/api/v1/notifications', user)).data[0].notificationId
assert.equal((await api('PATCH', `/api/v1/notifications/${notificationId}/read`, user)).status, 200)
const firstRead = sql(`SELECT read_at FROM notifications WHERE id=${notificationId};`)
await Promise.all(Array.from({ length: 8 }, () => api('PATCH', `/api/v1/notifications/${notificationId}/read`, user)))
assert.equal(sql(`SELECT read_at FROM notifications WHERE id=${notificationId};`), firstRead)
console.log('PASS eight repeated read requests preserve original read_at')

const hospitalId = (await api('POST', '/api/v1/admin/hospitals', admin, { name: 'Atomic hospital', address: 'Tokyo', phoneNumber: '03-0000-0000' })).data.hospitalId
const availableDate = new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10)
const scheduleId = (await api('POST', `/api/v1/admin/hospitals/${hospitalId}/schedules`, admin, { availableDate, startTime: '09:00', endTime: '09:30' })).data.scheduleId
const reservationId = (await api('POST', '/api/v1/reservations', user, { petId, hospitalId, scheduleId })).data.reservationId
// Inject a database failure for this reservation's notification only, then remove it.
sql(`ALTER TABLE notifications ADD CONSTRAINT review_reject_notification CHECK (reservation_id <> ${reservationId}) NOT VALID;`)
try {
  const result = await api('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, admin)
  assert.equal(result.status, 500)
  assert.equal(sql(`SELECT status FROM reservations WHERE id=${reservationId};`), 'REQUESTED')
  assert.equal(sql(`SELECT count(*) FROM notifications WHERE reservation_id=${reservationId};`), '0')
  console.log('PASS failed notification INSERT rolls reservation status back to REQUESTED')
} finally {
  sql('ALTER TABLE notifications DROP CONSTRAINT review_reject_notification;')
}
const race = await Promise.all([
  api('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, admin),
  api('PATCH', `/api/v1/admin/reservations/${reservationId}/reject`, admin),
])
assert.deepEqual(race.map(r => r.status).sort(), [200, 409])
assert.equal(sql(`SELECT count(*) FROM notifications WHERE reservation_id=${reservationId};`), '1')
const status = sql(`SELECT status FROM reservations WHERE id=${reservationId};`)
assert.equal(sql(`SELECT notification_type FROM notifications WHERE reservation_id=${reservationId};`), `RESERVATION_${status}`)
console.log('PASS approve/reject race creates exactly one matching notification')
