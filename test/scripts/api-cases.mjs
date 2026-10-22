// API単体テストのテストケース定義。期待結果は設計書（docs/07_API_Design）の記述を正とする。
// 各ケースは独立したデータを準備する（共通の利用者・病院だけを run-api-unit-tests.mjs で用意する）。
import { http, PASSWORD, todayJst, addDays, addMonths, that } from './lib.mjs'

export const FEATURES = [
  { code: 'UT-01', name: '会員・認証', doc: 'docs/07 §3・§4' },
  { code: 'UT-02', name: 'ペット', doc: 'docs/07 §5' },
  { code: 'UT-03', name: '健康記録', doc: 'docs/07 §6-1〜6-4' },
  { code: 'UT-04', name: 'ワクチン接種予定', doc: 'docs/07 §6-5〜6-11' },
  { code: 'UT-05', name: '病院', doc: 'docs/07 §8-1・8-2・11-1〜11-3・11-8' },
  { code: 'UT-06', name: '病院管理者の紐付け', doc: 'docs/07 §2-8・11-9〜11-11' },
  { code: 'UT-07', name: '診療時間（曜日別）', doc: 'docs/07 §8-7〜8-10' },
  { code: 'UT-08', name: '予約枠', doc: 'docs/07 §8-3〜8-6・8-11' },
  { code: 'UT-09', name: '予約（利用者）', doc: 'docs/07 §9-1〜9-4' },
  { code: 'UT-10', name: '予約管理（病院管理者）', doc: 'docs/07 §9-5〜9-8' },
  { code: 'UT-11', name: '通知', doc: 'docs/07 §10' },
  { code: 'UT-12', name: 'ユーザー管理（システム管理者）', doc: 'docs/07 §11-4〜11-7' },
  { code: 'UT-13', name: '共通（認証・入力・応答形式）', doc: 'docs/07 §2・docs/09' },
]

const TIME_WITH_OFFSET = that('JSTのオフセット付き（+09:00）', (v) => /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+09:00$/.test(v ?? ''))
const isNumber = that('数値のID', (v) => Number.isInteger(v) && v > 0)

