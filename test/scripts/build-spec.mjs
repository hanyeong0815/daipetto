// 単体テスト仕様書・結果（test/spec/*.md）と結果サマリ（test/RESULTS.md）を生成する。
// 1) spring-api/build のJUnit結果・JaCoCoレポートを test/evidence にコピー
// 2) API単体テストの定義（api-cases.mjs）と実行結果（evidence/api/_summary.json）、JUnit結果を機能ごとにまとめる
//   node test/scripts/build-spec.mjs
import { cpSync, existsSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { FEATURES, buildCases } from './api-cases.mjs'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const repo = join(root, '..')
const build = join(repo, 'spring-api', 'build')
const ev = join(root, 'evidence')

// ------------------------------------------------------------ 1. エビデンスの収集
if (existsSync(join(build, 'test-results', 'test'))) {
  rmSync(join(ev, 'junit'), { recursive: true, force: true })
  mkdirSync(join(ev, 'junit'), { recursive: true })
  for (const f of readdirSync(join(build, 'test-results', 'test')).filter((f) => f.endsWith('.xml'))) {
    cpSync(join(build, 'test-results', 'test', f), join(ev, 'junit', f))
  }
}
if (existsSync(join(build, 'reports', 'jacoco', 'test'))) {
  rmSync(join(ev, 'coverage'), { recursive: true, force: true })
  cpSync(join(build, 'reports', 'jacoco', 'test'), join(ev, 'coverage'), { recursive: true })
}

// ------------------------------------------------------------ 2. 読み込み
const decode = (s) => s.replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&')
const junit = readdirSync(join(ev, 'junit')).filter((f) => f.endsWith('.xml')).flatMap((file) => {
  const xml = readFileSync(join(ev, 'junit', file), 'utf8')
  const suite = xml.match(/<testsuite [^>]*timestamp="([^"]+)"/)
  return [...xml.matchAll(/<testcase name="([^"]*)" classname="([^"]*)" time="([^"]*)"(\/>|>([\s\S]*?)<\/testcase>)/g)].map((m) => ({
    file,
    name: decode(m[1]),
    className: m[2].replace('koh.portfolio.springapi.', ''),
    time: Number(m[3]),
    passed: !/<(failure|error)/.test(m[5] ?? ''),
    skipped: /<skipped/.test(m[5] ?? ''),
    timestamp: suite?.[1],
  }))
})

// JUnitのテストクラスを機能に割り当てる（クラス名の部分一致、上から順に判定）
const JUNIT_FEATURE = [
  [/AdminUser|UserPersistenceAdapter|UserMapper/, 'UT-12'],
  [/^application\.auth|presentation\.auth|security\.|RefreshToken|presentation\.user\.UserController/, 'UT-01'],
  [/\.pet\./, 'UT-02'],
  [/healthrecord/, 'UT-03'],
  [/vaccination/i, 'UT-04'],
  [/HospitalAdminServiceTest|HospitalAdminPersistenceAdapter/, 'UT-06'],
  [/BusinessHours/, 'UT-07'],
  [/HospitalSchedule/, 'UT-08'],
  [/hospital/i, 'UT-05'],
  [/ReservationStateTransition|AdminReservation|ReservationAdmin/, 'UT-10'],
  [/reservation/, 'UT-09'],
  [/notification/, 'UT-11'],
  [/SpringApiApplication/, 'UT-13'],
]
// 共通エラー（COMMON-xxx・SERVER-001）のテストは代表のControllerテストクラスにあるため、テスト名で共通（UT-13）に分類する
const junitFeature = (className, name = '') =>
  (/COMMON-\d{3}|SERVER-001/.test(name) ? 'UT-13' : JUNIT_FEATURE.find(([re]) => re.test(className))?.[1]) ?? 'UT-13'

const cases = buildCases({ tokens: {}, ids: {}, emails: {}, sql: () => '' }).map(({ run, ...meta }) => meta)
  .sort((a, b) => a.id.localeCompare(b.id))
