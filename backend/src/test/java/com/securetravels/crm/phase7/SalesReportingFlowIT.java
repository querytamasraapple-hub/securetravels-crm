package com.securetravels.crm.phase7;

import com.fasterxml.jackson.databind.JsonNode;
import com.securetravels.crm.BaseIT;
import com.securetravels.crm.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 Module 5 - pipeline, forecast and partner-commission reports.
 *
 * <p>Three things are being pinned here, and they are different from what the
 * report's own arithmetic tests would prove:
 *
 * <ol>
 *   <li><strong>Live computation.</strong> Re-weighting a stage must move the
 *       forecast immediately. If a rollup table ever creeps in, this fails.</li>
 *   <li><strong>Scoping.</strong> A SALES caller cannot widen their own pipeline
 *       by passing another consultant's id, and partner commission is
 *       manager-and-up.</li>
 *   <li><strong>Status separation.</strong> VOID payables are not silently netted
 *       out of partner commission, and OPEN deals are not counted as won.</li>
 * </ol>
 */
@TestPropertySource(properties = "app.feature-flags.partner-commissions=true")
class SalesReportingFlowIT extends BaseIT {

    @Test
    void pipelineReportCountsValueDwellAndStalenessPerStage() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        // Two open deals in QUALIFIED (weight 20), one in NEGOTIATION (weight 70).
        createOpportunity(sales, "Pipeline A", "+919700000031", "QUALIFIED", 100000, "2026-11-05");
        createOpportunity(sales, "Pipeline B", "+919700000032", "QUALIFIED", 50000, "2026-11-06");
        createOpportunity(sales, "Pipeline C", "+919700000033", "NEGOTIATION", 80000, "2026-11-07");

        String resp = pipeline(manager, "2026-11-01", "2026-12-01");

        JsonNode qualified = findStage(parse(resp).get("stages"), "QUALIFIED");
        assertThat(qualified.get("openCount").asLong()).isEqualTo(2);
        assertThat(qualified.get("openValue").asDouble()).isEqualTo(150000.0);
        // 100000*20% + 50000*20% = 30000
        assertThat(qualified.get("weightedValue").asDouble()).isEqualTo(30000.0);
        assertThat(qualified.get("staleOpenDeals").asLong()).isZero();
        assertThat(qualified.get("avgDaysInStage").asDouble()).isLessThanOrEqualTo(1.0);

