# Browser verification data for the frontend integration

Manual check, not part of any build. Needs the `daipetto-postgres` container, a backend started
against a throwaway database, and the Vite dev server pointed at that backend. Nothing is written to
`daipetto`, and the usual 8080 backend is not used.

```bash
# 1. scratch database
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "CREATE DATABASE daipetto_fe_1004;"

# 2. backend on 18081 (CORS already allows http://localhost:5173)
cd spring-api && ./gradlew bootJar && cd ..
java --enable-preview -jar spring-api/build/libs/spring-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local --DB_NAME=daipetto_fe_1004 --server.port=18081

# 3. frontend on 5173 against 18081 (a process env var overrides .env.local)
cd frontend && VITE_API_BASE_URL_WEB=http://localhost:18081 npm run dev -- --port 5173 --strictPort

# 4. fixtures
node .ai-collab/tasks/2026-10-04-frontend-api-integration/scripts/seed-fe.mjs

# 5. cleanup
docker exec daipetto-postgres psql -U daipetto_user -d postgres -c "DROP DATABASE daipetto_fe_1004;"
```

`seed-fe.mjs` creates a user and a system admin, one cat, one hospital, nine schedules over the
next three JST days with the third one BLOCKED, and three reservations: approved, rejected, and
approved then completed. Those produce four notifications (two approvals, one rejection, one
completion). It also creates three health records
dated 30, 14 and 2 days ago. Log in as `fe-user@daipetto.test`; the fixture password is in the
script and exists only in the scratch database.

For REVIEW-001 it also adds today's slots on hospital 1: 00:00 (already started) and 23:30 (later today,
so run before 23:30 JST). It adds an APPROVED reservation that started at 00:30 today, which must not be
the dashboard's "next" reservation. It creates a second hospital (one slot four days ahead) to switch to,
and a second account `fe-user2@daipetto.test` with its own pet. Past-time rows are inserted by SQL because
the API refuses them.

## `fix-acceptance.cjs` — REVIEW-001 F-01..F-03 (10 checks, no backend needed)

```bash
node .ai-collab/tasks/2026-10-04-frontend-api-integration/scripts/fix-acceptance.cjs
```

Same method as the reviewer's `review-store-repro.cjs`. The real `authStore`/`petStore`/`hospitalStore`/`utils/date.ts`
are transpiled with the project's TypeScript and run with real Zustand; only the HTTP wrappers are replaced
by controllable promises. It asserts the fixed behaviour. The cases are A→logout→B with a failed B fetch and
late A responses, re-login without logout, and refresh as a control. It reorders hospital responses, fails
B after A, and checks a same-hospital refetch as a control. It also covers fixed clocks at 15:00 JST and
around midnight. `review-store-repro.cjs` now stops at module loading, because `petStore` imports
`./authStore`, which that harness does not provide.

The component-level behaviour of `ReservationPage` (keyed remount, local state, gating, retry) is not
covered here; it was checked in the browser by delaying or failing specific XHRs.
