// Analysis API E2E: Spring (scratch DB) -> analysis-api (FastAPI) -> PostgreSQL.
// Expected values follow docs/07 §7 and are computed from the seeded data below.
//   BASE=http://localhost:18083 ANALYSIS_CONTAINER=daipetto-analysis-e2e OUT=<json> node analysis-e2e.mjs
import { execFileSync } from 'node:child_process'
import { writeFileSync } from 'node:fs'

const BASE = process.env.BASE ?? 'http://localhost:18083'
const CONTAINER = process.env.ANALYSIS_CONTAINER
const PASSWORD = 'Password123!' // localhost test fixture (same as test/scripts/lib.mjs); never written to the output

const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Tokyo' }).format(new Date())
const shift = (iso, days) => {
  const d = new Date(`${iso}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}
const T = (n) => shift(today, n)
const diffDays = (a, b) => Math.round((new Date(`${a}T00:00:00Z`) - new Date(`${b}T00:00:00Z`)) / 86400000)
const oneYearLater = (iso) => {
  const [y, m, d] = iso.split('-').map(Number)
  return m === 2 && d === 29 ? `${y + 1}-02-28` : `${y + 1}-${iso.slice(5)}`
}

async function call(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json
  try { json = JSON.parse(text) } catch { json = text }
  return { status: res.status, body: json }
}

async function must(method, path, options) {
  const res = await call(method, path, options)
  if (res.status >= 300) throw new Error(`${method} ${path} -> ${res.status} ${JSON.stringify(res.body)}`)
  return res.body.data
}

async function user(tag) {
  const email = `analysis-${tag}-${Date.now()}@example.com`
  await must('POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname: `analysis-${tag}` } })
  return (await must('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })).accessToken
}

const pet = async (token, name) =>
  (await must('POST', '/api/v1/pets', { token, body: { name, petType: 'CAT', birthDate: '2023-01-01', gender: 'FEMALE', weight: 4.0 } })).petId
const record = (token, petId, recordedDate, weight, symptom) =>
  must('POST', `/api/v1/pets/${petId}/health-records`, { token, body: { recordedDate, weight, symptom } })
const vaccination = async (token, petId, vaccineName, scheduledDate) =>
  (await must('POST', `/api/v1/pets/${petId}/vaccinations`, { token, body: { vaccineName, scheduledDate } })).vaccinationId

const results = []
function check(id, title, res, expectedStatus, expect) {
  const problems = []
  if (res.status !== expectedStatus) problems.push(`status ${res.status} != ${expectedStatus}`)
  try { expect?.(res.body, problems) } catch (e) { problems.push(String(e)) }
  results.push({ id, title, status: res.status, body: res.body, verdict: problems.length ? 'NG' : 'OK', problems })
}
const eq = (problems, label, actual, expected) => {
  if (JSON.stringify(actual) !== JSON.stringify(expected)) problems.push(`${label}: ${JSON.stringify(actual)} != ${JSON.stringify(expected)}`)
}
const errorCode = (code) => (body, p) => { eq(p, 'success', body.success, false); eq(p, 'code', body.code, code); eq(p, 'data', body.data, null) }

// ---- seed
const a = await user('a')
const b = await user('b')
const momo = await pet(a, 'Momo')
const empty = await pet(a, 'Empty')
const removed = await pet(a, 'Removed')
await must('DELETE', `/api/v1/pets/${removed}`, { token: a })

await record(a, momo, T(-40), 4.0, null)          // outside the 30-day score window
await record(a, momo, T(-20), 4.0, '咳')
await record(a, momo, T(-10), 4.1, null)
await record(a, momo, T(-10), 4.2, '咳, 食欲低下')  // same day, registered later -> daily value 4.2
await record(a, momo, T(-5), null, '嘔吐')         // symptom only
await record(a, momo, T(0), 4.3, null)

const rabies = await vaccination(a, momo, '狂犬病', T(-400))
await must('PATCH', `/api/v1/vaccinations/${rabies}/complete`, { token: a, body: { completedDate: T(-380) } })
const mixedOld = await vaccination(a, momo, '混合ワクチン', T(-100))
await must('PATCH', `/api/v1/vaccinations/${mixedOld}/complete`, { token: a, body: { completedDate: T(-100) } })
await vaccination(a, momo, '混合ワクチン', T(30))
const mixedNext = await vaccination(a, momo, '混合ワクチン', T(10))
const filaria = await vaccination(a, momo, 'フィラリア', T(5))
await must('PATCH', `/api/v1/vaccinations/${filaria}/cancel`, { token: a })

const P = (petId) => `/api/v1/analysis/pets/${petId}`

// ---- 7-1 weight
check('E2E-01', '体重推移（期間なし）: 日別・昇順、同日は後の記録、summary', await call('GET', `${P(momo)}/weight`, { token: a }), 200, (body, p) => {
  eq(p, 'points', body.data.points, [{ date: T(-40), weight: 4.0 }, { date: T(-20), weight: 4.0 }, { date: T(-10), weight: 4.2 }, { date: T(0), weight: 4.3 }])
  eq(p, 'summary', body.data.summary, { count: 4, first: 4.0, latest: 4.3, min: 4.0, max: 4.3, change: 0.3, changeRate: 7.5 })
})
check('E2E-02', '体重推移（期間あり、両端を含む）', await call('GET', `${P(momo)}/weight?from=${T(-20)}&to=${T(-10)}`, { token: a }), 200, (body, p) => {
  eq(p, 'points', body.data.points, [{ date: T(-20), weight: 4.0 }, { date: T(-10), weight: 4.2 }])
  eq(p, 'changeRate', body.data.summary.changeRate, 5.0)
})
check('E2E-03', '体重推移（記録なし）: points 空・summary null', await call('GET', `${P(empty)}/weight`, { token: a }), 200, (body, p) => {
  eq(p, 'data', body.data, { points: [], summary: null })
})
check('E2E-04', '期間の前後不正 → 400 ANALYSIS-002', await call('GET', `${P(momo)}/weight?from=${T(0)}&to=${T(-1)}`, { token: a }), 400, errorCode('ANALYSIS-002'))
check('E2E-05', '日付の形式不正 → 400 VALIDATION-001', await call('GET', `${P(momo)}/weight?from=2026/10/01`, { token: a }), 400, errorCode('VALIDATION-001'))

// ---- 7-2 health score
check('E2E-06', '健康スコア: 体重 7.5% → -15、症状3日 → -30、55 CAUTION', await call('GET', `${P(momo)}/health-score`, { token: a }), 200, (body, p) => {
  eq(p, 'score/grade', [body.data.score, body.data.grade], [55, 'CAUTION'])
  eq(p, 'period', body.data.period, { from: T(-29), to: T(0) })
  eq(p, 'factors', body.data.factors, [
    { type: 'WEIGHT_CHANGE', evaluated: true, penalty: 15, value: 7.5 },
    { type: 'SYMPTOM_FREQUENCY', evaluated: true, penalty: 30, value: 3 },
  ])
})
check('E2E-07', '健康スコア（記録なし）: NO_DATA', await call('GET', `${P(empty)}/health-score`, { token: a }), 200, (body, p) => {
  eq(p, 'score/grade', [body.data.score, body.data.grade], [null, 'NO_DATA'])
})

// ---- 7-4 vaccination
const rabiesNext = oneYearLater(T(-380))
check('E2E-08', '次回接種: 予定を優先（最も早い予定）、完了のみは1年後、中止のみは除外、日付順', await call('GET', `${P(momo)}/vaccination/next`, { token: a }), 200, (body, p) => {
  const expected = [
    { vaccineName: '狂犬病', lastCompletedDate: T(-380), nextDate: rabiesNext, source: 'RECOMMENDED', vaccinationId: null, overdue: rabiesNext < today, daysUntil: diffDays(rabiesNext, today) },
    { vaccineName: '混合ワクチン', lastCompletedDate: T(-100), nextDate: T(10), source: 'SCHEDULED', vaccinationId: mixedNext, overdue: false, daysUntil: 10 },
  ].sort((x, y) => x.nextDate.localeCompare(y.nextDate))
  eq(p, 'items', body.data.items, expected)
})
check('E2E-09', '次回接種（記録なし）: items 空', await call('GET', `${P(empty)}/vaccination/next`, { token: a }), 200, (body, p) => eq(p, 'items', body.data.items, []))

// ---- access control and gap
for (const path of ['weight', 'health-score', 'vaccination/next']) {
  check(`E2E-10 ${path}`, '他ユーザーのペット → 403 ANALYSIS-001', await call('GET', `${P(momo)}/${path}`, { token: b }), 403, errorCode('ANALYSIS-001'))
}
check('E2E-11', '削除済みのペット → 403 ANALYSIS-001', await call('GET', `${P(removed)}/health-score`, { token: a }), 403, errorCode('ANALYSIS-001'))
check('E2E-12', '存在しないペット → 403 ANALYSIS-001', await call('GET', `${P(999999)}/weight`, { token: a }), 403, errorCode('ANALYSIS-001'))
check('E2E-13', 'Tokenなし → 401 AUTH-001', await call('GET', `${P(momo)}/weight`), 401, errorCode('AUTH-001'))
check('E2E-14', '症状統計（練習用・未実装）→ 404 COMMON-001', await call('GET', `${P(momo)}/symptoms/summary`, { token: a }), 404, errorCode('COMMON-001'))

// ---- analysis API down (last: stops the container)
if (CONTAINER) {
  execFileSync('docker', ['stop', CONTAINER], { stdio: 'ignore' })
  check('E2E-15', '分析APIの停止中 → 503 ANALYSIS-003', await call('GET', `${P(momo)}/health-score`, { token: a }), 503, errorCode('ANALYSIS-003'))
}

const ok = results.filter((r) => r.verdict === 'OK').length
if (process.env.OUT) {
  writeFileSync(process.env.OUT, JSON.stringify({ executedAt: new Date().toISOString(), today, ok, total: results.length, results }, null, 2))
}
for (const r of results) console.log(`${r.verdict} ${r.id} ${r.title}${r.problems.length ? ' :: ' + r.problems.join('; ') : ''}`)
console.log(`${ok}/${results.length} OK`)
process.exit(ok === results.length ? 0 : 1)
