// Acceptance checks for hospital affiliation (hospital_admins), the per-hospital authorization of the
// hospital-admin APIs and the admin reservation list (SC-013/SC-014), through the real Spring stack +
// PostgreSQL. Needs a backend on a fresh throwaway database (see README.md). Fixture credentials exist
// only in that database.
import { execFileSync, spawn } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18081'
const DB = process.env.DB ?? 'daipetto_ha_1022'
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
const idOf = (email) => Number(sql(`SELECT id FROM users WHERE email='${email}';`))
const links = (userId) => Number(sql(`SELECT count(*) FROM hospital_admins WHERE user_id=${userId};`))
const show = (r) => `${r.status} ${r.code ?? ''}`

async function waitFor(predicate, label) {
  for (let i = 0; i < 100; i++) {
    if (predicate()) return
    await new Promise((r) => setTimeout(r, 100))
  }
  throw new Error(`timed out waiting for ${label}`)
}

await signUp('ha-admin@daipetto.test', 'Admin')
await signUp('ha-vet@daipetto.test', 'HappyVet')
await signUp('ha-vet2@daipetto.test', 'OtherVet')
await signUp('ha-tanaka@daipetto.test', 'Tanaka')
sql("UPDATE users SET role='ROLE_SYSTEM_ADMIN' WHERE email='ha-admin@daipetto.test'; UPDATE users SET role='ROLE_HOSPITAL_ADMIN' WHERE email IN ('ha-vet@daipetto.test','ha-vet2@daipetto.test');")
const vetId = idOf('ha-vet@daipetto.test')
const vet2Id = idOf('ha-vet2@daipetto.test')
const tanakaId = idOf('ha-tanaka@daipetto.test')
const admin = await login('ha-admin@daipetto.test')
const vet = await login('ha-vet@daipetto.test')
const vet2 = await login('ha-vet2@daipetto.test')
const tanaka = await login('ha-tanaka@daipetto.test')

const h1 = (await api('POST', '/api/v1/admin/hospitals', { token: admin, body: { name: 'さくら動物病院', address: '東京都渋谷区' } })).data.hospitalId
const h2 = (await api('POST', '/api/v1/admin/hospitals', { token: admin, body: { name: 'みどり動物病院', address: '東京都新宿区' } })).data.hospitalId
const rename = (token, id) => api('PATCH', `/api/v1/admin/hospitals/${id}`, { token, body: { name: '更新後の病院', address: '東京都' } })

// ------------------------------------------------------------ before linking
{
  const mine = await api('GET', '/api/v1/admin/hospitals/mine', { token: vet })
  const upd = await rename(vet, h1)
  check('HA-01 unlinked hospital admin: /mine is empty and updating any hospital is 403 HOSPITAL-005',
    mine.status === 200 && mine.data?.length === 0 && upd.status === 403 && upd.code === 'HOSPITAL-005', `${show(mine)} ${JSON.stringify(mine.data)} / ${show(upd)}`)
}

// --------------------------------------------------------------- assignment
{
  const ok = await api('POST', `/api/v1/admin/hospitals/${h1}/admins`, { token: admin, body: { userId: vetId } })
  const dup = await api('POST', `/api/v1/admin/hospitals/${h1}/admins`, { token: admin, body: { userId: vetId } })
  const notAdmin = await api('POST', `/api/v1/admin/hospitals/${h1}/admins`, { token: admin, body: { userId: tanakaId } })
  const noHospital = await api('POST', '/api/v1/admin/hospitals/999999/admins', { token: admin, body: { userId: vetId } })
  const noBody = await api('POST', `/api/v1/admin/hospitals/${h1}/admins`, { token: admin, body: {} })
  const byVet = await api('POST', `/api/v1/admin/hospitals/${h1}/admins`, { token: vet, body: { userId: vet2Id } })
  check('HA-02 assign: 201, duplicate 409 HOSPITAL-007, ROLE_USER 400 HOSPITAL-006, missing hospital 404 HOSPITAL-001, no userId 400, hospital admin 403',
    ok.status === 201 && dup.code === 'HOSPITAL-007' && dup.status === 409 && notAdmin.status === 400 && notAdmin.code === 'HOSPITAL-006'
      && noHospital.status === 404 && noHospital.code === 'HOSPITAL-001' && noBody.status === 400 && noBody.code === 'VALIDATION-001' && byVet.status === 403,
    [ok, dup, notAdmin, noHospital, noBody, byVet].map(show).join(' / '))

  const list = await api('GET', `/api/v1/admin/hospitals/${h1}/admins`, { token: admin })
  const listByVet = await api('GET', `/api/v1/admin/hospitals/${h1}/admins`, { token: vet })
  check('HA-03 admin list (SYSTEM_ADMIN only) returns userId/email/nickname',
    list.status === 200 && list.data?.length === 1 && list.data[0].userId === vetId && list.data[0].email === 'ha-vet@daipetto.test' && list.data[0].nickname === 'HappyVet'
      && listByVet.status === 403, `${show(list)} ${JSON.stringify(list.data)} / ${show(listByVet)}`)
}

