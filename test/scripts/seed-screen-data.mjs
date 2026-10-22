// 画面単体テスト（UT-14）用のデータを、使い捨てDBで起動したSpring APIにAPI経由で登録する。
// ログイン状態の注入に使うセッション（Refresh Token）は、エビデンスに残さないよう SESSION_OUT（リポジトリ外）に書き出す。
//   BASE=http://localhost:18081 DB=daipetto_ut_screen SESSION_OUT=<リポジトリ外のパス> node test/scripts/seed-screen-data.mjs
import { writeFileSync } from 'node:fs'
import { PASSWORD, http, sql, todayJst, addDays, addMonths } from './lib.mjs'

const users = {
  tanaka: ['screen-tanaka@daipetto.test', '田中', 'ROLE_USER'],
  sato: ['screen-sato@daipetto.test', '佐藤', 'ROLE_USER'],
  vet: ['screen-vet@daipetto.test', 'さくら院長', 'ROLE_HOSPITAL_ADMIN'],
  admin: ['screen-admin@daipetto.test', 'システム管理者', 'ROLE_SYSTEM_ADMIN'],
}
const token = {}
const session = {}
for (const [key, [email, nickname, role]] of Object.entries(users)) {
  await http('POST', '/api/v1/users', { body: { email, password: PASSWORD, nickname } })
  sql(`UPDATE users SET role='${role}' WHERE email='${email}';`)
  const login = (await http('POST', '/api/v1/auth/login', { body: { email, password: PASSWORD } })).data
  token[key] = login.accessToken
  const me = (await http('GET', '/api/v1/users/me', { token: login.accessToken })).data
  session[key] = { state: { refreshToken: login.refreshToken, user: me }, version: 0 }
}
const as = (key) => ({ token: token[key] })
const post = async (key, path, body) => (await http('POST', path, { ...as(key), body })).data
const today = todayJst()

// 病院（A: 院長が紐付き・診療時間あり、B: 営業中、C: 停止中）
const a = (await post('admin', '/api/v1/admin/hospitals', { name: 'さくら動物病院', address: '東京都渋谷区道玄坂1-2-3', phoneNumber: '03-1234-5678' })).hospitalId
const b = (await post('admin', '/api/v1/admin/hospitals', { name: 'みどり動物病院', address: '東京都新宿区西新宿2-8-1', phoneNumber: '03-2345-6789' })).hospitalId
const c = (await post('admin', '/api/v1/admin/hospitals', { name: 'ひかり動物病院', address: '大阪府大阪市北区梅田1-1-1' })).hospitalId
await http('PATCH', `/api/v1/admin/hospitals/${c}/suspend`, as('admin'))
await post('admin', `/api/v1/admin/hospitals/${a}/admins`, { userId: Number(sql(`SELECT id FROM users WHERE email='${users.vet[0]}';`)) })
for (const day of ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']) {
  await post('vet', `/api/v1/admin/hospitals/${a}/business-hours`, { dayOfWeek: day, openTime: '09:00', closeTime: '18:00', breakStartTime: '12:00', breakEndTime: '13:00', slotDurationMinutes: 30 })
}
await post('vet', `/api/v1/admin/hospitals/${a}/business-hours`, { dayOfWeek: 'SATURDAY', openTime: '09:00', closeTime: '12:00', slotDurationMinutes: 30 })
await post('vet', `/api/v1/admin/hospitals/${a}/schedules/generate`, { month: today.slice(0, 7) })
await post('vet', `/api/v1/admin/hospitals/${a}/schedules/generate`, { month: addMonths(today.slice(0, 7), 1) })
await post('admin', `/api/v1/admin/hospitals/${b}/schedules`, { availableDate: addDays(today, 3), startTime: '10:00', endTime: '10:30' })

// ペット・健康記録・ワクチン（田中）
const momo = (await post('tanaka', '/api/v1/pets', { name: 'モモ', petType: 'CAT', birthDate: '2021-05-10', gender: 'FEMALE', weight: 4.2, breed: 'スコティッシュフォールド', neutered: true, microchipNumber: '392141000123456' })).petId
await post('tanaka', '/api/v1/pets', { name: 'ポチ', petType: 'DOG', birthDate: '2019-08-01', gender: 'MALE', weight: 9.8, breed: '柴', neutered: false })
for (const [days, weight, symptom] of [[-60, 4.0, null], [-30, 4.1, '軽い咳'], [-7, 4.2, null]]) {
  await post('tanaka', `/api/v1/pets/${momo}/health-records`, { weight, symptom, memo: '定期チェック', recordedDate: addDays(today, days) })
}
await post('tanaka', `/api/v1/pets/${momo}/vaccinations`, { vaccineName: '3種混合ワクチン', scheduledDate: addDays(today, 5) })
const rabies = (await post('tanaka', `/api/v1/pets/${momo}/vaccinations`, { vaccineName: '狂犬病ワクチン', scheduledDate: addDays(today, -20) })).vaccinationId
await http('PATCH', `/api/v1/vaccinations/${rabies}/complete`, { ...as('tanaka'), body: { completedDate: addDays(today, -20) } })

// 予約（承認済み1件・申請中1件）と、承認による通知
const slots = (await http('GET', `/api/v1/hospitals/${a}/schedules`, as('tanaka'))).data.filter((s) => s.status === 'AVAILABLE' && s.availableDate > addDays(today, 1))
const approved = (await post('tanaka', '/api/v1/reservations', { petId: momo, hospitalId: a, scheduleId: slots[0].scheduleId, memo: '健康診断を希望します' })).reservationId
await http('PATCH', `/api/v1/admin/reservations/${approved}/approve`, as('vet'))
await post('tanaka', '/api/v1/reservations', { petId: momo, hospitalId: a, scheduleId: slots[3].scheduleId, memo: '咳が続いています' })

writeFileSync(process.env.SESSION_OUT, JSON.stringify({ hospitals: { a, b, c }, momo, sessions: session }, null, 2))
console.log(JSON.stringify({ hospitals: { a, b, c }, momo, approved }))
