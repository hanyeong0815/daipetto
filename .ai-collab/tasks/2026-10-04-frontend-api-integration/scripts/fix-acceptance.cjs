// Acceptance for REVIEW-001 F-01..F-03. Same method as review-store-repro.cjs: the actual TypeScript
// stores/utilities are transpiled and executed with real Zustand; only the HTTP wrappers are replaced by
// controllable promises. Asserts the FIXED behaviour. Run from the repository root.
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const { createRequire } = require('node:module')

const root = process.cwd()
const req = createRequire(path.join(root, 'frontend/package.json'))
const ts = req('typescript')
const storage = new Map()
global.localStorage = { getItem: (k) => storage.get(k) ?? null, setItem: (k, v) => storage.set(k, v), removeItem: (k) => storage.delete(k) }
global.window = { localStorage: global.localStorage }

function load(file, mocks = {}) {
  const js = ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const module = { exports: {} }
  vm.runInNewContext(js, { module, exports: module.exports, require: (name) => mocks[name] ?? req(name), console, localStorage })
  return module.exports
}

function deferred() {
  let resolve, reject
  const promise = new Promise((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

// vmで動くストアの配列は別realmのためdeepStrictEqualの型比較が合わない。値だけを比べる
const same = (actual, expected) => assert.equal(JSON.stringify(actual), JSON.stringify(expected))

let passed = 0
async function check(name, fn) {
  await fn()
  passed++
  console.log(`PASS  ${name}`)
}

;(async () => {
  // ------------------------------------------------------------------ F-01
  const auth = load('frontend/src/stores/authStore.ts')
  let petList = () => Promise.reject(new Error('unset'))
  let petDetail = () => Promise.reject(new Error('unset'))
  const petApi = { getList: () => petList(), getDetail: (id) => petDetail(id), remove: async () => ({ success: true }) }
  const { usePetStore: pets } = load('frontend/src/stores/petStore.ts', { '../api/pet': { petApi }, './authStore': auth })
  const { useAuthStore } = auth
  const loginAs = (who) => { useAuthStore.getState().clearAuth(); useAuthStore.getState().setTokens(`${who}-access`, `${who}-refresh`) } // as LoginPage does

  await check('F-01 logout immediately drops the previous user\'s pet list and selection', async () => {
    loginAs('A')
    pets.setState({ pets: [{ id: 11, name: 'Private pet A' }], selectedPet: { id: 11, name: 'Private pet A' } })
    useAuthStore.getState().clearAuth()
    same(pets.getState().pets, [])
    assert.equal(pets.getState().selectedPet, null)
  })

  await check('F-01 A -> logout -> B: failed B fetch and late A responses never show A data', async () => {
    loginAs('A')
    pets.setState({ pets: [{ id: 11, name: 'Private pet A' }] })
    const lateList = deferred()
    const lateDetail = deferred()
    petList = () => lateList.promise
    petDetail = () => lateDetail.promise
    const inFlightList = pets.getState().fetchPets()
    const inFlightDetail = pets.getState().fetchPetDetail(11)

    useAuthStore.getState().clearAuth() // logout
    loginAs('B')
    petList = () => Promise.reject(new Error('offline'))
    await pets.getState().fetchPets()
    same(pets.getState().pets, [])
    assert.equal(pets.getState().error, 'ペット一覧の取得に失敗しました。')

    lateList.resolve({ success: true, data: [{ id: 12, name: 'Late private pet A' }] })
    lateDetail.resolve({ success: true, data: { id: 11, name: 'Late private pet A' } })
    await Promise.all([inFlightList, inFlightDetail])
    same(pets.getState().pets, [])
    assert.equal(pets.getState().selectedPet, null)
    assert.equal(pets.getState().isLoading, false)

    petList = async () => ({ success: true, data: [{ id: 21, name: 'Pet B' }] })
    await pets.getState().fetchPets()
    same(pets.getState().pets.map((p) => p.name), ['Pet B'])
  })

  await check('F-01 logging in as B without logging out also drops A data', async () => {
    loginAs('A')
    pets.setState({ pets: [{ id: 11, name: 'Private pet A' }] })
    loginAs('B')
    same(pets.getState().pets, [])
  })

  await check('F-01 control: a token refresh (setTokens only) keeps the session, so in-flight responses still apply', async () => {
    loginAs('B')
    const slow = deferred()
    petList = () => slow.promise
    const inFlight = pets.getState().fetchPets()
    useAuthStore.getState().setTokens('B-access-2', 'B-refresh-2')
    slow.resolve({ success: true, data: [{ id: 21, name: 'Pet B' }] })
    await inFlight
    same(pets.getState().pets.map((p) => p.name), ['Pet B'])
  })

  // ------------------------------------------------------------------ F-02 (shared hospital store)
  const pendingSchedules = new Map()
  const pendingDetail = new Map()
  const queue = (map, id) => { const d = deferred(); map.set(id, d); return d.promise }
  const hospitalApi = {
    getScheduleList: (id) => queue(pendingSchedules, id),
    getDetail: (id) => queue(pendingDetail, id),
    getBusinessHoursList: async () => ({ success: true, data: [] }),
    getList: async () => ({ success: true, data: [] }),
  }
  const { useHospitalStore: hospitals } = load('frontend/src/stores/hospitalStore.ts', { '../api/hospital': { hospitalApi } })

  await check('F-02 late hospital A schedules do not overwrite the current hospital B', async () => {
    const fetchA = hospitals.getState().fetchSchedules(1)
    const fetchB = hospitals.getState().fetchSchedules(2)
    pendingSchedules.get(2).resolve({ success: true, data: [{ scheduleId: 202 }] })
    await fetchB
    pendingSchedules.get(1).resolve({ success: true, data: [{ scheduleId: 101 }] })
    await fetchA
    same(hospitals.getState().schedules.map((s) => s.scheduleId), [202])
  })

  await check('F-02 failed hospital B fetch after a successful A visit leaves no A slots', async () => {
    const fetchA = hospitals.getState().fetchSchedules(1)
    pendingSchedules.get(1).resolve({ success: true, data: [{ scheduleId: 101 }] })
    await fetchA
    const fetchB = hospitals.getState().fetchSchedules(2)
    same(hospitals.getState().schedules, [])
    pendingSchedules.get(2).reject(new Error('offline'))
    await fetchB
    same(hospitals.getState().schedules, [])
    assert.equal(hospitals.getState().error, '予約枠の取得に失敗しました。')
  })

  await check('F-02 hospital detail: switching clears A at once and a late A response is ignored', async () => {
    const fetchA = hospitals.getState().fetchHospitalDetail(1)
    pendingDetail.get(1).resolve({ success: true, data: { id: 1, name: 'A' } })
    await fetchA
    const lateA = hospitals.getState().fetchHospitalDetail(1)
    const fetchB = hospitals.getState().fetchHospitalDetail(2)
    assert.equal(hospitals.getState().selectedHospital, null)
    pendingDetail.get(2).resolve({ success: true, data: { id: 2, name: 'B' } })
    await fetchB
    pendingDetail.get(1).resolve({ success: true, data: { id: 1, name: 'A (late)' } })
    await lateA
    assert.equal(hospitals.getState().selectedHospital.name, 'B')
    assert.equal(hospitals.getState().isLoading, false)
  })

  await check('F-02 control: refetching the same hospital keeps its data visible while loading', async () => {
    const first = hospitals.getState().fetchSchedules(3)
    pendingSchedules.get(3).resolve({ success: true, data: [{ scheduleId: 301 }] })
    await first
    const again = hospitals.getState().fetchSchedules(3)
    same(hospitals.getState().schedules.map((s) => s.scheduleId), [301])
    pendingSchedules.get(3).resolve({ success: true, data: [{ scheduleId: 301 }, { scheduleId: 302 }] })
    await again
    assert.equal(hospitals.getState().schedules.length, 2)
  })

  // ------------------------------------------------------------------ F-03 (fixed clocks)
  const { isUpcoming, todayInJapan } = load('frontend/src/utils/date.ts')
  const at = (iso) => new Date(iso)

  await check('F-03 15:00 JST: same-day past and current slots are not bookable, later ones are', async () => {
    const now = at('2026-10-04T06:00:00Z') // 15:00 JST
    assert.equal(isUpcoming('2026-10-04', '09:00:00', now), false)
    assert.equal(isUpcoming('2026-10-04', '14:30:00', now), false)
    assert.equal(isUpcoming('2026-10-04', '15:00:00', now), false) // backend: start must be strictly after now
    assert.equal(isUpcoming('2026-10-04', '15:30:00', now), true)
    assert.equal(isUpcoming('2026-10-05', '09:00:00', now), true)
    assert.equal(isUpcoming('2026-10-03', '23:30:00', now), false)
    assert.equal(isUpcoming('2026-10-04', '15:30', now), true) // tolerates HH:mm as in docs/07
  })

  await check('F-03 day rollover in JST (UTC is still the previous day)', async () => {
    const before = at('2026-10-04T14:59:30Z') // 23:59:30 JST 10-04
    assert.equal(todayInJapan(before), '2026-10-04')
    assert.equal(isUpcoming('2026-10-04', '23:30:00', before), false)
    assert.equal(isUpcoming('2026-10-05', '00:00:00', before), true)
    const midnight = at('2026-10-04T15:00:00Z') // 00:00:00 JST 10-05
    assert.equal(todayInJapan(midnight), '2026-10-05')
    assert.equal(isUpcoming('2026-10-05', '00:00:00', midnight), false)
    assert.equal(isUpcoming('2026-10-05', '00:30:00', midnight), true)
    const morning = at('2026-10-04T23:30:00Z') // 08:30 JST 10-05, UTC date 10-04
    assert.equal(todayInJapan(morning), '2026-10-05')
    assert.equal(isUpcoming('2026-10-05', '08:00:00', morning), false)
    assert.equal(isUpcoming('2026-10-05', '09:00:00', morning), true)
  })

  console.log(`\n${passed} checks passed`)
})().catch((e) => {
  console.error(e)
  process.exitCode = 1
})
