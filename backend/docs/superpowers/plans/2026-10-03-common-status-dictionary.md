# Common Status and Dictionary Implementation Plan

> **For agentic workers:** Use the executing-plans workflow in this session. The user already authorized execution and requested no Git commits during development.

**Goal:** Centralize fixed enabled/disabled semantics and add maintainable dictionary types and items without changing existing status values or API payloads.

**Architecture:** `admin-base-core` owns the fixed `CommonStatus` enum and conversion. `admin-system-biz` owns dictionary persistence, permission checks and HTTP endpoints. Frontend shares status options and adds a dictionary management page using existing Vben patterns.

**Tech Stack:** Java 21, Spring Boot 4, MyBatis-Plus, Flyway, Vue 3, Vben Admin.

**Spec:** The plan agreed in the immediately preceding user conversation.

## Global Constraints

- Database enabled=1, disabled=0; API enabled=ENABLED, disabled=DISABLED.
- Do not alter published Flyway migrations or existing API routes/payloads.
- `mustChangePassword` and deletion flags are not enabled/disabled statuses.
- Dictionary item values for built-in common status cannot change.
- Do not create Git commits.

## Review Focus

- Invalid status rejected as a client error, without saving 0 by accident.
- Fixed status items cannot be deleted or assigned new values.
- Disabled dictionary items are absent from public options but visible in management.
- Duplicate dictionary codes and item values are rejected.
- New endpoints are protected by server permissions.

---

### Task 1: Fixed common status

**Files:** New `admin-base-core/.../common/constant/CommonStatus.java`; update existing system services and frontend shared status module.

- [x] Add tests for status conversions and invalid input.
- [x] Add `CommonStatus` and replace duplicate conversion methods.
- [x] Consolidate frontend status options and verify build/tests.

### Task 2: Dictionary persistence and API

**Files:** `schema/V12__system_dictionary.sql`; new `dict/entity`, `dict/mapper`, `dict/service`, `dict/service/impl`, `dict/controller`; resource menus and permissions.

- [x] Add migration with dictionary types, items, unique constraints and fixed common status rows.
- [x] Add repository/service tests for uniqueness, built-in protection and option filtering.
- [x] Implement MyBatis-Plus mappers, service interfaces/impl and guarded REST endpoints.
- [x] Run isolated MySQL integration test.

### Task 3: Dictionary management UI

**Files:** Frontend API system types, `views/system/dict`, route/permissions and translations.

- [x] Add dictionary API types and wrappers.
- [x] Add type and item management table/forms with status display and protected built-in values.
- [x] Add shared option loader and use it where useful.
- [x] Run frontend checks and backend full suite.

### Task 4: Documentation and final verification

**Files:** Backend/frontend `AGENTS.md`, `README.md`, project structure docs.

- [x] Document fixed status versus editable dictionaries.
- [x] Verify route compatibility, MySQL migration, all tests and builds.

## Verification

- `./mvnw -q clean test`: passed
- `PermissionBoundaryMySqlIT` against isolated MySQL schema: passed, including V12 and dictionary authorization
- `pnpm --filter @vben/web-antdv-next typecheck`: passed
- `pnpm --filter @vben/web-antdv-next build`: passed
- ESLint on changed frontend files: passed
