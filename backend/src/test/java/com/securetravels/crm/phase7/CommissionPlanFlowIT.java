package com.securetravels.crm.phase7;

import com.fasterxml.jackson.databind.JsonNode;
import com.securetravels.crm.BaseIT;
import com.securetravels.crm.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 Module 4 — partner commission plans, end to end: plan administration,
 * assignment to a travel-agent account, and the commission that actually lands
 * on a confirmed booking.
 *
 * <p>Runs with {@code partner-commissions=true}, the Module 1 feature flag,
 * because plans only affect behaviour when the flag is on.
 *
 * <p>The exact tier-boundary arithmetic is pinned separately and much faster in
 * {@code CommissionCalculatorTest}; what matters here is that the engine the
 * booking path calls is the same one, that plan terms reach the payable, and
 * that the RBAC/guard surface is enforced.
 */
@TestPropertySource(properties = "app.feature-flags.partner-commissions=true")
class CommissionPlanFlowIT extends BaseIT {

    @Test
    void tieredPlanAccruesTheAgreedCommissionOnConfirm() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Himalaya Journeys");
        String planResp = createTieredPlan(manager, "TIERED_AGENT",
                Map.of("fromAmount", 0, "toAmount", 50000, "ratePercent", 5),
                Map.of("fromAmount", 50000, "toAmount", 100000, "ratePercent", 10),
                Map.of("fromAmount", 100000, "ratePercent", 15))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID planId = UUID.fromString(parse(planResp).get("id").asText());
        assign(manager, agentId, planId).andExpect(status().isOk());

        UUID tripId = createCustomFitTrip(manager, "Spiti Pass", 80000);

        // 80000 net lands exactly on the 50000 boundary -> the second tier.
        UUID lead = createLead(sales, agentId, "Trek Traveller", "+919700000021");
        UUID booking = createBooking(sales, lead, tripId, 1);
        confirm(sales, booking).andExpect(status().isOk());

