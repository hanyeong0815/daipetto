// API単体テストの実行。使い捨てのDBで起動したSpring APIに対して全ケースを順番に実行し、
// test/evidence/api/<項目ID>.json と _summary.json を出力する。
//   BASE=http://localhost:18081 DB=daipetto_ut node test/scripts/run-api-unit-tests.mjs
import { writeFileSync, mkdirSync, rmSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { BASE, DB, PASSWORD, http, sql, nowJst, CaseRun, writeEvidence } from './lib.mjs'
import { FEATURES, buildCases } from './api-cases.mjs'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const evidenceDir = join(root, 'evidence', 'api')
rmSync(evidenceDir, { recursive: true, force: true })
mkdirSync(evidenceDir, { recursive: true })

// ---------------------------------------------------------------- 共通の準備データ
const fixtures = {
  tanaka: ['ut-tanaka@daipetto.test', 'Tanaka', 'ROLE_USER'],
  sato: ['ut-sato@daipetto.test', 'Sato', 'ROLE_USER'],
  vet: ['ut-vet@daipetto.test', 'HappyVet', 'ROLE_HOSPITAL_ADMIN'],
  vet2: ['ut-vet2@daipetto.test', 'OtherVet', 'ROLE_HOSPITAL_ADMIN'],
  admin: ['ut-admin@daipetto.test', 'Admin', 'ROLE_SYSTEM_ADMIN'],
}
const ctx = { tokens: {}, ids: {}, emails: {}, sql }
for (const [key, [email, nickname, role]] of Object.entries(fixtures)) {
  await http('POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname } })
  sql(`UPDATE users SET role='${role}' WHERE email='${email}';`)
  ctx.emails[key] = email
  ctx.ids[key] = Number(sql(`SELECT id FROM users WHERE email='${email}';`))
  ctx.tokens[key] = (await http('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })).data.accessToken
}
const hospital = async (name) => (await http('POST', '/api/v1/admin/hospitals', { token: ctx.tokens.admin, body: { name, address: '東京都渋谷区' } })).data.hospitalId
ctx.h1 = await hospital('さくら動物病院（病院1）')
ctx.h2 = await hospital('みどり動物病院（病院2）')
await http('POST', `/api/v1/admin/hospitals/${ctx.h1}/admins`, { token: ctx.tokens.admin, body: { userId: ctx.ids.vet } })

// ---------------------------------------------------------------- 実行
const results = []
for (const c of buildCases(ctx)) {
  const t = new CaseRun(ctx.tokens)
  const executedAt = nowJst()
  let error = null
  try {
    await c.run(t)
  } catch (e) {
    error = String(e?.stack ?? e)
  }
  const verdict = !error && t.checks.length > 0 && t.checks.every((k) => k.ok) ? 'OK' : 'NG'
  const { run, ...meta } = c
  writeEvidence(evidenceDir, { ...meta, executedAt, verdict, environment: { base: BASE, database: DB }, steps: t.steps, checks: t.checks, ...(error ? { error } : {}) })
  results.push({ id: c.id, feature: c.feature, verdict, failed: t.checks.filter((k) => !k.ok).map((k) => k.name), error: error?.split('\n')[0] ?? null })
  console.log(`${verdict}  ${c.id}  ${c.viewpoint}${verdict === 'NG' ? `  -- ${error?.split('\n')[0] ?? t.checks.filter((k) => !k.ok).map((k) => `${k.name}: expected ${JSON.stringify(k.expected)} actual ${JSON.stringify(k.actual)}`).join('; ')}` : ''}`)
}

const summary = {
  executedAt: nowJst(),
  environment: { base: BASE, database: DB },
  total: results.length,
  ok: results.filter((r) => r.verdict === 'OK').length,
  ng: results.filter((r) => r.verdict === 'NG').length,
  byFeature: FEATURES.map((f) => {
    const rs = results.filter((r) => r.feature === f.code)
    return { ...f, total: rs.length, ok: rs.filter((r) => r.verdict === 'OK').length }
  }),
  results,
}
writeFileSync(join(evidenceDir, '_summary.json'), JSON.stringify(summary, null, 2) + '\n')
console.log(`\n${summary.ok}/${summary.total} OK`)
process.exit(summary.ng ? 1 : 0)