        JsonNode negotiation = findStage(parse(resp).get("stages"), "NEGOTIATION");
        assertThat(negotiation.get("openCount").asLong()).isEqualTo(1);
        // 80000*70%
        assertThat(negotiation.get("weightedValue").asDouble()).isEqualTo(56000.0);

JsonNode totals = parse(resp).get("totals");
assertThat(totals.get("openCount").asLong()).isEqualTo(3);
assertThat(totals.get("openValue").asDouble()).isEqualTo(230000.0);
assertThat(totals.get("weightedValue").asDouble()).isEqualTo(86000.0);
    }

    /**
     * The window is optional on this report, and a half-supplied one is rejected
     * rather than quietly widened to everything.
     *
     * <p>Found by the Phase 7 hardening gate: a SALES caller asking for their own
     * pipeline health got a 400 telling them to pick a window, while the endpoint
     * contract said the window was optional. {@code openOutsideWindow} is what
     * keeps an optional window honest - a windowed report says how much open
     * pipeline it left out.
     */
    @Test
    void pipelineWindowIsOptionalButMustBeSuppliedAsAPair() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        createOpportunity(sales, "In Window", "+919700000041", "QUALIFIED", 100000, "2026-11-05");
        createOpportunity(sales, "Out Of Window", "+919700000042", "QUALIFIED", 70000, "2027-03-05");

        // No window at all: everything is in scope, nothing is excluded.
        String all = mockMvc.perform(get("/api/analytics/pipeline")
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(parse(all).get("from").isNull()).isTrue();
        assertThat(parse(all).get("totals").get("openCount").asLong()).isEqualTo(2);
        assertThat(parse(all).get("totals").get("openOutsideWindow").asLong()).isZero();

        // A window excludes the March deal, and says so instead of hiding it.
        JsonNode windowed = parse(pipeline(manager, "2026-11-01", "2026-12-01"));
        assertThat(windowed.get("totals").get("openCount").asLong()).isEqualTo(1);
        assertThat(windowed.get("totals").get("openOutsideWindow").asLong()).isEqualTo(1);

        // Half a window is a client bug, not a request for everything.
        for (String[] half : new String[][]{{"from", "2026-11-01"}, {"to", "2026-12-01"}}) {
            mockMvc.perform(get("/api/analytics/pipeline")
                            .header("Authorization", authHeader(manager))
                            .param(half[0], half[1]))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void pipelineReportDetectsStaleDealsAndIgnoresTheStageWeightForThem() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID stale = createOpportunity(sales, "Stale Deal", "+919700000034",
                "QUALIFIED", 40000, "2026-11-08");

        // Backdate the last stage move rather than waiting a month in a test.
        jdbcTemplate.update(
                "update opportunities set stage_moved_at = now() - interval '90 days' where id = ?", stale);

        JsonNode stage = findStage(parse(pipeline(manager, "2026-11-01", "2026-12-01")).get("stages"), "QUALIFIED");
        assertThat(stage.get("staleOpenDeals").asLong()).isEqualTo(1);
        assertThat(stage.get("avgDaysInStage").asDouble()).isGreaterThan(80.0);
        assertThat(parse(pipeline(manager, "2026-11-01", "2026-12-01")).get("totals").get("staleOpenDeals").asLong())
                .isEqualTo(1);

        // staleAfterDays is a parameter, not a constant: a 200-day threshold must not
        // flag a 90-day-old deal.
        String wide = mockMvc.perform(get("/api/analytics/pipeline")
                        .header("Authorization", authHeader(manager))
                        .param("from", "2026-11-01").param("to", "2026-12-01")
                        .param("staleAfterDays", "200"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(parse(wide).get("stages").findValuesAsText("staleOpenDeals")).isNotNull();
        assertThat(findStage(parse(wide).get("stages"), "QUALIFIED").get("staleOpenDeals").asLong()).isZero();

        mockMvc.perform(get("/api/analytics/pipeline")
                        .header("Authorization", authHeader(manager))
                        .param("from", "2026-11-01").param("to", "2026-12-01")
                        .param("staleAfterDays", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forecastBucketsAreLiveWeightedAndHalfOpen() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        createOpportunity(sales, "Deal Nov", "+919700000035", "QUALIFIED", 20000, "2026-11-15");
        createOpportunity(sales, "Deal Nov 2", "+919700000036", "NEGOTIATION", 10000, "2026-11-20");
        // Exactly on the exclusive upper bound: must not appear in the window.
        createOpportunity(sales, "Deal Dec", "+919700000037", "QUALIFIED", 70000, "2026-12-01");

        String resp = forecast(manager, "2026-11-01", "2026-12-01");
        JsonNode report = parse(resp);

        JsonNode nov = findMonth(report.get("months"), "2026-11");
        assertThat(nov.get("openCount").asLong()).isEqualTo(2);
        assertThat(nov.get("best").asDouble()).isEqualTo(30000.0);
        // 20000*20% + 10000*70% = 4000 + 7000
        assertThat(nov.get("expected").asDouble()).isEqualTo(11000.0);

        assertThat(report.get("totals").get("openInWindow").asLong()).isEqualTo(2);
        assertThat(report.get("totals").get("openOutsideWindow").asLong()).isEqualTo(1);
        assertThat(report.get("totals").get("expected").asDouble()).isEqualTo(11000.0);

        // Stage mix shares of expected value: 7000/11000 = 63.6%, 4000/11000 = 36.4%.
        JsonNode mix = report.get("stageMix");
        assertThat(mix.get(0).get("stageKey").asText()).isEqualTo("NEGOTIATION");
        assertThat(mix.get(0).get("expected").asDouble()).isEqualTo(7000.0);
        assertThat(mix.get(0).get("expectedSharePct").asDouble()).isEqualTo(63.6);
        assertThat(mix.get(1).get("expectedSharePct").asDouble()).isEqualTo(36.4);

        // Re-weighting the stage must move the forecast with no other write.
        UUID stageId = stageId(manager, "NEGOTIATION");
        jdbcTemplate.update("update pipeline_stages set probability_weight = 50 where id = ?", stageId);
        JsonNode after = parse(forecast(manager, "2026-11-01", "2026-12-01"));
        // 20000*20% + 10000*50% = 4000 + 5000
        assertThat(findMonth(after.get("months"), "2026-11").get("expected").asDouble()).isEqualTo(9000.0);
    }

    @Test
    void forecastWindowValidationIsEnforced() throws Exception {
        String manager = managerToken();

        mockMvc.perform(get("/api/analytics/forecast").header("Authorization", authHeader(manager))
                        .param("from", "2026-12-01").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("from < to")));

        // from is required: an unbounded forecast would silently mean "everything".
        mockMvc.perform(get("/api/analytics/forecast").header("Authorization", authHeader(manager))
                        .param("to", "2026-12-01"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/analytics/partner-commissions").header("Authorization", authHeader(manager))
                        .param("from", "2026-12-01").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void salesSeesOnlyTheirOwnPipeline() throws Exception {
        String manager = managerToken();
        UUID salesAId = createUser("sales.report@securetravels.in", "Sales Report", Role.SALES, "sales123");
        String salesA = login("sales.report@securetravels.in", "sales123");
        UUID salesBId = createUser("sales2.report@securetravels.in", "Sales Two", Role.SALES, "sales123");
        String salesB = login("sales2.report@securetravels.in", "sales123");

        createOpportunity(salesA, "Mine", "+919700000038", "QUALIFIED", 100000, "2026-11-09");
        createOpportunity(salesB, "Theirs", "+919700000039", "QUALIFIED", 90000, "2026-11-09");

        // The manager sees both.
        assertThat(parse(pipeline(manager, "2026-11-01", "2026-12-01")).get("totals").get("openCount").asLong())
                .isEqualTo(2);

        // Each sales user sees one, and passing the other consultant's id cannot
        // widen it - the service overwrites the filter with the caller's own id.
        assertScopedToSelf(pipeline(salesA, "2026-11-01", "2026-12-01"), salesA, salesAId, salesBId);
        assertScopedToSelf(pipeline(salesB, "2026-11-01", "2026-12-01"), salesB, salesBId, salesAId);
    }

    private void assertScopedToSelf(String plainReport, String token, UUID ownId, UUID otherId) throws Exception {
        JsonNode self = parse(plainReport);
        assertThat(self.get("scope").asText()).isEqualTo("SELF");
        assertThat(self.get("totals").get("openCount").asLong()).isEqualTo(1);
        assertThat(self.get("ownerFilter").asText()).isEqualTo(ownId.toString());

        // The service must overwrite ownerId with the caller's own id.
        JsonNode forcedReport = parse(mockMvc.perform(get("/api/analytics/pipeline")
                        .header("Authorization", authHeader(token))
                        .param("ownerId", otherId.toString())
                        .param("from", "2026-11-01").param("to", "2026-12-01"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(forcedReport.get("ownerFilter").asText()).isEqualTo(ownId.toString());
        assertThat(forcedReport.get("totals").get("openCount").asLong()).isEqualTo(1);

        JsonNode forecastSelf = parse(mockMvc.perform(get("/api/analytics/forecast")
                        .header("Authorization", authHeader(token))
                        .param("ownerId", otherId.toString())
                        .param("from", "2026-11-01").param("to", "2026-12-01"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(forecastSelf.get("scope").asText()).isEqualTo("SELF");
        assertThat(forecastSelf.get("ownerFilter").asText()).isEqualTo(ownId.toString());
        assertThat(forecastSelf.get("owners")).hasSize(1);
    }

    @Test
    void partnerCommissionReportSeparatesPaidOpenAndVoid() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agent = createAccount(manager, "TRAVEL_AGENT", "Report Agent");
        UUID planId = createPercentPlan(manager, "REPORT_TERMS", 10, null);
        assign(manager, agent, planId);

        UUID trip = createTrip(manager, "Reporting Trek", 50000);

        // One confirmed booking -> 5000 OPEN.
        UUID open = confirmBooking(sales, createLead(sales, agent, "Open Booker", "+919700000040"), trip);

        // One cancelled -> 5000 VOID, not silently netted out.
        UUID cancelled = confirmBooking(sales, createLead(sales, agent, "Canceller", "+919700000041"), trip);
        cancelBooking(manager, cancelled);

        // One settled -> PAID. Rate is changed to 20% first so the effective rate
        // is unambiguous.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePercent", 20))))
                .andExpect(status().isOk());
        UUID paid = confirmBooking(sales, createLead(sales, agent, "Paid Booker", "+919700000042"), trip);
        settlePayable(manager, agent, paid);

        JsonNode report = parse(mockMvc.perform(get("/api/analytics/partner-commissions")
                        .header("Authorization", authHeader(manager))
                        .param("from", today().toString()).param("to", today().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

JsonNode account = report.get("accounts").get(0);
        assertThat(account.get("accountId").asText()).isEqualTo(agent.toString());
        assertThat(account.get("planKey").asText()).isEqualTo("REPORT_TERMS");
        assertThat(account.get("payables").asLong()).isEqualTo(3);
        // 2 travellers x 50000 base = 100000 net: 10000 open (10%), 10000 voided,
        // 20000 paid (after the rate moved to 20%).
        assertThat(account.get("accrued").asDouble()).isEqualTo(40000.0);
        assertThat(account.get("paid").asDouble()).isEqualTo(20000.0);
        assertThat(account.get("openLiability").asDouble()).isEqualTo(10000.0);
        assertThat(account.get("voided").asLong()).isEqualTo(1);
        assertThat(account.get("voidedAmount").asDouble()).isEqualTo(10000.0);
        assertThat(account.get("effectiveRatePercent").asDouble()).isEqualTo(20.0);

        JsonNode totals = report.get("totals");
        assertThat(totals.get("payables").asLong()).isEqualTo(3);
        assertThat(totals.get("accrued").asDouble()).isEqualTo(40000.0);
        assertThat(totals.get("openLiability").asDouble()).isEqualTo(10000.0);
        assertThat(totals.get("voided").asDouble()).isEqualTo(10000.0);
        assertThat(totals.get("paidSharePct").asDouble()).isEqualTo(50.0);

        JsonNode plan = report.get("plans").get(0);
        assertThat(plan.get("planKey").asText()).isEqualTo("REPORT_TERMS");
        assertThat(plan.get("method").asText()).isEqualTo("PERCENT");
        assertThat(plan.get("activeAccounts").asLong()).isEqualTo(1);
        assertThat(plan.get("openLiability").asDouble()).isEqualTo(10000.0);

        assertThat(open).isNotNull();
    }

    @Test
    void partnerCommissionIsManagerOnlyAndScatteredAcrossPlans() throws Exception {
        String manager = managerToken();
        String sales = salesToken();
        String ops = opsToken();

        // An account with no plan accrues under the flat fallback, which must still
        // appear in the per-plan rollup instead of vanishing.
        UUID flatAgent = createAccount(manager, "TRAVEL_AGENT", "Flat Agent");
        UUID trip = createTrip(manager, "Flat Trek", 30000);
        confirmBooking(sales, createLead(sales, flatAgent, "Flat Booker", "+919700000043"), trip);

        UUID planAgent = createAccount(manager, "TRAVEL_AGENT", "Planned Agent");
        UUID planId = createPercentPlan(manager, "MIXED_TERMS", 10, null);
        assign(manager, planAgent, planId);
        confirmBooking(sales, createLead(sales, planAgent, "Planned Booker", "+919700000044"), trip);

        for (String token : List.of(sales, ops)) {
            mockMvc.perform(get("/api/analytics/partner-commissions").header("Authorization", authHeader(token))
                            .param("from", today().toString()).param("to", today().plusDays(1).toString()))
                    .andExpect(status().isForbidden());
        }

        JsonNode report = parse(mockMvc.perform(get("/api/analytics/partner-commissions")
                        .header("Authorization", authHeader(manager))
                        .param("from", today().toString()).param("to", today().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

assertThat(report.get("plans")).hasSize(2);
        assertThat(findPlan(report.get("plans"), "DEFAULT_FLAT_RATE").get("payables").asLong()).isEqualTo(1);
        assertThat(findPlan(report.get("plans"), "DEFAULT_FLAT_RATE").get("accrued").asDouble()).isEqualTo(6000.0);
        assertThat(findPlan(report.get("plans"), "MIXED_TERMS").get("accrued").asDouble()).isEqualTo(6000.0);
assertThat(report.get("totals").get("accounts").asLong()).isEqualTo(2);
        assertThat(report.get("totals").get("accrued").asDouble()).isEqualTo(12000.0);
    }

    /**
     * One account that changed plans is one account, however many rows it takes.
     *
     * <p>Rows are grouped by account and plan because a payable keeps the terms it
     * was struck under, so an account that was reassigned mid-history legitimately
     * appears twice. The {@code accounts} total is a count of partners paid, and
     * counting rows there would have inflated it to 2 for a single agent - a
     * number that goes into a commission report a manager reconciles by hand.
     */
    @Test
    void partnerCommissionTotalsCountAccountsNotAccountPlanRows() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agent = createAccount(manager, "TRAVEL_AGENT", "Reassigned Agent");
        UUID trip = createTrip(manager, "Reassigned Trek", 30000);

        // First booking under the flat fallback, then the account is given terms.
        confirmBooking(sales, createLead(sales, agent, "Before Terms", "+919700000045"), trip);
        UUID planId = createPercentPlan(manager, "LATER_TERMS", 10, null);
        assign(manager, agent, planId);
        confirmBooking(sales, createLead(sales, agent, "After Terms", "+919700000046"), trip);

        JsonNode report = parse(mockMvc.perform(get("/api/analytics/partner-commissions")
                        .header("Authorization", authHeader(manager))
                        .param("from", today().toString()).param("to", today().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // Two rows, two plans, two payables - and one account.
        assertThat(report.get("accounts")).hasSize(2);
        assertThat(report.get("plans")).hasSize(2);
        assertThat(report.get("totals").get("payables").asLong()).isEqualTo(2);
        assertThat(report.get("totals").get("accounts").asLong()).isEqualTo(1);
        // 10% flat on the first, 10% under the plan on the second.
        assertThat(report.get("totals").get("accrued").asDouble()).isEqualTo(12000.0);
    }

    @Test
    void emptyReportsAreZeroNotNull() throws Exception {
        String manager = managerToken();

        JsonNode pipeline = parse(pipeline(manager, "2026-11-01", "2026-12-01"));
        // Every stage exists so the UI can render a full column set; none has deals.
        assertThat(pipeline.get("stages")).isNotEmpty();
        assertThat(pipeline.get("totals").get("openCount").asLong()).isZero();
        assertThat(pipeline.get("totals").get("openValue").asDouble()).isZero();
        // No denominator, so no percentage is invented.
        assertThat(pipeline.get("totals").get("winRatePct").isNull()).isTrue();

        JsonNode commission = parse(mockMvc.perform(get("/api/analytics/partner-commissions")
                        .header("Authorization", authHeader(manager))
                        .param("from", today().toString()).param("to", today().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(commission.get("accounts")).isEmpty();
        assertThat(commission.get("totals").get("accrued").asDouble()).isZero();
        assertThat(commission.get("totals").get("avgCommissionPerPayable").asDouble()).isZero();
        assertThat(commission.get("totals").get("paidSharePct").isNull()).isTrue();
    }

    // ------------------------------------------------------------------ helpers

    private String pipeline(String token, String from, String to) throws Exception {
        return mockMvc.perform(get("/api/analytics/pipeline").header("Authorization", authHeader(token))
                        .param("from", from).param("to", to))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String forecast(String token, String from, String to) throws Exception {
        return mockMvc.perform(get("/api/analytics/forecast").header("Authorization", authHeader(token))
                        .param("from", from).param("to", to))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private UUID createOpportunity(String token, String customer, String mobile, String stageKey,
                                   int value, String expectedDate) throws Exception {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("leadId", createLead(token, customer, mobile).toString());
        fields.put("stageKey", stageKey);
        fields.put("expectedValue", value);
        fields.put("expectedDate", expectedDate);
        String resp = mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(fields)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

private UUID createLead(String token, String customer, String mobile) throws Exception {
        return createLead(token, null, customer, mobile);
    }

    private UUID createLead(String token, UUID accountId, String customer, String mobile) throws Exception {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("customerName", customer);
        fields.put("mobileNumber", mobile);
        fields.put("source", "WEBSITE");
        fields.put("consentGiven", true);
        fields.put("consentScope", "ALL");
        if (accountId != null) {
            fields.put("accountId", accountId.toString());
        }
        String resp = mockMvc.perform(post("/api/leads")
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(fields)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID stageId(String token, String key) throws Exception {
        String list = mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode row : parse(list)) {
            if (key.equals(row.get("key").asText())) {
                return UUID.fromString(row.get("id").asText());
            }
        }
        throw new AssertionError("stage not found: " + key);
    }

    private UUID createAccount(String token, String type, String name) throws Exception {
        String resp = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(Map.of("accountType", type, "name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createPercentPlan(String token, String key, int rate, Integer minSalesThreshold) throws Exception {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("key", key);
        fields.put("label", "Plan " + key);
        fields.put("basis", "NET");
        fields.put("method", "PERCENT");
        fields.put("ratePercent", rate);
        if (minSalesThreshold != null) {
            fields.put("minSalesThreshold", minSalesThreshold);
        }
        String resp = mockMvc.perform(post("/api/commission-plans")
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(fields)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private void assign(String token, UUID accountId, UUID planId) throws Exception {
        mockMvc.perform(post("/api/commission-plans/accounts/{accountId}", accountId)
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(Map.of("planId", planId.toString()))))
                .andExpect(status().isOk());
    }

    private UUID createTrip(String token, String name, int baseCost) throws Exception {
        String resp = mockMvc.perform(post("/api/trips")
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "name", name, "category", "TREK",
                                "bookingType", "CUSTOM_FIT", "baseCost", baseCost,
                                "durationDays", 5, "difficulty", "MODERATE"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createBooking(String token, UUID leadId, UUID tripId) throws Exception {
        String resp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "leadId", leadId.toString(), "tripId", tripId.toString(),
                                "travelDate", "2026-12-15", "numTravellers", 2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID confirmBooking(String token, UUID leadId, UUID tripId) throws Exception {
        UUID booking = createBooking(token, leadId, tripId);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/bookings/{id}/status", booking)
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());
        return booking;
    }

    private void cancelBooking(String token, UUID bookingId) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/bookings/{id}/status", bookingId)
                        .header("Authorization", authHeader(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
    }

    private void settlePayable(String token, UUID accountId, UUID bookingId) throws Exception {
        String payables = mockMvc.perform(get("/api/accounts/{id}/payables", accountId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode row : parse(payables)) {
            if (bookingId.toString().equals(row.get("bookingId").asText())) {
                mockMvc.perform(post("/api/accounts/{a}/payables/{p}/settle", accountId, row.get("id").asText())
                                .header("Authorization", authHeader(token))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"paidRef\":\"NEFT-TEST-1\"}"))
                        .andExpect(status().isOk());
                return;
            }
        }
        throw new AssertionError("no payable for booking " + bookingId);
    }

    private static JsonNode findStage(JsonNode array, String key) {
        for (JsonNode node : array) {
            if (key.equals(node.get("stageKey").asText())) return node;
        }
        throw new AssertionError("stage not found in report: " + key);
    }

    private static JsonNode findMonth(JsonNode array, String month) {
        for (JsonNode node : array) {
            if (month.equals(node.get("month").asText())) return node;
        }
        throw new AssertionError("month bucket not found: " + month);
    }

    private static JsonNode findPlan(JsonNode array, String key) {
        for (JsonNode node : array) {
            if (key.equals(node.get("planKey").asText())) return node;
        }
        throw new AssertionError("plan not found in report: " + key);
    }

    private static java.time.LocalDate today() {
        return java.time.LocalDate.now();
    }

    private String managerToken() throws Exception {
        createUser("mgr.report@securetravels.in", "Mgr Report", Role.MANAGER, "manager123");
        return login("mgr.report@securetravels.in", "manager123");
    }

    private String salesToken() throws Exception {
        createUser("sales.report@securetravels.in", "Sales Report", Role.SALES, "sales123");
        return login("sales.report@securetravels.in", "sales123");
    }

    private String otherSalesToken() throws Exception {
        createUser("sales2.report@securetravels.in", "Sales Two", Role.SALES, "sales123");
        return login("sales2.report@securetravels.in", "sales123");
    }

    private String opsToken() throws Exception {
        createUser("ops.report@securetravels.in", "Ops Report", Role.OPS, "ops123");
        return login("ops.report@securetravels.in", "ops123");
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode parse(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}