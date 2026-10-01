# SecureTravels CRM — Database Design

> **Single source of truth: `backend/src/main/resources/db/migration/`** —
> Flyway applies schema, `ddl-auto: validate` prevents drift. This document
> is a living map and must be updated in the same change as any new
> migration. Migrations shipped: **V1 → V22** (V1–V12 Phase 1–2; V12 = Module 4
> WhatsApp + timeline; **V13 = Phase 3 Module 2** reporting ledger + search;
> V14–V18 = Phase 6 automation/observability; **V19 = Phase 7 Module 1** accounts;
> **V20 = Phase 7 Module 2** pipeline stages; **V21 = Phase 7 Module 3**
> opportunities + revenue forecast; **V22 = Phase 7 Module 4** commission plans
> + tiers + account assignment).

Conventions used throughout:

- UUID PKs (`gen_random_uuid()`), `timestamptz` audit columns, `numeric(12,2)`
  money, `version bigint` optimistic locking on mutable rows.
- **Enums are varchar + named CHECK constraints** (not native PG enum types)
  so Hibernate `@Enumerated(STRING)` binds without JDBC casts while keeping
  referential guarantees.
- Every mutable table carries `created_at`, `updated_at`; append-only tables
  carry `created_at` only.
- Soft-delete is avoided; real deletions are rare and deliberate (a lead that
  turns out to be spam is handled via workflow, not row deletion).

---

## 1. Identity & auth

### `users`
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| email | varchar(255) UNIQUE | |
| password_hash | varchar(100) | BCrypt strength 12 |
| full_name | varchar(120) | |
| role | varchar(30) | `SALES, OPS, MANAGER, ADMIN, CEO` |
| phone | varchar(20) | |
| is_active | boolean | inactive users excluded from round-robin assignment |
| created_at / updated_at / version | | |

### `refresh_tokens`
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid FK → users | ON DELETE CASCADE |
| token_hash | varchar(64) UNIQUE | **SHA-256 of raw token — plaintext never stored** |
| expires_at | timestamptz | 7 days |
| revoked | boolean | rotated on every use |
| replaced_by | varchar(64) | hash of the rotated-out token (reuse detection) |

---

## 2. Trip catalogue (trips / batches / guides / seat_holds)

### `trips`
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| name | varchar(255) | |
| slug | varchar(100) UNIQUE | |
| category | varchar(30) | `TREK, PILGRIMAGE, LEISURE, CUSTOM` |
| **booking_type** | varchar(30) | `FIXED_BATCH \| CUSTOM_FIT` — **see ADR 0001** |
| base_cost | numeric(12,2) | |
| duration_days | int | `> 0` |
| itinerary / inclusions / exclusions | text | OWASP-sanitized HTML on write |
| is_active | boolean | |

### `guides`
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| full_name | varchar(120) | |
| phone | varchar(20) | |
| daily_rate | numeric(10,2) | |
| is_active | boolean | |

### `batches` (FIXED_BATCH departures only)
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| trip_id | uuid FK → trips | |
| departure_date | date | UNIQUE per trip |
| max_capacity | int | `> 0` |
| seats_booked | int | **derived + stored**: CONFIRMED bookings + active HELD holds (invariant I3) |
| guide_id | uuid FK → guides | |
| transport_plan | text | |
| status | varchar(20) | `OPEN, CLOSED, CANCELLED` |

### `seat_holds` (2-hour provisional hold)
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| batch_id | uuid FK → batches | |
| booking_id | uuid FK → bookings | backfilled on confirm |
| num_seats | int | `> 0` |
| held_until | timestamptz | auto-released when expired |
| status | varchar(20) | `HELD, CONFIRMED, RELEASED, EXPIRED` |

---

## 3. Leads (pipeline front)

