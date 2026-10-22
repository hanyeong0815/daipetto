// API単体テストの共通部品。実HTTP・実DB（使い捨てのPostgreSQL）に対してテストケースを実行し、
// ケースごとのエビデンス（リクエスト・レスポンス・DB確認）をJSONで残す。
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'

export const BASE = process.env.BASE ?? 'http://localhost:18081'
export const DB = process.env.DB ?? 'daipetto_ut'
export const PASSWORD = 'Password123!'

const psqlArgs = ['exec', '-i', 'daipetto-postgres', 'psql', '-U', process.env.PGUSER ?? 'daipetto_user', '-d', DB, '-t', '-A', '-v', 'ON_ERROR_STOP=1']
export const sql = (query) => execFileSync('docker', psqlArgs, { input: query, encoding: 'utf8' }).trim()

// JSTの日付・月（テストデータの日付をサーバーと同じ基準で作る）
export const todayJst = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Tokyo' }).format(new Date())
export const nowJst = () => new Date().toLocaleString('sv-SE', { timeZone: 'Asia/Tokyo' }).replace(' ', 'T') + '+09:00'
export function addDays(isoDate, days) {
  const d = new Date(`${isoDate}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}
export function addMonths(yearMonth, months) {
  const [y, m] = yearMonth.split('-').map(Number)
  const total = y * 12 + (m - 1) + months
  return `${Math.floor(total / 12)}-${String((total % 12) + 1).padStart(2, '0')}`
}

// エビデンスに秘密情報を残さない（パスワードは伏せ字、トークンは先頭だけ）。
// 文字列中のJWT（SQLの条件・DBの結果に含まれるものも）も先頭だけにする（Codex REVIEW-001 R-01）
const JWT = /eyJ[\w-]+\.[\w-]+\.[\w-]+/g
export const maskText = (s) => (typeof s === 'string' ? s.replace(JWT, (t) => `${t.slice(0, 12)}…(masked)`) : s)
function mask(value) {
  if (typeof value === 'string') return maskText(value)
  if (Array.isArray(value)) return value.map(mask)
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([k, v]) => {
      if (/password/i.test(k)) return [k, '********']
      if (/token/i.test(k) && typeof v === 'string') return [k, `${v.slice(0, 12)}…(masked)`]
      return [k, mask(v)]
    }))
  }
  return value
}

export async function http(method, path, { token, body, rawBody } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: rawBody ?? (body === undefined ? undefined : JSON.stringify(body)),
  })
  const text = await res.text()
  let json = null
  try { json = text ? JSON.parse(text) : null } catch { json = { raw: text } }
  return { status: res.status, body: json, code: json?.code ?? null, data: json?.data ?? null }
}

// 1ケース分の実行記録。call / db / check の結果を順番に残し、checkが1つでも失敗したらNG
export class CaseRun {
  constructor(actors) {
    this.actors = actors
    this.steps = []
    this.checks = []
  }

  async call(actor, method, path, options = {}) {
    // options.token があればそれを使う（準備で作ったユーザーなど）。'anon' は未認証
    const token = options.token ?? (actor === 'anon' ? undefined : this.actors[actor])
    const res = await http(method, path, { token, body: options.body, rawBody: options.rawBody })
    this.steps.push({
      type: 'http',
      actor,
      request: { method, path, ...(options.body !== undefined ? { body: mask(options.body) } : {}), ...(options.rawBody !== undefined ? { rawBody: options.rawBody } : {}) },
      response: { status: res.status, body: mask(res.body) },
    })
    return res
  }

  db(label, query) {
    const result = sql(query)
    // 実行するSQLはそのまま、記録だけを伏せる
    this.steps.push({ type: 'db', label, query: maskText(query.trim()), result: maskText(result) })
    return result
  }

  check(name, actual, expected) {
    const ok = typeof expected === 'function' ? Boolean(expected(actual)) : JSON.stringify(actual) === JSON.stringify(expected)
    this.checks.push({ name, expected: mask(typeof expected === 'function' ? expected.description ?? 'condition' : expected), actual: mask(actual), ok })
    return ok
  }

  // 一連の表の行数（書き込みが起きていないことの確認用）
  snapshot(label) {
    return this.db(label, "SELECT string_agg(t||'='||n, ',' ORDER BY t) FROM (SELECT 'hospitals' t, count(*) n FROM hospitals UNION ALL SELECT 'hospital_admins', count(*) FROM hospital_admins UNION ALL SELECT 'hospital_business_hours', count(*) FROM hospital_business_hours UNION ALL SELECT 'hospital_schedules', count(*) FROM hospital_schedules UNION ALL SELECT 'blocked_schedules', count(*) FROM hospital_schedules WHERE status='BLOCKED' UNION ALL SELECT 'reservations_not_requested', count(*) FROM reservations WHERE status<>'REQUESTED' UNION ALL SELECT 'notifications', count(*) FROM notifications UNION ALL SELECT 'suspended_users', count(*) FROM users WHERE status='SUSPENDED' UNION ALL SELECT 'admin_role_users', count(*) FROM users WHERE role<>'ROLE_USER') x;")
  }

  // 「ステータスとエラーコード」をまとめて確認する
  expectError(res, status, code) {
    this.check('HTTPステータス', res.status, status)
    this.check('エラーコード', res.code, code)
  }
}

// 条件に説明文を付ける（エビデンスの「期待値」欄に出す）
export const that = (description, predicate) => Object.assign(predicate, { description })

export function writeEvidence(dir, record) {
  mkdirSync(dir, { recursive: true })
  writeFileSync(`${dir}/${record.id}.json`, JSON.stringify(record, null, 2) + '\n')
}
