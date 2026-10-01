# SecureTravels CRM — Phase 7 Step 0 Delta (Sales CRM Depth)

> **Status: ratified 2026-10-01 (Step 0); Modules 1–3 shipped 2026-10-01 (§7, §8, §9).**
> This file is the overlap audit and
> build plan for Phase 7 — Accounts (B2B/corporate/travel-agent), configurable
> pipeline stages, revenue forecasting, and commission calculation. It records
> exactly what the Phase 7 kickoff assumed already exists vs what the codebase
> actually has, classifies each module as **extend existing / build new**, and
> fixes the package + migration plan. It must be updated whenever a module
> ships. Supporting docs to update in lockstep: `ARCHITECTURE.md`,
> `DATABASE.md`, `SECURITY.md`, `ROADMAP.md` (status).

---

## 1. Method

Every "reuse" anchor named in the Phase 7 kickoff was re-verified against the
live code (not assumed). Sources inspected: all 18 migrations (V1–V18), the
full `backend/src/main/java/com/securetravels/crm` package tree, `BaseIT`,
`application.yml` / `application-test.yml`, and the Phase 1–6 docs.

## 2. Kickoff claims vs reality

| Kickoff anchor ("reuse this") | Verdict | Evidence |
|---|---|---|
| **Phase 2 Module 5 real GSTIN validator** | **Does not exist as claimed.** Only a loose 15-char `^[0-9A-Z]{15}$` regex on `vendors.gstin`. No checksum, no state/PAN/Z check, DTO regex is case-insensitive (`[0-9A-Za-z]{15}`). No Invoice entity/table at all. | `vendors/VendorService.java:115-122`, `vendors/dto/VendorCreateRequest.java:33-35`, `V9__vendor_catalogue.sql:17` |
| **Phase 2 Module 6 "Money We Owe Vendors" payables** | **Does not exist.** Zero `Payable`/`PAYABLE` hits outside a javadoc. No settlement, no aging, no payout. `vendors` is master data only. Full payables remain Phase 9 per `ROADMAP.md`. | whole-tree grep; `commission/CommissionLedger.java:32` |
| **Phase 3 Team Performance revenue-per-consultant** | **Exists and is the correct attribution spine.** `sales_commission_ledger` (V13): `sum(net_amount)` where `consultant_id` + `revoked_at is null`, windowed on `credited_at`. One immutable credit per booking, revoke-never-delete. | `AnalyticsService.java:346-480`, `commission/CommissionLedgerService.java`, `BookingService.java:254,331-334` |
| **Phase 1 approval queue** | **Does not exist as a queue.** Only three narrow mechanisms: inline `bookings.discount_approved_by` (BookingService.confirm:226-234 + `DiscountPolicy`, auto-approve ≤5% of gross), automation `REQUEST_APPROVAL` (service-only, zero HTTP surface), `channel_templates.approval_status` (no controller). | audit grep; `booking/BookingService.java`, `automation/runtime/WorkflowRunService.java:301-325,420-442` |
| **Phase 1 Kanban board** | **No backend for it.** Single match is a stale frontend comment in `LeadFilters.tsx`. Lead listing is a flat paginated `GET /api/leads`. Not needed by Phase 7. | grep whole tree |
| **Phase 1 Lead pipeline / statuses** | **Exists but NOT the 7-stage design the kickoff describes.** Actual enum: `NEW, INTERESTED, QUOTATION_SENT, BOOKING_CONFIRMED, LOST` with a hard-coded `TRANSITIONS` map. The kickoff's New→Contacted→Qualified→Quotation Sent→Negotiation→Won is a *different* model and must be built as **Opportunities**, not by mutating `Lead.Status`. | `lead/Lead.java:29`, `lead/LeadService.java:51-56,341-348` |
| **Customer 360 aggregation pattern** | **Exists** — dedup by `mobile_digits`, `fromLead(...)` factory, `maintainAggregates(...)` recomputes totals, live-recompute on detail GET. This is the template for **Account 360**. | `customer/Customer360Service.java:143-166`, `customer/Customer360.java`, `booking/BookingService.java:412-436` |
| **CEO dashboard** | **Exists as generic dashboards only**: `/api/dashboard/summary` (company-wide to any authenticated user, incl. SALES — a known visibility gap), `/api/dashboard/performance`, `/api/analytics/*`. No CEO-specific surface (Phase 10). | `dashboard/DashboardController.java`, `analytics/AnalyticsController.java` |
| **Phase 6 workflow engine** | **Exists** (V17/V18, `automation/`). Available for forecast/pipeline automation; NOT required by Phase 7 core. Its `REQUEST_APPROVAL` may later power partner-commission approval. | `automation/` packages, V18. |
| **Feature-flag / config pattern** | **`AppProperties` only (env/YAML, no admin surface).** There is **no settings table anywhere** (38 tables, none settings). Pipeline-stage configuration and commission-plan administration are therefore **greenfield** — new tables, not `AppProperties`. | `common/config/AppProperties.java`, migration inventory |