const summary = JSON.parse(readFileSync(join(ev, 'api', '_summary.json'), 'utf8'))
const verdictOf = Object.fromEntries(summary.results.map((r) => [r.id, r.verdict]))

const coverageXml = readFileSync(join(ev, 'coverage', 'jacocoTestReport.xml'), 'utf8')
const reportTotals = coverageXml.slice(coverageXml.lastIndexOf('</package>'))
const coverage = Object.fromEntries([...reportTotals.matchAll(/<counter type="(\w+)" missed="(\d+)" covered="(\d+)"\/>/g)]
  .map(([, type, missed, covered]) => [type, { covered: +covered, total: +covered + +missed, pct: (+covered / (+covered + +missed)) * 100 }]))

const escapeCell = (s) => String(s ?? '').replace(/\|/g, '\\|').replace(/\n/g, ' ')
const pct = (c) => `${c.pct.toFixed(1)}%（${c.covered}/${c.total}）`
const slug = { 'UT-01': 'auth', 'UT-02': 'pet', 'UT-03': 'health-record', 'UT-04': 'vaccination', 'UT-05': 'hospital', 'UT-06': 'hospital-admin',
  'UT-07': 'business-hours', 'UT-08': 'schedule', 'UT-09': 'reservation', 'UT-10': 'reservation-admin', 'UT-11': 'notification', 'UT-12': 'user-admin', 'UT-13': 'common' }
const executedOn = summary.executedAt.slice(0, 10)

// ------------------------------------------------------------ 3. 機能別の仕様書・結果
mkdirSync(join(root, 'spec'), { recursive: true })
const rows = []
for (const f of FEATURES) {
  const fc = cases.filter((c) => c.feature === f.code)
  const fj = junit.filter((j) => junitFeature(j.className, j.name) === f.code)
  const apiOk = fc.filter((c) => verdictOf[c.id] === 'OK').length
  const junitOk = fj.filter((j) => j.passed && !j.skipped).length
  rows.push({ f, api: [apiOk, fc.length], junit: [junitOk, fj.length] })
  const lines = [
    `# 単体テスト仕様書・結果 ${f.code} ${f.name}`,
    '',
    '| 項目 | 内容 |',
    '| --- | --- |',
    `| 対象機能 | ${f.name}（${f.doc}） |`,
    `| 実施日 | ${executedOn} |`,
    '| 実施 | テスト設計: Claude Code（Opus 5.5）／実行・エビデンス取得: Claude Code（Sonnet 5.5 サブエージェントを含む）／テスト設計レビュー・結果検証: Codex（gpt-6.1-sol、[reviews](../reviews/)） |',
    `| 結果 | API単体テスト ${apiOk}/${fc.length} OK、自動テスト（JUnit）${junitOk}/${fj.length} 合格 |`,
    '| 環境 | [README.md](../README.md) の「4. 実施環境」「5. 実施手順」 |',
    '',
    '## 1. API単体テスト（実HTTP・実DB）',
    '',
    '使い捨てのPostgreSQLで起動したSpring APIへ実際にリクエストを送り、レスポンスとDBの状態を確認する。エビデンスはリクエスト・レスポンス・DB確認・判定をまとめたJSON。期待結果は設計書（docs/07）の記述を正とする。',
    '',
    '| 項目ID | 対象API | 区分 | テスト観点 | 前提条件・入力 | 期待結果 | 結果 | エビデンス |',
    '| --- | --- | --- | --- | --- | --- | --- | --- |',
    ...fc.map((c) => `| ${c.id} | \`${escapeCell(c.api)}\` | ${c.kind} | ${escapeCell(c.viewpoint)} | ${escapeCell(c.condition)} | ${escapeCell(c.expected)} | ${verdictOf[c.id] ?? '未実施'} | [${c.id}.json](../evidence/api/${c.id}.json) |`),
    '',
    '## 2. 自動テスト（JUnit：Service・Controller・Persistence）',
    '',
    'クラス・メソッド単位の単体テスト。Repository等はモック（Mockito）、Persistenceは H2 の `@DataJpaTest`、Controller は `@WebMvcTest` で実行する。テスト内容は各テストメソッドの `@DisplayName`。エビデンスはJUnitの結果XML（`../evidence/junit/`）。',
    '',
    '| No | テストクラス | テスト内容 | 結果 |',
    '| --- | --- | --- | --- |',
    ...fj.sort((a, b) => a.className.localeCompare(b.className) || a.name.localeCompare(b.name))
      .map((j, i) => `| ${i + 1} | [${j.className.split('.').pop()}](../evidence/junit/${j.file}) | ${escapeCell(j.name)} | ${j.skipped ? 'スキップ' : j.passed ? '合格' : '不合格'} |`),
    '',
  ]
  writeFileSync(join(root, 'spec', `${f.code}_${slug[f.code]}.md`), lines.join('\n'))
}