// --------------------------------------------- per-hospital authorization
let scheduleId
{
  const mine = await api('GET', '/api/v1/admin/hospitals/mine', { token: vet })
  const own = await rename(vet, h1)
  const other = await rename(vet, h2)
  const hoursOther = await api('POST', `/api/v1/admin/hospitals/${h2}/business-hours`, { token: vet, body: { dayOfWeek: 'MONDAY', openTime: '09:00', closeTime: '18:00', slotDurationMinutes: 30 } })
  const slotOther = await api('POST', `/api/v1/admin/hospitals/${h2}/schedules`, { token: vet, body: { availableDate: '2026-12-01', startTime: '10:00', endTime: '10:30' } })
  const slotOwn = await api('POST', `/api/v1/admin/hospitals/${h1}/schedules`, { token: vet, body: { availableDate: '2026-12-01', startTime: '10:00', endTime: '10:30' } })
  scheduleId = slotOwn.data?.scheduleId
  const sysOther = await rename(admin, h2)
  check('HA-04 linked hospital admin: /mine = [h1]; own hospital 200/201; other hospital update/business-hours/schedule 403 HOSPITAL-005; SYSTEM_ADMIN any hospital',
    JSON.stringify(mine.data?.map((h) => h.hospitalId)) === JSON.stringify([h1]) && own.status === 200 && slotOwn.status === 201
      && [other, hoursOther, slotOther].every((r) => r.status === 403 && r.code === 'HOSPITAL-005') && sysOther.status === 200,
    [mine, own, slotOwn, other, hoursOther, slotOther, sysOther].map(show).join(' / '))
  const blocked = await api('PATCH', `/api/v1/admin/hospitals/${h1}/schedules/${scheduleId}/block`, { token: vet2 })
  check('HA-05 another hospital admin cannot block the slot (403 HOSPITAL-005, still AVAILABLE)',
    blocked.status === 403 && blocked.code === 'HOSPITAL-005' && sql(`SELECT status FROM hospital_schedules WHERE id=${scheduleId};`) === 'AVAILABLE', show(blocked))
}

// ------------------------------------------------- admin reservation list
let reservationId
{
  const petId = (await api('POST', '/api/v1/pets', { token: tanaka, body: { name: 'Momo', petType: 'CAT', neutered: false } })).data.petId
  reservationId = (await api('POST', '/api/v1/reservations', { token: tanaka, body: { petId, hospitalId: h1, scheduleId, memo: '咳が続いています' } })).data?.reservationId
  const list = await api('GET', `/api/v1/admin/reservations?hospitalId=${h1}`, { token: vet })
  const r = list.data?.[0]
  check('HA-06 list for the linked hospital on PostgreSQL (null status/date) with nickname, pet, slot, memo, +09:00',
    list.status === 200 && list.data.length === 1 && r.reservationId === reservationId && r.status === 'REQUESTED' && r.userNickname === 'Tanaka'
      && r.petName === 'Momo' && r.petType === 'CAT' && r.availableDate === '2026-12-01' && r.startTime.startsWith('10:00') && r.memo === '咳が続いています'
      && /\+09:00$/.test(r.createdAt), `${show(list)} ${JSON.stringify(r)}`)

  const byDate = await api('GET', `/api/v1/admin/reservations?hospitalId=${h1}&date=2026-12-01&status=REQUESTED`, { token: vet })
  const otherDate = await api('GET', `/api/v1/admin/reservations?hospitalId=${h1}&date=2026-12-02`, { token: vet })
  const other = await api('GET', `/api/v1/admin/reservations?hospitalId=${h2}`, { token: vet })
  const missing = await api('GET', '/api/v1/admin/reservations', { token: vet })
  const badStatus = await api('GET', `/api/v1/admin/reservations?hospitalId=${h1}&status=FOO`, { token: vet })
  const badDate = await api('GET', `/api/v1/admin/reservations?hospitalId=${h1}&date=2026-13-01`, { token: vet })
  const byUser = await api('GET', `/api/v1/admin/reservations?hospitalId=${h1}`, { token: tanaka })
  check('HA-07 filters; other hospital 403 HOSPITAL-005; missing hospitalId / bad status / bad date 400; ROLE_USER 403',
    byDate.data?.length === 1 && otherDate.data?.length === 0 && other.status === 403 && other.code === 'HOSPITAL-005'
      && [missing, badStatus, badDate].every((x) => x.status === 400 && x.code === 'VALIDATION-001') && byUser.status === 403,
    [byDate, otherDate, other, missing, badStatus, badDate, byUser].map(show).join(' / '))
}

