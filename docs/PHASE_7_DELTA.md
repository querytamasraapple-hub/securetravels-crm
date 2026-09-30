# SecureTravels CRM — Phase 7 Step 0 Delta (Sales CRM Depth)

> **Status: ratified 2026-10-01 (Step 0); Module 1 shipped 2026-10-01 (§7).**
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
| `V21__opportunities_forecast.sql` | Module 3 | `opportunities` (subject = lead/account, `stage_id`, expected value, expected date, ownership) + forecast materialisation/rollup columns |
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