### `leads`
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| customer_name | varchar(200) | |
| mobile_number | varchar(30) | |
| mobile_digits | varchar(20) | normalized for dedup |
| whatsapp_number / email | varchar | optional |
| source | varchar(30) | `GOOGLE_ADS, FACEBOOK_ADS, INSTAGRAM, WEBSITE, WHATSAPP, REFERRAL, JUSTDIAL, WALK_IN, B2B, EXISTING_CUSTOMER, OTHER` |
| destination | varchar(120) | |
| trip_id | uuid FK → trips | |
| travel_date | date | |
| num_persons | int | 1..50 at API boundary |
| account_id | uuid FK → accounts | **V19** — B2B/travel-agent account the lead belongs to |
| budget | numeric(12,2) | `>= 0`, ≤12 int digits, ≤2 fraction digits |
| owner_id | uuid FK → users | |
| status | varchar(20) | `NEW → INTERESTED → QUOTATION_SENT → BOOKING_CONFIRMED` / `LOST` |
| heat | varchar(10) | `HOT, WARM, COLD` — rule-based (ADR 0004) |
| follow_up_date | date | |
| remarks | text | XSS-sanitized |
| consent_given | boolean | **DPDPA — mandatory true to create** |
| consent_captured_at / consent_scope | | |
| lost_reason | varchar(30) | `PRICE_TOO_HIGH, DATES_UNAVAILABLE, CHOSE_COMPETITOR, WENT_SILENT, NOT_GENUINE, POSTPONED` |
| duplicate_of_lead_id | uuid FK → leads | duplicate soft-linking |
| last_contacted_at | timestamptz | |
| customer360_id | uuid FK → customer360 | written when phone matches a canonical customer |
| created_by | uuid FK → users | NULL for webhook/website leads (=system) |
| created_at / updated_at / version | | |

---

## 4. Customers & bookings

### `customer360` (canonical person record; DPDPA source of truth)
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| full_name | varchar(200) | |
| mobile_number / mobile_digits | | UNIQUE on mobile_digits and email |
| whatsapp_number / email | | |
| consent_given / consent_captured_at / consent_scope | | canonical consent |
| marketing_opt_in | boolean | explicit, separate flag |
| total_trips | int | derived counters |
| last_trip_date | date | |
| total_spent | numeric(12,2) | |
| suggest_offer | varchar(200) | |
| offer_tags | text[] | remarketing tags (Kashmir, Char Dham, …) |

### `bookings`
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| booking_ref | varchar(20) UNIQUE | |
| trip_id | uuid FK → trips | |
| batch_id | uuid FK → batches | FIXED_BATCH only (I2) |
| customer_id | uuid FK → customer360 | |
| lead_id | uuid FK → leads | **originating lead** (added V4) |
| booking_type | varchar(30) | copy of trip.booking_type (I1) |
| num_travellers | int | `> 0` |
| total_amount / discount_amount / tax_amount | numeric(12,2) | discounts require `discount_approved_by` at UI level |
| status | varchar(20) | `QUOTATION, CONFIRMED, COMPLETED, CANCELLED` |
| travel_date | date | |
| account_id | uuid FK → accounts | **V19** — inherits `leads.account_id` at create; drives account invoicing/commissions |
| discount_approved_by | uuid FK → users | |
| notes / created_by | | |

**Invariants enforced by trigger `trg_booking_consistency`:**
- **I1** `bookings.booking_type` must equal `trips.booking_type`.
- **I2** FIXED_BATCH requires a `batch_id` of the same trip; CUSTOM_FIT must have `batch_id IS NULL`.
- **I3** `batches.seats_booked` = CONFIRMED bookings + active HELD holds (service-enforced under `PESSIMISTIC_WRITE`).

### `travellers`
| Field | Type | Notes |
|---|---|---|
| id / booking_id (FK, CASCADE) / customer360_id (FK) | | PII links upward when identifiable |
| full_name | varchar(200) | |
| age / gender / phone | | |
| medical_cert_required | boolean | |

---

## 5. Payments

### `payments`
| Field | Type | Notes |
|---|---|---|
| id / booking_id (FK) | | |
| amount | numeric(12,2) | `> 0` |
| amount_type | varchar(20) | `ADVANCE, BALANCE, FULL` |
| status | varchar(20) | `PENDING, PARTIAL, COMPLETED, OVERDUE, CANCELLED, REFUNDED` |
| due_date / paid_at | | CHECK: `STATUS=COMPLETED ⇒ paid_at NOT NULL` |
| gateway_ref | varchar(120) | **never raw card data** (gateway lands Phase 2) |
| recorded_by / notes | | |

---

## 6. Operations

