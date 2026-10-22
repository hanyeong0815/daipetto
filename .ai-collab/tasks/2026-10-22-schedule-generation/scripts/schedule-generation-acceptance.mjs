// Acceptance checks for reservation-slot auto-generation from hospital_business_hours (manual API) and
// the business-hours validation, through the real Spring stack + PostgreSQL. Needs a backend on a fresh
// throwaway database (see README.md). Fixture credentials exist only in that database. The monthly cron
// itself is not triggered here (unit-tested); it calls the same per-hospital generator.
import { execFileSync, spawn } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18081'
const DB = process.env.DB ?? 'daipetto_gen_1022'
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
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const json = await res.json().catch(() => null)
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null }
}

const signUp = (email, nickname) => api('POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname } })
const login = async (email) => (await api('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })).data.accessToken
const show = (r) => `${r.status} ${r.code ?? ''}`

async function waitFor(predicate, label) {
  for (let i = 0; i < 100; i++) {
    if (predicate()) return
    await new Promise((r) => setTimeout(r, 100))
  }
  throw new Error(`timed out waiting for ${label}`)
}

// JST months relative to today, and the expected slots per month (Mon: 6 incl. 1 BLOCKED, Wed: 1)
const todayJst = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Tokyo' }).format(new Date())
const monthAt = (offset) => {
  const [y, m] = todayJst.split('-').map(Number)
  const total = y * 12 + (m - 1) + offset
  return `${Math.floor(total / 12)}-${String((total % 12) + 1).padStart(2, '0')}`
}
function daysOf(month, fromDate = `${month}-01`) {
  const [y, m] = month.split('-').map(Number)
  const days = []
  for (let d = new Date(Date.UTC(y, m - 1, 1)); d.getUTCMonth() === m - 1; d.setUTCDate(d.getUTCDate() + 1)) {
    const iso = d.toISOString().slice(0, 10)
    if (iso >= fromDate) days.push({ iso, dow: d.getUTCDay() })
  }
  return days
}
const expected = (month, fromDate) => {
  const days = daysOf(month, fromDate)
  const mondays = days.filter((d) => d.dow === 1).length
  const wednesdays = days.filter((d) => d.dow === 3).length
  return { slots: mondays * 6 + wednesdays, dates: mondays + wednesdays, mondays, days }
}
const rows = (hospitalId, month) => Number(sql(`SELECT count(*) FROM hospital_schedules WHERE hospital_id=${hospitalId} AND to_char(available_date,'YYYY-MM')='${month}';`))

await signUp('gen-admin@daipetto.test', 'Admin')
await signUp('gen-vet@daipetto.test', 'HappyVet')
await signUp('gen-other@daipetto.test', 'OtherVet')
await signUp('gen-tanaka@daipetto.test', 'Tanaka')
sql("UPDATE users SET role='ROLE_SYSTEM_ADMIN' WHERE email='gen-admin@daipetto.test'; UPDATE users SET role='ROLE_HOSPITAL_ADMIN' WHERE email IN ('gen-vet@daipetto.test','gen-other@daipetto.test');")
const admin = await login('gen-admin@daipetto.test')
const vet = await login('gen-vet@daipetto.test')
const other = await login('gen-other@daipetto.test')
const tanaka = await login('gen-tanaka@daipetto.test')
const vetId = Number(sql("SELECT id FROM users WHERE email='gen-vet@daipetto.test';"))
const h = (await api('POST', '/api/v1/admin/hospitals', { token: admin, body: { name: 'さくら動物病院', address: '東京都渋谷区' } })).data.hospitalId
await api('POST', `/api/v1/admin/hospitals/${h}/admins`, { token: admin, body: { userId: vetId } })
const hours = (body) => api('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: vet, body })
const generate = (month, token = vet, hospitalId = h) => api('POST', `/api/v1/admin/hospitals/${hospitalId}/schedules/generate`, { token, body: { month } })

// ------------------------------------------------- business-hours validation
{
  const reversed = await hours({ dayOfWeek: 'MONDAY', openTime: '18:00', closeTime: '09:00', slotDurationMinutes: 30 })
  const zero = await hours({ dayOfWeek: 'MONDAY', openTime: '09:00', closeTime: '12:00', slotDurationMinutes: 0 })
  const breakOutside = await hours({ dayOfWeek: 'MONDAY', openTime: '09:00', closeTime: '12:00', breakStartTime: '11:30', breakEndTime: '12:30', slotDurationMinutes: 30 })
  const halfBreak = await hours({ dayOfWeek: 'MONDAY', openTime: '09:00', closeTime: '12:00', breakStartTime: '10:00', slotDurationMinutes: 30 })
  const mon = await hours({ dayOfWeek: 'MONDAY', openTime: '09:00', closeTime: '12:00', breakStartTime: '10:00', breakEndTime: '10:30', slotDurationMinutes: 30 })
  const wed = await hours({ dayOfWeek: 'WEDNESDAY', openTime: '14:00', closeTime: '15:00', slotDurationMinutes: 60 })
  const badUpdate = await api('PATCH', `/api/v1/admin/hospitals/${h}/business-hours/${wed.data?.businessHoursId}`, { token: vet, body: { openTime: '15:00', closeTime: '14:00', slotDurationMinutes: 60 } })
  check('GEN-01 invalid business hours (reversed, 0 min, break outside, half break, bad update) -> 400 HOSPITAL-009; valid ones 201',
    [reversed, zero, breakOutside, halfBreak, badUpdate].every((r) => r.status === 400 && r.code === 'HOSPITAL-009') && mon.status === 201 && wed.status === 201,
    [reversed, zero, breakOutside, halfBreak, badUpdate, mon, wed].map(show).join(' / '))
}

// ------------------------------------------------------- manual generation
const m1 = monthAt(1)
{
  const exp = expected(m1)
  const r = await generate(m1)
  const blocked = Number(sql(`SELECT count(*) FROM hospital_schedules WHERE hospital_id=${h} AND to_char(available_date,'YYYY-MM')='${m1}' AND status='BLOCKED' AND start_time='10:00';`))
  const firstMonday = exp.days.find((d) => d.dow === 1).iso
  const mondaySlots = sql(`SELECT string_agg(to_char(start_time,'HH24:MI')||'-'||to_char(end_time,'HH24:MI')||' '||status, ',' ORDER BY start_time) FROM hospital_schedules WHERE hospital_id=${h} AND available_date='${firstMonday}';`)
  check(`GEN-02 generate ${m1}: Mon 6 slots (10:00 BLOCKED) + Wed 1 slot; counts match the calendar`,
    r.status === 200 && r.data?.createdSlots === exp.slots && r.data?.generatedDates === exp.dates && r.data?.skippedDates === 0
      && rows(h, m1) === exp.slots && blocked === exp.mondays
      && mondaySlots === '09:00-09:30 AVAILABLE,09:30-10:00 AVAILABLE,10:00-10:30 BLOCKED,10:30-11:00 AVAILABLE,11:00-11:30 AVAILABLE,11:30-12:00 AVAILABLE',
    `${show(r)} ${JSON.stringify(r.data)} expected ${exp.slots}/${exp.dates}; rows=${rows(h, m1)} blocked=${blocked}; ${firstMonday}: ${mondaySlots}`)

  const again = await generate(m1)
  check('GEN-03 rerun for the same month creates nothing and reports every open day as skipped',
    again.status === 200 && again.data?.createdSlots === 0 && again.data?.generatedDates === 0 && again.data?.skippedDates === exp.dates && rows(h, m1) === exp.slots,
    `${show(again)} ${JSON.stringify(again.data)} rows=${rows(h, m1)}`)
}

// ------------------------------- manual slot / BLOCKED day is not overwritten
const m2 = monthAt(2)
{
  const exp = expected(m2)
  const day = exp.days.find((d) => d.dow === 3).iso // first Wednesday
  const manual = await api('POST', `/api/v1/admin/hospitals/${h}/schedules`, { token: vet, body: { availableDate: day, startTime: '16:00', endTime: '16:30' } })
  await api('PATCH', `/api/v1/admin/hospitals/${h}/schedules/${manual.data?.scheduleId}/block`, { token: vet })
  const r = await generate(m2)
  const onDay = sql(`SELECT string_agg(to_char(start_time,'HH24:MI')||' '||status, ',') FROM hospital_schedules WHERE hospital_id=${h} AND available_date='${day}';`)
  check('GEN-04 a day that already has a (manually BLOCKED) slot is skipped and left as is',
    r.data?.createdSlots === exp.slots - 1 && r.data?.skippedDates === 1 && onDay === '16:00 BLOCKED', `${JSON.stringify(r.data)} ${day}: ${onDay}`)
}

// ------------------------------------------------------- month range rules
{
  const past = await generate(monthAt(-1))
  const tooFar = await generate(monthAt(13))
  const farthest = await generate(monthAt(12))
  const noMonth = await api('POST', `/api/v1/admin/hospitals/${h}/schedules/generate`, { token: vet, body: {} })
  const badMonth = await generate('2026/11')
  const current = await generate(monthAt(0))
  const beforeToday = Number(sql(`SELECT count(*) FROM hospital_schedules WHERE hospital_id=${h} AND to_char(available_date,'YYYY-MM')='${monthAt(0)}' AND available_date < DATE '${todayJst}';`))
  const exp0 = expected(monthAt(0), todayJst)
  check('GEN-05 past month / 13 months ahead -> 400 HOSPITAL-010; 12 months ahead OK; missing/bad month 400; current month only from today (JST)',
    [past, tooFar].every((r) => r.status === 400 && r.code === 'HOSPITAL-010') && farthest.status === 200
      && [noMonth, badMonth].every((r) => r.status === 400 && r.code === 'VALIDATION-001')
      && current.status === 200 && current.data?.createdSlots === exp0.slots && beforeToday === 0,
    [past, tooFar, farthest, noMonth, badMonth, current].map(show).join(' / ') + ` current=${JSON.stringify(current.data)} expected ${exp0.slots}, beforeToday=${beforeToday}`)
}

// ------------------------------------------------------------- authorization
{
  const unlinked = await generate(monthAt(3), other)
  const user = await generate(monthAt(3), tanaka)
  const sys = await generate(monthAt(3), admin)
  check('GEN-06 unlinked hospital admin 403 HOSPITAL-005 (nothing created), ROLE_USER 403, SYSTEM_ADMIN 200',
    unlinked.status === 403 && unlinked.code === 'HOSPITAL-005' && user.status === 403 && sys.status === 200 && sys.data?.createdSlots === expected(monthAt(3)).slots,
    [unlinked, user, sys].map(show).join(' / '))
}

// ------------------------------------------ generated slots are bookable
{
  const exp = expected(m1)
  const monday = exp.days.find((d) => d.dow === 1).iso
  const ids = Object.fromEntries(sql(`SELECT to_char(start_time,'HH24:MI')||'='||id FROM hospital_schedules WHERE hospital_id=${h} AND available_date='${monday}';`).split('\n').map((l) => l.split('=')))
  const petId = (await api('POST', '/api/v1/pets', { token: tanaka, body: { name: 'Momo', petType: 'CAT', neutered: false } })).data.petId
  const ok = await api('POST', '/api/v1/reservations', { token: tanaka, body: { petId, hospitalId: h, scheduleId: Number(ids['09:00']) } })
  const onBreak = await api('POST', '/api/v1/reservations', { token: tanaka, body: { petId, hospitalId: h, scheduleId: Number(ids['10:00']) } })
  check('GEN-07 a generated AVAILABLE slot can be booked; the generated break slot is BLOCKED (RESERVATION-003)',
    ok.status === 201 && onBreak.status === 409 && onBreak.code === 'RESERVATION-003', [ok, onBreak].map(show).join(' / '))
}

// --------------------------------------------- concurrent generation
{
  const m4 = monthAt(4)
  const all = await Promise.all(Array.from({ length: 4 }, () => generate(m4)))
  const created = all.reduce((sum, r) => sum + (r.data?.createdSlots ?? 0), 0)
  const dup = Number(sql(`SELECT count(*) FROM (SELECT available_date, start_time FROM hospital_schedules WHERE hospital_id=${h} GROUP BY 1, 2 HAVING count(*) > 1) d;`))
  check('GEN-08 4 concurrent generations of one month -> slots created once in total, no duplicate (date, start)',
    all.every((r) => r.status === 200) && created === expected(m4).slots && rows(h, m4) === expected(m4).slots && dup === 0,
    `${all.map((r) => r.data?.createdSlots).join(',')} rows=${rows(h, m4)} expected=${expected(m4).slots} dupGroups=${dup}`)
}

// ---------------------------- race: generation waits for the hospital row lock
// psql plays another generation that holds the hospital row lock and has already inserted a slot on the
// first Monday. The HTTP generation must block on the lock, then see that day as taken and skip it.
{
  const m5 = monthAt(5)
  const exp = expected(m5)
  const monday = exp.days.find((d) => d.dow === 1).iso
  const holder = spawn('docker', psqlArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
  let output = ''
  holder.stdout.on('data', (c) => { output += c })
  const closed = new Promise((resolve) => holder.on('close', resolve))
  holder.stdin.write(`BEGIN; SELECT id FROM hospitals WHERE id=${h} FOR UPDATE; INSERT INTO hospital_schedules(hospital_id, available_date, start_time, end_time, status) VALUES (${h}, '${monday}', '09:00', '09:30', 'AVAILABLE'); SELECT 'HOLDING';\n`)
  let pending
  try {
    await waitFor(() => output.includes('HOLDING'), 'holder')
    pending = generate(m5)
    await waitFor(() => Number(sql("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query ILIKE 'select%from hospitals%for%';")) > 0,
      'generation blocked on the hospital row lock')
  } finally {
    holder.stdin.end('COMMIT;\n')
    await closed
  }
  const r = await pending
  const onMonday = Number(sql(`SELECT count(*) FROM hospital_schedules WHERE hospital_id=${h} AND available_date='${monday}';`))
  check('GEN-09 race: generation waits for a concurrent one holding the hospital lock, then skips the day it filled',
    r.status === 200 && r.data?.skippedDates === 1 && r.data?.createdSlots === exp.slots - 6 && onMonday === 1,
    `${show(r)} ${JSON.stringify(r.data)} expected created ${exp.slots - 6}; ${monday} rows=${onMonday}`)
}

const failed = results.filter((r) => !r.ok)
console.log(`\n${results.length - failed.length}/${results.length} passed`)
process.exit(failed.length ? 1 : 0)