        JsonNode payable = payables(manager, agentId).get(0);
        assertThat(payable.get("commissionAmount").asDouble()).isEqualTo(8000.0);
        assertThat(payable.get("ratePercent").asDouble()).isEqualTo(10.0);
        assertThat(payable.get("planId").asText()).isEqualTo(planId.toString());
        assertThat(payable.get("status").asText()).isEqualTo("OPEN");
    }

    @Test
    void accountWithoutAPlanKeepsTheFlatDefaultRate() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "No Plan Travels");
        UUID tripId = createCustomFitTrip(manager, "Valley Trail", 30000);

        UUID lead = createLead(sales, agentId, "Unassigned Traveller", "+919700000022");
        confirm(sales, createBooking(sales, lead, tripId, 1)).andExpect(status().isOk());

        JsonNode payable = payables(manager, agentId).get(0);
        // Module 1 behaviour: 10% flat, and no plan behind it.
        assertThat(payable.get("commissionAmount").asDouble()).isEqualTo(3000.0);
        assertThat(payable.get("ratePercent").asDouble()).isEqualTo(10.0);
        assertThat(payable.get("planId").isNull()).isTrue();
    }

    @Test
    void minimumSalesThresholdSuppressesThePayable() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Threshold Travels");
        UUID planId = createPercentPlan(manager, "HIGH_MINIMUM", 12, 100000);
        assign(manager, agentId, planId).andExpect(status().isOk());

        UUID tripId = createCustomFitTrip(manager, "Short Trek", 40000);

        UUID under = createLead(sales, agentId, "Below Threshold", "+919700000023");
        confirm(sales, createBooking(sales, under, tripId, 1)).andExpect(status().isOk());
        assertThat(payables(manager, agentId)).isEmpty();

        // Exactly at the threshold is commissionable.
        UUID tripBig = createCustomFitTrip(manager, "Big Trek", 100000);
        UUID at = createLead(sales, agentId, "At Threshold", "+919700000024");
        confirm(sales, createBooking(sales, at, tripBig, 1)).andExpect(status().isOk());

        JsonNode payable = payables(manager, agentId).get(0);
        assertThat(payable.get("commissionAmount").asDouble()).isEqualTo(12000.0);
        assertThat(payable.get("ratePercent").asDouble()).isEqualTo(12.0);
    }

    @Test
    void grossBasisIgnoresTheBookingDiscount() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Gross Basis Travels");
        UUID planId = createPercentPlan(manager, "GROSS_BASIS", 10, null);
        mockMvc.perform(patch("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("basis", "GROSS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis").value("GROSS"));
        assign(manager, agentId, planId).andExpect(status().isOk());

        UUID tripId = createCustomFitTrip(manager, "Discounted Trek", 40000);
        UUID lead = createLead(sales, agentId, "Discounted Traveller", "+919700000025");
        UUID booking = createBooking(sales, lead, tripId, 1, 5000);
        // Sales may request a discount, but a manager confirms a discounted booking.
        confirm(manager, booking).andExpect(status().isOk());

        JsonNode payable = payables(manager, agentId).get(0);
        assertThat(payable.get("basis").asText()).isEqualTo("GROSS");
        // 10% of the undiscounted gross, not of 35000.
        assertThat(payable.get("commissionAmount").asDouble()).isEqualTo(4000.0);
    }

    @Test
    void fixedPlanPaysAFlatFeeRegardlessOfSize() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Flat Fee Travels");
        String planResp = mockMvc.perform(post("/api/commission-plans")
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "key", "FLAT_FEE", "label", "Flat fee partner",
                                "basis", "NET", "method", "FIXED", "fixedAmount", 3000))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fixedAmount").value(3000.0))
                .andExpect(jsonPath("$.ratePercent").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assign(manager, agentId, UUID.fromString(parse(planResp).get("id").asText()))
                .andExpect(status().isOk());

        UUID tripId = createCustomFitTrip(manager, "Expensive Trek", 250000);
        UUID lead = createLead(sales, agentId, "Big Group", "+919700000026");
        confirm(sales, createBooking(sales, lead, tripId, 4)).andExpect(status().isOk());

        JsonNode payable = payables(manager, agentId).get(0);
        assertThat(payable.get("commissionAmount").asDouble()).isEqualTo(3000.0);
        assertThat(payable.get("ratePercent").asDouble()).isEqualTo(0.0);
    }

    @Test
    void reAssigningASupersedesButKeepsTheOldTermsOnThePayable() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Changing Terms");
        UUID first = createPercentPlan(manager, "TERMS_V1", 10, null);
        UUID second = createPercentPlan(manager, "TERMS_V2", 20, null);

        UUID tripId = createCustomFitTrip(manager, "Rishikesh Trek", 10000);

        assign(manager, agentId, first).andExpect(status().isOk());
        UUID leadOne = createLead(sales, agentId, "Early Booker", "+919700000027");
        confirm(sales, createBooking(sales, leadOne, tripId, 1)).andExpect(status().isOk());

        mockMvc.perform(post("/api/commission-plans/accounts/{accountId}", agentId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("planId", second.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planKey").value("TERMS_V2"));

        UUID leadTwo = createLead(sales, agentId, "Late Booker", "+919700000028");
        confirm(sales, createBooking(sales, leadTwo, tripId, 1)).andExpect(status().isOk());

        JsonNode rows = payables(manager, agentId);
        assertThat(rows).hasSize(2);
        JsonNode early = null;
        JsonNode late = null;
        for (JsonNode row : rows) {
            if (first.toString().equals(row.get("planId").asText())) early = row;
            if (second.toString().equals(row.get("planId").asText())) late = row;
        }
        assertThat(early).as("the earlier booking keeps the earlier terms").isNotNull();
        assertThat(early.get("commissionAmount").asDouble()).isEqualTo(1000.0);
        assertThat(late).isNotNull();
        assertThat(late.get("commissionAmount").asDouble()).isEqualTo(2000.0);

        // Unassigning leaves the historic payables alone.
        mockMvc.perform(delete("/api/commission-plans/accounts/{accountId}", agentId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/commission-plans/accounts/{accountId}", agentId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNotFound());
        assertThat(payables(manager, agentId)).hasSize(2);
    }

    @Test
    void corporateAccountsCannotCarryAPlan() throws Exception {
        String manager = managerToken();
        UUID corpId = createAccount(manager, "CORPORATE", "BigCorp India");
        UUID planId = createPercentPlan(manager, "AGENT_TERMS", 10, null);

        assign(manager, corpId, planId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("TRAVEL_AGENT")));
    }

    @Test
    void tierBandsMustTileTheWholeRangeWithoutGapsOrOverlap() throws Exception {
        String manager = managerToken();

        // Gap: the first band must start at 0.
        createTieredPlan(manager, "TIER_GAP",
                Map.of("fromAmount", 1000, "toAmount", 50000, "ratePercent", 5),
                Map.of("fromAmount", 50000, "ratePercent", 10))
                .andExpect(status().isBadRequest());

        // Overlap.
        createTieredPlan(manager, "TIER_OVERLAP",
                Map.of("fromAmount", 0, "toAmount", 60000, "ratePercent", 5),
                Map.of("fromAmount", 50000, "ratePercent", 10))
                .andExpect(status().isBadRequest());

        // The top band must be open-ended, or values above it would be unpriced.
        createTieredPlan(manager, "TIER_CLOSED_TOP",
                Map.of("fromAmount", 0, "toAmount", 50000, "ratePercent", 5),
                Map.of("fromAmount", 50000, "toAmount", 100000, "ratePercent", 10))
                .andExpect(status().isBadRequest());

        // Open-ended band in the middle would shadow the tiers after it.
        createTieredPlan(manager, "TIER_EARLY_OPEN",
                Map.of("fromAmount", 0, "ratePercent", 5),
                Map.of("fromAmount", 50000, "ratePercent", 10))
                .andExpect(status().isBadRequest());

        // A TIERED plan needs tiers at all.
        mockMvc.perform(post("/api/commission-plans")
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "key", "TIER_NONE", "label", "No bands",
                                "basis", "NET", "method", "TIERED"))))
                .andExpect(status().isBadRequest());

        // ...and a PERCENT plan must not carry them.
        mockMvc.perform(post("/api/commission-plans")
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "key", "PERCENT_WITH_TIERS", "label", "Confused",
                                "basis", "NET", "method", "PERCENT", "ratePercent", 10,
                                "tiers", List.of(Map.of("fromAmount", 0, "ratePercent", 5))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void planKeysAreUniqueAndImmutableAndTermsAreAudited() throws Exception {
        String manager = managerToken();

        UUID planId = createPercentPlan(manager, "IMMUTABLE_KEY", 10, null);

        mockMvc.perform(post("/api/commission-plans")
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("key", "IMMUTABLE_KEY", "label", "Duplicate",
                                "basis", "NET", "method", "PERCENT", "ratePercent", 12))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("key", "RENAMED_KEY"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("immutable")));

        mockMvc.perform(patch("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("ratePercent", 15))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratePercent").value(15.0));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where entity = ? and entity_id = ?",
                Integer.class, "COMMISSION_PLAN", planId)).isGreaterThan(0);
    }

    @Test
    void anInUsePlanCannotBeDeactivatedOrDeleted() throws Exception {
        String manager = managerToken();
        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Locked Terms");
        UUID planId = createPercentPlan(manager, "LOCKED_TERMS", 10, null);
        assign(manager, agentId, planId).andExpect(status().isOk());

        mockMvc.perform(delete("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("active", false))))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/commission-plans/accounts/{accountId}", agentId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNoContent());

        // Unassigning releases the plan, but the assignment history still pins
        // the terms, so it stays undeletable and must be deactivated instead.
        mockMvc.perform(delete("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void anUnusedPlanCanBeDeleted() throws Exception {
        String manager = managerToken();
        UUID planId = createPercentPlan(manager, "THROW_AWAY", 10, null);

        mockMvc.perform(delete("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anInactivePlanCannotBeAssigned() throws Exception {
        String manager = managerToken();
        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Dormant Terms");
        UUID planId = createPercentPlan(manager, "DORMANT_TERMS", 10, null);

        mockMvc.perform(patch("/api/commission-plans/{id}", planId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("active", false))))
                .andExpect(status().isOk());
        assign(manager, agentId, planId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("inactive")));
    }

    @Test
    void onlyManagersSeeOrChangeCommissionTerms() throws Exception {
        String manager = managerToken();
        String sales = salesToken();
        String ops = opsToken();
        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Watchful Agent");
        UUID planId = createPercentPlan(manager, "PRIVATE_TERMS", 10, null);

        for (String token : List.of(sales, ops)) {
            mockMvc.perform(get("/api/commission-plans").header("Authorization", authHeader(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/commission-plans/{id}", planId)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/commission-plans")
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("key", "SNEAKY", "label", "Nope",
                                    "basis", "NET", "method", "PERCENT", "ratePercent", 50))))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/commission-plans/accounts/{accountId}", agentId)
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("planId", planId.toString()))))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/commission-plans/accounts/{accountId}/quote", agentId)
                            .header("Authorization", authHeader(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("grossAmount", 10000))))
                    .andExpect(status().isForbidden());

            // Money owed to a partner stays manager-and-up, even for the
            // sales rep who owns the account.
            mockMvc.perform(get("/api/accounts/{id}/payables", agentId)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/accounts/{id}/invoices", agentId)
                            .header("Authorization", authHeader(token)))
                    .andExpect(status().isForbidden());
        }

        // The manager view works, and shows what the agent would be paid.
        mockMvc.perform(get("/api/commission-plans").header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].key").value("PRIVATE_TERMS"));

        assign(manager, agentId, planId).andExpect(status().isOk());
        mockMvc.perform(post("/api/commission-plans/accounts/{accountId}/quote", agentId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("grossAmount", 10000, "discountAmount", 1000, "taxAmount", 900))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planKey").value("PRIVATE_TERMS"))
                .andExpect(jsonPath("$.basisAmount").value(9900.0))
                .andExpect(jsonPath("$.commissionAmount").value(990.0));
    }

    @Test
    void quoteFallsBackToTheFlatRateForAnUnassignedAccount() throws Exception {
        String manager = managerToken();
        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "No Terms Yet");

        mockMvc.perform(post("/api/commission-plans/accounts/{accountId}/quote", agentId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("grossAmount", 25000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planKey").value("DEFAULT_FLAT_RATE"))
                .andExpect(jsonPath("$.commissionAmount").value(2500.0));

        mockMvc.perform(post("/api/commission-plans/accounts/{accountId}/quote", agentId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("grossAmount", -5))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ helpers

    private UUID createPercentPlan(String token, String key, int rate, Integer minSalesThreshold) throws Exception {
        Map<String, Object> fields = new java.util.LinkedHashMap<>();
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(fields)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private org.springframework.test.web.servlet.ResultActions createTieredPlan(
            String token, String key, Map<String, Object>... bands) throws Exception {
        Map<String, Object> fields = new java.util.LinkedHashMap<>();
        fields.put("key", key);
        fields.put("label", "Plan " + key);
        fields.put("basis", "NET");
        fields.put("method", "TIERED");
        fields.put("tiers", List.of(bands));
        return mockMvc.perform(post("/api/commission-plans")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(fields)));
    }

    private org.springframework.test.web.servlet.ResultActions assign(String token, UUID accountId, UUID planId)
            throws Exception {
        return mockMvc.perform(post("/api/commission-plans/accounts/{accountId}", accountId)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("planId", planId.toString()))));
    }

    private UUID createAccount(String token, String type, String name) throws Exception {
        String resp = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("accountType", type, "name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createLead(String token, UUID accountId, String name, String mobile) throws Exception {
        String resp = mockMvc.perform(post("/api/leads")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "customerName", name, "mobileNumber", mobile,
                                "source", "WEBSITE", "consentGiven", true,
                                "consentScope", "ALL", "accountId", accountId.toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createBooking(String token, UUID leadId, UUID tripId, int travellers) throws Exception {
        return createBooking(token, leadId, tripId, travellers, null);
    }

    private UUID createBooking(String token, UUID leadId, UUID tripId, int travellers,
                               Integer discountAmount) throws Exception {
        Map<String, Object> fields = new java.util.LinkedHashMap<>();
        fields.put("leadId", leadId.toString());
        fields.put("tripId", tripId.toString());
        fields.put("travelDate", "2026-11-20");
        fields.put("numTravellers", travellers);
        if (discountAmount != null) {
            fields.put("discountAmount", discountAmount);
        }
        String resp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(fields)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createCustomFitTrip(String token, String name, int baseCost) throws Exception {
        String resp = mockMvc.perform(post("/api/trips")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "name", name, "category", "TREK",
                                "bookingType", "CUSTOM_FIT", "baseCost", baseCost,
                                "durationDays", 6, "difficulty", "MODERATE"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, UUID bookingId) throws Exception {
        return mockMvc.perform(patch("/api/bookings/{id}/status", bookingId)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\"}"));
    }

    private JsonNode payables(String token, UUID accountId) throws Exception {
        String resp = mockMvc.perform(get("/api/accounts/{id}/payables", accountId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return parse(resp);
    }

    private String managerToken() throws Exception {
        createUser("mgr.plan@securetravels.in", "Mgr Plan", Role.MANAGER, "manager123");
        return login("mgr.plan@securetravels.in", "manager123");
    }

    private String salesToken() throws Exception {
        createUser("sales.plan@securetravels.in", "Sales Plan", Role.SALES, "sales123");
        return login("sales.plan@securetravels.in", "sales123");
    }

    private String opsToken() throws Exception {
        createUser("ops.plan@securetravels.in", "Ops Plan", Role.OPS, "ops123");
        return login("ops.plan@securetravels.in", "ops123");
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode parse(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}