### `operations_handoffs` (auto-created once on booking CONFIRMED — invariant I6)
| Field | Type | Notes |
|---|---|---|
| id / booking_id (FK, **UNIQUE**) / batch_id (FK) | | one ops record per booking |
| ops_ref | varchar(20) UNIQUE | e.g. `TOH-2026-0002` |
| travel_date / pax | | `pax > 0` |
| hotel_status | | `NOT_ARRANGED, PENDING, CONFIRMED` |
| transport_status | | same triad |
| guide_id | uuid FK → guides | |
| driver_id | uuid | Phase-2 vendor record |
| payment_status | | mirrors payments tri-state |
| trip_sheet_generated_at | timestamptz | |

---

## 7. Tasks & notifications

### `tasks` (automation engine, Phase-1 form)
| Field | Type | Notes |
|---|---|---|
| id / lead_id (FK) / booking_id (FK) / assignee_id (FK users) | | assignee required |
| type | varchar(30) | `INITIAL_CALL, FOLLOW_UP_1D, FOLLOW_UP_3D, FOLLOW_UP_8D, FOLLOW_UP_15D, QUOTATION, PAYMENT_REMINDER, OPS, REVIEW, CUSTOM` (cadence per spec §19.2: cumulative +1/+3/+8/+15; codified by migration V7) |
| status | | `PENDING, COMPLETED, OVERDUE, CANCELLED` |
| due_at | timestamptz | |
| sla_deadline / completed_at / escalated_at / notes | | |

### `notifications`
| Field | Type | Notes |
|---|---|---|
| id / user_id (FK) | | |
| channel | | `IN_APP, EMAIL` |
| title / body / link | | |
| is_read / read_at | | |

---

## 8. Documents (Phase 2 workflows; entity since Phase 1)

### `documents`
| Field | Type | Notes |
|---|---|---|
| id / related_type | | `TRAVELLER, BOOKING, LEAD` |
| traveller_id / booking_id (FK) | | |
| doc_type | | `ID_PROOF, MEDICAL_CERT, TRIP_PHOTO` |
| storage_key | varchar(300) | **S3 object key — file bytes never in Postgres** |
| mime_type / size_bytes / uploaded_by | | |

---

## 9. Dashboard & targets

### `sales_targets`
| Field | Type | Notes |
|---|---|---|
| id / user_id (FK) | NULL = company-wide row for the month |
| month | date | first day of target month |
| target_bookings / target_revenue | numeric(12,2) | at least one > 0 |
| created_by | | |
| — | | UNIQUE (user, month) where user not null; UNIQUE (month) where user null |

---

## 10. Audit

### `audit_log` (append-only)
| Field | Type | Notes |
|---|---|---|
| id / entity / entity_id | | |
| action | | `CREATE, UPDATE, STATUS_CHANGE, DELETE, LOGIN, LOGOUT` |
| field / old_value / new_value | | |
| actor_id (FK users) | | NULL = system/webhook |
| created_at / seq (bigserial) | | monotonic key for activity timelines |

---

## 11. Communications — Module 4 (`V12__whatsapp_communication_timeline.sql`)

### `whatsapp_templates` (reference data, seeded by V12)
The nine approved templates. Reference data, not configuration churn: Interakt
has no template-management API, so the Interakt side is created in the dashboard
and this table mirrors it.

| Field | Type | Notes |
|---|---|---|
| id | bigserial | |
| code | varchar(40) unique | internal key, e.g. `BOOKING_CONFIRMED` |
| interakt_name | varchar(80) unique | the **dashboard** name, e.g. `securetravels_booking_confirmed` |
| label / language_code | varchar | `languageCode` is always `en` today |
| expected_params | int | positional `bodyValues` count; enforced in `enqueue` so a caller bug is a 400, not a wasted provider quota |
| enabled | boolean | operational toggle; a disabled template is a permanent failure, never a retry |

`TRIP_LOGISTICS` deliberately merges hotel, driver, and pickup, which is what
brings the catalogue to nine distinct templates.

