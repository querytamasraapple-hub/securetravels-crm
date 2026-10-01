# Phase 7 Sign-off — Sales CRM Depth

**Status: accepted pending operator review**
Date: 2026-10-01
Scope: Modules 1–5, the four hardening gates, and a security review of what
Phase 7 added.

## What shipped

| # | Module | Commit | Content |
| --- | --- | --- | --- |
| 1 | Accounts | `c947f5f` | `accounts` + `account_invoices` (V19/V20 base), B2B/corporate/travel-agent types, billing identity, account 360 |
| 2 | Pipeline stages | `b229c70` | `pipeline_stages` seeded reference data, ordered weighted stages, manager-only administration |
| 3 | Opportunities and forecast | `5644b16` | `opportunities`, one per lead, validated stage moves, terminal WON/LOST, `GET /api/forecast` |
| 4 | Commission plans | `74daaa4` | `commission_plans`, `commission_tiers`, `account_commission_plans` (V22), NET/GROSS, PERCENT/FIXED/TIERED, pure `CommissionCalculator`, manager-only terms |
| 5 | Reporting and analytics | `74c8d52` | `GET /api/analytics/pipeline`, `/forecast`, `/partner-commissions` over live tables (V23 = indexes only) |

Full suite at sign-off: **534 tests, 0 failures, 11 skipped.** The skips are
Docker-dependent (RabbitMQ and Testcontainers) and are not a Phase 7
dependency; the suite runs against native PostgreSQL 16.

## The four gates

All four are implemented in `Phase7HardeningIT` and pass. They are restatements
of guarantees the modules claimed, proven through the HTTP API.

| Gate | Test | Result |
| --- | --- | --- |
| Commission correctness incl. tiered boundaries | `gate1_tieredBoundaryIsInclusiveAtTheLastDigit` | Pass. 49999.99 stays at 5%, exactly 50000 moves to 10% |
| Pipeline validation | `gate2_invalidTransitionsAreRefusedAndEntryConditionsAreAdvisory` | Pass, with a documented exception — see below |
| Commission visibility, 403 across owners | `gate3_commissionIsInvisibleEvenToTheRepWhoEarnedIt` | Pass. Forbidden for the earning rep and for a second rep; manager settles |
| Account invoicing, no retail regression | `gate4_retailBookingsInheritNoInvoiceOrCommission` | Pass. One invoice for the account booking, none for retail |

### Known limitation, not worked around

`pipeline_stages.entry_condition` is descriptive text, not an enforced rule. It
is stored, sanitised and returned; nothing evaluates it. Gate 2 asserts this
explicitly rather than asserting a rejection that the system does not perform.
Machine-evaluated entry conditions require a condition vocabulary to evaluate
against and are deferred to Phase 9.

A stage note that reads like a constraint while being ignored is worse than no
note, so this is recorded here rather than left for someone to discover in
production.

## Defects found during hardening and fixed

| Defect | Impact | Fix |
| --- | --- | --- |
| `pipelineReport` required the window it documented as optional | A SALES rep asking for their own pipeline health received 400 | `validateOptionalWindow`: both absent = whole pipeline; exactly one = 400 |
| `openOutsideWindow` counted all open deals when no window was supplied | Reported deals as "excluded" when nothing was excluded | Query skipped when there is no window; 0 by definition |
| `partner-commission totals.accounts` counted rows, not accounts | An account reassigned mid-history reported 2 accounts | Distinct `account_id` |
| `ValueBasis.won` description missing a closing bracket | Cosmetic, in a client-visible formula string | Corrected |

All three reporting defects carry regression tests in `SalesReportingFlowIT`.

## Security review

Reviewed what Phase 7 added. Findings:

1. **Commission data is manager-and-up.** Payables list, invoice list, plan CRUD,
   assignment, quote, settlement and the `partner-commissions` aggregate are
   MANAGER/ADMIN/CEO, enforced by `@PreAuthorize` *and* re-checked in the service
   layer. Gate 3 verifies the earning sales rep is refused on every one of these.
2. **SALES self-scoping holds on the new reports.** Pipeline and forecast pin the
   owner to `caller.id()` regardless of any `ownerId` parameter, so passing
   another consultant's ID cannot widen scope. Verified for two distinct SALES
   users.
3. **Financial records are append-only in effect.** Cancellation VOIDs the
   invoice and the payable; it does not delete either. This preserves the audit
   trail a settlement dispute needs.
4. **Terms are pinned at accrual.** Each payable records the plan that produced
   it, so reassigning an account cannot retroactively change what a partner was
   owed, and unassignment cannot erase it.
5. **The flat fallback remains visible.** Payables with `plan_id IS NULL` group
   under `DEFAULT_FLAT_RATE` in every report, so assigning a plan never makes
   earlier commission disappear from a per-plan rollup.
6. **No PII beyond what the phase required.** Accounts carry billing identity
   (GSTIN) because invoicing requires it; commission reports expose amounts and
   plan keys, not traveller or customer PII.
7. **Reports are live queries, no rollup tables.** This removes an entire class
   of staleness and cache-poisoning risk. Re-weighting a stage moves the forecast
   immediately, which `SalesReportingFlowIT` enforces with a bare `UPDATE`.
8. **Free-text fields are sanitised.** `entry_condition` and other note fields
   pass through `XssSanitizer` on write.

### Not addressed here

- `pipeline_stages.entry_condition` is unenforced (documented above).
- GitHub reports 32 Dependabot findings on the default branch (3 critical,
  7 high, 20 moderate, 2 low). These are pre-existing and outside Phase 7's
  diff, but they are not triaged and should not be mistaken for clean.
- Docker-dependent tests remain skipped in this environment.

## Deliberate non-goals

Phase 7 built the data model and rules. It did **not** build:

- Commission **payout execution** — settlement records that money moved; it does
  not call a bank or accounting system.
- Automated **pipeline entry/exit rules** (see entry conditions above).
- **Currency conversion** — all amounts are single-currency as stored.
- **Historical plan versioning** — a plan's terms are immutable once created, but
  changing a rate means a new plan, not an edit in place.
- Reporting **caching or pagination** — all three reports aggregate in one query
  per response, sized for a single-tenant CRM's current data volume.

## Recommendation

Ship. The four gates pass, the full suite is green, and the security boundaries
hold under test from every angle a caller could attempt. The one incomplete
guarantee (entry conditions) is documented in the test that would otherwise
assert it, rather than silently claimed. Phase 8 should not start until an
operator accepts this sign-off.