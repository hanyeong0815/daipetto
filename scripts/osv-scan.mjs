// 依存ライブラリの既知の脆弱性を OSV（https://osv.dev）で確認する（docs/09 §22）。
// 送るのは公開パッケージの名前とバージョンだけ。どのフォルダからでも実行できる:
//   node scripts/osv-scan.mjs [--json <out.json>] [--python <インタープリター>]
// 対象: spring-api の runtimeClasspath（Gradle）、frontend/package-lock.json（npm、推移的依存を含む）、
//       analysis-api の Python 環境（インストール済みの全パッケージ。既定は .venv、無ければ .review-venv）
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const WINDOWS = process.platform === 'win32'
// Windows の .bat はシェル経由でしか起動できないため、そのときだけ引用符で囲む
const run = (cmd, args, cwd) => execFileSync(WINDOWS ? `"${cmd}"` : cmd, WINDOWS ? args.map((a) => `"${a}"`) : args,
  { cwd, encoding: 'utf8', shell: WINDOWS, maxBuffer: 64 * 1024 * 1024 })
const option = (name) => { const i = process.argv.indexOf(name); return i > 0 ? process.argv[i + 1] : undefined }

function mavenPackages() {
  const dir = join(root, 'spring-api')
  const gradlew = join(dir, WINDOWS ? 'gradlew.bat' : 'gradlew')
  const out = run(gradlew, ['dependencies', '--configuration', 'runtimeClasspath', '-q'], dir)
  const pkgs = new Map()
  for (const m of out.matchAll(/([\w.-]+):([\w.-]+)(?::([0-9][^\s]*))?(?: -> ([0-9][^\s]*))?/g)) {
    const version = m[4] ?? m[3]
    if (version) pkgs.set(`${m[1]}:${m[2]}`, version)
  }
  return [...pkgs].map(([name, version]) => ({ ecosystem: 'Maven', name, version }))
}

function npmPackages() {
  const lock = JSON.parse(readFileSync(join(root, 'frontend', 'package-lock.json'), 'utf8'))
  const pkgs = new Map()
  for (const [path, info] of Object.entries(lock.packages ?? {})) {
    if (!path || !info.version) continue
    const name = info.name ?? path.slice(path.lastIndexOf('node_modules/') + 'node_modules/'.length)
    pkgs.set(`${name}@${info.version}`, { ecosystem: 'npm', name, version: info.version, dev: !!info.dev })
  }
  return [...pkgs.values()]
}

// 推移的依存まで調べるため、インストール済みの環境から読む。環境が無ければ不完全な結果を出さずに止める
function pypiPackages() {
  const candidates = option('--python') ? [option('--python')]
    : ['.venv', '.review-venv'].map((v) => join(root, 'analysis-api', v, WINDOWS ? 'Scripts/python.exe' : 'bin/python'))
  const python = candidates.find(existsSync)
  if (!python) {
    console.error(`Python environment not found (${candidates.join(', ')}). Create analysis-api/.venv (README) or pass --python <interpreter>.`)
    process.exit(2)
  }
  const list = 'import importlib.metadata as m; print("\\n".join(f"{d.metadata[\'Name\']}=={d.version}" for d in m.distributions()))'
  return execFileSync(python, ['-c', list], { encoding: 'utf8' }).split('\n').map((l) => l.trim()).filter((l) => l.includes('=='))
    .map((l) => { const [name, version] = l.split('=='); return { ecosystem: 'PyPI', name, version } })
}

const post = async (url, body) =>
  (await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })).json()

const pypi = pypiPackages() // 環境が無いときは Gradle の収集より先に止める
const queries = [...mavenPackages(), ...npmPackages(), ...pypi]
const hits = []
for (let i = 0; i < queries.length; i += 500) {
  const chunk = queries.slice(i, i + 500)
  const res = await post('https://api.osv.dev/v1/querybatch', {
    queries: chunk.map((q) => ({ package: { ecosystem: q.ecosystem, name: q.name }, version: q.version })),
  })
  res.results.forEach((r, j) => { if (r.vulns?.length) hits.push({ ...chunk[j], ids: r.vulns.map((v) => v.id) }) })
}

const details = Object.fromEntries(await Promise.all([...new Set(hits.flatMap((h) => h.ids))]
  .map(async (id) => [id, await (await fetch(`https://api.osv.dev/v1/vulns/${id}`)).json()])))
const fixed = (vuln, name) => [...new Set((vuln.affected ?? []).filter((a) => a.package?.name === name)
  .flatMap((a) => (a.ranges ?? []).flatMap((r) => (r.events ?? []).filter((e) => e.fixed).map((e) => e.fixed))))].join(', ')
const report = hits.map((h) => ({
  ecosystem: h.ecosystem, name: h.name, version: h.version, dev: h.dev,
  vulns: h.ids.map((id) => ({
    id, cve: (details[id].aliases ?? []).filter((a) => a.startsWith('CVE')).join(','),
    severity: details[id].database_specific?.severity ?? '', published: details[id].published?.slice(0, 10),
    fixed: fixed(details[id], h.name), summary: (details[id].summary ?? '').slice(0, 120),
  })),
}))

const count = (eco) => queries.filter((q) => q.ecosystem === eco).length
console.log(`scanned ${queries.length} packages (Maven ${count('Maven')}, npm ${count('npm')}, PyPI ${count('PyPI')}); vulnerable ${report.length}`)
for (const r of report) {
  console.log(`\n[${r.ecosystem}] ${r.name}@${r.version}${r.dev ? ' (dev)' : ''}`)
  for (const v of r.vulns) console.log(`  ${v.id} ${v.cve} ${v.severity} ${v.published} fixed=${v.fixed} :: ${v.summary}`)
}
const jsonAt = process.argv.indexOf('--json')
if (jsonAt > 0) writeFileSync(process.argv[jsonAt + 1], JSON.stringify({ scannedAt: new Date().toISOString(), report }, null, 2))