### `whatsapp_messages` (one row per outbound message)
| Field | Type | Notes |
|---|---|---|
| id | uuid | also the queue payload in `BROKER` mode |
| subject_type / subject_id | varchar(20) / uuid | polymorphic `LEAD, CUSTOMER, BOOKING`; **no FK** — the owning module owns the row |
| template_code | varchar(40) | logical reference to `whatsapp_templates` |
| recipient_mobile | varchar(20) | normalised digits; correlated to inbound replies by this |
| country_code | varchar(6) | quoted (`"+91"`); the documented Interakt trap |
| body_values | text[] | positional `{{1}}..{{n}}` values |
| status | varchar(16) | `QUEUED, SENDING, SENT, DELIVERED, READ, FAILED, DEAD_LETTERED` |
| attempts | int | advanced by the same `UPDATE` that claims the row, so the two cannot drift |
| provider / provider_message_id | varchar | |
| callback_data | varchar(64) unique | `st-<uuid>`; **unique** makes a replayed send a constraint violation rather than a second message |
| queued_at / sent_at / delivered_at / read_at | timestamptz | |
| last_error / channel_error_code / channel_failure_reason | varchar | Interakt's error code is a *string* (e.g. `1013`) |
| created_at / updated_at | timestamptz | |

Indexes: partial on `(status, queued_at)` for the `SENDING` recovery sweep, and
a unique `callback_data` for idempotency.

### `timeline_events` (unified Lead/Customer/Booking timeline)
| Field | Type | Notes |
|---|---|---|
| id / seq | uuid / bigserial | `seq` breaks ties within one timestamp |
| subject_type / subject_id | varchar(20) / uuid | polymorphic, no FK |
| direction | varchar(10) | `INBOUND, OUTBOUND` |
| channel | varchar(20) | `WHATSAPP` (future: `EMAIL, SYSTEM`) |
| kind | varchar(30) | `TEMPLATE_QUEUED, TEMPLATE_SENT, TEMPLATE_DELIVERED, TEMPLATE_READ, TEMPLATE_FAILED, REPLY_RECEIVED, MEDIA_RECEIVED, BUTTON_CLICKED, SYSTEM_NOTE` |
| template_code / summary / body | | `body` is inbound customer text; never store it in a log line |
| provider / provider_message_id / actor_mobile | | correlation |
| body_values | jsonb | what was actually sent, for audit |
| created_at | timestamptz | |

**Unique `(provider, provider_message_id, kind)`** is the backstop that makes
re-delivered webhooks harmless. The service also checks for existence first, so
a duplicate becomes a no-op rather than a 500 that provokes another retry.

Deliberately **separate from `audit_log`**: audit answers "who changed what",
the timeline answers "what did this customer receive and say". Conflating them
produces a log nobody can read.

---

## 12. Reporting — Phase 3 Module 2 (`V13__reporting_commission_ledger_and_fts.sql`)

### `sales_commission_ledger` (credited revenue snapshot)
| Field | Type | Notes |
|---|---|---|
| id | uuid | |
| booking_id | uuid **unique** | one credit per booking — `ON CONFLICT DO NOTHING` makes re-credit harmless |
| lead_id / customer_id (FK) | | customer from `bookings.customer_id`; lead via `bookings.lead_id` |
| consultant_id (FK users) | | **owner at confirmation**, snapshotted — `leads.owner_id` is mutable |
| trip_id / batch_id (FK) | | for trip-level attribution |
| source | varchar(30) | `Web, Referral, Walk-in, ...` (string, not a lookup) |
| gross_amount / discount_amount / net_amount | numeric(12,2) | `net_amount` ties to `bookings` (IT-verified) |
| base_cost_assumed | numeric(12,2) | derived; see Phase 9 for true P&L |
| effective_net_amount | numeric(12,2) | source of the report's "net booked value" |
| credited_at | timestamptz | confirmed-at time |
| revoked_at / revoked_by (FK users) | timestamptz / uuid **null** | **revoke never deletes**; cancelled booking keeps its row |
| version | bigint | |

One row per confirmed booking. `revoked_at` set on cancellation. Direct DML
never changes it — only `CommissionLedgerService` writes.

### FTS search vectors
Generated `tsvector` columns over **raw + `regexp_replace`-stripped** text
(single vectors lose partial-email matches, e.g. `Ramesh.New@x.com` becomes one
lexeme):

