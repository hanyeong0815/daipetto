// R-08 regression check: queued requests must settle when the refresh call fails.
// Transpiles the real frontend/src/api/client.ts and drives its 401 interceptor
// with stubbed axios / auth store transport. Isolated client-logic check, not browser E2E.
const fs = require('fs')
const path = require('path')

const frontendRoot = process.cwd()
const ts = require(path.join(frontendRoot, 'node_modules', 'typescript'))

const clientPath = process.argv[2] || path.join(frontendRoot, 'src', 'api', 'client.ts')
const source = fs
  .readFileSync(clientPath, 'utf8')
  .replace(/import\.meta\.env/g, '__ENV__')

const transpiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
}).outputText

let capturedErrorHandler = null
const retriedRequests = []

const apiClient = function (config) {
  retriedRequests.push(config)
  return Promise.resolve({ status: 200, config })
}
apiClient.interceptors = {
  request: { use: () => {} },
  response: {
    use: (_onFulfilled, onRejected) => {
      capturedErrorHandler = onRejected
    },
  },
}

const refreshFailure = new Error('refresh endpoint returned 401')
const axiosStub = {
  create: () => apiClient,
  post: () => Promise.reject(refreshFailure),
}

let cleared = false
const authStoreStub = {
  useAuthStore: {
    getState: () => ({
      accessToken: 'expired-access-token',
      refreshToken: 'stored-refresh-token',
      setTokens: () => {},
      clearAuth: () => {
        cleared = true
      },
    }),
  },
}

const stubs = {
  axios: Object.assign(axiosStub, { default: axiosStub }),
  '@capacitor/core': { Capacitor: { getPlatform: () => 'web' } },
  '../stores/authStore': authStoreStub,
}

const moduleExports = {}
const factory = new Function('require', 'module', 'exports', '__ENV__', transpiled)
factory(
  (name) => {
    if (!(name in stubs)) throw new Error('unexpected import: ' + name)
    return stubs[name]
  },
  { exports: moduleExports },
  moduleExports,
  { VITE_API_BASE_URL_WEB: 'http://localhost:8080' },
)

if (typeof capturedErrorHandler !== 'function') {
  console.error('FAIL: response interceptor error handler was not registered')
  process.exit(1)
}

const unauthorized = () => ({ response: { status: 401 }, config: { headers: {} } })

const settled = { first: 'PENDING', queued: 'PENDING' }

const first = capturedErrorHandler(unauthorized())
const queued = capturedErrorHandler(unauthorized())

first.then(
  () => (settled.first = 'resolved'),
  () => (settled.first = 'rejected'),
)
queued.then(
  () => (settled.queued = 'resolved'),
  () => (settled.queued = 'rejected'),
)

setTimeout(() => {
  console.log('first request :', settled.first)
  console.log('queued request:', settled.queued)
  console.log('clearAuth called:', cleared)
  console.log('retried requests:', retriedRequests.length)

  const ok = settled.first === 'rejected' && settled.queued === 'rejected'
  console.log(ok ? 'PASS: every waiter settled after refresh failure' : 'FAIL: a waiter is still pending')
  process.exit(ok ? 0 : 1)
}, 200)
