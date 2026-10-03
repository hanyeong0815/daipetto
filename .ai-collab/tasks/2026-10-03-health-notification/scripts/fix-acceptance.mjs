// Acceptance checks for the REVIEW-001 fixes, using the reviewer's deterministic interleaving:
// a psql session holds the conflicting mutation uncommitted, the real HTTP request reads the
// previous row version and blocks on its own UPDATE (for health records, since REVIEW-002 R-04, on
// its locking SELECT), confirmed via pg_stat_activity; then the holder commits and the final state
// is asserted. Asserts the FIXED behaviour.
// Run after smoke-health-notification.mjs against the same isolated backend (fixtures reused).
import { execFileSync, spawn } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18080'
const DB = process.env.DB ?? 'daipetto_hn'
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
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let json = null
  try { json = text ? JSON.parse(text) : null } catch { json = null }
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null }
}

const login = async (email) => (await api('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })).data.accessToken

async function waitFor(predicate, label) {
  for (let i = 0; i < 100; i++) {
    if (predicate()) return
    await new Promise((r) => setTimeout(r, 100))
  }
  throw new Error(`timed out waiting for ${label}`)
}

// Holds `mutation` uncommitted, fires `request` once it is blocked on `update <table>` or a locking
// `select ... from <table> ... for ...`, then commits.
async function interleave(mutation, table, request) {
  const holder = spawn('docker', psqlArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
  let output = ''
  holder.stdout.on('data', (chunk) => { output += chunk })
  const closed = new Promise((resolve) => holder.on('close', resolve))
  holder.stdin.write(`BEGIN; ${mutation}; SELECT 'HOLDING';\n`)
  let pending
  try {
    await waitFor(() => output.includes('HOLDING'), 'holder')
    pending = request()
    await waitFor(() => Number(sql(
      `SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND (query LIKE 'update ${table}%' OR query LIKE 'select % from ${table} % for %');`,
    )) > 0, `request blocked on ${table}`)
  } finally {
    holder.stdin.end('COMMIT;\n')
    await closed
  }
  return pending
}

const user = await login('hn-user@daipetto.test')
const admin = await login('hn-admin@daipetto.test')
const petId = Number(sql("SELECT p.id FROM pets p JOIN users u ON p.user_id=u.id WHERE u.email='hn-user@daipetto.test' AND p.deleted_at IS NULL ORDER BY p.id LIMIT 1;"))

const createRecord = async () => (await api('POST', `/api/v1/pets/${petId}/health-records`, {
  token: user, body: { weight: 4.7, symptom: 'cough', memo: 'original', recordedDate: '2026-05-20' },
})).data.healthRecordId
const recordState = (id) => sql(`SELECT coalesce(weight::text,'NULL') || '/' || coalesce(symptom,'NULL') || '/' || coalesce(memo,'NULL') || '/' || recorded_date FROM health_records WHERE id=${id};`)

// --------------------------------------------------------------- R-01
{
  const id = await createRecord()
  const r = await api('PATCH', `/api/v1/health-records/${id}`, { token: user, body: { memo: 'memo-only' } })
  check('R-01 memo-only PATCH keeps weight, symptom and recordedDate', r.status === 200 && recordState(id) === '4.70/cough/memo-only/2026-05-20', `${r.status} ${recordState(id)}`)

  const empty = await api('PATCH', `/api/v1/health-records/${id}`, { token: user, body: {} })
  check('R-01 empty PATCH changes nothing', empty.status === 200 && recordState(id) === '4.70/cough/memo-only/2026-05-20', `${empty.status} ${recordState(id)}`)

  const weightOnly = await api('PATCH', `/api/v1/health-records/${id}`, { token: user, body: { weight: 5.3 } })
  check('R-01 weight-only PATCH keeps text fields', weightOnly.status === 200 && recordState(id) === '5.30/cough/memo-only/2026-05-20', `${weightOnly.status} ${recordState(id)}`)

  const clear = await api('PATCH', `/api/v1/health-records/${id}`, { token: user, body: { symptom: '' } })
  check('R-01 blank symptom clears it to NULL, other fields kept', clear.status === 200 && recordState(id) === '5.30/NULL/memo-only/2026-05-20', `${clear.status} ${recordState(id)}`)
}

// --------------------------------------------------------------- R-02
{
  const id = await createRecord()
  const response = await interleave(
    `UPDATE health_records SET deleted_at=CURRENT_TIMESTAMP WHERE id=${id}`,
    'health_records',
    () => api('PATCH', `/api/v1/health-records/${id}`, { token: user, body: { weight: 5.2, symptom: 'better', memo: 'racing' } }),
  )
  const deleted = sql(`SELECT (deleted_at IS NOT NULL)::text || '/' || memo FROM health_records WHERE id=${id};`)
  const listed = (await api('GET', `/api/v1/pets/${petId}/health-records`, { token: user })).data.some((x) => x.healthRecordId === id)
  check('R-02 PATCH blocked behind a committed soft delete returns HEALTH-001 and leaves the record deleted',
    response.status === 404 && response.code === 'HEALTH-001' && deleted === 'true/original' && !listed,
    `${response.status} ${response.code} deleted/memo=${deleted} listed=${listed}`)
}

// --------------------------------------------------------------- R-03 and its mirror
const createHospital = async (name) => (await api('POST', '/api/v1/admin/hospitals', {
  token: admin, body: { name, address: 'Tokyo', phoneNumber: '03-0000-0000' },
})).data.hospitalId

{
  const hospitalId = await createHospital('Race hospital')
  const response = await interleave(
    `UPDATE hospitals SET status='SUSPENDED' WHERE id=${hospitalId}`,
    'hospitals',
    () => api('PATCH', `/api/v1/admin/hospitals/${hospitalId}`, { token: admin, body: { name: 'Renamed hospital', address: 'Tokyo, Shinjuku', phoneNumber: '03-1111-1111' } }),
  )
  const state = sql(`SELECT status || '/' || name FROM hospitals WHERE id=${hospitalId};`)
  check('R-03 info PATCH blocked behind a committed suspension keeps the hospital SUSPENDED',
    response.status === 200 && state === 'SUSPENDED/Renamed hospital', `${response.status} ${state}`)

  const date = new Date(Date.now() + 2 * 86400000).toISOString().slice(0, 10)
  const scheduleId = Number(sql(`INSERT INTO hospital_schedules (hospital_id, available_date, start_time, end_time) VALUES (${hospitalId}, '${date}', '09:00', '09:30') RETURNING id;`).split('\n')[0])
  const booking = await api('POST', '/api/v1/reservations', { token: user, body: { petId, hospitalId, scheduleId } })
  check('R-03 impact: the hospital still rejects new reservations (RESERVATION-009)',
    booking.status === 409 && booking.code === 'RESERVATION-009', `${booking.status} ${booking.code}`)
}

{
  const hospitalId = await createHospital('Mirror hospital')
  const response = await interleave(
    `UPDATE hospitals SET name='Held name' WHERE id=${hospitalId}`,
    'hospitals',
    () => api('PATCH', `/api/v1/admin/hospitals/${hospitalId}/suspend`, { token: admin }),
  )
  const state = sql(`SELECT status || '/' || name FROM hospitals WHERE id=${hospitalId};`)
  check('R-03 mirror: suspension blocked behind a committed info update keeps the new name',
    response.status === 200 && state === 'SUSPENDED/Held name', `${response.status} ${state}`)
}

// --------------------------------------------------------------- equivalent patterns
{
  const pet = await api('POST', '/api/v1/pets', {
    token: user, body: { name: 'Kuma', petType: 'DOG', birthDate: '2022-01-01', gender: 'MALE', weight: 8.0 },
  })
  const racedPetId = pet.data.petId
  const response = await interleave(
    `UPDATE pets SET deleted_at=CURRENT_TIMESTAMP WHERE id=${racedPetId}`,
    'pets',
    () => api('PATCH', `/api/v1/pets/${racedPetId}`, { token: user, body: { name: 'KumaRenamed', petType: 'DOG', gender: 'MALE' } }),
  )
  const state = sql(`SELECT (deleted_at IS NOT NULL)::text || '/' || name FROM pets WHERE id=${racedPetId};`)
  check('Pet PATCH blocked behind a committed soft delete returns PET-001 and leaves the pet deleted',
    response.status === 404 && response.code === 'PET-001' && state === 'true/Kuma', `${response.status} ${response.code} ${state}`)
}

{
  const hospitalId = await createHospital('Hours hospital')
  const created = await api('POST', `/api/v1/admin/hospitals/${hospitalId}/business-hours`, {
    token: admin, body: { dayOfWeek: 'MONDAY', openTime: '09:00', closeTime: '18:00', slotDurationMinutes: 30 },
  })
  const hoursId = created.data.businessHoursId ?? created.data.id
  const response = await interleave(
    `DELETE FROM hospital_business_hours WHERE id=${hoursId}`,
    'hospital_business_hours',
    () => api('PATCH', `/api/v1/admin/hospitals/${hospitalId}/business-hours/${hoursId}`, {
      token: admin, body: { openTime: '10:00', closeTime: '19:00', slotDurationMinutes: 20 },
    }),
  )
  const remaining = Number(sql(`SELECT count(*) FROM hospital_business_hours WHERE hospital_id=${hospitalId};`))
  check('Business hours PATCH blocked behind a committed delete returns HOSPITAL-003 and recreates no row',
    response.status === 404 && response.code === 'HOSPITAL-003' && remaining === 0, `${response.status} ${response.code} rows=${remaining}`)
}

const failed = results.filter((r) => !r.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