| Table | Column | Contents |
|---|---|---|
| `customer360` | `full_name_search` / `email_search` | name; email |
| `customer360` | trigram on `mobile_number` | typo-tolerant number search |
| `leads` | `contact_search` | name + email + mobile |
| `bookings` | `reference_search` | ref + customer name + trip name |

GIN indexes on each vector; `pg_trgm` GIN on mobile. Audit search ranks by
`ts_rank` + `similarity()` and reports `matchedBy`.

### Indexes added
`idx_bookings_travel_date`, `idx_leads_travel_date`, `idx_leads_owner_created`,
`idx_batches_departure_date`, `idx_customer360_created_at`,
`idx_operations_handoffs_departure`. (`idx_bookings_travel_date` etc. were
verified against the live schema as genuinely missing before being created —
duplicate/prefix-covered indexes were **not** re-added.)

### Backfill (ratified)
Two eligible bookings were credited to their *current* `leads.owner_id` with
`credited_at = bookings.updated_at`. This is documented as **lossy**: the owner
at confirmation was never recorded before Module 2, so historical attribution is
"as of today" by stated policy. `ON CONFLICT (booking_id) DO NOTHING` guards the
rerun.

---

## 13. Module 9 — webhook automation

### `assignment_state` (round-robin sales-assignment cursor)
| Field | Type | Notes |
|---|---|---|
| id / user_id (FK) | | one row per (user, month) |
| month | date | resets alignment per month |
| last_assigned_at | timestamptz | least-recently-assigned first |
| leads_assigned_this_month | int | |

### `webhook_logs` (every inbound call, audit)
| Field | Type | Notes |
|---|---|---|
| id / source | | e.g. `WEBSITE` |
| payload | text | bounded to 10,000 chars at write |
| lead_id (FK) | | set on success |
| status | | `success, duplicate, failed` |
| error_message | text | |
| created_at | timestamptz | indexed |

---

## 14. Relationship map

```
users 1─* refresh_tokens
users 1─* leads.owner_id        users 1─* tasks.assignee_id
users 1─* audit_log.actor_id    users 1─* sales_targets.user_id
users 1─* payments.recorded_by  users 1─* assignment_state.user_id

trips 1─* batches              trips 1─* leads.trip_id
batches 1─* seat_holds         trips 1─* bookings
batches 1─* operations_handoffs
guides 1─* batches.guide_id    guides 1─* operations_handoffs.guide_id

leads 1─0..1 customer360        leads 1─0..1 duplicate leads
leads 1─0..* bookings.lead_id   leads 1─* tasks.lead_id
leads 1─* audit_log.lead        leads 1─* webhook_logs.lead_id
accounts 1─* leads.account_id   accounts 1─* bookings.account_id
accounts 1─0..* opportunities.account_id
accounts 1─* account_commission_payables   bookings 1─0..1 account_commission_payables.booking_id
leads 1─0..1 opportunities.lead_id   users 1─* opportunities.owner_id
pipeline_stages 1─* opportunities.stage_id
bookings 1─0..1 invoices.booking_id        accounts 1─* invoices.account_id

customer360 1─* bookings        customer360 1─* travellers
bookings 1─* travellers         bookings 1─* payments
bookings 1─1 operations_handoffs
bookings 1─* tasks.booking_id   bookings 1─* documents.booking_id
bookings 1─0..1 sales_commission_ledger.booking_id   (revocable credit)

sales_commission_ledger *─1 users.consultant_id   sales_commission_ledger *─1 leads.lead_id

whatsapp_templates 1─0..1 whatsapp_messages  (logical ref on template_code, no FK)
whatsapp_messages  1─0..* timeline_events     (same subject_type/subject_id, no FK)
```

## 15. Accounts & billing — Phase 7 Module 1 (`V19__accounts_and_links.sql`)

Phase 7 nuance (tracked in `PHASE_7_DELTA.md`): the "invoicing"/"payables"
this module ships are **account shapes only**. A minimal account-first invoice
and the travel-agent commission payable exist so account-ledger behaviour can be
verified end-to-end and regression-safe; full invoicing/payouts/P&L stay
**Phase 9** per `ROADMAP.md`.

