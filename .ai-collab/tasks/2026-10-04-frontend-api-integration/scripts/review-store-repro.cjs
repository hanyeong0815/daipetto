// Reviewer harness: executes actual TypeScript stores, replacing only their HTTP dependencies.
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const { createRequire } = require('node:module')
const root = process.cwd()
const req = createRequire(path.join(root, 'frontend/package.json'))
const ts = req('typescript')
const storage = new Map()
global.localStorage = { getItem: k => storage.get(k) ?? null, setItem: (k,v) => storage.set(k,v), removeItem: k => storage.delete(k) }
global.window = { localStorage: global.localStorage }
function load(file, mocks) {
  const js = ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const module = { exports: {} }
  vm.runInNewContext(js, { module, exports: module.exports, require: name => mocks[name] ?? req(name), console, localStorage })
  return module.exports
}
;(async () => {
  let fail = false
  let resolveOld
  const oldResponse = new Promise(resolve => { resolveOld = resolve })
  const petApi = { getList: () => fail ? Promise.reject(new Error('offline')) : oldResponse }
  const { usePetStore: pets } = load('frontend/src/stores/petStore.ts', { '../api/pet': { petApi } })
  const { useAuthStore: auth } = load('frontend/src/stores/authStore.ts', {})
  auth.getState().setTokens('account-A-access', 'account-A-refresh')
  pets.setState({ pets: [{ id: 11, name: 'Private pet A', weight: 4.7, petType: 'CAT' }] })
  const inFlight = pets.getState().fetchPets()
  auth.getState().clearAuth()
  auth.getState().setTokens('account-B-access', 'account-B-refresh')
  fail = true
  await pets.getState().fetchPets()
  assert.equal(pets.getState().pets[0].name, 'Private pet A')
  resolveOld({ success: true, data: [{ id: 12, name: 'Late private pet A', petType: 'CAT' }] })
  await inFlight
  assert.equal(pets.getState().pets[0].name, 'Late private pet A')
  console.log('F-01 reproduced: A cache survives B login/fetch failure; late A response repopulates B session.')

  let resolveA, resolveB
  const pendingA = new Promise(r => { resolveA = r })
  const pendingB = new Promise(r => { resolveB = r })
  const hospitalApi = { getScheduleList: id => id === 1 ? pendingA : pendingB }
  const { useHospitalStore: hospitals } = load('frontend/src/stores/hospitalStore.ts', { '../api/hospital': { hospitalApi } })
  const fetchA = hospitals.getState().fetchSchedules(1)
  const fetchB = hospitals.getState().fetchSchedules(2)
  resolveB({ success: true, data: [{ scheduleId: 202, hospitalId: 2 }] })
  await fetchB
  resolveA({ success: true, data: [{ scheduleId: 101, hospitalId: 1 }] })
  await fetchA
  assert.equal(hospitals.getState().schedules[0].hospitalId, 1)
  console.log('F-02 reproduced: late hospital A response overwrites current hospital B schedules.')
})().catch(e => { console.error(e); process.exitCode = 1 })