**Additive gaps confirmed by audit (not covered by any kickoff anchor):**

- `LeadService` ownership is `SEES_ALL = {MANAGER, ADMIN, CEO}` (**OPS excluded** for leads, included for bookings/analytics). Phase 7 commission/pipeline visibility must define its own boundaries explicitly; the non-negotiable *commission visibility 403 cross-owner* test will enforce it.
- `LeadCreateRequest.ownerId` is accepted unvalidated from any SALES user; assignment is immutable afterwards. Opportunities should inherit `leads.owner_id` as primary ownership to stay consistent.
- Team Performance deliberately **ignores** the `source` filter and reads only `net_amount` from the ledger (gross/discount/tax stored but unread). Phase 7 commission reports must decide explicitly which basis they consume.
- `customer360` list endpoint reads denormalised columns while detail recomputes live (documented hazard). Account 360 must pick one contract and repeat it.

## 3. Package & schema decisions (ratified)

### 3.1 New package: `com.securetravels.crm.accounts`

Phase 7 owns the "Sales CRM Depth" module boundary. `accounts/` carries:
Account entity, Account 360, Opportunity + configurable PipelineStage,
Forecast, and the travel-agent commission payable (a payable *shape*, not the
Phase 9 payables subsystem). Empty `package-info.java` placeholders are NOT
created because `accounts/` ships for real this phase.

### 3.2 Extended packages

- `commission/` — gain `CommissionPlan`, `CommissionTier`, plan assignment,
  and the automated calculation invoked from `BookingService.confirm`
  (extending the existing `CommissionLedgerService.credit` call contract, not
  replacing the sales consultant ledger).
- `analytics/` — gain the three Phase 5 reports (pipeline, forecast,
  commission) following the existing `AnalyticsService` + `CommissionLedger`
  pattern (NamedParameterJdbcTemplate, `ReportFilter`, `scopeToCaller`).
- `common/config/AppProperties.java` + `application*.yml` — add the
  `app.feature-flags.partner-commissions` gate (and any sibling flags) exactly
  as the kickoff requires for Module 1.

### 3.3 Migration plan (`db/migration/`, next numbers confirmed V1–V18 → V19+)

