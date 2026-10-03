// End-to-end checks for REVIEW-001 fixes through the real Spring stack + PostgreSQL.
// Target: isolated backend on :18080 backed by the throwaway database daipetto_e2e.
// Credentials below are fixtures that exist only in that throwaway database.
import { execSync } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18080'
const DB = process.env.DB ?? 'daipetto_e2e'
const PASSWORD = 'Password123!'
const USER = 'e2e-user@daipetto.test'
const ADMIN = 'e2e-admin@daipetto.test'

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
  try { json = text ? JSON.parse(text) : null } catch { json = { raw: text } }
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null }
}

function sql(query) {
  return execSync(`docker exec daipetto-postgres psql -U daipetto_user -d ${DB} -t -A -c "${query}"`, { encoding: 'utf8' }).trim()
}

const jwtPayload = (t) => JSON.parse(Buffer.from(t.split('.')[1], 'base64url').toString())
const tally = (rs) => rs.reduce((m, r) => ((m[`${r.status}${r.code ? ' ' + r.code : ''}`] = (m[`${r.status}${r.code ? ' ' + r.code : ''}`] ?? 0) + 1), m), {})

async function login(email) {
  const r = await api('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })
  if (r.status !== 200) throw new Error(`login ${email} -> ${r.status} ${r.code}`)
  return r.data
}

// ------------------------------------------------------------------ setup
await api('POST', '/api/v1/users', { body: { email: USER, password: PASSWORD, nickname: 'e2euser' } })
await api('POST', '/api/v1/users', { body: { email: ADMIN, password: PASSWORD, nickname: 'e2eadmin' } })
sql(`UPDATE users SET role='ROLE_SYSTEM_ADMIN' WHERE email='${ADMIN}'`)
const admin = await login(ADMIN)
const userId = Number(sql(`SELECT id FROM users WHERE email='${USER}'`))

// ------------------------------------------------------------------ R-02
{
  let a, b
  for (let i = 0; i < 20; i++) {
    a = await login(USER)
    b = await login(USER)
    if (jwtPayload(a.refreshToken).iat === jwtPayload(b.refreshToken).iat) break
  }
  const sameSecond = jwtPayload(a.refreshToken).iat === jwtPayload(b.refreshToken).iat
  const persisted = Number(sql(`SELECT count(*) FROM refresh_tokens WHERE token IN ('${a.refreshToken}','${b.refreshToken}')`))
  check('R-02 back-to-back logins in the same second both succeed (no UNIQUE 500)',
    sameSecond && a.refreshToken !== b.refreshToken && persisted === 2,
    `iat=${jwtPayload(a.refreshToken).iat} sameSecond=${sameSecond} distinct=${a.refreshToken !== b.refreshToken} persistedRows=${persisted}`)
}

// ------------------------------------------------------------------ R-01
{
  const session = await login(USER)
  const racers = await Promise.all(
    Array.from({ length: 8 }, () => api('POST', '/api/v1/auth/refresh', { body: { refreshToken: session.refreshToken } })),
  )
  const ok = racers.filter((r) => r.status === 200).length
  const rejected = racers.filter((r) => r.status === 401 && r.code === 'AUTH-003').length
  const active = Number(sql(`SELECT count(*) FROM refresh_tokens WHERE user_id=${userId} AND revoked=false`))
  check('R-01 8 concurrent refreshes of one token: exactly one succeeds, the rest AUTH-003',
    ok === 1 && rejected === 7, JSON.stringify(tally(racers)))
  check('R-01 exactly one active refresh token remains for the user', active === 1, `active=${active}`)

  const replay = await api('POST', '/api/v1/auth/refresh', { body: { refreshToken: session.refreshToken } })
  check('R-01 replaying the consumed token is rejected', replay.status === 401 && replay.code === 'AUTH-003', `${replay.status} ${replay.code}`)

  // ---------------------------------------------------------------- R-07
  const winner = racers.find((r) => r.status === 200).data
  const asBearer = await api('GET', '/api/v1/users/me', { token: winner.refreshToken })
  check('R-07 refresh token used as Bearer -> 401 AUTH-001 (no filter exception / 500)',
    asBearer.status === 401 && asBearer.code === 'AUTH-001', `${asBearer.status} ${asBearer.code}`)
  const asAccess = await api('GET', '/api/v1/users/me', { token: winner.accessToken })
  check('R-07 access token still authenticates', asAccess.status === 200, `${asAccess.status}`)
}

// ------------------------------------------------------------------ fixtures for reservations
const user = await login(USER)
const hospital = await api('POST', '/api/v1/admin/hospitals', {
  token: admin.accessToken, body: { name: 'E2E Hospital', address: 'Tokyo', phoneNumber: '03-0000-0000' },
})
const hospitalId = hospital.data.hospitalId
let slot = 0
async function schedule() {
  // 12 slots per day (08:00-19:30), spread over consecutive future days
  const n = slot++
  const date = new Date(Date.now() + (2 + Math.floor(n / 12)) * 86400000).toISOString().slice(0, 10)
  const hh = String(8 + (n % 12)).padStart(2, '0')
  const r = await api('POST', `/api/v1/admin/hospitals/${hospitalId}/schedules`, {
    token: admin.accessToken, body: { availableDate: date, startTime: `${hh}:00`, endTime: `${hh}:30` },
  })
  return r.data.scheduleId
}
const pet = await api('POST', '/api/v1/pets', {
  token: user.accessToken, body: { name: 'Momo', petType: 'CAT', birthDate: '2023-01-01', gender: 'FEMALE', weight: 4.5 },
})
const petId = pet.data.petId
const reserve = (scheduleId) => api('POST', '/api/v1/reservations', {
  token: user.accessToken, body: { petId, hospitalId, scheduleId, memo: 'e2e' },
})
let created = 0

// ------------------------------------------------------------------ R-03
{
  const scheduleId = await schedule()
  const racers = await Promise.all(Array.from({ length: 8 }, () => reserve(scheduleId)))
  const ok = racers.filter((r) => r.status === 201).length
  const dup = racers.filter((r) => r.status === 409 && r.code === 'RESERVATION-001').length
  const serverErrors = racers.filter((r) => r.status >= 500).length
  const active = Number(sql(`SELECT count(*) FROM reservations WHERE schedule_id=${scheduleId} AND status IN ('REQUESTED','APPROVED') AND deleted_at IS NULL`))
  created += ok
  check('R-03 8 concurrent bookings of one schedule: one 201, the rest RESERVATION-001, no 5xx',
    ok === 1 && dup === 7 && serverErrors === 0, JSON.stringify(tally(racers)))
  check('R-03 exactly one active reservation exists for the schedule', active === 1, `active=${active}`)
}

// ------------------------------------------------------------------ R-04
async function race(label, fromApproved, actions) {
  let consistent = 0
  const rounds = 10
  for (let i = 0; i < rounds; i++) {
    const r = await reserve(await schedule())
    created++
    const id = r.data.reservationId
    if (fromApproved) await api('PATCH', `/api/v1/admin/reservations/${id}/approve`, { token: admin.accessToken })
    const outcomes = await Promise.all(actions.map((a) => a.call(id)))
    const winners = outcomes.map((o, idx) => (o.status === 200 ? actions[idx].result : null)).filter(Boolean)
    const losers = outcomes.filter((o) => o.status === 409 && o.code === 'RESERVATION-008').length
    const finalStatus = sql(`SELECT status FROM reservations WHERE id=${id}`)
    if (winners.length === 1 && losers === 1 && finalStatus === winners[0]) consistent++
  }
  check(`R-04 ${label}: every round has exactly one winner and the stored status matches it`,
    consistent === rounds, `${consistent}/${rounds} rounds consistent`)
}
await race('approve vs reject (REQUESTED)', false, [
  { call: (id) => api('PATCH', `/api/v1/admin/reservations/${id}/approve`, { token: admin.accessToken }), result: 'APPROVED' },
  { call: (id) => api('PATCH', `/api/v1/admin/reservations/${id}/reject`, { token: admin.accessToken }), result: 'REJECTED' },
])
await race('complete vs cancel (APPROVED)', true, [
  { call: (id) => api('PATCH', `/api/v1/admin/reservations/${id}/complete`, { token: admin.accessToken }), result: 'COMPLETED' },
  { call: (id) => api('PATCH', `/api/v1/reservations/${id}/cancel`, { token: user.accessToken }), result: 'CANCELLED' },
])

// ------------------------------------------------------------------ R-05
{
  const scheduleId = await schedule()
  const suspend = await api('PATCH', `/api/v1/admin/hospitals/${hospitalId}/suspend`, { token: admin.accessToken })
  const r = await reserve(scheduleId)
  check('R-05 booking a SUSPENDED hospital -> 409 RESERVATION-009',
    suspend.status === 200 && r.status === 409 && r.code === 'RESERVATION-009', `suspend=${suspend.status} reserve=${r.status} ${r.code}`)
}

// ------------------------------------------------------------------ R-06
{
  const del = await api('DELETE', `/api/v1/pets/${petId}`, { token: user.accessToken })
  const list = await api('GET', '/api/v1/reservations', { token: user.accessToken })
  const rendered = Array.isArray(list.data) ? list.data : []
  check('R-06 after deleting the pet, the reservation list still renders every reservation',
    del.status === 200 && list.status === 200 && rendered.length === created && rendered.every((x) => x.petName === 'Momo'),
    `delete=${del.status} list=${list.status} ${list.code ?? ''} items=${rendered.length}/${created}`)
}

const failed = results.filter((r) => !r.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