// ------------------------------------------------ state transitions by owner
{
  const foreign = await api('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, { token: vet2 })
  const afterForeign = sql(`SELECT status FROM reservations WHERE id=${reservationId};`)
  const approve = await api('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, { token: vet })
  const complete = await api('PATCH', `/api/v1/admin/reservations/${reservationId}/complete`, { token: vet })
  check('HA-08 approve by an unlinked hospital admin 403 HOSPITAL-005 (unchanged); linked admin approves and completes',
    foreign.status === 403 && foreign.code === 'HOSPITAL-005' && afterForeign === 'REQUESTED' && approve.status === 200 && complete.status === 200
      && sql(`SELECT status FROM reservations WHERE id=${reservationId};`) === 'COMPLETED', [foreign, approve, complete].map(show).join(' / '))
}

// -------------------------------------------------- suspended hospitals stay manageable
{
  await api('PATCH', `/api/v1/admin/hospitals/${h1}/suspend`, { token: admin })
  const sys = await api('GET', '/api/v1/admin/hospitals/mine', { token: admin })
  const mine = await api('GET', '/api/v1/admin/hospitals/mine', { token: vet })
  check('HA-09 /mine includes SUSPENDED hospitals (SYSTEM_ADMIN: all in id order; hospital admin: linked)',
    JSON.stringify(sys.data?.map((h) => [h.hospitalId, h.status])) === JSON.stringify([[h1, 'SUSPENDED'], [h2, 'ACTIVE']])
      && mine.data?.[0]?.status === 'SUSPENDED', `${JSON.stringify(sys.data?.map((h) => h.status))} / ${JSON.stringify(mine.data)}`)
}

// --------------------------------------------------------------- unassign
{
  await api('POST', `/api/v1/admin/hospitals/${h2}/admins`, { token: admin, body: { userId: vetId } })
  const ok = await api('DELETE', `/api/v1/admin/hospitals/${h2}/admins/${vetId}`, { token: admin })
  const again = await api('DELETE', `/api/v1/admin/hospitals/${h2}/admins/${vetId}`, { token: admin })
  const after = await rename(vet, h2)
  check('HA-10 unassign 200, again 404 HOSPITAL-008; the unlinked admin loses access on the next request',
    ok.status === 200 && again.status === 404 && again.code === 'HOSPITAL-008' && after.status === 403 && after.code === 'HOSPITAL-005', [ok, again, after].map(show).join(' / '))
}

// ---------------------------------------------- demotion removes all links
{
  const demote = await api('PATCH', `/api/v1/admin/users/${vetId}/role`, { token: admin, body: { role: 'ROLE_USER' } })
  const linksAfter = links(vetId)
  await api('PATCH', `/api/v1/admin/users/${vetId}/role`, { token: admin, body: { role: 'ROLE_HOSPITAL_ADMIN' } })
  const mine = await api('GET', '/api/v1/admin/hospitals/mine', { token: vet })
  check('HA-11 demotion deletes every link; promoting again does not restore them',
    demote.status === 200 && linksAfter === 0 && mine.status === 200 && mine.data?.length === 0, `${show(demote)} links=${linksAfter} mine=${JSON.stringify(mine.data)}`)
}

// --------------------------------- concurrent duplicate assignment
{
  const all = await Promise.all(Array.from({ length: 5 }, () =>
    api('POST', `/api/v1/admin/hospitals/${h2}/admins`, { token: admin, body: { userId: vet2Id } })))
  const created = all.filter((r) => r.status === 201).length
  const dup = all.filter((r) => r.status === 409 && r.code === 'HOSPITAL-007').length
  check('HA-12 5 concurrent identical assignments -> one 201, four 409 HOSPITAL-007, one row', created === 1 && dup === 4 && links(vet2Id) === 1,
    `${all.map(show).join(' / ')} rows=${links(vet2Id)}`)
}

// --------------------------- race: assignment in flight vs demotion
// A psql session plays the role change: it holds the user row lock, sets ROLE_USER and deletes the links.
// The assignment must block on the user row lock, then read the new role and fail with HOSPITAL-006,
// so no link is left behind for a ROLE_USER.
{
  const holder = spawn('docker', psqlArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
  let output = ''
  holder.stdout.on('data', (c) => { output += c })
  const closed = new Promise((resolve) => holder.on('close', resolve))
  holder.stdin.write(`BEGIN; UPDATE users SET role='ROLE_USER' WHERE id=${vet2Id}; DELETE FROM hospital_admins WHERE user_id=${vet2Id}; SELECT 'HOLDING';\n`)
  let pending
  try {
    await waitFor(() => output.includes('HOLDING'), 'holder')
    pending = api('POST', `/api/v1/admin/hospitals/${h1}/admins`, { token: admin, body: { userId: vet2Id } })
    await waitFor(() => Number(sql("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query ILIKE 'select%from users%for%';")) > 0,
      'assignment blocked on the user row lock')
  } finally {
    holder.stdin.end('COMMIT;\n')
    await closed
  }
  const r = await pending
  check('HA-13 race: assignment waits for a demotion, then 400 HOSPITAL-006 and no link remains', r.status === 400 && r.code === 'HOSPITAL-006' && links(vet2Id) === 0,
    `${show(r)} links=${links(vet2Id)}`)
}

const failed = results.filter((r) => !r.ok)
console.log(`\n${results.length - failed.length}/${results.length} passed`)
process.exit(failed.length ? 1 : 0)