| Migration | Ships with | Contents (summary) |
|---|---|---|
| `V19__accounts_and_links.sql` | Module 1 | `accounts` (type CORPORATE/TRAVEL_AGENT, name, GSTIN, billing/contact fields, status, timestamps); nullable `account_id` FK on `leads` + `bookings`; travel-agent commission payable table (`booking_id` UNIQUE, account, amounts, status, settle fields); account-first `invoices` (INV-ref, ISSUED/VOID) |
| `V20__pipeline_stages.sql` | Module 2 | `pipeline_stages` (key, label, sort order, probability weight, entry condition metadata) + seeded **default** stages qualified by the kickoff defaults (Qualified 20%, Quotation Sent 40%, Negotiation 70%) |
| `V21__opportunities_forecast.sql` | Module 3 | `opportunities` (subject = lead/account, `stage_id`, expected value, expected date, ownership, close fields). **Shipped without rollup columns** — forecast is live-computed (§9) |
| `V22__commission_plans.sql` | Module 4 | `commission_plans`, `commission_tiers`, `account_commission_plans` |
| `V23__sales_reporting.sql` | Module 5 | indexes/read-model support for pipeline + forecast + commission reports (aggregates only if measurement shows they're needed) |

Schema conventions carried forward: UUID PKs, `numeric(12,2)` money, varchar +
CHECK (never PG enums), `created_at/updated_at/version` on mutable tables,
append-only `created_at` on event tables, `ddl-auto: validate`.

### 3.4 BaseIT impact

`BaseIT.truncateAll()` truncates a hardcoded list via
`TRUNCATE ... RESTART IDENTITY CASCADE` (`base` + the 36 tables). Every new
table from V19–V23 **must be added to this list in the same commit** that adds
the migration, or tests will leak rows across IT classes.

## 4. Per-module plan (extend / new)

**Module 1 — Account records + Account 360 (mostly NEW, reusing patterns)**

- **New:** `accounts` entity + CRUD + links (`leads.account_id`,
  `bookings.account_id` nullable); travel-agent commission payable; Account 360
  aggregation (modeled on `Customer360Service.maintainAggregates` +
  `customer360` detail contract — pick the live-recompute contract).
- **New-but-reusing-pattern:** account-aware invoicing. Kickoff assumes
  "Phase 2 Module 5 invoicing" exists; it does **not**. Module 1 therefore
  introduces a minimal account-first invoice (invoice-to-account with the
  account's sanitized billing GSTIN; retail bookings produce no invoice,
  preserving today's behavior exactly = the "no retail regression" test).
- **Refactor, small:** consolidate GSTIN validation into a single shared
  validator (full Indian GSTIN format: state code, PAN, entity-type `Z`,
  mod-36 checksum), used by `vendors` and `accounts`; keep `vendors` behavior
  green via existing tests.
- **Gate:** everything partner-commission is behind
  `app.feature-flags.partner-commissions` (default, per kickoff).
- **Ownership:** MANAGER/ADMIN/CEO manage accounts; SALES/OPS read account
  360 for their own leads; commission payable visibility = the 403 test.

**Module 2 — Pipeline stage configuration (NEW)**

- `pipeline_stages` table, admin CRUD (MANAGER/ADMIN/CEO), ordered stages,
  per-stage probability weight and entry-condition text, `active` flag.
- **Decision (rationale in §2):** configurable stages apply to a new
  `Opportunity` model. `Lead.Status`, its `TRANSITIONS` map, the follow-up
  cadence chain, the funnel SQL, and `DashboardService.summary` are all
  hard-coupled to the fixed 5-value enum; mutating it would break Phase 1
  invariants and the funnel report. Opportunities reuse `leads.owner_id` for
  ownership.
- Seed the kickoff default stages (Qualified 20 / Quotation Sent 40 /
  Negotiation 70) as reference data; admin may edit.

**Module 3 — Revenue forecasting (NEW)**

- Forecast over opportunities × stage probability: weighted (expected-value)
  and best/committed views, windowed by expected date; reuse the
  `ReportFilter` half-open date window convention.

**Module 4 — Commission calculation (NEW engine, reusing ledger discipline)**

- `commission_plans` (basis NET|GROSS; method PERCENT|FIXED|TIERED; min-sales
  threshold), `commission_tiers` (boundary-aware, exact-edge behavior fixed by
  tests), plan assignment to accounts.
- Automated computation inside `BookingService.confirm` beside the existing
  sales-consultant credit-account; writes the travel-agent commission payable
  (Module 1 table) **only** when the flag is on and the booking has an
  account+plan. Never edits `sales_commission_ledger` (its immutability
  contract stands).
- **Non-negotiable test:** tiered-boundary exactness (edge amounts), separate
  from visibility.

**Module 5 — Reporting (NEW, extend `analytics/`)**

- Pipeline report, forecast report, commission report (per-account and
  per-plan), all respecting `scopeToCaller` / explicit visibility boundaries.

## 5. Build order & gates (from the kickoff — unchanged)

1. Module 1 → 2 → 3 → 4 → 5, strict order; test + report per module; commit
   and push after each module.
2. Update `ARCHITECTURE.md`, `DATABASE.md`, `SECURITY.md`, `ROADMAP.md` in the
   same change as each module's code.
3. After Module 5: full suite, security review, `docs/PHASE_7_SIGNOFF.md`.
4. Phase 7 hardening gates — four non-negotiable tests:
   - commission correctness incl. tiered boundaries,
   - pipeline validation (no invalid stage transitions / entry conditions),
   - commission visibility 403 cross-owner,
   - account-linked invoicing with no retail regression.
5. Do **not** start Phase 8 until the user confirms acceptance of Phase 7.

## 6. Known risks carried into implementation

- Invoicing assumed-but-absent (§4 Module 1) — the delta is the largest single
  surprise; scope held minimal and regression-safe.
- No admin-config surface exists at all; pipeline/commission admin is net-new
  UI+API+table (no precedent to reuse, but no conflict either).
- `customer360` list/detail denormalisation divergence is a documented hazard;
  Account 360 will not repeat it.
- Commission money is `numeric(12,2)`; tier rounding uses half-up at the
  `scale=2` boundary, asserted in tests.

## 7. Module 1 — shipped 2026-10-01

Shipped in the same commit as this update: `accounts/` package, V19, tests,
and the support changes below.

- **New `com.securetravels.crm.accounts` package** — `Account`, `AccountRepository`,
  `AccountService`, `AccountController` (`/api/accounts`, `/{id}`,
  `/{id}/detail` Account 360, POST/PATCH, `/{id}/payables` + settle,
  `/{id}/invoices`, `/invoices/{invoiceId}`), `AccountCommissionPayable` +
  repository + `AccountCommissionPayableService` (credit on confirm, void on
  cancel, settle), `AccountInvoice` + repository + `AccountInvoiceService`
  (issue on confirm, void on cancel), DTOs, `package-info.java`.
- **`V19__accounts_and_links.sql`** — as ratified with two implementation
  corrections from §3.3: there is **no `account_contacts` table**; primary
  contact fields (`primary_contact_name/email/phone`) live on the `accounts`
  row and are sanitised on write like customer PII. FK columns are named
  `account_id` (not `accounts_id`).
- **GSTIN validator consolidated** — `common/util/GstinValidator.java`: full
  Indian format (state code, PAN, entity `Z`, mod-36 checksum). Blank ⇒ `null`;
  present-but-invalid ⇒ `BadRequestException`. Used by `accounts`; `vendors`
  behaviour unchanged (still its loose regex).
- **Gate config** — `app.feature-flags.partner-commissions` (default `false`)
  and `app.commission.default-travel-agent-percent` (10.00) added to
  `AppProperties` + `application.yml` + `application-test.yml`.
- **Behaviour (regression-safe)** — invoice row created on confirm **only** when
  the booking carries an `accountId` (retail = unchanged); commission payable
  credited **only** when flag on + account type `TRAVEL_AGENT`; cancel voids
  the invoice and voids the payable (PAID payable on cancel ⇒ warn + leave).
- **Ownership** — account writes / Account 360 detail / payables / invoices are
  MANAGER+; reads are any authenticated user (SALES/OPS included). Commissions
  credited at 10% of NET by default; paid/settled via management settle.
- **Tests** — `GstinValidatorTest` (unit), `AccountFlowIT` (gate off: CRUD,
  RBAC, GSTIN checksum + duplicate, lead/booking links, invoice-on-confirm,
  cancel void, deactivation), `AccountBillingFlowIT` (gate on: 10% payable,
  settle, void-on-cancel, 360 sums, retail no-payable). Full suite green
  (486 tests, 0 failures, 11 skipped — Rabbit/Docker).
- **BaseIT** truncate list extended with `accounts`, `account_commission_payables`,
  `invoices`. `Lead`/`Booking` entities + DTOs carry `accountId`.

Module 2 (pipeline stages) is next.

## 8. Module 2 — shipped 2026-10-01

Shipped in the same commit as this update: `V20__pipeline_stages.sql`, the
`PipelineStage` entity/repository/service/controller + DTOs, and
`PipelineStageFlowIT`.

- **Schema** — `pipeline_stages` (immutable `stage_key` UNIQUE, `label`,
  `sort_order >= 0`, `probability_weight numeric(5,2)` CHECK 0–100,
  `entry_condition` text, `is_active`), seeded with the kickoff defaults
  (Qualified 20 / Quotation Sent 40 / Negotiation 70) under fixed ids.
  A **partial unique index on `sort_order WHERE is_active`** makes list order
  deterministic; the service turns key/slot collisions into `409 CONFLICT`.
- **Package** — lives in `com.securetravels.crm.accounts` per §3.1 (the Phase 7
  boundary), served at `GET/POST /api/pipeline-stages`,
  `GET/PATCH/DELETE /api/pipeline-stages/{id}`.
- **RBAC** — reads (active list, by id) any authenticated user; writes,
  deactivation, and `?includeInactive=true` listing are MANAGER–CEO (enforced
  in `PipelineStageService`).
- **Guards** — cannot deactivate **or** delete the last remaining active stage;
  `key` is immutable after create; weights/sort validated at DTO + DB; entries
  OWASP-sanitized; create/update/delete audited as `PIPELINE_STAGE`.
- **Tests** — `PipelineStageFlowIT`: seeded-order list with weights,
  unordered insert, validation (150 weight, bad key, empty label), duplicate
  key/slot → 409, reorder + deactivate + last-active refusal, inactive listing
  gating (sales → 403), sales read-only, 404. Full suite green (493 tests,
  0 failures, 11 skipped — Rabbit/Docker).
- **BaseIT** — `pipeline_stages` is reference data (like `whatsapp_templates` /
  `channel_templates`): not truncated; instead the seeded set is restored
  before each test (delete-all + re-insert the three defaults). The reset runs
  **after** the table truncation list, so once Module 3 added the
  `opportunities.stage_id` FK the ordering keeps it safe.

## 9. Module 3 — shipped 2026-10-01

Shipped in the same commit as this update: `V21__opportunities_forecast.sql`, the
`Opportunity` entity/repository/service/controller + DTOs (including
`ForecastResponse`), and `OpportunityFlowIT`.

- **Schema** — `opportunities`: `lead_id` **UNIQUE** FK (one opportunity per
  lead), `account_id`/`owner_id` FKs, `stage_id` FK → `pipeline_stages`,
  `expected_value numeric(12,2)` CHECK `>= 0`, `expected_date date`,
  `status` OPEN/WON/LOST, `stage_moved_at`, `closed_at`/`closed_by`/`closing_note`,
  plus `chk_opportunity_close_consistency` (OPEN ⇒ close fields NULL; terminal ⇒
  both set). Indexes on `(owner_id, status)`, `stage_id`, `expected_date`, `lead_id`.
- **Package** — `com.securetravels.crm.accounts` per §3.1; served at
  `GET/POST /api/opportunities`, `GET /api/opportunities/{id}`,
  `PATCH /api/opportunities/{id}/stage`, `POST /api/opportunities/{id}/close`,
  and `GET /api/forecast?from&to` (both bounds required).
- **RBAC** — create is SALES–CEO (OPS → 403); list/get/move/close are
  ownership-scoped in the service (MANAGER–CEO may pass `ownerId`, SALES/OPS are
  pinned to their own rows) exactly as `LeadService` does. The owner is
  **snapshotted from `leads.owner_id`**, never accepted from the client.
- **Guards** — one opportunity per lead → 409; opportunity on a LOST lead → 409;
  a second close or any stage move on a terminal opportunity → 409; only *active*
  stages are valid targets (unknown → 404, deactivated → 409); no active stage
  → 409; `from >= to` → 400. Stage moves/outcomes audited as `statusChange`.
- **Forecast** — computed **live** from the current stage weights over the
  half-open `[from, to)` window: `expected = Σ(value × weight/100)` (HALF_UP,
  scale 2), `best = Σ open`, `won = Σ WON`, grouped by stage and by month.
  Deviation from §3.3, deliberate: **no materialisation/rollup columns**. The
  kickoff's rationale (re-weighting a stage must immediately change every
  forecast) is fully satisfied by live compute, and a rollup table would be a
  second source of truth to rebuild and drift. Deferred to Module 5, to be added
  only if measurement of volume/latency demands it.
- **Repository note** — the scoped search is a native query with explicit
  `CAST(:param AS …)` on every optional filter. A JPQL `(:param is null or …)`
  form compiles fine but leaves Postgres unable to type-infer the null-check
  placeholders (`could not determine data type of parameter $n` → 500); the cast
  form is the established convention (`LeadRepository.search`).
- **Tests** — `OpportunityFlowIT`: create/move/close + forecast math (20000 @ 40% =
  8000, 50000 @ 70% = 35000), cross-owner 403s (list, get, move, close),
  one-per-lead/LOST-lead/OPS-create → 409/403, terminal-stage + window
  validation, stage and month bucketing (43000 expected / 70000 best / 8000 won).
  Full suite green (498 tests, 0 failures, 11 skipped — Rabbit/Docker).

## 10. Module 4 — shipped 2026-10-01

Commission is no longer "flat rate × net, hardcoded". It is a per-account
**commission plan**, still falling back to the Module 1 flat rate when an account
has none, so no existing behaviour regresses.

- **Schema (V22)** — `commission_plans` (`plan_key` UNIQUE uppercase snake_case,
  `label`, `basis` NET/GROSS, `method` PERCENT/FIXED/TIERED, `rate_percent`
  numeric(5,2), `fixed_amount` numeric(12,2), `min_sales_threshold numeric(12,2)`,
  `active`, `notes`, audit columns) with `chk_commission_plan_method_inputs`
  enforcing that PERCENT carries a rate, FIXED a fee, and TIERED neither;
  `commission_tiers` (`plan_id` FK **ON DELETE CASCADE**, `from_amount`,
  `to_amount` NULL = open-ended, `rate_percent`) unique on `(plan_id, from_amount)`;
  `account_commission_plans` (`account_id` FK, `plan_id` FK, `assigned_by`,
  `assigned_at`, `active`) with partial unique `idx_account_commission_plans_active`
  (one active row per account); `account_commission_payables.plan_id` FK added for
  provenance.
- **Package** — new `com.securetravels.crm.commission`. The payable ledger stays in
  `accounts`; plans only change *how* the amount is derived.
- **Calculation** — `CommissionCalculator` is pure (no Spring, no clock), so the
  arithmetic is unit-testable at exact boundaries. `NET = gross − discount + tax`;
  `GROSS` ignores the discount. Tiers are contiguous half-open `[from, to)` bands
  starting at 0 with a mandatory open-ended top band. Rate math is HALF_UP at
  scale 2; amounts stay `numeric(12,2)`.
- **Threshold** — a plan's `min_sales_threshold` is **inclusive**: at exactly the
  threshold the booking still pays. Below it no payable row is written (a row that
  can never be settled is ledger noise) and the skip is logged.
- **Assignment** — only `TRAVEL_AGENT` accounts can hold a plan (corporate → 400).
  Re-assignment **supersedes**: the old row is deactivated, never updated or
  deleted, because it records which terms an earlier booking was confirmed under.
  The deactivation is `saveAndFlush`-ed before the insert — Hibernate otherwise
  issues the INSERT first and the partial unique index rejects it.
- **Guards** — plan `key` is immutable after creation (400); duplicate key → 409;
  tier gaps/overlaps/closed top/early open-ended band → 400; a plan that has
  payables or assignment history cannot be deleted (409, deactivate instead) and
  cannot have its method changed while in use (409); a plan that is *actively*
  assigned cannot be deactivated (409); assigning an inactive plan → 409; unknown
  account/plan → 404. Terms are audited field-by-field (`basis`, `method`,
  `rate_percent`, `fixed_amount`, `min_sales_threshold`, assignment changes).
- **RBAC** — plan CRUD, assignment, and quote are manager-and-up (SALES/OPS → 403).
  Quote preview (`POST /api/commission-plans/accounts/{id}/quote`) shows a manager
  what an account *would* be paid, including the `DEFAULT_FLAT_RATE` fallback
  (`planKey: "DEFAULT_FLAT_RATE"`) when nothing is assigned.
- **Security follow-up** — the Module 1 docs claimed payables/invoices were
  manager-gated "in the service", but only the controller had `isAuthenticated()`.
  `listForAccount` (payables and invoices), `get` (invoice) and `markPaid` now take
  the `UserPrincipal` and enforce manager-and-up themselves, so the invariant does
  not rest on an annotation.
- **Tests** — `CommissionCalculatorTest` (8) pins the arithmetic: exact tier
  boundaries (49999.99 → 5%, 50000 → 10%, 100000 → 15%), HALF_UP half-cent,
  NET/GROSS with a discount, FIXED, and the threshold edge. `CommissionPlanFlowIT`
  (14) drives the HTTP surface: tiered accrual on confirm (80000 → 10% = 8000 with
  `planId` on the payable), flat fallback, threshold suppression *and* the
  inclusive edge, GROSS ignoring a discount, FIXED paying 3000 on a 250000 booking,
  re-assignment keeping the old terms on the earlier payable, corporate rejection,
  five tier-shape rejections, unique/immutable/audited keys, in-use delete and
  deactivate conflicts, unused-plan delete, inactive-plan assignment, and RBAC over
  plans/assignments/quotes/payables/invoices for SALES and OPS.

Module 5 (reporting & analytics) is next.

## 11. Module 5 — shipped 2026-10-01

Three reports over the Phase 7 tables, in a new `SalesReportingService`
(`analytics/`) rather than folded into the 1000-line `AnalyticsService`: the
Phase 3 Module 2 reports are lead/trip/team shaped and share one filter object,
these three read `opportunities` and `account_commission_payables`, and each
carries its own visibility rule.

- **`GET /api/analytics/pipeline`** — pipeline health per stage: open count/value,
  weighted value, mean dwell time (`stage_moved_at`), stale-deal count, and
  won/lost with a win rate. `staleAfterDays` is a parameter (default 30, range
  1–365). The query is driven **from** `pipeline_stages` with a LEFT JOIN so an
  empty stage still returns a row — a health board that hides the stage nobody is
  quoting in is hiding the problem. Scope and window live in the `ON` clause, not
  `WHERE`, which would silently degrade it to an inner join for a scoped caller.
- **`GET /api/analytics/forecast`** — monthly buckets plus the two cuts a manager
  actually asks for, which Module 3's forecast lacked: by owner, and stage *mix*
  (share of expected value). Adds a `valueBasis` block so the payload documents
  its own expected/best/won formulas, half-open window and live weighting, and
  reports `openOutsideWindow` so a reader can see what was excluded and why: an
  OPEN deal whose expected date falls outside the window is not assigned to a
  month it is not expected in. `pipelineCoveragePct` is the sum of stage weights
  holding open value (capped at 100) — below 100% means part of the forecast sits
  in stages that cannot be won.
- **`GET /api/analytics/partner-commissions`** — commission per account and per
  plan from the payable ledger, split into accrued / paid / open liability /
  voided. **Never netted**: a VOID payable is reported in its own column, because
  VOID-not-delete exists precisely so a reversal stays visible. Payables with
  `plan_id IS NULL` group under the synthetic key `DEFAULT_FLAT_RATE`, so moving
  an account onto a plan cannot make its earlier commission disappear from a
  per-plan rollup. `effectiveRatePercent` is the rate of the most recent accrual
  and is labelled as such, not as the plan's current terms.

**Decisions worth recording**

- **No rollup tables.** V23 is indexes only, for the reason Module 3 gave: a report
  worth recomputing should be recomputed, and a stored total is a second source of
  truth that can only drift. A test asserts this directly — it re-weights a stage
  with a bare `UPDATE` and requires the next forecast response to change. If a
  rollup is ever introduced, that test fails.
- **Window semantics differ per report, deliberately.** Pipeline and forecast
  window on `expected_date` for OPEN deals and `closed_at` for terminal ones, so a
  deal that closed in the period counts even when its expected date was earlier.
  Partner commission windows on `payable_at`, because that is when the liability
  was created.
- **LOST is excluded from forecast** and counted only in the pipeline report: it is
  a terminal outcome, not pipeline, and counting it would inflate the forecast.
- **Visibility is per report, not global.** Pipeline and forecast follow
  `AnalyticsService`: SALES pinned to their own rows, OPS and above see all.
  Partner commission is MANAGER-and-up and is re-checked in the service, not only by
  the annotation — a report that aggregated Module 4's money would otherwise reopen
  the exact hole Module 4 closed, in aggregate form.
- **Empty is zero, not null.** Money and counts are `0.00`; percentages are `null`
  when the denominator is zero, because "0% of 0 deals" and "no deals in scope" are
  different facts.
- **Missing vs bad window.** `from`/`to` are declared `required = false` so an
  omitted parameter fails through the service's own validation with a clear message
  instead of surfacing as a framework 500.

**Tests** — `SalesReportingFlowIT` (8): weighted pipeline math (150000 @ 20% =
30000, 80000 @ 70% = 56000), staleness via a backdated `stage_moved_at` with
`staleAfterDays` proven to be a parameter, half-open forecast buckets with a deal
sitting exactly on the exclusive bound, stage-mix shares (63.6/36.4), **live
re-weighting moving the forecast**, cross-owner scoping for two SALES users across
both reports, window validation, partner commission paid/open/void separation
(40000 accrued = 10000 open + 10000 void + 20000 paid), the flat-fallback bucket
sitting alongside a real plan, SALES/OPS 403, and empty-report semantics. Full
suite green: 528 tests, 0 failures, 11 skipped.

Remaining Phase 7 work: the four hardening gates and `docs/PHASE_7_SIGNOFF.md`.