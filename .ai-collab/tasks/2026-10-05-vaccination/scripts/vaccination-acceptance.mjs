// Acceptance checks for the Vaccination API through the real Spring stack + PostgreSQL.
// Needs a backend started against a throwaway database (see README.md). Fixture credentials exist
// only in that database. Races use the deterministic method of the earlier rounds: a psql session
// holds the competing change uncommitted, the HTTP request is sent, the script waits until
// pg_stat_activity shows it blocked on vaccinations (it throws otherwise), then the holder commits.
import { execFileSync, spawn } from 'node:child_process'

const BASE = process.env.BASE ?? 'http://localhost:18081'
const DB = process.env.DB ?? 'daipetto_vac_1021'
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

async function signUp(email) {
  await api('POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname: email.split('@')[0] } })
  return (await api('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })).data.accessToken
}

async function waitFor(predicate, label) {
  for (let i = 0; i < 100; i++) {
    if (predicate()) return
    await new Promise((r) => setTimeout(r, 100))
  }
  throw new Error(`timed out waiting for ${label}`)
}

async function interleave(statement, request) {
  const holder = spawn('docker', psqlArgs, { stdio: ['pipe', 'pipe', 'pipe'] })
  let output = ''
  holder.stdout.on('data', (chunk) => { output += chunk })
  const closed = new Promise((resolve) => holder.on('close', resolve))
  holder.stdin.write(`BEGIN; ${statement}; SELECT 'HOLDING';\n`)
  let pending
  try {
    await waitFor(() => output.includes('HOLDING'), 'holder')
    pending = request()
    await waitFor(() => Number(sql(
      "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE 'update vaccinations%';",
    )) > 0, 'request blocked on update vaccinations')
  } finally {
    holder.stdin.end('COMMIT;\n')
    await closed
  }
  return pending
}

const owner = await signUp('vac-owner@daipetto.test')
const other = await signUp('vac-other@daipetto.test')
const petId = (await api('POST', '/api/v1/pets', {
  token: owner, body: { name: 'Momo', petType: 'CAT', birthDate: '2023-01-01', gender: 'FEMALE', weight: 4.5 },
})).data.petId

const create = async (vaccineName, scheduledDate) => (await api('POST', `/api/v1/pets/${petId}/vaccinations`, {
  token: owner, body: { vaccineName, scheduledDate },
})).data.vaccinationId
const state = (id) => sql(`SELECT status || '/' || coalesce(completed_date::text,'-') || '/' || (deleted_at IS NOT NULL)::text FROM vaccinations WHERE id=${id};`)

// ------------------------------------------------------------- basic flow
{
  const a = await create('混合ワクチン', '2026-11-20')
  const b = await create('狂犬病', '2026-11-01')
  const list = await api('GET', `/api/v1/pets/${petId}/vaccinations`, { token: owner })
  check('VAC-T001 register and list in scheduled-date order',
    list.status === 200 && list.data.map((v) => v.vaccinationId).join(',') === `${b},${a}` && list.data.every((v) => v.status === 'SCHEDULED'),
    `${list.status} ${list.data?.map((v) => `${v.vaccinationId}:${v.scheduledDate}`).join(',')}`)

  const foreign = await api('PATCH', `/api/v1/vaccinations/${a}/cancel`, { token: other })
  check('VAC-T002 other user cannot cancel', foreign.status === 403 && foreign.code === 'VACCINATION-002' && state(a).startsWith('SCHEDULED'), `${foreign.status} ${foreign.code}`)

  const cancel = await api('PATCH', `/api/v1/vaccinations/${a}/cancel`, { token: owner })
  check('VAC-T003 SCHEDULED -> CANCELLED', cancel.status === 200 && state(a) === 'CANCELLED/-/false', `${cancel.status} ${state(a)}`)

  const again = await api('PATCH', `/api/v1/vaccinations/${a}/cancel`, { token: owner })
  check('VAC-T004 cancelling a CANCELLED row -> 409 VACCINATION-003', again.status === 409 && again.code === 'VACCINATION-003', `${again.status} ${again.code}`)

  const resched = await api('PATCH', `/api/v1/vaccinations/${a}/reschedule`, { token: owner, body: { scheduledDate: '2026-12-01' } })
  const afterResched = sql(`SELECT status || '/' || scheduled_date FROM vaccinations WHERE id=${a};`)
  check('CANCELLED -> SCHEDULED with the new date', resched.status === 200 && afterResched === 'SCHEDULED/2026-12-01', `${resched.status} ${afterResched}`)

  const done = await api('PATCH', `/api/v1/vaccinations/${b}/complete`, { token: owner, body: { completedDate: '2026-10-20' } })
  check('SCHEDULED -> COMPLETED with completedDate', done.status === 200 && state(b) === 'COMPLETED/2026-10-20/false', `${done.status} ${state(b)}`)

  const cancelDone = await api('PATCH', `/api/v1/vaccinations/${b}/cancel`, { token: owner })
  check('cancelling a COMPLETED row -> 409 VACCINATION-003', cancelDone.status === 409 && cancelDone.code === 'VACCINATION-003', `${cancelDone.status} ${cancelDone.code}`)

  const foreignDelete = await api('DELETE', `/api/v1/vaccinations/${b}`, { token: other })
  check('other user cannot delete', foreignDelete.status === 403 && foreignDelete.code === 'VACCINATION-002' && state(b).endsWith('/false'), `${foreignDelete.status} ${foreignDelete.code}`)

  const del = await api('DELETE', `/api/v1/vaccinations/${b}`, { token: owner })
  const listed = (await api('GET', `/api/v1/pets/${petId}/vaccinations`, { token: owner })).data.some((v) => v.vaccinationId === b)
  check('VAC-T005 delete sets deleted_at and hides the row', del.status === 200 && state(b).endsWith('/true') && !listed, `${del.status} ${state(b)} listed=${listed}`)

  const delAgain = await api('DELETE', `/api/v1/vaccinations/${b}`, { token: owner })
  check('deleting a deleted row -> 404 VACCINATION-001', delAgain.status === 404 && delAgain.code === 'VACCINATION-001', `${delAgain.status} ${delAgain.code}`)
}

// ------------------------------------------------------------------ races
{
  const id = await create('混合ワクチン', '2026-11-05')
  const r = await interleave(`UPDATE vaccinations SET status='COMPLETED', completed_date='2026-10-21' WHERE id=${id}`,
    () => api('PATCH', `/api/v1/vaccinations/${id}/cancel`, { token: owner }))
  check('cancel blocked behind a committed completion -> 409, row stays COMPLETED',
    r.status === 409 && r.code === 'VACCINATION-003' && state(id) === 'COMPLETED/2026-10-21/false', `${r.status} ${r.code} ${state(id)}`)
}

{
  const id = await create('狂犬病', '2026-11-06')
  const r = await interleave(`UPDATE vaccinations SET deleted_at=CURRENT_TIMESTAMP WHERE id=${id}`,
    () => api('DELETE', `/api/v1/vaccinations/${id}`, { token: owner }))
  check('delete blocked behind a committed delete -> 404 VACCINATION-001', r.status === 404 && r.code === 'VACCINATION-001' && state(id).endsWith('/true'), `${r.status} ${r.code} ${state(id)}`)
}

{
  const id = await create('混合ワクチン', '2026-11-07')
  const r = await interleave(`UPDATE vaccinations SET deleted_at=CURRENT_TIMESTAMP WHERE id=${id}`,
    () => api('PATCH', `/api/v1/vaccinations/${id}/cancel`, { token: owner }))
  check('cancel blocked behind a committed delete -> 409, row stays deleted and SCHEDULED',
    r.status === 409 && r.code === 'VACCINATION-003' && state(id) === 'SCHEDULED/-/true', `${r.status} ${r.code} ${state(id)}`)
}

const failed = results.filter((r) => !r.ok).length
console.log(`\n${results.length - failed}/${results.length} checks passed`)
process.exit(failed ? 1 : 0)
