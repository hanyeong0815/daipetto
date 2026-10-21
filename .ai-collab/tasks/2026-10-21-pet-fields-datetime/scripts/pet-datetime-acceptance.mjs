// Acceptance checks for the pet-fields merge and the +09:00 datetime format, through the real Spring
// stack + PostgreSQL. Needs a backend started against a fresh throwaway database (see README.md).
// Fixture credentials exist only in that database.
import { execFileSync } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18081'
const DB = process.env.DB ?? 'daipetto_pet_1021'
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
  const json = await res.json().catch(() => null)
  return { status: res.status, code: json?.code ?? null, data: json?.data ?? null }
}

await api('POST', '/api/v1/users', { body: { email: 'pet-owner@daipetto.test', password: PASSWORD, nickname: 'petowner' } })
const token = (await api('POST', '/api/v1/auth/login', { body: { email: 'pet-owner@daipetto.test', password: PASSWORD } })).data.accessToken
const row = (id) => sql(`SELECT name || '/' || coalesce(gender,'NULL') || '/' || coalesce(weight::text,'NULL') || '/' || coalesce(breed,'NULL') || '/' || neutered::text || '/' || coalesce(microchip_number,'NULL') FROM pets WHERE id=${id};`)

// ------------------------------------------------------------------ migrations
const versions = sql("SELECT string_agg(version, ',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success;")
check('Flyway applied V1..V14 in order', versions === '1,2,3,4,5,6,7,8,9,10,11,12,13,14', versions)

// ------------------------------------------------------------------ create
const noGender = await api('POST', '/api/v1/pets', { token, body: { name: 'Kuro', petType: 'DOG' } })
check('PET-T007 create without gender -> 201, gender NULL', noGender.status === 201 && row(noGender.data.petId) === 'Kuro/NULL/NULL/NULL/false/NULL',
  `${noGender.status} ${noGender.data && row(noGender.data.petId)}`)

const full = await api('POST', '/api/v1/pets', {
  token, body: { name: 'Momo', petType: 'CAT', birthDate: '2023-01-01', gender: 'FEMALE', weight: 4.5, breed: 'Scottish Fold', neutered: true, microchipNumber: '392000000000001' },
})
const petId = full.data.petId
check('create with breed/neutered/microchip stores them', full.status === 201 && row(petId) === 'Momo/FEMALE/4.50/Scottish Fold/true/392000000000001', row(petId))

for (const [label, body] of [
  ['name 51 chars', { name: 'a'.repeat(51), petType: 'CAT' }],
  ['breed 101 chars', { name: 'x', petType: 'CAT', breed: 'b'.repeat(101) }],
  ['negative weight', { name: 'x', petType: 'CAT', weight: -0.1 }],
  ['weight 1000.00', { name: 'x', petType: 'CAT', weight: 1000 }],
  ['microchip 16 chars', { name: 'x', petType: 'CAT', microchipNumber: '1'.repeat(16) }],
]) {
  const r = await api('POST', '/api/v1/pets', { token, body })
  check(`PET-T006 ${label} -> 400 VALIDATION-001 (not 500)`, r.status === 400 && r.code === 'VALIDATION-001', `${r.status} ${r.code}`)
}

// ------------------------------------------------------------------ read
const detail = await api('GET', `/api/v1/pets/${petId}`, { token })
check('detail returns the new fields', detail.data?.breed === 'Scottish Fold' && detail.data?.neutered === true && detail.data?.microchipNumber === '392000000000001',
  JSON.stringify({ breed: detail.data?.breed, neutered: detail.data?.neutered, microchipNumber: detail.data?.microchipNumber }))
check('COMMON-T001 createdAt is JST with offset, second precision', /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+09:00$/.test(detail.data?.createdAt ?? ''), detail.data?.createdAt)
const dbCreated = sql(`SELECT to_char(created_at, 'YYYY-MM-DD"T"HH24:MI:SS') FROM pets WHERE id=${petId};`)
check('createdAt wall-clock equals the stored JST value', detail.data?.createdAt === `${dbCreated}+09:00`, `${detail.data?.createdAt} vs ${dbCreated}`)
check('birthDate stays a plain date (no offset)', detail.data?.birthDate === '2023-01-01', detail.data?.birthDate)

const list = await api('GET', '/api/v1/pets', { token })
check('list includes breed', list.data?.find((p) => p.id === petId)?.breed === 'Scottish Fold', JSON.stringify(list.data?.map((p) => p.breed)))

// ------------------------------------------------------------------ update (full replacement)
const replaced = await api('PATCH', `/api/v1/pets/${petId}`, { token, body: { name: 'Momo2', petType: 'CAT', breed: 'Mix' } })
check('PET-T008 PATCH replaces every field (omitted -> NULL/false)', replaced.status === 200 && row(petId) === 'Momo2/NULL/NULL/Mix/false/NULL', `${replaced.status} ${row(petId)}`)

const updatedAll = await api('PATCH', `/api/v1/pets/${petId}`, {
  token, body: { name: 'Momo3', petType: 'CAT', gender: 'FEMALE', weight: 5.25, breed: 'Mix', neutered: true, microchipNumber: '392000000000002' },
})
check('PATCH writes breed/neutered/microchip through the conditional UPDATE', updatedAll.status === 200 && row(petId) === 'Momo3/FEMALE/5.25/Mix/true/392000000000002', row(petId))

const badUpdate = await api('PATCH', `/api/v1/pets/${petId}`, { token, body: { name: 'Momo3', petType: 'CAT', breed: 'b'.repeat(101) } })
check('PATCH validation -> 400 and row unchanged', badUpdate.status === 400 && row(petId) === 'Momo3/FEMALE/5.25/Mix/true/392000000000002', `${badUpdate.status} ${row(petId)}`)

// ------------------------------------------------------------------ notification createdAt
const userId = Number(sql("SELECT id FROM users WHERE email='pet-owner@daipetto.test';"))
sql(`INSERT INTO notifications(user_id,notification_type,title,message,created_at) VALUES (${userId},'VACCINATION','t','m','2026-10-21 23:59:58.765');`)
const notifications = await api('GET', '/api/v1/notifications', { token })
check('notification createdAt keeps the JST wall clock and adds +09:00', notifications.data?.[0]?.createdAt === '2026-10-21T23:59:58+09:00', notifications.data?.[0]?.createdAt)

const failed = results.filter((r) => !r.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
