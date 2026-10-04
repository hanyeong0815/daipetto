// Seeds a throwaway database for manual/browser checks of the frontend integration.
// Needs the backend on BASE (default :18081) against DB (default daipetto_fe_1004); see README.md.
// Fixture credentials exist only in that throwaway database.
import { execFileSync } from 'node:child_process'
import { writeFileSync } from 'node:fs'

const BASE = process.env.BASE ?? 'http://localhost:18081'
const DB = process.env.DB ?? 'daipetto_fe_1004'
const OUT = process.env.OUT // optional: file that receives the user's session for localStorage injection
const PASSWORD = 'Password123!'
const sql = (q) => execFileSync('docker', ['exec', '-i', 'daipetto-postgres', 'psql', '-U', 'daipetto_user', '-d', DB, '-t', '-A', '-v', 'ON_ERROR_STOP=1'], { input: q, encoding: 'utf8' }).trim()

async function api(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  const json = await res.json().catch(() => null)
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status} ${json?.code}`)
  return json?.data
}
const login = (email) => api('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })

// Asia/Tokyo calendar date offset by n days
const jstDate = (n) => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Tokyo' }).format(new Date(Date.now() + n * 86400000))

await api('POST', '/api/v1/users', { body: { email: 'fe-user@daipetto.test', password: PASSWORD, nickname: 'たろう' } })
await api('POST', '/api/v1/users', { body: { email: 'fe-admin@daipetto.test', password: PASSWORD, nickname: 'admin' } })
sql("UPDATE users SET role='ROLE_SYSTEM_ADMIN' WHERE email='fe-admin@daipetto.test';")
const admin = (await login('fe-admin@daipetto.test')).accessToken
const session = await login('fe-user@daipetto.test')
const user = session.accessToken

const { petId } = await api('POST', '/api/v1/pets', { token: user, body: { name: 'モモ', petType: 'CAT', birthDate: '2023-01-01', gender: 'FEMALE', weight: 4.5 } })
const { hospitalId } = await api('POST', '/api/v1/admin/hospitals', { token: admin, body: { name: 'さくら動物病院', address: '東京都渋谷区1-2-3', phoneNumber: '03-1234-5678' } })

const slots = []
for (const day of [1, 2, 3]) {
  for (const [start, end] of [['09:00', '09:30'], ['09:30', '10:00'], ['10:00', '10:30']]) {
    const { scheduleId } = await api('POST', `/api/v1/admin/hospitals/${hospitalId}/schedules`, { token: admin, body: { availableDate: jstDate(day), startTime: start, endTime: end } })
    slots.push(scheduleId)
  }
}
await api('PATCH', `/api/v1/admin/hospitals/${hospitalId}/schedules/${slots[2]}/block`, { token: admin })

// History: one approved, one rejected, one completed -> three notifications
const reserve = (scheduleId, memo) => api('POST', '/api/v1/reservations', { token: user, body: { petId, hospitalId, scheduleId, memo } })
const approved = await reserve(slots[3], '診療目的: 健康診断')
await api('PATCH', `/api/v1/admin/reservations/${approved.reservationId}/approve`, { token: admin })
const rejected = await reserve(slots[4])
await api('PATCH', `/api/v1/admin/reservations/${rejected.reservationId}/reject`, { token: admin })
const completed = await reserve(slots[6])
await api('PATCH', `/api/v1/admin/reservations/${completed.reservationId}/approve`, { token: admin })
await api('PATCH', `/api/v1/admin/reservations/${completed.reservationId}/complete`, { token: admin })

for (const [days, body] of [[-30, { weight: 4.3 }], [-14, { weight: 4.5, symptom: '食欲低下' }], [-2, { weight: 4.6, symptom: '軽い咳', memo: '2日続いている' }]]) {
  await api('POST', `/api/v1/pets/${petId}/health-records`, { token: user, body: { ...body, recordedDate: jstDate(days) } })
}

// REVIEW-001 F-03: today's slots on hospital 1, one already started (00:00) and one later today (23:30;
// run before 23:30 JST). Inserted by SQL because the API would reject booking a past slot. Plus an APPROVED
// reservation that started earlier today, which the dashboard must not show as "next".
const today = jstDate(0)
const [pastSlot, futureSlot, startedSlot] = sql(`INSERT INTO hospital_schedules (hospital_id, available_date, start_time, end_time) VALUES
  (${hospitalId}, '${today}', '00:00', '00:30'), (${hospitalId}, '${today}', '23:30', '23:59'), (${hospitalId}, '${today}', '00:30', '01:00') RETURNING id;`).split('\n').map(Number)
const userId = Number(sql("SELECT id FROM users WHERE email='fe-user@daipetto.test';"))
sql(`INSERT INTO reservations (user_id, pet_id, hospital_id, schedule_id, reservation_datetime, status) VALUES (${userId}, ${petId}, ${hospitalId}, ${startedSlot}, '${today} 00:30', 'APPROVED');`)

// REVIEW-001 F-02: a second hospital to switch to
const { hospitalId: hospitalB } = await api('POST', '/api/v1/admin/hospitals', { token: admin, body: { name: 'みどり動物クリニック', address: '東京都新宿区4-5-6' } })
await api('POST', `/api/v1/admin/hospitals/${hospitalB}/schedules`, { token: admin, body: { availableDate: jstDate(4), startTime: '14:00', endTime: '14:30' } })

// REVIEW-001 F-01: a second account with its own pet
await api('POST', '/api/v1/users', { body: { email: 'fe-user2@daipetto.test', password: PASSWORD, nickname: 'はなこ' } })
const user2 = (await login('fe-user2@daipetto.test')).accessToken
await api('POST', '/api/v1/pets', { token: user2, body: { name: 'クロ', petType: 'DOG', birthDate: '2021-05-05', gender: 'MALE', weight: 9.2 } })

if (OUT) writeFileSync(OUT, JSON.stringify({ state: { refreshToken: session.refreshToken, user: await api('GET', '/api/v1/users/me', { token: user }) }, version: 0 }))
console.log(JSON.stringify({ petId, hospitalId, hospitalB, slots, today: { pastSlot, futureSlot, startedSlot }, reservations: [approved.reservationId, rejected.reservationId, completed.reservationId] }))