### `accounts` (corporate / travel agent)
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| account_type | varchar(20) | `CORPORATE, TRAVEL_AGENT` (CHECK) |
| name | varchar(200) | |
| gstin | varchar(15) | full-format Indian GSTIN, **mod-36 checksum** (see SECURITY.md); null allowed |
| billing_name / billing_address / city | varchar / text / varchar | invoice-to details |
| primary_contact_name / email / phone | varchar | **PII — sanitized on write like customer data** |
| bank_account_ref | varchar(40) | opaque reference, never full bank details |
| credit_terms_days | int | `>= 0`, nullable |
| notes | text | OWASP-sanitized |
| is_active | boolean | inactive accounts cannot be linked to new leads/bookings |
| created_at / updated_at / version | | |

`leads.account_id` and `bookings.account_id` are nullable FKs set on create
(inherited lead→booking); retail stays untouched.

### `account_commission_payables` (what SecureTravels owes a travel agent)
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| booking_id | uuid **UNIQUE** FK | one payable per booking — the idempotency guard, mirroring `sales_commission_ledger` |
| account_id | uuid FK → accounts | |
| basis | varchar(10) | `NET, GROSS` (default NET) |
| rate_percent | numeric(6,2) | |
| gross / discount / tax / net / commission_amount | numeric(12,2) | commission = net × rate when basis NET |
| status | varchar(20) | `OPEN, PAID, VOID` |
| paid_at / paid_ref / settled_by (FK users) / notes | | settlement trail |
| created_at / updated_at / version | | |

Credited on booking **CONFIRMED** only when
`app.feature-flags.partner-commissions=true` + account type `TRAVEL_AGENT` +
`net > 0`; **VOID** (never delete) on cancel; **PAID** on settle (manager+).
Default rate: `app.commission.default-travel-agent-percent` (10.00).

### `invoices` (account-first, minimal)
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| invoice_ref | varchar(20) UNIQUE | `INV-YYYY-####` |
| booking_id | uuid **UNIQUE** FK | one invoice per booking |
| account_id | uuid FK → accounts | NULL while retail; billing entity below is authoritative |
| billing_entity | varchar(20) | `ACCOUNT, RETAIL` (CHECK) |
| billing_name / address / gstin | | snapshot of the account's billing details at issue |
| gross / discount / tax / net_amount | numeric(12,2) | net = gross − discount + tax (matches ledger definition) |
| status | varchar(20) | `ISSUED, VOID` |
| issued_at / issued_by (FK users) / notes | | |
| created_at / updated_at / version | | |

Issued on CONFIRMED **only when the booking carries an account** (retail
bookings produce no invoice — Phase 1 behaviour unchanged); VOID on cancel.

## 16. Pipeline stages — Phase 7 Module 2 (`V20__pipeline_stages.sql`)

### `pipeline_stages` (configurable opportunity stages)
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| stage_key | varchar(40) UNIQUE | immutable identity, uppercase snake_case |
| label | varchar(80) | sanitized on write |
| sort_order | int | `>= 0`; **unique among active stages** (partial unique index) for deterministic list order |
| probability_weight | numeric(5,2) | `0–100`, the Module 3 forecast expected-value basis |
| entry_condition | text | note, sanitized on write |
| is_active | boolean | default true; at least one active must always remain (service guard) |
| created_at / updated_at / version | | |

Seeded defaults (kickoff weights): `QUALIFIED` (20%), `QUOTATION_SENT` (40%),
`NEGOTIATION` (70%) — reference data an admin may edit. Referenced by
`opportunities.stage_id` (Module 3, V21), which is why stage deletion is a
deactivation rather than a `DELETE`.

## 17. Opportunities & forecast — Phase 7 Module 3 (`V21__opportunities_forecast.sql`)

### `opportunities`
| Field | Type | Notes |
|---|---|---|
| id | uuid PK | |
| lead_id | uuid UNIQUE FK → `leads` | **one opportunity per lead** (service returns 409 on a second) |
| account_id | uuid NULL FK → `accounts` | snapshotted from the lead for account-level rollups |
| stage_id | uuid FK → `pipeline_stages` | the current stage; drives the forecast weight |
| owner_id | uuid FK → `users` | snapshotted from `leads.owner_id` at create; ownership scoping basis |
| expected_value | numeric(12,2) | `>= 0` (CHECK), the pipeline amount |
| expected_date | date | target close date; the forecast window filters on this |
| status | varchar | `OPEN` / `WON` / `LOST` (named CHECK) |
| stage_moved_at | timestamptz | last stage change, for velocity reporting |
| closed_at / closed_by / closing_note | | set together on close (`chk_opportunity_close_consistency`) |
| created_at / updated_at / version | | |

