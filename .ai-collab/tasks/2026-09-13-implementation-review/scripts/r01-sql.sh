#!/usr/bin/env bash
# R-01 mechanism check: refresh interleaved with logout / login.
# Reproduces REVIEW-002's scenario on the real schema, comparing the old statement
# order (no user-row lock) with the new one (SELECT ... FOR UPDATE first).
set -u
U=daipetto_user
DB=${1:-daipetto_r01_sql}
PSQL="docker exec -i daipetto-postgres psql -U $U -d $DB -q"
# the race sessions keep command tags (UPDATE 1 / INSERT 0 1) so affected-row counts are visible
PSQL_TAGS="docker exec -i daipetto-postgres psql -U $U -d $DB"

docker exec daipetto-postgres psql -U $U -d postgres -q -c "DROP DATABASE IF EXISTS $DB;" -c "CREATE DATABASE $DB;"
for f in spring-api/src/main/resources/db/migration/V*.sql; do
  docker exec -i daipetto-postgres psql -U $U -d $DB -v ON_ERROR_STOP=1 -q < "$f"
done

seed() {
  $PSQL -c "TRUNCATE users, refresh_tokens RESTART IDENTITY CASCADE;" >/dev/null
  $PSQL -c "INSERT INTO users (id,email,password,nickname) VALUES (1,'race@example.com','x','racer');" >/dev/null
  $PSQL -c "INSERT INTO refresh_tokens (user_id,token,expires_at) VALUES (1,'old', now()+interval '14 days');" >/dev/null
}

active() { $PSQL -t -A -c "SELECT coalesce(string_agg(token,','),'(none)') FROM refresh_tokens WHERE user_id=1 AND revoked=false;"; }

run_case() {
  local label="$1" lock="$2" b_inserts="$3"
  seed
  # Session A: refresh. Pauses after consuming the old token, before inserting the successor.
  $PSQL_TAGS > /tmp/r01-a.log 2>&1 <<SQL &
BEGIN;
${lock}
UPDATE refresh_tokens SET revoked=true WHERE token='old' AND revoked=false;
SELECT pg_sleep(4);
INSERT INTO refresh_tokens (user_id,token,expires_at) VALUES (1,'refresh-successor', now()+interval '14 days');
COMMIT;
SQL
  sleep 1
  # Session B: logout (and login when it also inserts), started while A is paused.
  $PSQL_TAGS > /tmp/r01-b.log 2>&1 <<SQL
BEGIN;
${lock}
UPDATE refresh_tokens SET revoked=true WHERE user_id=1 AND revoked=false;
${b_inserts}
COMMIT;
SQL
  wait
  echo "$label"
  echo "   A: $(grep -E '^UPDATE|^INSERT' /tmp/r01-a.log | tr '\n' ' ')"
  echo "   B: $(grep -E '^UPDATE|^INSERT' /tmp/r01-b.log | tr '\n' ' ')"
  echo "   active tokens after both commits: $(active)"
}

LOCK="SELECT id FROM users WHERE id=1 FOR UPDATE;"
INS_LOGIN="INSERT INTO refresh_tokens (user_id,token,expires_at) VALUES (1,'login-successor', now()+interval '14 days');"

echo "=== OLD statement order (no user-row lock) — the defect REVIEW-002 reported ==="
run_case "refresh + logout" "" ""
run_case "refresh + login " "" "$INS_LOGIN"

echo
echo "=== NEW statement order (user row locked first) — current implementation ==="
run_case "refresh + logout" "$LOCK" ""
run_case "refresh + login " "$LOCK" "$INS_LOGIN"

docker exec daipetto-postgres psql -U $U -d postgres -q -c "DROP DATABASE $DB;"
echo
echo "scratch database $DB dropped"
