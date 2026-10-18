#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ -z "${TEST_DB_PASSWORD:-}" ]]; then
  echo "TEST_DB_PASSWORD is required for the isolated MySQL integration test." >&2
  exit 1
fi

./mvnw clean test
RUN_MYSQL_E2E=true ./mvnw \
  -pl admin-module-system/admin-system-start \
  -am \
  -Dtest=PermissionBoundaryMySqlIT \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