Indexes: `(owner_id, status)`, `stage_id`, `expected_date`, `lead_id`.

**Close consistency** — `chk_opportunity_close_consistency` enforces
`OPEN ⇒ closed_at/closed_by IS NULL` and `WON|LOST ⇒ both set`, so the terminal
states cannot be recorded half-written at the database level (the service
guards first, 409 on a second close or a post-close stage move).

**Forecast is computed live**, not materialised: `GET /api/forecast?from&to`
reads the scoped rows for the half-open `[from, to)` window on `expected_date`
and aggregates `expected = Σ(value × stage_weight / 100)` (HALF_UP, scale 2),
`best = Σ open value`, `won = Σ WON value`, grouped by stage and by month.
Deliberate: re-weighting a stage changes every forecast immediately, so there
is no rollup table to rebuild or drift from source. Rollup columns/tables are
deferred — Module 5 will only add them if volume or latency requires it.

## 18. Commission plans - Phase 7 Module 4 (`V22__commission_plans.sql`)

### `commission_plans` (partner payout terms)

| column | notes |
| --- | --- |
| `plan_key` | UNIQUE, uppercase snake_case; **immutable after creation** |
| `label` | human name shown in UI/exports |
| `basis` | `NET` = gross − discount + tax, `GROSS` = undiscounted gross |
| `method` | `PERCENT`, `FIXED`, `TIERED` |
| `rate_percent` | `numeric(5,2)`, required for `PERCENT` |
| `fixed_amount` | `numeric(12,2)`, required for `FIXED` |
| `min_sales_threshold` | inclusive floor; below it no payable is written |
| `active` | soft delete — a plan with history stays readable |
| `notes`, `created_at`, `updated_at`, `version` | audit surface |

`chk_commission_plan_method_inputs` enforces the input shape at the database
level, not only in the service: `PERCENT` ⇒ `rate_percent` NOT NULL,
`FIXED` ⇒ `fixed_amount` NOT NULL, `TIERED` ⇒ both NULL.

### `commission_tiers` (banded rates for `TIERED`)

`plan_id` FK **ON DELETE CASCADE**, `from_amount`, `to_amount`
(NULL = open-ended), `rate_percent`; unique on `(plan_id, from_amount)`.
Bands are contiguous half-open `[from, to)` intervals that must start at 0 and
end open-ended — enforced in `CommissionPlanService` (a DB-level check cannot
see rows from other bands), which rejects gaps, overlaps, a closed top band, and
an open-ended band that would shadow later bands.

### `account_commission_plans` (which account runs on which plan)

`account_id` FK, `plan_id` FK, `assigned_by`, `assigned_at`, `active`.
Partial unique index `idx_account_commission_plans_active` enforces **one active
plan per account**. Re-assignment deactivates the previous row instead of updating
it: the history records which terms an earlier booking was confirmed under, which
is also why a plan with any assignment history cannot be deleted (409 → deactivate).

### `account_commission_payables.plan_id`

Added in V22 so every payable records the plan that produced it. NULL means the
flat `app.commission.default-travel-agent-percent` fallback applied. Historical
payables keep the terms they were accrued under even after a re-assignment.

## 19. Booking-type decision

FIXED_BATCH vs CUSTOM_FIT is the **foundational schema decision** of the
Phase-1 build (it shapes `trips`, `batches`, `seat_holds`, `bookings`, and
the I1/I2/I3 invariants). Full rationale: **ADR `0001-booking-type-model`**.

## 20. Change discipline

- Every new table/column ships as a new `V{n}__*.sql` in order — **never
  auto-DDL** (`ddl-auto: validate` enforces this).
- Update this document in the same commit, and update `WebhookAutomationIT`
  / `BaseIT` truncation lists and `DATABASE.md` together.
- Money columns stay `numeric(12,2)`; never float.
