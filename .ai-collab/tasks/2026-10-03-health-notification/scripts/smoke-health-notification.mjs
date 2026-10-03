// Smoke check for the HealthRecord and Notification APIs against a running isolated backend.
// Needs the backend on :18080 with --DB_NAME=daipetto_hn (see README.md). Fixture credentials
// exist only in that throwaway database.
import { execSync } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18080'
const DB = process.env.DB ?? 'daipetto_hn'
const PASSWORD = 'Password123!'
const USER = 'hn-user@daipetto.test'
const ADMIN = 'hn-admin@daipetto.test'

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
  const text = await res.text()
  let json = null
  try { json = text ? JSON.parse(text) : null } catch { json = null }
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null }
}

const sql = (q) => execSync(`docker exec daipetto-postgres psql -U daipetto_user -d ${DB} -t -A -c "${q}"`, { encoding: 'utf8' }).trim()
const login = async (email) => (await api('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })).data

// ------------------------------------------------------------------ setup
await api('POST', '/api/v1/users', { body: { email: USER, password: PASSWORD, nickname: 'hnuser' } })
await api('POST', '/api/v1/users', { body: { email: ADMIN, password: PASSWORD, nickname: 'hnadmin' } })
sql(`UPDATE users SET role='ROLE_SYSTEM_ADMIN' WHERE email='${ADMIN}'`)
const user = await login(USER)
const admin = await login(ADMIN)
const otherUser = await (async () => {
  await api('POST', '/api/v1/users', { body: { email: 'hn-other@daipetto.test', password: PASSWORD, nickname: 'hnother' } })
  return login('hn-other@daipetto.test')
})()

const pet = await api('POST', '/api/v1/pets', {
  token: user.accessToken,
  body: { name: 'Momo', petType: 'CAT', birthDate: '2023-01-01', gender: 'FEMALE', weight: 4.5 },
})
const petId = pet.data.petId

// -------------------------------------------------------------- HealthRecord
{
  const created = await api('POST', `/api/v1/pets/${petId}/health-records`, {
    token: user.accessToken, body: { weight: 4.7, symptom: '咳, 食欲低下', memo: '少し元気がない', recordedDate: '2026-05-20' },
  })
  const recordId = created.data?.healthRecordId
  check('HEALTH-T001 health record created', created.status === 201 && Number.isInteger(recordId), `${created.status} id=${recordId}`)

  // Expected date is the server's local date (Asia/Tokyo, ServerTime.ZONE_ID), not UTC; the request
  // may straddle midnight, so either side of it is accepted (REVIEW-002 T-01).
  const serverDate = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Tokyo' }).format(new Date())
  const dateBefore = serverDate()
  const defaulted = await api('POST', `/api/v1/pets/${petId}/health-records`, {
    token: user.accessToken, body: { weight: 4.8 },
  })
  const dateAfter = serverDate()
  const storedDate = sql(`SELECT recorded_date FROM health_records WHERE id=${defaulted.data.healthRecordId}`)
  check('recordedDate defaults to today when omitted', defaulted.status === 201 && [dateBefore, dateAfter].includes(storedDate),
    `stored=${storedDate} expected=${dateBefore}${dateAfter !== dateBefore ? `|${dateAfter}` : ''} (Asia/Tokyo)`)

  const otherUsersAttempt = await api('POST', `/api/v1/pets/${petId}/health-records`, {
    token: otherUser.accessToken, body: { weight: 1.0 },
  })
  check('HEALTH-T002 other user cannot add a record', otherUsersAttempt.status === 403 && otherUsersAttempt.code === 'HEALTH-002',
    `${otherUsersAttempt.status} ${otherUsersAttempt.code}`)

  const negative = await api('POST', `/api/v1/pets/${petId}/health-records`, { token: user.accessToken, body: { weight: -1 } })
  check('HEALTH-T003 negative weight rejected', negative.status === 400 && negative.code === 'VALIDATION-001', `${negative.status} ${negative.code}`)

  const list = await api('GET', `/api/v1/pets/${petId}/health-records`, { token: user.accessToken })
  check('HEALTH-T004 list returns records newest first',
    list.status === 200 && list.data.length === 2 && list.data[0].recordedDate >= list.data[1].recordedDate,
    `items=${list.data?.length} dates=${list.data?.map((r) => r.recordedDate).join(',')}`)

  const updated = await api('PATCH', `/api/v1/health-records/${recordId}`, {
    token: user.accessToken, body: { weight: 5.1, symptom: '食欲回復', memo: '元気になった' },
  })
  const afterUpdate = sql(`SELECT weight || '/' || symptom || '/' || recorded_date FROM health_records WHERE id=${recordId}`)
  check('update keeps recordedDate and applies new values', updated.status === 200 && afterUpdate === '5.10/食欲回復/2026-05-20', `${updated.status} stored=${afterUpdate}`)

  const deleted = await api('DELETE', `/api/v1/health-records/${recordId}`, { token: user.accessToken })
  const deletedAt = sql(`SELECT coalesce(deleted_at::text,'null') FROM health_records WHERE id=${recordId}`)
  const listAfterDelete = await api('GET', `/api/v1/pets/${petId}/health-records`, { token: user.accessToken })
  check('HEALTH-T005 delete sets deleted_at and hides the record',
    deleted.status === 200 && deletedAt !== 'null' && listAfterDelete.data.length === 1, `deleted_at=${deletedAt} remaining=${listAfterDelete.data?.length}`)
}

// -------------------------------------------------------------- Notification
{
  const hospital = await api('POST', '/api/v1/admin/hospitals', {
    token: admin.accessToken, body: { name: 'HN Hospital', address: 'Tokyo', phoneNumber: '03-0000-0000' },
  })
  const hospitalId = hospital.data.hospitalId
  const schedule = async (hour) => {
    const date = new Date(Date.now() + 2 * 86400000).toISOString().slice(0, 10)
    const r = await api('POST', `/api/v1/admin/hospitals/${hospitalId}/schedules`, {
      token: admin.accessToken, body: { availableDate: date, startTime: `${String(hour).padStart(2, '0')}:00`, endTime: `${String(hour).padStart(2, '0')}:30` },
    })
    return r.data.scheduleId
  }
  const reserve = async (scheduleId) => (await api('POST', '/api/v1/reservations', {
    token: user.accessToken, body: { petId, hospitalId, scheduleId, memo: 'smoke' },
  })).data.reservationId

  const approved = await reserve(await schedule(9))
  await api('PATCH', `/api/v1/admin/reservations/${approved}/approve`, { token: admin.accessToken })
  const rejected = await reserve(await schedule(10))
  await api('PATCH', `/api/v1/admin/reservations/${rejected}/reject`, { token: admin.accessToken })
  const completedTarget = await reserve(await schedule(11))
  await api('PATCH', `/api/v1/admin/reservations/${completedTarget}/approve`, { token: admin.accessToken })
  await api('PATCH', `/api/v1/admin/reservations/${completedTarget}/complete`, { token: admin.accessToken })

  const list = await api('GET', '/api/v1/notifications', { token: user.accessToken })
  const types = (list.data ?? []).map((n) => n.type)
  check('NOTI-T001〜T003 reservation events create notifications',
    list.status === 200
    && types.filter((t) => t === 'RESERVATION_APPROVED').length === 2
    && types.includes('RESERVATION_REJECTED')
    && types.includes('TREATMENT_COMPLETED'),
    `types=${types.join(',')}`)
  check('NOTI-T004 list is scoped to the user and unread by default',
    list.data.every((n) => n.isRead === false) && Number(sql(`SELECT count(*) FROM notifications WHERE user_id<>${sql(`SELECT id FROM users WHERE email='${USER}'`)}`)) === 0,
    `items=${list.data.length}`)

  const target = list.data[0].notificationId
  const read = await api('PATCH', `/api/v1/notifications/${target}/read`, { token: user.accessToken })
  const stored = sql(`SELECT is_read::text || '/' || (read_at IS NOT NULL)::text FROM notifications WHERE id=${target}`)
  check('NOTI-T005 read marking sets is_read and read_at', read.status === 200 && stored === 'true/true', `${read.status} is_read/read_at_set=${stored}`)

  const again = await api('PATCH', `/api/v1/notifications/${target}/read`, { token: user.accessToken })
  check('re-reading an already read notification stays successful (idempotent)', again.status === 200, `${again.status}`)

  const foreign = await api('PATCH', `/api/v1/notifications/${target}/read`, { token: otherUser.accessToken })
  check('other user cannot read someone else\'s notification', foreign.status === 403 && foreign.code === 'NOTIFICATION-002', `${foreign.status} ${foreign.code}`)

  const missing = await api('PATCH', '/api/v1/notifications/999999/read', { token: user.accessToken })
  check('unknown notification returns NOTIFICATION-001', missing.status === 404 && missing.code === 'NOTIFICATION-001', `${missing.status} ${missing.code}`)

  const cancelTarget = await reserve(await schedule(12))
  const before = (await api('GET', '/api/v1/notifications', { token: user.accessToken })).data.length
  await api('PATCH', `/api/v1/reservations/${cancelTarget}/cancel`, { token: user.accessToken })
  const after = (await api('GET', '/api/v1/notifications', { token: user.accessToken })).data.length
  check('cancel creates no notification (not defined in 08_State_Design §6-6)', before === after, `before=${before} after=${after}`)
}

const failed = results.filter((r) => !r.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