// ------------------------------------------------------------ 4. 結果サマリ
const sum = (k, i) => rows.reduce((s, r) => s + r[k][i], 0)
const packages = [...coverageXml.matchAll(/<package name="([^"]+)">([\s\S]*?)<\/package>/g)].map(([, name, body]) => {
  const tail = body.slice(body.lastIndexOf('</class>'))
  const get = (type) => { const m = tail.match(new RegExp(`<counter type="${type}" missed="(\\d+)" covered="(\\d+)"/>`)); return m ? { covered: +m[2], total: +m[1] + +m[2], pct: (+m[2] / (+m[1] + +m[2])) * 100 } : null }
  return { name: name.replace('koh/portfolio/springapi/', '').replaceAll('/', '.'), line: get('LINE'), branch: get('BRANCH') }
}).filter((p) => p.line)

const results = [
  '# 単体テスト結果サマリ',
  '',
  `- 実施日: ${executedOn}（API単体テストの実行: ${summary.executedAt}）`,
  `- API単体テスト: **${summary.ok}/${summary.total} OK**`,
  `- 自動テスト（JUnit）: **${junit.filter((j) => j.passed && !j.skipped).length}/${junit.length} 合格**（スキップ ${junit.filter((j) => j.skipped).length}）`,
  `- カバレッジ（JaCoCo）: 命令 ${pct(coverage.INSTRUCTION)}、行（C0相当）${pct(coverage.LINE)}、分岐（C1相当）${pct(coverage.BRANCH)}`,
  '',
  '## 機能別',
  '',
  '| 機能 | 仕様書・結果 | API単体テスト | 自動テスト（JUnit） |',
  '| --- | --- | --- | --- |',
  ...rows.map((r) => `| ${r.f.code} ${r.f.name} | [${r.f.code}_${slug[r.f.code]}.md](spec/${r.f.code}_${slug[r.f.code]}.md) | ${r.api[0]}/${r.api[1]} | ${r.junit[0]}/${r.junit[1]} |`),
  `| 合計 | | ${sum('api', 0)}/${sum('api', 1)} | ${sum('junit', 0)}/${sum('junit', 1)} |`,
  '',
  '画面単体テストは [UT-14_screens.md](spec/UT-14_screens.md)、不具合・指摘は [DEFECTS.md](DEFECTS.md)。',
  '',
  '## パッケージ別カバレッジ（JaCoCo）',
  '',
  '詳細（クラス・メソッド・行単位）は [evidence/coverage/html/index.html](evidence/coverage/html/index.html)。',
  '',
  '| パッケージ | 行（C0相当） | 分岐（C1相当） |',
  '| --- | --- | --- |',
  ...packages.map((p) => `| ${p.name} | ${p.line.pct.toFixed(1)}% | ${p.branch ? `${p.branch.pct.toFixed(1)}%` : '—（分岐なし）'} |`),
  '',
]
writeFileSync(join(root, 'RESULTS.md'), results.join('\n'))
console.log(`spec: ${rows.length} files, API ${sum('api', 0)}/${sum('api', 1)}, JUnit ${sum('junit', 0)}/${sum('junit', 1)}, line ${coverage.LINE.pct.toFixed(1)}%, branch ${coverage.BRANCH.pct.toFixed(1)}%`)