// ctx: 共通の利用者のトークン・ID、病院ID、準備用の関数（エビデンスには残らない準備処理）
export function buildCases(ctx) {
  const today = todayJst()
  const future = (days) => addDays(today, days)
  let slotSeq = 0
  // 予約枠の時刻を重ならないようにずらす（同じ病院・日付で複数作るため）
  const nextSlotTime = () => {
    const minutes = 8 * 60 + (slotSeq++ % 40) * 15
    const hh = (m) => `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`
    return { startTime: hh(minutes), endTime: hh(minutes + 15) }
  }
  const prep = {
    async pet(actor, body = {}) {
      const res = await http('POST', '/api/v1/pets', { token: ctx.tokens[actor], body: { name: 'Momo', petType: 'CAT', neutered: false, ...body } })
      return res.data.petId
    },
    async slot(hospitalId = ctx.h1, days = 10) {
      const res = await http('POST', `/api/v1/admin/hospitals/${hospitalId}/schedules`, {
        token: ctx.tokens.admin, body: { availableDate: future(days), ...nextSlotTime() },
      })
      return res.data.scheduleId
    },
    async reservation(actor = 'tanaka', hospitalId = ctx.h1) {
      const petId = await prep.pet(actor)
      const scheduleId = await prep.slot(hospitalId)
      const res = await http('POST', '/api/v1/reservations', { token: ctx.tokens[actor], body: { petId, hospitalId, scheduleId, memo: '咳が続いています' } })
      return { reservationId: res.data.reservationId, petId, scheduleId }
    },
    async healthRecord(actor, petId, body = {}) {
      const res = await http('POST', `/api/v1/pets/${petId}/health-records`, { token: ctx.tokens[actor], body: { weight: 4.5, symptom: '咳', memo: 'memo', ...body } })
      return res.data.healthRecordId
    },
    async vaccination(actor, petId, body = {}) {
      const res = await http('POST', `/api/v1/pets/${petId}/vaccinations`, { token: ctx.tokens[actor], body: { vaccineName: '混合ワクチン', scheduledDate: future(14), ...body } })
      return res.data.vaccinationId
    },
    async user(prefix, role = 'ROLE_USER') {
      const email = `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@daipetto.test`
      await http('POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname: prefix } })
      ctx.sql(`UPDATE users SET role='${role}' WHERE email='${email}';`)
      const login = await http('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })
      const id = Number(ctx.sql(`SELECT id FROM users WHERE email='${email}';`))
      return { email, id, token: login.data.accessToken, refreshToken: login.data.refreshToken }
    },
    async hospital(name = 'テスト動物病院') {
      const res = await http('POST', '/api/v1/admin/hospitals', { token: ctx.tokens.admin, body: { name, address: '東京都千代田区' } })
      return res.data.hospitalId
    },
  }

  const cases = []
  const add = (c) => cases.push(c)

  // ================================================================ UT-01 会員・認証
  add({ id: 'UT-01-001', feature: 'UT-01', api: 'POST /api/v1/users', kind: '正常', viewpoint: '会員登録',
    condition: '未登録のメールアドレス・8文字以上のパスワード・ニックネーム', expected: '201、userIdを返す。権限ROLE_USER・状態ACTIVEで登録され、パスワードはハッシュで保存される',
    async run(t) {
      const email = `ut-new-${Date.now()}@daipetto.test`
      const res = await t.call('anon', 'POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname: 'NewUser' } })
      t.check('HTTPステータス', res.status, 201)
      t.check('userId', res.data?.userId, isNumber)
      const row = t.db('登録内容', `SELECT role||','||status||','||(password LIKE '$2%')::text FROM users WHERE email='${email}';`)
      t.check('権限・状態・パスワードのハッシュ化', row, 'ROLE_USER,ACTIVE,true')
    } })
  add({ id: 'UT-01-002', feature: 'UT-01', api: 'POST /api/v1/users', kind: '異常', viewpoint: 'メールアドレスの重複',
    condition: '登録済みのメールアドレス', expected: '409 USER-001',
    async run(t) {
      const res = await t.call('anon', 'POST', '/api/v1/users', { body: { email: ctx.emails.tanaka, password: PASSWORD, nickname: 'Dup' } })
      t.expectError(res, 409, 'USER-001')
    } })
  add({ id: 'UT-01-003', feature: 'UT-01', api: 'POST /api/v1/users', kind: '異常', viewpoint: 'パスワードの長さ不足',
    condition: '7文字のパスワード', expected: '400 VALIDATION-001（docs/07 §4-1）',
    async run(t) {
      const res = await t.call('anon', 'POST', '/api/v1/users', { body: { email: `ut-short-${Date.now()}@daipetto.test`, password: 'Pass12!', nickname: 'Short' } })
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-01-004', feature: 'UT-01', api: 'POST /api/v1/users', kind: '異常', viewpoint: 'メールアドレスの形式',
    condition: '「@」の無いメールアドレス', expected: '400 VALIDATION-001（docs/07 §4-1）',
    async run(t) {
      const res = await t.call('anon', 'POST', '/api/v1/users', { body: { email: 'not-an-email', password: PASSWORD, nickname: 'Bad' } })
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-01-005', feature: 'UT-01', api: 'POST /api/v1/auth/login', kind: '正常', viewpoint: 'ログイン',
    condition: '登録済みのメールアドレスと正しいパスワード', expected: '200、Access Token・Refresh Tokenを返し、有効なRefresh TokenがDBに1件だけ残る',
    async run(t) {
      const u = await prep.user('ut-login')
      const res = await t.call('anon', 'POST', '/api/v1/auth/login', { body: { email: u.email, password: PASSWORD } })
      t.check('HTTPステータス', res.status, 200)
      t.check('accessToken', typeof res.data?.accessToken, 'string')
      t.check('refreshToken', typeof res.data?.refreshToken, 'string')
      t.check('有効なRefresh Token数', t.db('有効なRefresh Token', `SELECT count(*) FROM refresh_tokens WHERE user_id=${u.id} AND revoked=false;`), '1')
    } })
  add({ id: 'UT-01-006', feature: 'UT-01', api: 'POST /api/v1/auth/login', kind: '異常', viewpoint: 'パスワードの誤り',
    condition: '登録済みのメールアドレスと誤ったパスワード', expected: '401 AUTH-001',
    async run(t) {
      const res = await t.call('anon', 'POST', '/api/v1/auth/login', { body: { email: ctx.emails.tanaka, password: 'WrongPass999!' } })
      t.expectError(res, 401, 'AUTH-001')
    } })
  add({ id: 'UT-01-007', feature: 'UT-01', api: 'POST /api/v1/auth/login', kind: '異常', viewpoint: '未登録のメールアドレス',
    condition: '登録されていないメールアドレス', expected: '401 AUTH-001（存在の有無を区別しない）',
    async run(t) {
      const res = await t.call('anon', 'POST', '/api/v1/auth/login', { body: { email: 'nobody@daipetto.test', password: PASSWORD } })
      t.expectError(res, 401, 'AUTH-001')
    } })
  add({ id: 'UT-01-008', feature: 'UT-01', api: 'POST /api/v1/auth/login', kind: '異常', viewpoint: '利用停止中のアカウント',
    condition: '状態SUSPENDEDのユーザー', expected: '403 AUTH-002、Tokenを発行しない',
    async run(t) {
      const u = await prep.user('ut-suspended')
      t.db('利用停止にする（準備）', `UPDATE users SET status='SUSPENDED' WHERE id=${u.id} RETURNING status;`)
      const res = await t.call('anon', 'POST', '/api/v1/auth/login', { body: { email: u.email, password: PASSWORD } })
      t.expectError(res, 403, 'AUTH-002')
    } })
  add({ id: 'UT-01-009', feature: 'UT-01', api: 'GET /api/v1/users/me', kind: '正常', viewpoint: 'マイページ取得',
    condition: 'ログイン中の一般ユーザー', expected: '200、userId・email・nickname・role・statusを返し、パスワードを含まない',
    async run(t) {
      const res = await t.call('tanaka', 'GET', '/api/v1/users/me')
      t.check('HTTPステータス', res.status, 200)
      t.check('本人の情報', [res.data?.userId, res.data?.email, res.data?.role, res.data?.status], [ctx.ids.tanaka, ctx.emails.tanaka, 'ROLE_USER', 'ACTIVE'])
      t.check('パスワードを含まない', 'password' in (res.data ?? {}), false)
    } })
  add({ id: 'UT-01-010', feature: 'UT-01', api: 'POST /api/v1/auth/refresh', kind: '正常', viewpoint: 'Token再発行（ローテーション）',
    condition: '有効なRefresh Token', expected: '200、新しいAccess Token・Refresh Tokenを返し、古いRefresh Tokenは失効する',
    async run(t) {
      const u = await prep.user('ut-refresh')
      const res = await t.call('anon', 'POST', '/api/v1/auth/refresh', { body: { refreshToken: u.refreshToken } })
      t.check('HTTPステータス', res.status, 200)
      t.check('新しいRefresh Token', res.data?.refreshToken !== u.refreshToken && typeof res.data?.refreshToken === 'string', true)
      t.check('古いRefresh Tokenの失効', t.db('古いTokenの状態', `SELECT revoked::text FROM refresh_tokens WHERE token='${u.refreshToken}';`), 'true')
      const again = await t.call('anon', 'POST', '/api/v1/auth/refresh', { body: { refreshToken: u.refreshToken } })
      t.check('古いTokenの再利用（UT-01-011と同観点）', again.code, 'AUTH-003')
    } })
  add({ id: 'UT-01-011', feature: 'UT-01', api: 'POST /api/v1/auth/refresh', kind: '異常', viewpoint: '無効なRefresh Token',
    condition: '形式の正しくない文字列', expected: '401 AUTH-003',
    async run(t) {
      const res = await t.call('anon', 'POST', '/api/v1/auth/refresh', { body: { refreshToken: 'invalid.refresh.token' } })
      t.check('HTTPステータス', res.status, 401)
      t.check('エラーコード（AUTH-003）', res.code, 'AUTH-003')
    } })
  add({ id: 'UT-01-012', feature: 'UT-01', api: 'POST /api/v1/auth/refresh', kind: '異常', viewpoint: '利用停止中ユーザーの再発行',
    condition: '有効なRefresh Tokenを持つユーザーをDBで利用停止にした状態', expected: '403 AUTH-002（docs/07 §3-2）',
    async run(t) {
      const u = await prep.user('ut-refresh-suspended')
      t.db('利用停止にする（準備）', `UPDATE users SET status='SUSPENDED' WHERE id=${u.id} RETURNING status;`)
      const res = await t.call('anon', 'POST', '/api/v1/auth/refresh', { body: { refreshToken: u.refreshToken } })
      t.expectError(res, 403, 'AUTH-002')
    } })
  add({ id: 'UT-01-013', feature: 'UT-01', api: 'POST /api/v1/auth/logout', kind: '正常', viewpoint: 'ログアウト',
    condition: 'ログイン中のユーザー（Authorization: Bearer <Access Token>、Request本文なし）', expected: '200、data.isLogout=true。そのユーザーの有効なRefresh Tokenがすべて失効し、以後の再発行はAUTH-003（docs/07 §3-3）',
    async run(t) {
      const u = await prep.user('ut-logout')
      const res = await t.call('準備したユーザー', 'POST', '/api/v1/auth/logout', { token: u.token })
      t.check('HTTPステータス', res.status, 200)
      t.check('data.isLogout', res.data?.isLogout, true)
      t.check('Refresh Tokenの失効', t.db('有効なRefresh Token', `SELECT count(*) FROM refresh_tokens WHERE user_id=${u.id} AND revoked=false;`), '0')
      const refresh = await t.call('anon', 'POST', '/api/v1/auth/refresh', { body: { refreshToken: u.refreshToken } })
      t.check('ログアウト後の再発行', refresh.code, 'AUTH-003')
    } })
  add({ id: 'UT-01-016', feature: 'UT-01', api: 'POST /api/v1/auth/logout', kind: '異常', viewpoint: '未認証でのログアウト',
    condition: 'Authorizationヘッダーなし', expected: '401 AUTH-001（docs/07 §3-3）',
    async run(t) {
      const res = await t.call('anon', 'POST', '/api/v1/auth/logout')
      t.expectError(res, 401, 'AUTH-001')
    } })
  add({ id: 'UT-01-014', feature: 'UT-01', api: 'GET /api/v1/users/me', kind: '異常', viewpoint: 'Refresh TokenをAccess Tokenとして使用',
    condition: 'Authorization: Bearer <Refresh Token>', expected: '401 AUTH-001（Token種別の取り違えを認証しない）',
    async run(t) {
      const u = await prep.user('ut-misuse')
      const res = await t.call('準備したユーザー（Refresh Token）', 'GET', '/api/v1/users/me', { token: u.refreshToken })
      t.expectError(res, 401, 'AUTH-001')
    } })
  add({ id: 'UT-01-015', feature: 'UT-01', api: 'GET /api/v1/users/me', kind: '異常', viewpoint: '改ざんしたAccess Token',
    condition: '署名部分を書き換えたAccess Token', expected: '401 AUTH-001',
    async run(t) {
      const tampered = ctx.tokens.tanaka.slice(0, -4) + (ctx.tokens.tanaka.endsWith('AAAA') ? 'BBBB' : 'AAAA')
      const res = await t.call('tanaka（改ざんしたToken）', 'GET', '/api/v1/users/me', { token: tampered })
      t.expectError(res, 401, 'AUTH-001')
    } })

  // ================================================================ UT-02 ペット
  add({ id: 'UT-02-001', feature: 'UT-02', api: 'POST /api/v1/pets', kind: '正常', viewpoint: 'ペット登録（全項目）',
    condition: '名前・種類・誕生日・性別・体重・品種・去勢/避妊・マイクロチップ番号', expected: '201、petIdを返し、全項目が保存される',
    async run(t) {
      const body = { name: 'Momo', petType: 'CAT', birthDate: '2022-04-01', gender: 'FEMALE', weight: 4.25, breed: 'スコティッシュフォールド', neutered: true, microchipNumber: '392141000123456' }
      const res = await t.call('tanaka', 'POST', '/api/v1/pets', { body })
      t.check('HTTPステータス', res.status, 201)
      t.check('petId', res.data?.petId, isNumber)
      const row = t.db('保存内容', `SELECT name||','||pet_type||','||birth_date||','||gender||','||weight||','||breed||','||neutered||','||microchip_number FROM pets WHERE id=${res.data?.petId};`)
      t.check('保存内容', row, 'Momo,CAT,2022-04-01,FEMALE,4.25,スコティッシュフォールド,true,392141000123456')
    } })
  add({ id: 'UT-02-002', feature: 'UT-02', api: 'POST /api/v1/pets', kind: '正常', viewpoint: '任意項目の省略',
    condition: '名前・種類だけ（性別・体重・品種などを省略）', expected: '201、省略した項目はNULL（性別は「不明」）、去勢/避妊はfalse',
    async run(t) {
      const res = await t.call('tanaka', 'POST', '/api/v1/pets', { body: { name: 'Kuro', petType: 'DOG' } })
      t.check('HTTPステータス', res.status, 201)
      const row = t.db('保存内容', `SELECT coalesce(gender,'NULL')||','||coalesce(weight::text,'NULL')||','||coalesce(breed,'NULL')||','||neutered FROM pets WHERE id=${res.data?.petId};`)
      t.check('省略項目', row, 'NULL,NULL,NULL,false')
    } })
  add({ id: 'UT-02-003', feature: 'UT-02', api: 'POST /api/v1/pets', kind: '異常', viewpoint: '名前の必須・長さ',
    condition: '名前なし／51文字の名前', expected: 'いずれも400 VALIDATION-001',
    async run(t) {
      const blank = await t.call('tanaka', 'POST', '/api/v1/pets', { body: { name: '', petType: 'CAT' } })
      t.expectError(blank, 400, 'VALIDATION-001')
      const long = await t.call('tanaka', 'POST', '/api/v1/pets', { body: { name: 'あ'.repeat(51), petType: 'CAT' } })
      t.expectError(long, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-02-004', feature: 'UT-02', api: 'POST /api/v1/pets', kind: '異常', viewpoint: '体重の範囲・桁数',
    condition: '体重 -1／体重 1000（整数部4桁）', expected: 'いずれも400 VALIDATION-001',
    async run(t) {
      const negative = await t.call('tanaka', 'POST', '/api/v1/pets', { body: { name: 'Momo', petType: 'CAT', weight: -1 } })
      t.expectError(negative, 400, 'VALIDATION-001')
      const tooLarge = await t.call('tanaka', 'POST', '/api/v1/pets', { body: { name: 'Momo', petType: 'CAT', weight: 1000 } })
      t.expectError(tooLarge, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-02-005', feature: 'UT-02', api: 'POST /api/v1/pets', kind: '異常', viewpoint: '種類（enum）の不正値',
    condition: 'petType = "DRAGON"（許容値 CAT/DOG/RABBIT/HAMSTER/BIRD/OTHER 以外）', expected: '400 VALIDATION-001（500にしない）',
    async run(t) {
      const res = await t.call('tanaka', 'POST', '/api/v1/pets', { body: { name: 'Piyo', petType: 'DRAGON' } })
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-02-006', feature: 'UT-02', api: 'GET /api/v1/pets', kind: '正常', viewpoint: 'ペット一覧',
    condition: '本人と他人がそれぞれペットを持つ', expected: '200、本人のペットだけを返す（他人のペットを含まない）',
    async run(t) {
      const mine = await prep.pet('tanaka', { name: 'Mine' })
      const others = await prep.pet('sato', { name: 'Others' })
      const res = await t.call('tanaka', 'GET', '/api/v1/pets')
      t.check('HTTPステータス', res.status, 200)
      const ids = (res.data ?? []).map((p) => p.id)
      t.check('本人のペットを含む', ids.includes(mine), true)
      t.check('他人のペットを含まない', ids.includes(others), false)
    } })
  add({ id: 'UT-02-007', feature: 'UT-02', api: 'GET /api/v1/pets/{petId}', kind: '正常', viewpoint: 'ペット詳細',
    condition: '本人のペット', expected: '200、全項目とcreatedAt（+09:00）を返す',
    async run(t) {
      const petId = await prep.pet('tanaka', { breed: '柴', gender: 'MALE', weight: 9.5 })
      const res = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}`)
      t.check('HTTPステータス', res.status, 200)
      t.check('項目', [res.data?.id, res.data?.breed, res.data?.gender, res.data?.weight], [petId, '柴', 'MALE', 9.5])
      t.check('createdAt', res.data?.createdAt, TIME_WITH_OFFSET)
    } })
  add({ id: 'UT-02-008', feature: 'UT-02', api: 'GET /api/v1/pets/{petId}', kind: '異常', viewpoint: '他人のペットの詳細',
    condition: '他のユーザーのペットID', expected: '401 PET-002（飼い主が異なる）',
    async run(t) {
      const petId = await prep.pet('sato')
      const res = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}`)
      t.expectError(res, 401, 'PET-002')
    } })
  add({ id: 'UT-02-009', feature: 'UT-02', api: 'GET /api/v1/pets/{petId}', kind: '異常', viewpoint: '存在しないペット',
    condition: '存在しないペットID', expected: '404 PET-001',
    async run(t) {
      const res = await t.call('tanaka', 'GET', '/api/v1/pets/99999999')
      t.expectError(res, 404, 'PET-001')
    } })
  add({ id: 'UT-02-010', feature: 'UT-02', api: 'PATCH /api/v1/pets/{petId}', kind: '正常', viewpoint: 'ペット更新（全項目の置き換え）',
    condition: '品種・体重ありのペットを、品種・体重を省略して更新', expected: '200、送った項目は更新され、省略した任意項目はNULLになる（docs/07 §5-4）',
    async run(t) {
      const petId = await prep.pet('tanaka', { breed: '柴', weight: 9.5 })
      const res = await t.call('tanaka', 'PATCH', `/api/v1/pets/${petId}`, { body: { name: 'Renamed', petType: 'DOG', neutered: true } })
      t.check('HTTPステータス', res.status, 200)
      const row = t.db('更新後', `SELECT name||','||pet_type||','||coalesce(breed,'NULL')||','||coalesce(weight::text,'NULL')||','||neutered FROM pets WHERE id=${petId};`)
      t.check('更新後の内容', row, 'Renamed,DOG,NULL,NULL,true')
    } })
  add({ id: 'UT-02-011', feature: 'UT-02', api: 'PATCH /api/v1/pets/{petId}', kind: '異常', viewpoint: '他人のペットの更新',
    condition: '他のユーザーのペットID', expected: '401 PET-002、変更されない',
    async run(t) {
      const petId = await prep.pet('sato', { name: 'Original' })
      const res = await t.call('tanaka', 'PATCH', `/api/v1/pets/${petId}`, { body: { name: 'Hijacked', petType: 'CAT' } })
      t.expectError(res, 401, 'PET-002')
      t.check('変更されない', t.db('名前', `SELECT name FROM pets WHERE id=${petId};`), 'Original')
    } })
  add({ id: 'UT-02-012', feature: 'UT-02', api: 'DELETE /api/v1/pets/{petId}', kind: '正常', viewpoint: 'ペット削除（論理削除）',
    condition: '本人のペット', expected: '200、deleted_atが設定され、一覧に出ず、詳細は404 PET-001',
    async run(t) {
      const petId = await prep.pet('tanaka', { name: 'ToDelete' })
      const res = await t.call('tanaka', 'DELETE', `/api/v1/pets/${petId}`)
      t.check('HTTPステータス', res.status, 200)
      t.check('論理削除', t.db('deleted_at', `SELECT (deleted_at IS NOT NULL)::text FROM pets WHERE id=${petId};`), 'true')
      const list = await t.call('tanaka', 'GET', '/api/v1/pets')
      t.check('一覧に出ない', (list.data ?? []).some((p) => p.id === petId), false)
      const detail = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}`)
      t.check('詳細', detail.code, 'PET-001')
    } })
  add({ id: 'UT-02-013', feature: 'UT-02', api: 'DELETE /api/v1/pets/{petId}', kind: '異常', viewpoint: '他人のペットの削除',
    condition: '他のユーザーのペットID', expected: '401 PET-002、削除されない',
    async run(t) {
      const petId = await prep.pet('sato')
      const res = await t.call('tanaka', 'DELETE', `/api/v1/pets/${petId}`)
      t.expectError(res, 401, 'PET-002')
      t.check('削除されない', t.db('deleted_at', `SELECT (deleted_at IS NULL)::text FROM pets WHERE id=${petId};`), 'true')
    } })

  // ================================================================ UT-03 健康記録
  add({ id: 'UT-03-001', feature: 'UT-03', api: 'POST /api/v1/pets/{petId}/health-records', kind: '正常', viewpoint: '健康記録の登録（記録日省略）',
    condition: '体重・症状・メモ、recordedDate省略', expected: '201、healthRecordIdを返し、記録日は当日（JST）',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const res = await t.call('tanaka', 'POST', `/api/v1/pets/${petId}/health-records`, { body: { weight: 4.3, symptom: '咳', memo: '夜に咳' } })
      t.check('HTTPステータス', res.status, 201)
      t.check('healthRecordId', res.data?.healthRecordId, isNumber)
      t.check('記録日（JST当日）', t.db('recorded_date', `SELECT recorded_date FROM health_records WHERE id=${res.data?.healthRecordId};`), today)
    } })
  add({ id: 'UT-03-002', feature: 'UT-03', api: 'POST /api/v1/pets/{petId}/health-records', kind: '正常', viewpoint: '過去日の記録',
    condition: 'recordedDate = 30日前', expected: '201、指定した記録日で保存される',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const res = await t.call('tanaka', 'POST', `/api/v1/pets/${petId}/health-records`, { body: { weight: 4.1, recordedDate: addDays(today, -30) } })
      t.check('HTTPステータス', res.status, 201)
      t.check('記録日', t.db('recorded_date', `SELECT recorded_date FROM health_records WHERE id=${res.data?.healthRecordId};`), addDays(today, -30))
    } })
  add({ id: 'UT-03-003', feature: 'UT-03', api: 'POST /api/v1/pets/{petId}/health-records', kind: '異常', viewpoint: '体重の不正値',
    condition: 'weight = -0.5', expected: '400 VALIDATION-001',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const res = await t.call('tanaka', 'POST', `/api/v1/pets/${petId}/health-records`, { body: { weight: -0.5 } })
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-03-004', feature: 'UT-03', api: 'POST /api/v1/pets/{petId}/health-records', kind: '異常', viewpoint: '他人のペットへの登録',
    condition: '他のユーザーのペットID', expected: '403 HEALTH-002',
    async run(t) {
      const petId = await prep.pet('sato')
      const res = await t.call('tanaka', 'POST', `/api/v1/pets/${petId}/health-records`, { body: { weight: 4 } })
      t.expectError(res, 403, 'HEALTH-002')
    } })
  add({ id: 'UT-03-005', feature: 'UT-03', api: 'GET /api/v1/pets/{petId}/health-records', kind: '正常', viewpoint: '健康記録一覧の並び順',
    condition: '記録日の異なる3件', expected: '200、記録日の降順',
    async run(t) {
      const petId = await prep.pet('tanaka')
      for (const d of [-10, -1, -5]) await prep.healthRecord('tanaka', petId, { recordedDate: addDays(today, d) })
      const res = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}/health-records`)
      t.check('HTTPステータス', res.status, 200)
      t.check('記録日の降順', (res.data ?? []).map((r) => r.recordedDate), [addDays(today, -1), addDays(today, -5), addDays(today, -10)])
    } })
  add({ id: 'UT-03-006', feature: 'UT-03', api: 'GET /api/v1/pets/{petId}/health-records', kind: '異常', viewpoint: '他人のペットの一覧',
    condition: '他のユーザーのペットID', expected: '403 HEALTH-002',
    async run(t) {
      const petId = await prep.pet('sato')
      const res = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}/health-records`)
      t.expectError(res, 403, 'HEALTH-002')
    } })
  add({ id: 'UT-03-007', feature: 'UT-03', api: 'PATCH /api/v1/health-records/{healthRecordId}', kind: '正常', viewpoint: '部分更新',
    condition: '体重4.5・症状「咳」の記録に、症状だけを送る', expected: '200、症状だけ変わり、体重は維持される',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.healthRecord('tanaka', petId, { weight: 4.5, symptom: '咳' })
      const res = await t.call('tanaka', 'PATCH', `/api/v1/health-records/${id}`, { body: { symptom: 'くしゃみ' } })
      t.check('HTTPステータス', res.status, 200)
      t.check('更新後', t.db('更新後', `SELECT weight||','||symptom FROM health_records WHERE id=${id};`), '4.50,くしゃみ')
    } })
  add({ id: 'UT-03-008', feature: 'UT-03', api: 'PATCH /api/v1/health-records/{healthRecordId}', kind: '異常', viewpoint: '他人の記録の更新',
    condition: '他のユーザーのペットの記録', expected: '403 HEALTH-002、変更されない',
    async run(t) {
      const petId = await prep.pet('sato')
      const id = await prep.healthRecord('sato', petId, { symptom: '元の症状' })
      const res = await t.call('tanaka', 'PATCH', `/api/v1/health-records/${id}`, { body: { symptom: '書き換え' } })
      t.expectError(res, 403, 'HEALTH-002')
      t.check('変更されない', t.db('symptom', `SELECT symptom FROM health_records WHERE id=${id};`), '元の症状')
    } })
  add({ id: 'UT-03-009', feature: 'UT-03', api: 'PATCH /api/v1/health-records/{healthRecordId}', kind: '異常', viewpoint: '存在しない記録の更新',
    condition: '存在しない健康記録ID', expected: '404 HEALTH-001',
    async run(t) {
      const res = await t.call('tanaka', 'PATCH', '/api/v1/health-records/99999999', { body: { symptom: 'x' } })
      t.expectError(res, 404, 'HEALTH-001')
    } })
  add({ id: 'UT-03-010', feature: 'UT-03', api: 'DELETE /api/v1/health-records/{healthRecordId}', kind: '正常', viewpoint: '削除（論理削除）',
    condition: '本人のペットの記録', expected: '200、一覧に出なくなり、もう一度削除すると404 HEALTH-001',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.healthRecord('tanaka', petId)
      const res = await t.call('tanaka', 'DELETE', `/api/v1/health-records/${id}`)
      t.check('HTTPステータス', res.status, 200)
      const list = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}/health-records`)
      t.check('一覧に出ない', (list.data ?? []).length, 0)
      const again = await t.call('tanaka', 'DELETE', `/api/v1/health-records/${id}`)
      t.expectError(again, 404, 'HEALTH-001')
    } })
  add({ id: 'UT-03-011', feature: 'UT-03', api: 'DELETE /api/v1/health-records/{healthRecordId}', kind: '異常', viewpoint: '他人の記録の削除',
    condition: '他のユーザーのペットの記録', expected: '403 HEALTH-002、削除されない',
    async run(t) {
      const petId = await prep.pet('sato')
      const id = await prep.healthRecord('sato', petId)
      const res = await t.call('tanaka', 'DELETE', `/api/v1/health-records/${id}`)
      t.expectError(res, 403, 'HEALTH-002')
      t.check('削除されない', t.db('deleted_at', `SELECT (deleted_at IS NULL)::text FROM health_records WHERE id=${id};`), 'true')
    } })

  add({ id: 'UT-03-012', feature: 'UT-03', api: 'PATCH /api/v1/health-records/{healthRecordId}', kind: '異常', viewpoint: '更新時の体重の不正値（Codex G-03）',
    condition: '本人の記録に weight = -0.01／weight = 1000（整数部4桁）', expected: 'いずれも400 VALIDATION-001、記録は変更されない',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.healthRecord('tanaka', petId, { weight: 4.5 })
      t.expectError(await t.call('tanaka', 'PATCH', `/api/v1/health-records/${id}`, { body: { weight: -0.01 } }), 400, 'VALIDATION-001')
      t.expectError(await t.call('tanaka', 'PATCH', `/api/v1/health-records/${id}`, { body: { weight: 1000 } }), 400, 'VALIDATION-001')
      t.check('変更されない', t.db('weight', `SELECT weight FROM health_records WHERE id=${id};`), '4.50')
    } })

  // ================================================================ UT-04 ワクチン接種予定
  add({ id: 'UT-04-014', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/reschedule', kind: '異常', viewpoint: '存在しない予定の再登録（Codex G-04）',
    condition: '存在しない接種予定ID、正しい接種予定日', expected: '404 VACCINATION-001',
    async run(t) {
      const res = await t.call('tanaka', 'PATCH', '/api/v1/vaccinations/99999999/reschedule', { body: { scheduledDate: future(30) } })
      t.expectError(res, 404, 'VACCINATION-001')
    } })
  add({ id: 'UT-04-001', feature: 'UT-04', api: 'POST /api/v1/pets/{petId}/vaccinations', kind: '正常', viewpoint: '接種予定の登録',
    condition: 'ワクチン名・接種予定日', expected: '201、vaccinationIdを返し、状態SCHEDULEDで保存される',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const res = await t.call('tanaka', 'POST', `/api/v1/pets/${petId}/vaccinations`, { body: { vaccineName: '狂犬病', scheduledDate: future(20) } })
      t.check('HTTPステータス', res.status, 201)
      t.check('状態', t.db('status', `SELECT status FROM vaccinations WHERE id=${res.data?.vaccinationId};`), 'SCHEDULED')
    } })
  add({ id: 'UT-04-002', feature: 'UT-04', api: 'POST /api/v1/pets/{petId}/vaccinations', kind: '異常', viewpoint: '入力チェック',
    condition: 'ワクチン名が空／101文字／接種予定日なし', expected: 'いずれも400 VALIDATION-001',
    async run(t) {
      const petId = await prep.pet('tanaka')
      for (const body of [{ vaccineName: '', scheduledDate: future(5) }, { vaccineName: 'あ'.repeat(101), scheduledDate: future(5) }, { vaccineName: '混合' }]) {
        const res = await t.call('tanaka', 'POST', `/api/v1/pets/${petId}/vaccinations`, { body })
        t.expectError(res, 400, 'VALIDATION-001')
      }
    } })
  add({ id: 'UT-04-003', feature: 'UT-04', api: 'POST /api/v1/pets/{petId}/vaccinations', kind: '異常', viewpoint: '他人のペットへの登録',
    condition: '他のユーザーのペットID', expected: '403 VACCINATION-002',
    async run(t) {
      const petId = await prep.pet('sato')
      const res = await t.call('tanaka', 'POST', `/api/v1/pets/${petId}/vaccinations`, { body: { vaccineName: '混合', scheduledDate: future(5) } })
      t.expectError(res, 403, 'VACCINATION-002')
    } })
  add({ id: 'UT-04-004', feature: 'UT-04', api: 'GET /api/v1/pets/{petId}/vaccinations', kind: '正常', viewpoint: '一覧の並び順',
    condition: '接種予定日の異なる3件', expected: '200、接種予定日の昇順',
    async run(t) {
      const petId = await prep.pet('tanaka')
      for (const d of [30, 10, 20]) await prep.vaccination('tanaka', petId, { scheduledDate: future(d) })
      const res = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}/vaccinations`)
      t.check('HTTPステータス', res.status, 200)
      t.check('接種予定日の昇順', (res.data ?? []).map((v) => v.scheduledDate), [future(10), future(20), future(30)])
    } })
  add({ id: 'UT-04-005', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/complete', kind: '正常', viewpoint: '接種完了（完了日省略）',
    condition: 'SCHEDULEDの予定、Request本文なし', expected: '200、COMPLETED、完了日は当日（JST）',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/complete`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態・完了日', t.db('状態', `SELECT status||','||completed_date FROM vaccinations WHERE id=${id};`), `COMPLETED,${today}`)
    } })
  add({ id: 'UT-04-006', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/complete', kind: '異常', viewpoint: '未来日の完了日',
    condition: 'completedDate = 明日', expected: '400 VALIDATION-001',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/complete`, { body: { completedDate: future(1) } })
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-04-007', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/complete', kind: '異常', viewpoint: '不正な状態遷移（完了済みの完了）',
    condition: 'COMPLETEDの予定', expected: '409 VACCINATION-003',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/complete`)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/complete`)
      t.expectError(res, 409, 'VACCINATION-003')
    } })
  add({ id: 'UT-04-008', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/cancel', kind: '正常', viewpoint: '接種予定の中止',
    condition: 'SCHEDULEDの予定', expected: '200、CANCELLED',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/cancel`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態', t.db('status', `SELECT status FROM vaccinations WHERE id=${id};`), 'CANCELLED')
    } })
  add({ id: 'UT-04-009', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/cancel', kind: '異常', viewpoint: '不正な状態遷移（完了済みの中止）',
    condition: 'COMPLETEDの予定', expected: '409 VACCINATION-003',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/complete`)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/cancel`)
      t.expectError(res, 409, 'VACCINATION-003')
    } })
  add({ id: 'UT-04-010', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/reschedule', kind: '正常', viewpoint: '中止した予定の再登録',
    condition: 'CANCELLEDの予定、新しい接種予定日', expected: '200、SCHEDULEDに戻り、新しい予定日になる',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/cancel`)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/reschedule`, { body: { scheduledDate: future(40) } })
      t.check('HTTPステータス', res.status, 200)
      t.check('状態・予定日', t.db('状態', `SELECT status||','||scheduled_date FROM vaccinations WHERE id=${id};`), `SCHEDULED,${future(40)}`)
    } })
  add({ id: 'UT-04-011', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/reschedule', kind: '異常', viewpoint: '不正な状態遷移（予定中の再登録）',
    condition: 'SCHEDULEDの予定', expected: '409 VACCINATION-003',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/reschedule`, { body: { scheduledDate: future(40) } })
      t.expectError(res, 409, 'VACCINATION-003')
    } })
  add({ id: 'UT-04-012', feature: 'UT-04', api: 'PATCH /api/v1/vaccinations/{vaccinationId}/complete', kind: '異常', viewpoint: '他人の予定の操作',
    condition: '他のユーザーのペットの予定', expected: '403 VACCINATION-002、変更されない',
    async run(t) {
      const petId = await prep.pet('sato')
      const id = await prep.vaccination('sato', petId)
      const res = await t.call('tanaka', 'PATCH', `/api/v1/vaccinations/${id}/complete`)
      t.expectError(res, 403, 'VACCINATION-002')
      t.check('変更されない', t.db('status', `SELECT status FROM vaccinations WHERE id=${id};`), 'SCHEDULED')
    } })
  add({ id: 'UT-04-013', feature: 'UT-04', api: 'DELETE /api/v1/vaccinations/{vaccinationId}', kind: '正常', viewpoint: '接種予定の削除',
    condition: '本人のペットの予定', expected: '200、一覧に出なくなり、もう一度削除すると404 VACCINATION-001',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const id = await prep.vaccination('tanaka', petId)
      const res = await t.call('tanaka', 'DELETE', `/api/v1/vaccinations/${id}`)
      t.check('HTTPステータス', res.status, 200)
      const list = await t.call('tanaka', 'GET', `/api/v1/pets/${petId}/vaccinations`)
      t.check('一覧に出ない', (list.data ?? []).length, 0)
      const again = await t.call('tanaka', 'DELETE', `/api/v1/vaccinations/${id}`)
      t.expectError(again, 404, 'VACCINATION-001')
    } })

  // ================================================================ UT-05 病院
  add({ id: 'UT-05-001', feature: 'UT-05', api: 'POST /api/v1/admin/hospitals', kind: '正常', viewpoint: '病院登録（システム管理者）',
    condition: '名前・住所・電話番号', expected: '201、hospitalIdを返し、状態ACTIVEで登録される',
    async run(t) {
      const res = await t.call('admin', 'POST', '/api/v1/admin/hospitals', { body: { name: 'さくら動物病院', address: '東京都渋谷区', phoneNumber: '03-1234-5678' } })
      t.check('HTTPステータス', res.status, 201)
      t.check('状態', t.db('status', `SELECT status FROM hospitals WHERE id=${res.data?.hospitalId};`), 'ACTIVE')
    } })
  add({ id: 'UT-05-002', feature: 'UT-05', api: 'POST /api/v1/admin/hospitals', kind: '異常', viewpoint: '必須項目なし',
    condition: '病院名なし', expected: '400 VALIDATION-001',
    async run(t) {
      const res = await t.call('admin', 'POST', '/api/v1/admin/hospitals', { body: { name: '', address: '東京都' } })
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-05-003', feature: 'UT-05', api: 'POST /api/v1/admin/hospitals', kind: '異常', viewpoint: '権限（病院管理者による登録）',
    condition: 'ROLE_HOSPITAL_ADMIN', expected: '403 AUTH-005',
    async run(t) {
      const res = await t.call('vet', 'POST', '/api/v1/admin/hospitals', { body: { name: 'X', address: 'Y' } })
      t.expectError(res, 403, 'AUTH-005')
    } })
  add({ id: 'UT-05-004', feature: 'UT-05', api: 'GET /api/v1/hospitals', kind: '正常', viewpoint: '病院一覧（検索条件・停止中の除外）',
    condition: '名前・住所の異なる営業中の病院と停止中の病院', expected: '200、営業中の病院だけを返し、keyword（病院名）・area（住所）で絞り込める',
    async run(t) {
      const tag = `検索${Date.now() % 100000}`
      const a = await prep.hospital(`${tag}A動物病院`)
      const b = await prep.hospital(`${tag}B動物病院`)
      await http('PATCH', `/api/v1/admin/hospitals/${b}`, { token: ctx.tokens.admin, body: { name: `${tag}B動物病院`, address: '大阪府大阪市' } })
      const suspended = await prep.hospital(`${tag}停止動物病院`)
      await http('PATCH', `/api/v1/admin/hospitals/${suspended}/suspend`, { token: ctx.tokens.admin })
      const byKeyword = await t.call('tanaka', 'GET', `/api/v1/hospitals?keyword=${encodeURIComponent(tag)}`)
      t.check('HTTPステータス', byKeyword.status, 200)
      t.check('keyword一致かつ営業中だけ', (byKeyword.data ?? []).map((h) => h.id).sort(), [a, b].sort())
      const byArea = await t.call('tanaka', 'GET', `/api/v1/hospitals?keyword=${encodeURIComponent(tag)}&area=${encodeURIComponent('大阪')}`)
      t.check('areaで絞り込み', (byArea.data ?? []).map((h) => h.id), [b])
    } })
  add({ id: 'UT-05-005', feature: 'UT-05', api: 'GET /api/v1/hospitals/{hospitalId}', kind: '正常', viewpoint: '病院詳細',
    condition: '登録済みの病院', expected: '200、名前・住所・状態・createdAt（+09:00）を返す',
    async run(t) {
      const res = await t.call('tanaka', 'GET', `/api/v1/hospitals/${ctx.h1}`)
      t.check('HTTPステータス', res.status, 200)
      t.check('id・状態', [res.data?.id, res.data?.status], [ctx.h1, 'ACTIVE'])
      t.check('createdAt', res.data?.createdAt, TIME_WITH_OFFSET)
    } })
  add({ id: 'UT-05-006', feature: 'UT-05', api: 'GET /api/v1/hospitals/{hospitalId}', kind: '異常', viewpoint: '存在しない病院',
    condition: '存在しない病院ID', expected: '404 HOSPITAL-001',
    async run(t) {
      const res = await t.call('tanaka', 'GET', '/api/v1/hospitals/99999999')
      t.expectError(res, 404, 'HOSPITAL-001')
    } })
  add({ id: 'UT-05-007', feature: 'UT-05', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}', kind: '正常', viewpoint: '病院更新（紐付いた病院管理者）',
    condition: '病院管理者が紐付いた病院の名前・住所・電話番号を変更', expected: '200、情報列だけが更新され、状態は変わらない',
    async run(t) {
      const h = await prep.hospital('更新前動物病院')
      await http('POST', `/api/v1/admin/hospitals/${h}/admins`, { token: ctx.tokens.admin, body: { userId: ctx.ids.vet } })
      const res = await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${h}`, { body: { name: '更新後動物病院', address: '東京都港区', phoneNumber: '03-0000-0000' } })
      t.check('HTTPステータス', res.status, 200)
      t.check('更新後', t.db('更新後', `SELECT name||','||address||','||phone_number||','||status FROM hospitals WHERE id=${h};`), '更新後動物病院,東京都港区,03-0000-0000,ACTIVE')
    } })
  add({ id: 'UT-05-008', feature: 'UT-05', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}', kind: '異常', viewpoint: '紐付いていない病院の更新',
    condition: '病院管理者が紐付いていない病院', expected: '403 HOSPITAL-005、変更されない',
    async run(t) {
      const res = await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${ctx.h2}`, { body: { name: '乗っ取り', address: 'X' } })
      t.expectError(res, 403, 'HOSPITAL-005')
      t.check('変更されない', t.db('name', `SELECT (name <> '乗っ取り')::text FROM hospitals WHERE id=${ctx.h2};`), 'true')
    } })
  add({ id: 'UT-05-009', feature: 'UT-05', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}/suspend', kind: '正常', viewpoint: '病院停止（システム管理者）',
    condition: '営業中の病院', expected: '200、SUSPENDEDになり、一般向けの一覧から消え、詳細は状態SUSPENDEDで取得できる',
    async run(t) {
      const name = `停止対象${Date.now() % 100000}`
      const h = await prep.hospital(name)
      const res = await t.call('admin', 'PATCH', `/api/v1/admin/hospitals/${h}/suspend`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態', t.db('status', `SELECT status FROM hospitals WHERE id=${h};`), 'SUSPENDED')
      const list = await t.call('tanaka', 'GET', `/api/v1/hospitals?keyword=${encodeURIComponent(name)}`)
      t.check('一覧に出ない', (list.data ?? []).length, 0)
      const detail = await t.call('tanaka', 'GET', `/api/v1/hospitals/${h}`)
      t.check('詳細の状態', detail.data?.status, 'SUSPENDED')
    } })
  add({ id: 'UT-05-010', feature: 'UT-05', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}/suspend', kind: '異常', viewpoint: '権限・存在チェック',
    condition: '病院管理者による停止／存在しない病院の停止（システム管理者）', expected: '403 AUTH-005／404 HOSPITAL-001',
    async run(t) {
      const byVet = await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${ctx.h1}/suspend`)
      t.expectError(byVet, 403, 'AUTH-005')
      const missing = await t.call('admin', 'PATCH', '/api/v1/admin/hospitals/99999999/suspend')
      t.expectError(missing, 404, 'HOSPITAL-001')
    } })
  add({ id: 'UT-05-011', feature: 'UT-05', api: 'GET /api/v1/admin/hospitals/mine', kind: '正常', viewpoint: '管理対象の病院一覧',
    condition: 'システム管理者／病院1だけに紐付いた病院管理者', expected: '200。システム管理者は停止中を含む全病院、病院管理者は紐付いた病院だけ',
    async run(t) {
      const sys = await t.call('admin', 'GET', '/api/v1/admin/hospitals/mine')
      t.check('HTTPステータス', sys.status, 200)
      const all = Number(t.db('論理削除されていない病院数', 'SELECT count(*) FROM hospitals WHERE deleted_at IS NULL;'))
      t.check('システム管理者は全病院', (sys.data ?? []).length, all)
      t.check('停止中も含む', (sys.data ?? []).some((h) => h.status === 'SUSPENDED'), true)
      const vet = await t.call('vet2', 'GET', '/api/v1/admin/hospitals/mine')
      const linked = t.db('紐付いた病院', `SELECT coalesce(string_agg(hospital_id::text, ',' ORDER BY hospital_id), '') FROM hospital_admins WHERE user_id=${ctx.ids.vet2};`)
      t.check('病院管理者は紐付いた病院だけ', (vet.data ?? []).map((h) => h.hospitalId).join(','), linked)
    } })

  // ================================================================ UT-06 病院管理者の紐付け
  add({ id: 'UT-06-001', feature: 'UT-06', api: 'POST /api/v1/admin/hospitals/{hospitalId}/admins', kind: '正常', viewpoint: '紐付け',
    condition: 'ROLE_HOSPITAL_ADMINのユーザー', expected: '201、hospital_adminsに1件登録される',
    async run(t) {
      const h = await prep.hospital()
      const res = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/admins`, { body: { userId: ctx.ids.vet2 } })
      t.check('HTTPステータス', res.status, 201)
      t.check('紐付け', t.db('件数', `SELECT count(*) FROM hospital_admins WHERE user_id=${ctx.ids.vet2} AND hospital_id=${h};`), '1')
    } })
  add({ id: 'UT-06-002', feature: 'UT-06', api: 'POST /api/v1/admin/hospitals/{hospitalId}/admins', kind: '異常', viewpoint: '紐付けの重複',
    condition: '既に紐付いているユーザー', expected: '409 HOSPITAL-007',
    async run(t) {
      const res = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/admins`, { body: { userId: ctx.ids.vet } })
      t.expectError(res, 409, 'HOSPITAL-007')
    } })
  add({ id: 'UT-06-003', feature: 'UT-06', api: 'POST /api/v1/admin/hospitals/{hospitalId}/admins', kind: '異常', viewpoint: '病院管理者権限のないユーザー',
    condition: 'ROLE_USERのユーザー', expected: '400 HOSPITAL-006',
    async run(t) {
      const res = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/admins`, { body: { userId: ctx.ids.tanaka } })
      t.expectError(res, 400, 'HOSPITAL-006')
    } })
  add({ id: 'UT-06-004', feature: 'UT-06', api: 'POST /api/v1/admin/hospitals/{hospitalId}/admins', kind: '異常', viewpoint: '存在しない病院・ユーザー、userIdなし',
    condition: '存在しない病院／存在しないユーザー／Request本文が{}', expected: '404 HOSPITAL-001／404 USER-003／400 VALIDATION-001',
    async run(t) {
      const noHospital = await t.call('admin', 'POST', '/api/v1/admin/hospitals/99999999/admins', { body: { userId: ctx.ids.vet2 } })
      t.expectError(noHospital, 404, 'HOSPITAL-001')
      const noUser = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/admins`, { body: { userId: 99999999 } })
      t.expectError(noUser, 404, 'USER-003')
      const noBody = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/admins`, { body: {} })
      t.expectError(noBody, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-06-005', feature: 'UT-06', api: 'POST /api/v1/admin/hospitals/{hospitalId}/admins', kind: '異常', viewpoint: '権限（病院管理者による紐付け）',
    condition: 'ROLE_HOSPITAL_ADMIN', expected: '403 AUTH-005',
    async run(t) {
      const res = await t.call('vet', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/admins`, { body: { userId: ctx.ids.vet2 } })
      t.expectError(res, 403, 'AUTH-005')
    } })
  add({ id: 'UT-06-006', feature: 'UT-06', api: 'GET /api/v1/admin/hospitals/{hospitalId}/admins', kind: '正常', viewpoint: '病院管理者一覧',
    condition: '病院1（病院管理者1名が紐付き）', expected: '200、userId・email・nicknameを返す。存在しない病院は404 HOSPITAL-001',
    async run(t) {
      const res = await t.call('admin', 'GET', `/api/v1/admin/hospitals/${ctx.h1}/admins`)
      t.check('HTTPステータス', res.status, 200)
      t.check('紐付いた病院管理者', (res.data ?? []).map((a) => [a.userId, a.email]), [[ctx.ids.vet, ctx.emails.vet]])
      const missing = await t.call('admin', 'GET', '/api/v1/admin/hospitals/99999999/admins')
      t.expectError(missing, 404, 'HOSPITAL-001')
    } })
  add({ id: 'UT-06-007', feature: 'UT-06', api: 'DELETE /api/v1/admin/hospitals/{hospitalId}/admins/{userId}', kind: '正常', viewpoint: '紐付け解除',
    condition: '紐付いた病院管理者', expected: '200、解除後はその病院を操作できず（403 HOSPITAL-005）、もう一度解除すると404 HOSPITAL-008',
    async run(t) {
      const h = await prep.hospital()
      await http('POST', `/api/v1/admin/hospitals/${h}/admins`, { token: ctx.tokens.admin, body: { userId: ctx.ids.vet2 } })
      const res = await t.call('admin', 'DELETE', `/api/v1/admin/hospitals/${h}/admins/${ctx.ids.vet2}`)
      t.check('HTTPステータス', res.status, 200)
      const after = await t.call('vet2', 'PATCH', `/api/v1/admin/hospitals/${h}`, { body: { name: 'X', address: 'Y' } })
      t.expectError(after, 403, 'HOSPITAL-005')
      const again = await t.call('admin', 'DELETE', `/api/v1/admin/hospitals/${h}/admins/${ctx.ids.vet2}`)
      t.expectError(again, 404, 'HOSPITAL-008')
    } })
  add({ id: 'UT-06-008', feature: 'UT-06', api: 'PATCH /api/v1/admin/users/{userId}/role', kind: '正常', viewpoint: '権限変更による紐付けの削除',
    condition: '病院に紐付いた病院管理者を一般ユーザーに変更', expected: '紐付けがすべて削除され、病院管理者に戻しても復元されない',
    async run(t) {
      const u = await prep.user('ut-demote', 'ROLE_HOSPITAL_ADMIN')
      const h = await prep.hospital()
      await http('POST', `/api/v1/admin/hospitals/${h}/admins`, { token: ctx.tokens.admin, body: { userId: u.id } })
      await t.call('admin', 'PATCH', `/api/v1/admin/users/${u.id}/role`, { body: { role: 'ROLE_USER' } })
      t.check('降格後の紐付け', t.db('件数', `SELECT count(*) FROM hospital_admins WHERE user_id=${u.id};`), '0')
      await t.call('admin', 'PATCH', `/api/v1/admin/users/${u.id}/role`, { body: { role: 'ROLE_HOSPITAL_ADMIN' } })
      t.check('再昇格後も復元されない', t.db('件数', `SELECT count(*) FROM hospital_admins WHERE user_id=${u.id};`), '0')
    } })

  // ================================================================ UT-07 診療時間
  const hours = (body) => ({ dayOfWeek: 'MONDAY', openTime: '09:00', closeTime: '18:00', breakStartTime: '12:00', breakEndTime: '13:00', slotDurationMinutes: 30, ...body })
  add({ id: 'UT-07-001', feature: 'UT-07', api: 'POST /api/v1/admin/hospitals/{hospitalId}/business-hours', kind: '正常', viewpoint: '診療時間の登録',
    condition: '月曜 9:00〜18:00、休憩12:00〜13:00、30分単位', expected: '201、businessHoursIdを返す',
    async run(t) {
      const h = await prep.hospital()
      const res = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/business-hours`, { body: hours() })
      t.check('HTTPステータス', res.status, 201)
      t.check('businessHoursId', res.data?.businessHoursId, isNumber)
    } })
  add({ id: 'UT-07-002', feature: 'UT-07', api: 'POST /api/v1/admin/hospitals/{hospitalId}/business-hours', kind: '異常', viewpoint: '同じ曜日の重複登録',
    condition: '登録済みの曜日', expected: '409 HOSPITAL-004',
    async run(t) {
      const h = await prep.hospital()
      await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/business-hours`, { body: hours() })
      const res = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/business-hours`, { body: hours() })
      t.expectError(res, 409, 'HOSPITAL-004')
    } })
  add({ id: 'UT-07-003', feature: 'UT-07', api: 'POST /api/v1/admin/hospitals/{hospitalId}/business-hours', kind: '異常', viewpoint: '診療時間の不正値',
    condition: '終了が開始より前／単位0分／休憩の片方だけ／休憩が営業時間外', expected: 'いずれも400 HOSPITAL-009',
    async run(t) {
      const h = await prep.hospital()
      for (const body of [hours({ openTime: '18:00', closeTime: '09:00' }), hours({ slotDurationMinutes: 0 }), hours({ breakEndTime: null }), hours({ breakStartTime: '17:30', breakEndTime: '19:00' })]) {
        const res = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/business-hours`, { body })
        t.expectError(res, 400, 'HOSPITAL-009')
      }
    } })
  add({ id: 'UT-07-004', feature: 'UT-07', api: 'GET /api/v1/hospitals/{hospitalId}/business-hours', kind: '正常', viewpoint: '診療時間一覧（一般ユーザー）',
    condition: '月曜・水曜を登録した病院', expected: '200、登録した曜日の診療時間を返す。存在しない病院は404 HOSPITAL-001',
    async run(t) {
      const h = await prep.hospital()
      await http('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: ctx.tokens.admin, body: hours() })
      await http('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: ctx.tokens.admin, body: hours({ dayOfWeek: 'WEDNESDAY', breakStartTime: null, breakEndTime: null }) })
      const res = await t.call('tanaka', 'GET', `/api/v1/hospitals/${h}/business-hours`)
      t.check('HTTPステータス', res.status, 200)
      t.check('曜日', (res.data ?? []).map((b) => b.dayOfWeek).sort(), ['MONDAY', 'WEDNESDAY'])
      const missing = await t.call('tanaka', 'GET', '/api/v1/hospitals/99999999/business-hours')
      t.expectError(missing, 404, 'HOSPITAL-001')
    } })
  add({ id: 'UT-07-005', feature: 'UT-07', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}', kind: '正常', viewpoint: '診療時間の更新',
    condition: '登録済みの月曜を10:00〜19:00、休憩なし、20分単位に変更', expected: '200、時間・単位が更新され、曜日は変わらない',
    async run(t) {
      const h = await prep.hospital()
      const id = (await http('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: ctx.tokens.admin, body: hours() })).data.businessHoursId
      const res = await t.call('admin', 'PATCH', `/api/v1/admin/hospitals/${h}/business-hours/${id}`, { body: { openTime: '10:00', closeTime: '19:00', slotDurationMinutes: 20 } })
      t.check('HTTPステータス', res.status, 200)
      t.check('更新後', t.db('更新後', `SELECT day_of_week||','||open_time||','||close_time||','||coalesce(break_start_time::text,'NULL')||','||slot_duration_minutes FROM hospital_business_hours WHERE id=${id};`), 'MONDAY,10:00:00,19:00:00,NULL,20')
    } })
  add({ id: 'UT-07-006', feature: 'UT-07', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}', kind: '異常', viewpoint: '別の病院の診療時間ID',
    condition: 'パスの病院に属さない診療時間ID', expected: '404 HOSPITAL-003、変更されない',
    async run(t) {
      const h = await prep.hospital()
      const other = await prep.hospital()
      const id = (await http('POST', `/api/v1/admin/hospitals/${other}/business-hours`, { token: ctx.tokens.admin, body: hours() })).data.businessHoursId
      const res = await t.call('admin', 'PATCH', `/api/v1/admin/hospitals/${h}/business-hours/${id}`, { body: { openTime: '10:00', closeTime: '19:00', slotDurationMinutes: 20 } })
      t.expectError(res, 404, 'HOSPITAL-003')
    } })
  add({ id: 'UT-07-007', feature: 'UT-07', api: 'DELETE /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}', kind: '正常', viewpoint: '診療時間の削除',
    condition: '登録済みの曜日', expected: '200、一覧から消える（休診扱い）',
    async run(t) {
      const h = await prep.hospital()
      const id = (await http('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: ctx.tokens.admin, body: hours() })).data.businessHoursId
      const res = await t.call('admin', 'DELETE', `/api/v1/admin/hospitals/${h}/business-hours/${id}`)
      t.check('HTTPステータス', res.status, 200)
      const list = await t.call('admin', 'GET', `/api/v1/hospitals/${h}/business-hours`)
      t.check('一覧から消える', (list.data ?? []).length, 0)
    } })
  add({ id: 'UT-07-008', feature: 'UT-07', api: 'POST /api/v1/admin/hospitals/{hospitalId}/business-hours', kind: '異常', viewpoint: '紐付いていない病院の診療時間',
    condition: '病院管理者が紐付いていない病院', expected: '403 HOSPITAL-005',
    async run(t) {
      const res = await t.call('vet', 'POST', `/api/v1/admin/hospitals/${ctx.h2}/business-hours`, { body: hours({ dayOfWeek: 'SUNDAY' }) })
      t.expectError(res, 403, 'HOSPITAL-005')
    } })

  add({ id: 'UT-07-009', feature: 'UT-07', api: 'PATCH・DELETE /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}', kind: '異常', viewpoint: '紐付いていない病院の診療時間の更新・削除（Codex G-08）',
    condition: '病院1だけに紐付いた病院管理者が、病院2の診療時間を更新・削除／存在しない病院IDを指定', expected: 'いずれも403 HOSPITAL-005（所属の確認が先）。診療時間は変更・削除されない',
    async run(t) {
      const id = (await http('POST', `/api/v1/admin/hospitals/${ctx.h2}/business-hours`, { token: ctx.tokens.admin, body: hours({ dayOfWeek: 'TUESDAY' }) })).data.businessHoursId
      t.expectError(await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${ctx.h2}/business-hours/${id}`, { body: { openTime: '10:00', closeTime: '19:00', slotDurationMinutes: 20 } }), 403, 'HOSPITAL-005')
      t.expectError(await t.call('vet', 'DELETE', `/api/v1/admin/hospitals/${ctx.h2}/business-hours/${id}`), 403, 'HOSPITAL-005')
      t.expectError(await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/99999999/business-hours/${id}`, { body: { openTime: '10:00', closeTime: '19:00', slotDurationMinutes: 20 } }), 403, 'HOSPITAL-005')
      t.check('変更・削除されない', t.db('診療時間', `SELECT open_time||','||slot_duration_minutes FROM hospital_business_hours WHERE id=${id};`), '09:00:00,30')
    } })
  add({ id: 'UT-07-010', feature: 'UT-07', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId}', kind: '異常', viewpoint: '更新時の診療時間の不正値（Codex G-09）',
    condition: '終了が開始より前／単位241分／休憩が営業時間外', expected: 'いずれも400 HOSPITAL-009、診療時間は変更されない',
    async run(t) {
      const h = await prep.hospital()
      const id = (await http('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: ctx.tokens.admin, body: hours() })).data.businessHoursId
      for (const body of [{ openTime: '18:00', closeTime: '09:00', slotDurationMinutes: 30 }, { openTime: '09:00', closeTime: '18:00', slotDurationMinutes: 241 }, { openTime: '09:00', closeTime: '18:00', breakStartTime: '08:00', breakEndTime: '10:00', slotDurationMinutes: 30 }]) {
        t.expectError(await t.call('admin', 'PATCH', `/api/v1/admin/hospitals/${h}/business-hours/${id}`, { body }), 400, 'HOSPITAL-009')
      }
      t.check('変更されない', t.db('診療時間', `SELECT open_time||','||close_time||','||slot_duration_minutes FROM hospital_business_hours WHERE id=${id};`), '09:00:00,18:00:00,30')
    } })

  // ================================================================ UT-08 予約枠
  add({ id: 'UT-08-010', feature: 'UT-08', api: 'PATCH /api/v1/admin/hospitals/{hospitalId}/schedules/{scheduleId}/unblock', kind: '異常', viewpoint: '紐付いていない病院の予約枠のAVAILABLE化（Codex G-08）',
    condition: '病院2のBLOCKEDの予約枠を、病院1だけに紐付いた病院管理者がunblock', expected: '403 HOSPITAL-005、BLOCKEDのまま',
    async run(t) {
      const id = await prep.slot(ctx.h2)
      await http('PATCH', `/api/v1/admin/hospitals/${ctx.h2}/schedules/${id}/block`, { token: ctx.tokens.admin })
      t.expectError(await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${ctx.h2}/schedules/${id}/unblock`), 403, 'HOSPITAL-005')
      t.check('BLOCKEDのまま', t.db('status', `SELECT status FROM hospital_schedules WHERE id=${id};`), 'BLOCKED')
    } })
  add({ id: 'UT-08-001', feature: 'UT-08', api: 'POST /api/v1/admin/hospitals/{hospitalId}/schedules', kind: '正常', viewpoint: '予約枠の個別登録',
    condition: '日付・開始時刻・終了時刻（紐付いた病院管理者）', expected: '201、scheduleIdを返し、AVAILABLEで登録される',
    async run(t) {
      const res = await t.call('vet', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/schedules`, { body: { availableDate: future(12), startTime: '18:00', endTime: '18:30' } })
      t.check('HTTPステータス', res.status, 201)
      t.check('状態', t.db('status', `SELECT status FROM hospital_schedules WHERE id=${res.data?.scheduleId};`), 'AVAILABLE')
    } })
  add({ id: 'UT-08-002', feature: 'UT-08', api: 'POST /api/v1/admin/hospitals/{hospitalId}/schedules', kind: '異常', viewpoint: '必須項目・存在しない病院',
    condition: '日付なし／存在しない病院（システム管理者）', expected: '400 VALIDATION-001／404 HOSPITAL-001',
    async run(t) {
      const noDate = await t.call('vet', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/schedules`, { body: { startTime: '10:00', endTime: '10:30' } })
      t.expectError(noDate, 400, 'VALIDATION-001')
      const missing = await t.call('admin', 'POST', '/api/v1/admin/hospitals/99999999/schedules', { body: { availableDate: future(12), startTime: '10:00', endTime: '10:30' } })
      t.expectError(missing, 404, 'HOSPITAL-001')
    } })
  add({ id: 'UT-08-003', feature: 'UT-08', api: 'GET /api/v1/hospitals/{hospitalId}/schedules', kind: '正常', viewpoint: '予約枠一覧（一般ユーザー）',
    condition: '日付・時刻の異なる予約枠', expected: '200、日付・開始時刻の昇順で、AVAILABLE・BLOCKEDの両方を返す',
    async run(t) {
      const h = await prep.hospital()
      const later = (await http('POST', `/api/v1/admin/hospitals/${h}/schedules`, { token: ctx.tokens.admin, body: { availableDate: future(9), startTime: '09:00', endTime: '09:30' } })).data.scheduleId
      const earlier = (await http('POST', `/api/v1/admin/hospitals/${h}/schedules`, { token: ctx.tokens.admin, body: { availableDate: future(8), startTime: '15:00', endTime: '15:30' } })).data.scheduleId
      await http('PATCH', `/api/v1/admin/hospitals/${h}/schedules/${later}/block`, { token: ctx.tokens.admin })
      const res = await t.call('tanaka', 'GET', `/api/v1/hospitals/${h}/schedules`)
      t.check('HTTPステータス', res.status, 200)
      t.check('並び順と状態', (res.data ?? []).map((s) => [s.scheduleId, s.status]), [[earlier, 'AVAILABLE'], [later, 'BLOCKED']])
    } })
  add({ id: 'UT-08-004', feature: 'UT-08', api: 'PATCH .../schedules/{scheduleId}/block, /unblock', kind: '正常', viewpoint: 'BLOCKED・AVAILABLEの切り替え',
    condition: 'AVAILABLEの予約枠', expected: 'blockで200・BLOCKED、unblockで200・AVAILABLE',
    async run(t) {
      const id = await prep.slot()
      const block = await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${ctx.h1}/schedules/${id}/block`)
      t.check('block', [block.status, t.db('status', `SELECT status FROM hospital_schedules WHERE id=${id};`)], [200, 'BLOCKED'])
      const unblock = await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${ctx.h1}/schedules/${id}/unblock`)
      t.check('unblock', [unblock.status, t.db('status', `SELECT status FROM hospital_schedules WHERE id=${id};`)], [200, 'AVAILABLE'])
    } })
  add({ id: 'UT-08-005', feature: 'UT-08', api: 'PATCH .../schedules/{scheduleId}/block', kind: '異常', viewpoint: '別の病院の予約枠',
    condition: 'パスの病院に属さない予約枠ID（システム管理者）', expected: '404 HOSPITAL-002、変更されない',
    async run(t) {
      const id = await prep.slot(ctx.h2)
      const res = await t.call('admin', 'PATCH', `/api/v1/admin/hospitals/${ctx.h1}/schedules/${id}/block`)
      t.expectError(res, 404, 'HOSPITAL-002')
      t.check('変更されない', t.db('status', `SELECT status FROM hospital_schedules WHERE id=${id};`), 'AVAILABLE')
    } })
  add({ id: 'UT-08-006', feature: 'UT-08', api: 'POST .../schedules (block)', kind: '異常', viewpoint: '紐付いていない病院の予約枠',
    condition: '病院管理者が紐付いていない病院への登録・block', expected: 'いずれも403 HOSPITAL-005',
    async run(t) {
      const create = await t.call('vet', 'POST', `/api/v1/admin/hospitals/${ctx.h2}/schedules`, { body: { availableDate: future(12), startTime: '10:00', endTime: '10:30' } })
      t.expectError(create, 403, 'HOSPITAL-005')
      const id = await prep.slot(ctx.h2)
      const block = await t.call('vet', 'PATCH', `/api/v1/admin/hospitals/${ctx.h2}/schedules/${id}/block`)
      t.expectError(block, 403, 'HOSPITAL-005')
    } })
  add({ id: 'UT-08-007', feature: 'UT-08', api: 'POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate', kind: '正常', viewpoint: '予約枠の自動生成',
    condition: '月曜 9:00〜11:00（休憩10:00〜10:30、30分単位）だけを登録した病院で、翌月を指定', expected: '200。月曜ごとに4枠（09:00・09:30・10:30はAVAILABLE、休憩の10:00はBLOCKED）、作成数・日数が暦と一致する',
    async run(t) {
      const h = await prep.hospital()
      await http('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: ctx.tokens.admin, body: hours({ closeTime: '11:00', breakStartTime: '10:00', breakEndTime: '10:30' }) })
      const month = addMonths(today.slice(0, 7), 1)
      const mondays = Number(t.db('翌月の月曜日数', `SELECT count(*) FROM generate_series(date '${month}-01', (date '${month}-01' + interval '1 month' - interval '1 day')::date, interval '1 day') d WHERE extract(isodow FROM d)=1;`))
      const res = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/schedules/generate`, { body: { month } })
      t.check('HTTPステータス', res.status, 200)
      t.check('件数', [res.data?.createdSlots, res.data?.generatedDates, res.data?.skippedDates], [mondays * 4, mondays, 0])
      const first = t.db('最初の月曜の枠', `SELECT string_agg(to_char(start_time,'HH24:MI')||' '||status, ',' ORDER BY start_time) FROM hospital_schedules WHERE hospital_id=${h} AND available_date=(SELECT min(available_date) FROM hospital_schedules WHERE hospital_id=${h});`)
      t.check('1日の枠', first, '09:00 AVAILABLE,09:30 AVAILABLE,10:00 BLOCKED,10:30 AVAILABLE')
    } })
  add({ id: 'UT-08-008', feature: 'UT-08', api: 'POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate', kind: '正常', viewpoint: '同じ月の再実行',
    condition: 'UT-08-007と同じ条件で2回実行', expected: '2回目は作成0件、診療日はすべて対象外（skippedDates）として数え、枠は増えない',
    async run(t) {
      const h = await prep.hospital()
      await http('POST', `/api/v1/admin/hospitals/${h}/business-hours`, { token: ctx.tokens.admin, body: hours({ closeTime: '11:00', breakStartTime: '10:00', breakEndTime: '10:30' }) })
      const month = addMonths(today.slice(0, 7), 2)
      const first = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/schedules/generate`, { body: { month } })
      const second = await t.call('admin', 'POST', `/api/v1/admin/hospitals/${h}/schedules/generate`, { body: { month } })
      t.check('2回目の件数', [second.data?.createdSlots, second.data?.skippedDates], [0, first.data?.generatedDates])
      t.check('枠の総数', Number(t.db('枠数', `SELECT count(*) FROM hospital_schedules WHERE hospital_id=${h};`)), first.data?.createdSlots)
    } })
  add({ id: 'UT-08-009', feature: 'UT-08', api: 'POST /api/v1/admin/hospitals/{hospitalId}/schedules/generate', kind: '異常', viewpoint: '生成できない月・形式不正・権限',
    condition: '先月／13か月先／"2026/11"／紐付いていない病院管理者', expected: '400 HOSPITAL-010／400 HOSPITAL-010／400 VALIDATION-001／403 HOSPITAL-005',
    async run(t) {
      const current = today.slice(0, 7)
      t.expectError(await t.call('admin', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/schedules/generate`, { body: { month: addMonths(current, -1) } }), 400, 'HOSPITAL-010')
      t.expectError(await t.call('admin', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/schedules/generate`, { body: { month: addMonths(current, 13) } }), 400, 'HOSPITAL-010')
      t.expectError(await t.call('admin', 'POST', `/api/v1/admin/hospitals/${ctx.h1}/schedules/generate`, { body: { month: '2026/11' } }), 400, 'VALIDATION-001')
      t.expectError(await t.call('vet', 'POST', `/api/v1/admin/hospitals/${ctx.h2}/schedules/generate`, { body: { month: addMonths(current, 1) } }), 403, 'HOSPITAL-005')
    } })

  // ================================================================ UT-09 予約（利用者）
  add({ id: 'UT-09-001', feature: 'UT-09', api: 'POST /api/v1/reservations', kind: '正常', viewpoint: '予約申請',
    condition: '本人のペット・営業中の病院の未来のAVAILABLE枠・メモ', expected: '201、reservationIdを返し、状態REQUESTED',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const scheduleId = await prep.slot()
      const res = await t.call('tanaka', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: ctx.h1, scheduleId, memo: '健康診断' } })
      t.check('HTTPステータス', res.status, 201)
      t.check('状態', [res.data?.status, t.db('status', `SELECT status FROM reservations WHERE id=${res.data?.reservationId};`)], ['REQUESTED', 'REQUESTED'])
    } })
  add({ id: 'UT-09-002', feature: 'UT-09', api: 'POST /api/v1/reservations', kind: '異常', viewpoint: '同じ予約枠への重複予約',
    condition: '申請中の予約がある枠に別ユーザーが申請', expected: '409 RESERVATION-001',
    async run(t) {
      const { scheduleId } = await prep.reservation('tanaka')
      const petId = await prep.pet('sato')
      const res = await t.call('sato', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: ctx.h1, scheduleId } })
      t.expectError(res, 409, 'RESERVATION-001')
    } })
  add({ id: 'UT-09-003', feature: 'UT-09', api: 'POST /api/v1/reservations', kind: '異常', viewpoint: 'BLOCKEDの予約枠',
    condition: 'BLOCKEDにした予約枠', expected: '409 RESERVATION-003',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const scheduleId = await prep.slot()
      await http('PATCH', `/api/v1/admin/hospitals/${ctx.h1}/schedules/${scheduleId}/block`, { token: ctx.tokens.admin })
      const res = await t.call('tanaka', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: ctx.h1, scheduleId } })
      t.expectError(res, 409, 'RESERVATION-003')
    } })
  add({ id: 'UT-09-004', feature: 'UT-09', api: 'POST /api/v1/reservations', kind: '異常', viewpoint: '他人のペットでの申請',
    condition: '他のユーザーのペットID', expected: '403 RESERVATION-004',
    async run(t) {
      const petId = await prep.pet('sato')
      const scheduleId = await prep.slot()
      const res = await t.call('tanaka', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: ctx.h1, scheduleId } })
      t.expectError(res, 403, 'RESERVATION-004')
    } })
  add({ id: 'UT-09-005', feature: 'UT-09', api: 'POST /api/v1/reservations', kind: '異常', viewpoint: '過去の予約枠',
    condition: '昨日の日付の予約枠（DBに直接作成）', expected: '400 RESERVATION-005',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const scheduleId = Number(t.db('昨日の予約枠を作る（準備）', `INSERT INTO hospital_schedules(hospital_id, available_date, start_time, end_time, status) VALUES (${ctx.h1}, '${addDays(today, -1)}', '10:00', '10:30', 'AVAILABLE') RETURNING id;`).split('\n')[0])
      const res = await t.call('tanaka', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: ctx.h1, scheduleId } })
      t.expectError(res, 400, 'RESERVATION-005')
    } })
  add({ id: 'UT-09-006', feature: 'UT-09', api: 'POST /api/v1/reservations', kind: '異常', viewpoint: '病院に属さない予約枠',
    condition: '病院2の予約枠を病院1として申請', expected: '404 RESERVATION-002',
    async run(t) {
      const petId = await prep.pet('tanaka')
      const scheduleId = await prep.slot(ctx.h2)
      const res = await t.call('tanaka', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: ctx.h1, scheduleId } })
      t.expectError(res, 404, 'RESERVATION-002')
    } })
  add({ id: 'UT-09-007', feature: 'UT-09', api: 'POST /api/v1/reservations', kind: '異常', viewpoint: '停止中の病院への申請',
    condition: 'SUSPENDEDの病院の予約枠', expected: '409 RESERVATION-009',
    async run(t) {
      const h = await prep.hospital()
      const scheduleId = await prep.slot(h)
      await http('PATCH', `/api/v1/admin/hospitals/${h}/suspend`, { token: ctx.tokens.admin })
      const petId = await prep.pet('tanaka')
      const res = await t.call('tanaka', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: h, scheduleId } })
      t.expectError(res, 409, 'RESERVATION-009')
    } })
  add({ id: 'UT-09-008', feature: 'UT-09', api: 'GET /api/v1/reservations', kind: '正常', viewpoint: '予約一覧',
    condition: '本人と他人がそれぞれ予約している', expected: '200、本人の予約だけを病院名・ペット名・日時・状態付きで返す',
    async run(t) {
      const mine = await prep.reservation('tanaka')
      const others = await prep.reservation('sato')
      const res = await t.call('tanaka', 'GET', '/api/v1/reservations')
      t.check('HTTPステータス', res.status, 200)
      const ids = (res.data ?? []).map((r) => r.reservationId)
      t.check('本人の予約を含む', ids.includes(mine.reservationId), true)
      t.check('他人の予約を含まない', ids.includes(others.reservationId), false)
      const item = (res.data ?? []).find((r) => r.reservationId === mine.reservationId)
      t.check('表示項目', [typeof item?.hospitalName, item?.petName, item?.status], ['string', 'Momo', 'REQUESTED'])
    } })
  add({ id: 'UT-09-009', feature: 'UT-09', api: 'GET /api/v1/reservations/{reservationId}', kind: '正常', viewpoint: '予約詳細',
    condition: '本人の予約', expected: '200、メモを含む',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      const res = await t.call('tanaka', 'GET', `/api/v1/reservations/${reservationId}`)
      t.check('HTTPステータス', res.status, 200)
      t.check('メモ', res.data?.memo, '咳が続いています')
    } })
  add({ id: 'UT-09-010', feature: 'UT-09', api: 'GET /api/v1/reservations/{reservationId}', kind: '異常', viewpoint: '他人の予約・存在しない予約',
    condition: '他のユーザーの予約ID／存在しない予約ID', expected: '403 RESERVATION-007／404 RESERVATION-006',
    async run(t) {
      const { reservationId } = await prep.reservation('sato')
      t.expectError(await t.call('tanaka', 'GET', `/api/v1/reservations/${reservationId}`), 403, 'RESERVATION-007')
      t.expectError(await t.call('tanaka', 'GET', '/api/v1/reservations/99999999'), 404, 'RESERVATION-006')
    } })
  add({ id: 'UT-09-011', feature: 'UT-09', api: 'PATCH /api/v1/reservations/{reservationId}/cancel', kind: '正常', viewpoint: '予約キャンセル',
    condition: '申請中の本人の予約', expected: '200、CANCELLEDになり、同じ予約枠に別の予約を申請できる',
    async run(t) {
      const { reservationId, scheduleId } = await prep.reservation('tanaka')
      const res = await t.call('tanaka', 'PATCH', `/api/v1/reservations/${reservationId}/cancel`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態', t.db('status', `SELECT status FROM reservations WHERE id=${reservationId};`), 'CANCELLED')
      const petId = await prep.pet('sato')
      const rebook = await t.call('sato', 'POST', '/api/v1/reservations', { body: { petId, hospitalId: ctx.h1, scheduleId } })
      t.check('同じ枠への再予約', rebook.status, 201)
    } })
  add({ id: 'UT-09-012', feature: 'UT-09', api: 'PATCH /api/v1/reservations/{reservationId}/cancel', kind: '異常', viewpoint: '不正な状態遷移・他人の予約',
    condition: 'キャンセル済みの予約／他のユーザーの予約', expected: '409 RESERVATION-008／403 RESERVATION-007',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      await t.call('tanaka', 'PATCH', `/api/v1/reservations/${reservationId}/cancel`)
      t.expectError(await t.call('tanaka', 'PATCH', `/api/v1/reservations/${reservationId}/cancel`), 409, 'RESERVATION-008')
      const others = await prep.reservation('sato')
      t.expectError(await t.call('tanaka', 'PATCH', `/api/v1/reservations/${others.reservationId}/cancel`), 403, 'RESERVATION-007')
    } })

  add({ id: 'UT-09-013', feature: 'UT-09', api: 'PATCH /api/v1/reservations/{reservationId}/cancel', kind: '異常', viewpoint: '存在しない予約のキャンセル（Codex G-05）',
    condition: '存在しない予約ID', expected: '404 RESERVATION-006',
    async run(t) {
      t.expectError(await t.call('tanaka', 'PATCH', '/api/v1/reservations/99999999/cancel'), 404, 'RESERVATION-006')
    } })

  // ================================================================ UT-10 予約管理（病院管理者）
  add({ id: 'UT-10-009', feature: 'UT-10', api: 'PATCH /api/v1/admin/reservations/{reservationId}/complete', kind: '異常', viewpoint: '存在しない予約の診療完了（Codex G-05）',
    condition: '存在しない予約ID（病院管理者）', expected: '404 RESERVATION-006（予約の確認が所属の確認より先）、通知は作られない',
    async run(t) {
      const before = t.snapshot('書き込み前の行数')
      t.expectError(await t.call('vet', 'PATCH', '/api/v1/admin/reservations/99999999/complete'), 404, 'RESERVATION-006')
      t.check('書き込みなし', t.snapshot('書き込み後の行数'), before)
    } })
  add({ id: 'UT-10-001', feature: 'UT-10', api: 'GET /api/v1/admin/reservations', kind: '正常', viewpoint: '病院の予約一覧',
    condition: '紐付いた病院1の予約', expected: '200、予約者のニックネーム・ペット名・メモ・createdAt（+09:00）を含む',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      const res = await t.call('vet', 'GET', `/api/v1/admin/reservations?hospitalId=${ctx.h1}`)
      t.check('HTTPステータス', res.status, 200)
      const item = (res.data ?? []).find((r) => r.reservationId === reservationId)
      t.check('表示項目', [item?.userNickname, item?.petName, item?.memo], ['Tanaka', 'Momo', '咳が続いています'])
      t.check('createdAt', item?.createdAt, TIME_WITH_OFFSET)
    } })
  add({ id: 'UT-10-002', feature: 'UT-10', api: 'GET /api/v1/admin/reservations', kind: '正常', viewpoint: '状態・日付での絞り込み',
    condition: '状態REQUESTED・予約枠の日付を指定', expected: '200、条件に一致する予約だけを日付・開始時刻順で返す',
    async run(t) {
      const h = await prep.hospital()
      await http('POST', `/api/v1/admin/hospitals/${h}/admins`, { token: ctx.tokens.admin, body: { userId: ctx.ids.vet2 } })
      const a = await prep.reservation('tanaka', h)
      const b = await prep.reservation('sato', h)
      await http('PATCH', `/api/v1/admin/reservations/${b.reservationId}/approve`, { token: ctx.tokens.admin })
      const date = future(10)
      const res = await t.call('vet2', 'GET', `/api/v1/admin/reservations?hospitalId=${h}&status=REQUESTED&date=${date}`)
      t.check('HTTPステータス', res.status, 200)
      t.check('REQUESTEDだけ', (res.data ?? []).map((r) => r.reservationId), [a.reservationId])
    } })
  add({ id: 'UT-10-003', feature: 'UT-10', api: 'PATCH /api/v1/admin/reservations/{reservationId}/approve', kind: '正常', viewpoint: '予約承認と通知',
    condition: '申請中の予約（紐付いた病院管理者）', expected: '200、APPROVEDになり、予約者にRESERVATION_APPROVEDの通知が1件作られる',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      const res = await t.call('vet', 'PATCH', `/api/v1/admin/reservations/${reservationId}/approve`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態', t.db('status', `SELECT status FROM reservations WHERE id=${reservationId};`), 'APPROVED')
      t.check('通知', t.db('通知', `SELECT count(*)||','||min(notification_type) FROM notifications WHERE reservation_id=${reservationId} AND user_id=${ctx.ids.tanaka};`), '1,RESERVATION_APPROVED')
    } })
  add({ id: 'UT-10-004', feature: 'UT-10', api: 'PATCH /api/v1/admin/reservations/{reservationId}/reject', kind: '正常', viewpoint: '予約却下と通知',
    condition: '申請中の予約', expected: '200、REJECTEDになり、予約者にRESERVATION_REJECTEDの通知が1件だけ作られる',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      const res = await t.call('vet', 'PATCH', `/api/v1/admin/reservations/${reservationId}/reject`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態', t.db('status', `SELECT status FROM reservations WHERE id=${reservationId};`), 'REJECTED')
      t.check('通知（件数・宛先・種別）', t.db('通知', `SELECT count(*)||','||min(user_id)||','||min(notification_type) FROM notifications WHERE reservation_id=${reservationId};`), `1,${ctx.ids.tanaka},RESERVATION_REJECTED`)
    } })
  add({ id: 'UT-10-005', feature: 'UT-10', api: 'PATCH /api/v1/admin/reservations/{reservationId}/complete', kind: '正常', viewpoint: '診療完了と通知',
    condition: '承認済みの予約', expected: '200、COMPLETEDになり、予約者にTREATMENT_COMPLETEDの通知が作られる（承認時の通知と合わせて2件、docs/07 §12）',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      await http('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, { token: ctx.tokens.vet })
      const res = await t.call('vet', 'PATCH', `/api/v1/admin/reservations/${reservationId}/complete`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態', t.db('status', `SELECT status FROM reservations WHERE id=${reservationId};`), 'COMPLETED')
      t.check('通知（種別・宛先）', t.db('通知', `SELECT string_agg(notification_type||'@'||user_id, ',' ORDER BY id) FROM notifications WHERE reservation_id=${reservationId};`), `RESERVATION_APPROVED@${ctx.ids.tanaka},TREATMENT_COMPLETED@${ctx.ids.tanaka}`)
    } })
  add({ id: 'UT-10-006', feature: 'UT-10', api: 'PATCH .../approve, /complete', kind: '異常', viewpoint: '不正な状態遷移',
    condition: '申請中の予約を診療完了／承認済みの予約を再承認', expected: 'いずれも409 RESERVATION-008、通知は作られない',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      t.expectError(await t.call('vet', 'PATCH', `/api/v1/admin/reservations/${reservationId}/complete`), 409, 'RESERVATION-008')
      await http('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, { token: ctx.tokens.vet })
      t.expectError(await t.call('vet', 'PATCH', `/api/v1/admin/reservations/${reservationId}/approve`), 409, 'RESERVATION-008')
      t.check('通知は承認の1件だけ', t.db('通知数', `SELECT count(*) FROM notifications WHERE reservation_id=${reservationId};`), '1')
    } })
  add({ id: 'UT-10-007', feature: 'UT-10', api: 'PATCH /api/v1/admin/reservations/{reservationId}/approve', kind: '異常', viewpoint: '存在しない予約・紐付いていない病院',
    condition: '存在しない予約ID／病院2の予約を病院1の管理者が承認', expected: '404 RESERVATION-006／403 HOSPITAL-005（状態は変わらない）',
    async run(t) {
      t.expectError(await t.call('vet', 'PATCH', '/api/v1/admin/reservations/99999999/approve'), 404, 'RESERVATION-006')
      const { reservationId } = await prep.reservation('tanaka', ctx.h2)
      t.expectError(await t.call('vet', 'PATCH', `/api/v1/admin/reservations/${reservationId}/approve`), 403, 'HOSPITAL-005')
      t.check('状態は変わらない', t.db('status', `SELECT status FROM reservations WHERE id=${reservationId};`), 'REQUESTED')
    } })
  add({ id: 'UT-10-008', feature: 'UT-10', api: 'GET /api/v1/admin/reservations', kind: '異常', viewpoint: '権限・入力チェック',
    condition: '一般ユーザー／hospitalIdなし／status=FOO／紐付いていない病院', expected: '403 AUTH-005／400 VALIDATION-001／400 VALIDATION-001／403 HOSPITAL-005',
    async run(t) {
      t.expectError(await t.call('tanaka', 'GET', `/api/v1/admin/reservations?hospitalId=${ctx.h1}`), 403, 'AUTH-005')
      t.expectError(await t.call('vet', 'GET', '/api/v1/admin/reservations'), 400, 'VALIDATION-001')
      t.expectError(await t.call('vet', 'GET', `/api/v1/admin/reservations?hospitalId=${ctx.h1}&status=FOO`), 400, 'VALIDATION-001')
      t.expectError(await t.call('vet', 'GET', `/api/v1/admin/reservations?hospitalId=${ctx.h2}`), 403, 'HOSPITAL-005')
    } })

  // ================================================================ UT-11 通知
  add({ id: 'UT-11-001', feature: 'UT-11', api: 'GET /api/v1/notifications', kind: '正常', viewpoint: '通知一覧',
    condition: '承認・却下の通知を受け取った利用者と、通知を持つ他のユーザー', expected: '200、本人宛の通知だけを作成日時の降順で返し、未読（isRead=false）',
    async run(t) {
      const u = await prep.user('ut-noti')
      const petId = (await http('POST', '/api/v1/pets', { token: u.token, body: { name: 'Pochi', petType: 'DOG' } })).data.petId
      const book = async () => {
        const scheduleId = await prep.slot()
        return (await http('POST', '/api/v1/reservations', { token: u.token, body: { petId, hospitalId: ctx.h1, scheduleId } })).data.reservationId
      }
      const first = await book()
      await http('PATCH', `/api/v1/admin/reservations/${first}/approve`, { token: ctx.tokens.vet })
      const second = await book()
      await http('PATCH', `/api/v1/admin/reservations/${second}/reject`, { token: ctx.tokens.vet })
      const res = await t.call('準備したユーザー', 'GET', '/api/v1/notifications', { token: u.token })
      t.check('HTTPステータス', res.status, 200)
      t.check('本人宛のみ・作成日時の降順', (res.data ?? []).map((n) => n.type), ['RESERVATION_REJECTED', 'RESERVATION_APPROVED'])
      t.check('未読', (res.data ?? []).every((n) => n.isRead === false), true)
      t.check('createdAt', res.data?.[0]?.createdAt, TIME_WITH_OFFSET)
    } })
  add({ id: 'UT-11-002', feature: 'UT-11', api: 'PATCH /api/v1/notifications/{notificationId}/read', kind: '正常', viewpoint: '既読化',
    condition: '本人宛の未読の通知', expected: '200、is_read=trueになる',
    async run(t) {
      const { reservationId } = await prep.reservation('tanaka')
      await http('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, { token: ctx.tokens.vet })
      const id = Number(t.db('通知ID', `SELECT id FROM notifications WHERE reservation_id=${reservationId};`))
      const res = await t.call('tanaka', 'PATCH', `/api/v1/notifications/${id}/read`)
      t.check('HTTPステータス', res.status, 200)
      t.check('既読', t.db('is_read', `SELECT is_read::text FROM notifications WHERE id=${id};`), 'true')
    } })
  add({ id: 'UT-11-003', feature: 'UT-11', api: 'PATCH /api/v1/notifications/{notificationId}/read', kind: '異常', viewpoint: '他人の通知・存在しない通知',
    condition: '他のユーザー宛の通知／存在しない通知ID', expected: '403 NOTIFICATION-002（既読にならない）／404 NOTIFICATION-001',
    async run(t) {
      const { reservationId } = await prep.reservation('sato')
      await http('PATCH', `/api/v1/admin/reservations/${reservationId}/approve`, { token: ctx.tokens.vet })
      const id = Number(t.db('他人の通知ID', `SELECT id FROM notifications WHERE reservation_id=${reservationId};`))
      t.expectError(await t.call('tanaka', 'PATCH', `/api/v1/notifications/${id}/read`), 403, 'NOTIFICATION-002')
      t.check('既読にならない', t.db('is_read', `SELECT is_read::text FROM notifications WHERE id=${id};`), 'false')
      t.expectError(await t.call('tanaka', 'PATCH', '/api/v1/notifications/99999999/read'), 404, 'NOTIFICATION-001')
    } })

  // ================================================================ UT-12 ユーザー管理
  add({ id: 'UT-12-001', feature: 'UT-12', api: 'GET /api/v1/admin/users', kind: '正常', viewpoint: '検索（キーワード・権限・状態・ページ）',
    condition: 'ニックネームの一部（大文字小文字違い）、role=ROLE_HOSPITAL_ADMIN、size=1', expected: '200、条件に一致するユーザーをID順・ページ形式で返し、パスワードを含まない',
    async run(t) {
      const kw = await t.call('admin', 'GET', '/api/v1/admin/users?keyword=TANAKA')
      t.check('HTTPステータス', kw.status, 200)
      t.check('キーワード一致（大文字小文字を区別しない）', (kw.data?.content ?? []).map((u) => u.userId).includes(ctx.ids.tanaka), true)
      t.check('パスワードを含まない', (kw.data?.content ?? []).every((u) => !('password' in u)), true)
      const role = await t.call('admin', 'GET', '/api/v1/admin/users?role=ROLE_HOSPITAL_ADMIN&status=ACTIVE&page=0&size=1')
      t.check('ページ形式', [role.data?.content?.length, role.data?.size, role.data?.number, role.data?.totalElements >= 2], [1, 1, 0, true])
      t.check('権限で絞り込み', role.data?.content?.[0]?.role, 'ROLE_HOSPITAL_ADMIN')
    } })
  add({ id: 'UT-12-002', feature: 'UT-12', api: 'PATCH /api/v1/admin/users/{userId}/suspend', kind: '正常', viewpoint: 'アカウント停止',
    condition: 'ログイン中の一般ユーザー', expected: '200、SUSPENDEDになり、Refresh Tokenがすべて失効し、発行済みのAccess Tokenでも次のリクエストは401',
    async run(t) {
      const u = await prep.user('ut-suspend')
      const res = await t.call('admin', 'PATCH', `/api/v1/admin/users/${u.id}/suspend`)
      t.check('HTTPステータス', res.status, 200)
      t.check('状態・有効なToken', t.db('状態', `SELECT u.status||','||(SELECT count(*) FROM refresh_tokens r WHERE r.user_id=u.id AND r.revoked=false) FROM users u WHERE u.id=${u.id};`), 'SUSPENDED,0')
      const me = await t.call('準備したユーザー', 'GET', '/api/v1/users/me', { token: u.token })
      t.check('発行済みAccess Token', me.status, 401)
    } })
  add({ id: 'UT-12-003', feature: 'UT-12', api: 'PATCH /api/v1/admin/users/{userId}/activate', kind: '正常', viewpoint: '停止解除',
    condition: '停止中のユーザー', expected: '200、ACTIVEに戻り、再ログインできる',
    async run(t) {
      const u = await prep.user('ut-activate')
      await http('PATCH', `/api/v1/admin/users/${u.id}/suspend`, { token: ctx.tokens.admin })
      const res = await t.call('admin', 'PATCH', `/api/v1/admin/users/${u.id}/activate`)
      t.check('HTTPステータス', res.status, 200)
      const login = await t.call('anon', 'POST', '/api/v1/auth/login', { body: { email: u.email, password: PASSWORD } })
      t.check('再ログイン', login.status, 200)
    } })
  add({ id: 'UT-12-004', feature: 'UT-12', api: 'PATCH /api/v1/admin/users/{userId}/role', kind: '正常', viewpoint: '権限変更の即時反映',
    condition: '一般ユーザーを病院管理者に変更', expected: '200、同じAccess Tokenの次のリクエストから新しい権限で認可される（管理API 403 → 200）',
    async run(t) {
      const u = await prep.user('ut-role')
      const before = await t.call('準備したユーザー', 'GET', '/api/v1/admin/hospitals/mine', { token: u.token })
      t.check('変更前', before.status, 403)
      const res = await t.call('admin', 'PATCH', `/api/v1/admin/users/${u.id}/role`, { body: { role: 'ROLE_HOSPITAL_ADMIN' } })
      t.check('HTTPステータス', res.status, 200)
      const after = await t.call('準備したユーザー', 'GET', '/api/v1/admin/hospitals/mine', { token: u.token })
      t.check('変更後（同じToken）', after.status, 200)
    } })
  add({ id: 'UT-12-005', feature: 'UT-12', api: 'PATCH /api/v1/admin/users/{userId}/suspend, /role', kind: '異常', viewpoint: '自分自身・存在しないユーザー・不正な権限名',
    condition: '自分自身の停止／存在しないユーザー／role=ROLE_ROOT', expected: '400 USER-005／404 USER-003／400 VALIDATION-001',
    async run(t) {
      t.expectError(await t.call('admin', 'PATCH', `/api/v1/admin/users/${ctx.ids.admin}/suspend`), 400, 'USER-005')
      t.expectError(await t.call('admin', 'PATCH', '/api/v1/admin/users/99999999/suspend'), 404, 'USER-003')
      t.expectError(await t.call('admin', 'PATCH', `/api/v1/admin/users/${ctx.ids.sato}/role`, { body: { role: 'ROLE_ROOT' } }), 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-12-006', feature: 'UT-12', api: 'GET /api/v1/admin/users', kind: '異常', viewpoint: '権限（システム管理者以外）',
    condition: '一般ユーザー／病院管理者', expected: 'いずれも403 AUTH-005',
    async run(t) {
      t.expectError(await t.call('tanaka', 'GET', '/api/v1/admin/users'), 403, 'AUTH-005')
      t.expectError(await t.call('vet', 'PATCH', `/api/v1/admin/users/${ctx.ids.sato}/suspend`), 403, 'AUTH-005')
    } })

  add({ id: 'UT-12-007', feature: 'UT-12', api: 'PATCH /api/v1/admin/users/{userId}/activate', kind: '異常', viewpoint: '停止解除の自分自身・存在しないユーザー（Codex G-06）',
    condition: '自分自身の停止解除／存在しないユーザーの停止解除', expected: '400 USER-005／404 USER-003',
    async run(t) {
      t.expectError(await t.call('admin', 'PATCH', `/api/v1/admin/users/${ctx.ids.admin}/activate`), 400, 'USER-005')
      t.expectError(await t.call('admin', 'PATCH', '/api/v1/admin/users/99999999/activate'), 404, 'USER-003')
    } })

  // ================================================================ UT-13 共通
  add({ id: 'UT-13-001', feature: 'UT-13', api: 'GET /api/v1/pets（代表）', kind: '異常', viewpoint: '未認証のアクセス',
    condition: 'Authorizationヘッダーなし', expected: '401 AUTH-001（共通エラー形式 success=false・code・message）',
    async run(t) {
      const res = await t.call('anon', 'GET', '/api/v1/pets')
      t.expectError(res, 401, 'AUTH-001')
      t.check('共通エラー形式', [res.body?.success, typeof res.body?.message], [false, 'string'])
    } })
  add({ id: 'UT-13-002', feature: 'UT-13', api: 'POST /api/v1/pets（代表）', kind: '異常', viewpoint: '読み取れないJSON',
    condition: '壊れたJSON（閉じ括弧なし）', expected: '400 VALIDATION-001（500にしない）',
    async run(t) {
      const res = await t.call('tanaka', 'POST', '/api/v1/pets', { rawBody: '{"name": "Momo", "petType": "CAT"' })
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-13-003', feature: 'UT-13', api: 'GET /api/v1/pets/{petId}（代表）', kind: '異常', viewpoint: 'パス変数の型不正',
    condition: 'petId = "abc"', expected: '400 VALIDATION-001（500にしない）',
    async run(t) {
      const res = await t.call('tanaka', 'GET', '/api/v1/pets/abc')
      t.expectError(res, 400, 'VALIDATION-001')
    } })
  add({ id: 'UT-13-004', feature: 'UT-13', api: '存在しないURL', kind: '異常', viewpoint: '存在しないURL',
    condition: 'GET /api/v1/unknown（認証済み）', expected: '404 COMMON-001（共通エラー形式。500にしない、docs/07 §2-4）',
    async run(t) {
      const res = await t.call('tanaka', 'GET', '/api/v1/unknown')
      t.expectError(res, 404, 'COMMON-001')
      t.check('共通エラー形式', [res.body?.success, res.body?.data, typeof res.body?.message], [false, null, 'string'])
    } })
  add({ id: 'UT-13-005', feature: 'UT-13', api: 'DELETE /api/v1/hospitals（代表）', kind: '異常', viewpoint: '許可されていないHTTPメソッド',
    condition: '定義されていないメソッド（DELETE /api/v1/hospitals）', expected: '405 COMMON-002（共通エラー形式。500にしない）',
    async run(t) {
      const res = await t.call('tanaka', 'DELETE', '/api/v1/hospitals')
      t.expectError(res, 405, 'COMMON-002')
      t.check('共通エラー形式', [res.body?.success, res.body?.data, typeof res.body?.message], [false, null, 'string'])
    } })
  add({ id: 'UT-13-007', feature: 'UT-13', api: 'POST /api/v1/pets（代表）', kind: '異常', viewpoint: '対応していないContent-Type',
    condition: 'Content-Type: text/plain でJSON本文を送る', expected: '415 COMMON-003（共通エラー形式。500にしない）',
    async run(t) {
      const res = await fetch(`${process.env.BASE ?? 'http://localhost:18081'}/api/v1/pets`, {
        method: 'POST', headers: { 'Content-Type': 'text/plain', Authorization: `Bearer ${ctx.tokens.tanaka}` }, body: '{"name":"Momo","petType":"CAT"}',
      })
      const body = await res.json().catch(() => null)
      t.steps.push({ type: 'http', actor: 'tanaka', request: { method: 'POST', path: '/api/v1/pets', headers: { 'Content-Type': 'text/plain' }, rawBody: '{"name":"Momo","petType":"CAT"}' }, response: { status: res.status, body } })
      t.check('HTTPステータス', res.status, 415)
      t.check('エラーコード', body?.code, 'COMMON-003')
      t.check('共通エラー形式', [body?.success, body?.data, typeof body?.message], [false, null, 'string'])
    } })
  add({ id: 'UT-13-008', feature: 'UT-13', api: '管理系15エンドポイント', kind: '異常', viewpoint: '権限（Role）不足の拒否（Codex G-07）',
    condition: '一般ユーザー（ROLE_USER）で管理系の15エンドポイントを呼ぶ。そのうちシステム管理者専用の4つ（病院管理者一覧・紐付け解除・停止解除・権限変更）は病院管理者でも呼ぶ。IDとRequest本文は正しい値', expected: 'いずれも403 AUTH-005、データは書き込まれない（関連テーブルの行数・状態が変わらない）',
    async run(t) {
      const slot = await prep.slot()
      const bh = (await http('POST', `/api/v1/admin/hospitals/${ctx.h1}/business-hours`, { token: ctx.tokens.admin, body: hours({ dayOfWeek: 'FRIDAY' }) })).data?.businessHoursId
        ?? Number(t.db('病院1の診療時間ID', `SELECT id FROM hospital_business_hours WHERE hospital_id=${ctx.h1} ORDER BY id LIMIT 1;`))
      const { reservationId } = await prep.reservation('sato')
      const routes = [
        ['PATCH', `/api/v1/admin/hospitals/${ctx.h1}`, { name: 'X', address: 'Y' }],
        ['GET', `/api/v1/admin/hospitals/${ctx.h1}/admins`],
        ['DELETE', `/api/v1/admin/hospitals/${ctx.h1}/admins/${ctx.ids.vet}`],
        ['POST', `/api/v1/admin/hospitals/${ctx.h1}/business-hours`, hours({ dayOfWeek: 'SATURDAY' })],
        ['PATCH', `/api/v1/admin/hospitals/${ctx.h1}/business-hours/${bh}`, { openTime: '10:00', closeTime: '19:00', slotDurationMinutes: 20 }],
        ['DELETE', `/api/v1/admin/hospitals/${ctx.h1}/business-hours/${bh}`],
        ['POST', `/api/v1/admin/hospitals/${ctx.h1}/schedules`, { availableDate: future(12), startTime: '19:00', endTime: '19:30' }],
        ['POST', `/api/v1/admin/hospitals/${ctx.h1}/schedules/generate`, { month: addMonths(today.slice(0, 7), 3) }],
        ['PATCH', `/api/v1/admin/hospitals/${ctx.h1}/schedules/${slot}/block`],
        ['PATCH', `/api/v1/admin/hospitals/${ctx.h1}/schedules/${slot}/unblock`],
        ['PATCH', `/api/v1/admin/reservations/${reservationId}/approve`],
        ['PATCH', `/api/v1/admin/reservations/${reservationId}/reject`],
        ['PATCH', `/api/v1/admin/reservations/${reservationId}/complete`],
        ['PATCH', `/api/v1/admin/users/${ctx.ids.sato}/activate`],
        ['PATCH', `/api/v1/admin/users/${ctx.ids.sato}/role`, { role: 'ROLE_SYSTEM_ADMIN' }],
      ]
      const systemAdminOnly = new Set([1, 2, 13, 14])
      const before = t.snapshot('書き込み前の行数')
      for (const [i, [method, path, body]] of routes.entries()) {
        const asUser = await t.call('tanaka', method, path, body === undefined ? {} : { body })
        t.check(`ROLE_USER ${method} ${path}`, [asUser.status, asUser.code], [403, 'AUTH-005'])
        if (systemAdminOnly.has(i)) {
          const asVet = await t.call('vet', method, path, body === undefined ? {} : { body })
          t.check(`ROLE_HOSPITAL_ADMIN ${method} ${path}`, [asVet.status, asVet.code], [403, 'AUTH-005'])
        }
      }
      t.check('書き込みなし', t.snapshot('書き込み後の行数'), before)
    } })
  add({ id: 'UT-13-006', feature: 'UT-13', api: '成功レスポンス（代表）', kind: '正常', viewpoint: '共通の成功形式',
    condition: 'GET /api/v1/users/me', expected: '200、success=true・dataを持つ共通形式',
    async run(t) {
      const res = await t.call('tanaka', 'GET', '/api/v1/users/me')
      t.check('共通の成功形式', [res.status, res.body?.success, typeof res.body?.data], [200, true, 'object'])
    } })

  return cases
}
