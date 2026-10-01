package com.securetravels.crm.phase7;

import com.fasterxml.jackson.databind.JsonNode;
import com.securetravels.crm.BaseIT;
import com.securetravels.crm.customer.Customer360;
import com.securetravels.crm.customer.Customer360Repository;
import com.securetravels.crm.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The four Phase 7 hardening gates from {@code PHASE_7_DELTA.md} §5, in the
 * order the plan lists them.
 *
 * <p>None of this is new functionality. Each test restates a guarantee Phase 7
 * claimed and proves it through the HTTP API end to end, because the per-module
 * tests answer module questions while these answer the phase question a reviewer
 * actually asks: did any of it hold together?
 *
 * <p>Where a guarantee is only partly true, the test says so in its name and
 * javadoc rather than quietly asserting the weaker half. See
 * {@link #gate2_invalidTransitionsAreRefusedAndEntryConditionsAreAdvisory()} for
 * the one case where that applies.
 */
@TestPropertySource(properties = "app.feature-flags.partner-commissions=true")
class Phase7HardeningIT extends BaseIT {

    @Autowired private Customer360Repository customerRepository;

    // ==================================================================
    // Gate 1 - commission correctness, including tiered boundaries
    // ==================================================================

    /**
     * A tiered plan's boundary is inclusive and it is the last digit that decides.
     *
     * <p>49999.99 net stays on the 5% band; exactly 50000.00 moves to 10%. Both
     * bookings are the same trip and the same party size; the only difference is
     * a one-paisa discount. A comparator using {@code >} instead of {@code >=}
     * would pay the wrong band on one of them, and the assertion that catches it
     * is {@code ratePercent}, not {@code commissionAmount}: at 49999.99 the 5%
     * result rounds to the same 2500.00 as a would-be off-by-one, so only the
     * recorded rate reveals which band was selected.
     */
    @Test
    void gate1_tieredBoundaryIsInclusiveAtTheLastDigit() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Boundary Gate Agent");
        String resp = mockMvc.perform(post("/api/commission-plans")
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "key", "GATE_TIERED", "label", "Gate Tiered",
                                "basis", "NET", "method", "TIERED",
                                "tiers", List.of(
                                        Map.of("fromAmount", 0, "toAmount", 50000, "ratePercent", 5),
                                        Map.of("fromAmount", 50000, "ratePercent", 10))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID planId = UUID.fromString(parse(resp).get("id").asText());
        assign(manager, agentId, planId).andExpect(status().isOk());

        // baseCost 25000 x 2 travellers = 50000 gross either way.
        UUID tripId = createTrip(manager, "Boundary Gate Trek", 25000);

        confirm(sales, createLeadBooking(sales, agentId, "One Paisa Below", "+919700000061", tripId,
                new BigDecimal("0.01")));
        confirm(sales, createLeadBooking(sales, agentId, "Exactly On", "+919700000062", tripId, null));

        JsonNode payables = payables(manager, agentId);
        assertThat(payables).hasSize(2);

        JsonNode below = payableWithNet(payables, 49999.99);
        assertThat(below.get("ratePercent").asDouble()).isEqualTo(5.0);
        // 49999.99 x 5% = 2499.9995, which HALF_UP to 2dp renders as 2500.00.
        assertThat(below.get("commissionAmount").asDouble()).isEqualTo(2500.00);
        assertThat(below.get("planId").asText()).isEqualTo(planId.toString());

        JsonNode on = payableWithNet(payables, 50000.00);
        assertThat(on.get("ratePercent").asDouble()).isEqualTo(10.0);
        assertThat(on.get("commissionAmount").asDouble()).isEqualTo(5000.00);
    }

    // ==================================================================
    // Gate 2 - pipeline validation
    // ==================================================================

    /**
     * Invalid transitions are refused, and entry conditions are documented as
     * what they actually are.
     *
     * <p><strong>Refused:</strong> an unknown stage key (404), any move on an
     * opportunity that is no longer OPEN (409), and a second close (409).
     *
     * <p><strong>Not enforced, and this test says so on purpose:</strong>
     * {@code pipeline_stages.entry_condition} is free text — "Qualified inbound /
     * B2B lead" — which the API stores, sanitises and returns, but never
     * evaluates. It is a note for the humans who configure a pipeline, not a rule
     * the server checks, and no status code would make it one. The stronger
     * version of this test would post an opportunity whose lead plainly fails
     * "B2B lead" and assert a rejection; that test would have to lie, because the
     * condition is prose. Machine-evaluated entry conditions need a condition
     * vocabulary to evaluate against, which is Phase 9 scope.
     */
    @Test
    void gate2_invalidTransitionsAreRefusedAndEntryConditionsAreAdvisory() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID opportunityId = createOpportunity(sales, "Gate Two Lead", "+919700000063",
                "QUALIFIED", 50000);

        // The seeded default stage carries its condition text to clients...
        mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(sales)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryCondition").value("Qualified inbound / B2B lead"))
                .andExpect(jsonPath("$[0].probabilityWeight").value(20));

        // ...and that text gates nothing. An unknown key is the only stage-level
        // refusal on a move; there is no entry-condition check to trip.
        mockMvc.perform(patch("/api/opportunities/{id}/stage", opportunityId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("stageKey", "NO_SUCH_STAGE"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/opportunities/{id}/stage", opportunityId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("stageKey", "NEGOTIATION"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/opportunities/{id}/close", opportunityId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("outcome", "WON"))))
                .andExpect(status().isOk());

        // WON is terminal: absorbing for moves, and final for close.
        mockMvc.perform(patch("/api/opportunities/{id}/stage", opportunityId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("stageKey", "QUOTATION_SENT"))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/opportunities/{id}/close", opportunityId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("outcome", "LOST"))))
                .andExpect(status().isConflict());

        // A lead may hold only one opportunity.
        UUID secondLead = createLead(sales, "Gate Two Second", "+919700000064");
        createOpportunity(sales, secondLead, "QUALIFIED", 1000);
        mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "leadId", secondLead.toString(), "stageKey", "QUALIFIED",
                                "expectedValue", 2000, "expectedDate", "2026-11-30"))))
                .andExpect(status().isConflict());
    }

    // ==================================================================
    // Gate 3 - commission visibility, 403 across owners
    // ==================================================================

    /**
     * Money owed to a partner stays manager-and-up from every angle a caller
     * might try, including the sales rep who booked the trip themselves.
     *
     * <p>The rep's own booking is the interesting case: they created the revenue,
     * so they can see their own opportunity and their own dashboard. What they
     * must not see is what the partner is owed for it, or the terms behind that
     * number — a competitor's rate card is the whole reason. Settlement is tested
     * here too, because a read-only boundary is not a boundary if the write is
     * open.
     */
    @Test
    void gate3_commissionIsInvisibleEvenToTheRepWhoEarnedIt() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Watched Agent");
        UUID planId = createPercentPlan(manager, "GATE_PRIVATE", 10);
        assign(manager, agentId, planId).andExpect(status().isOk());

        UUID tripId = createTrip(manager, "Watched Trek", 50000);
        UUID leadId = createLead(sales, agentId, "Earned It", "+919700000065");
        UUID bookingId = createBooking(sales, leadId, tripId, 1);
        confirm(sales, bookingId).andExpect(status().isOk());
        assertThat(payables(manager, agentId)).hasSize(1);

        // The rep owns the revenue and can see that much.
        mockMvc.perform(get("/api/analytics/pipeline").header("Authorization", authHeader(sales)))
                .andExpect(status().isOk());

        // But not the liability, the invoice, the aggregate, or the terms.
        mockMvc.perform(get("/api/accounts/{id}/payables", agentId).header("Authorization", authHeader(sales)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/accounts/{id}/invoices", agentId).header("Authorization", authHeader(sales)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/analytics/partner-commissions")
                        .header("Authorization", authHeader(sales))
                        .param("from", LocalDate.now().toString())
                        .param("to", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/commission-plans/{id}", planId).header("Authorization", authHeader(sales)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/commission-plans").header("Authorization", authHeader(sales)))
                .andExpect(status().isForbidden());

        // And cannot settle it.
        String payableId = payables(manager, agentId).get(0).get("id").asText();
        mockMvc.perform(post("/api/accounts/{a}/payables/{p}/settle", agentId, payableId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("paidRef", "NEFT-SHOULD-NOT-WORK"))))
                .andExpect(status().isForbidden());

        // A different rep is refused identically — this is a role boundary, not an
        // ownership accident.
        String otherSales = otherSalesToken();
        mockMvc.perform(get("/api/accounts/{id}/payables", agentId).header("Authorization", authHeader(otherSales)))
                .andExpect(status().isForbidden());

        // The manager settles, and the settlement is attributed.
        mockMvc.perform(post("/api/accounts/{a}/payables/{p}/settle", agentId, payableId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("paidRef", "NEFT-GATE-001"))))
                .andExpect(status().isOk());

        JsonNode settled = payables(manager, agentId).get(0);
        assertThat(settled.get("status").asText()).isEqualTo("PAID");
        assertThat(settled.get("paidRef").asText()).isEqualTo("NEFT-GATE-001");
    }

    // ==================================================================
    // Gate 4 - account-linked invoicing, no retail regression
    // ==================================================================

    /**
     * An account booking is invoiced once, billed to the account's own GSTIN; a
     * retail booking on the same trip and the same price produces neither an
     * invoice nor a commission payable.
     *
     * <p>The retail half is the one worth having. Account invoicing was added to
     * a booking path that every retail booking already walked, so the risk was
     * never "accounts get invoiced wrongly" but "retail starts getting invoiced
     * at all". Module 1 covered the agent case and never asserted the absence of
     * the retail one, so nothing was standing between a policy decision and every
     * walk-in customer in the database getting a GST invoice.
     */
    @Test
    void gate4_retailBookingsInheritNoInvoiceOrCommission() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createAccount(manager, "TRAVEL_AGENT", "Invoiced Agent");
        mockMvc.perform(patch("/api/accounts/{id}", agentId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("gstin", "33AAACC1206D1ZN"))))
                .andExpect(status().isOk());

        UUID tripId = createTrip(manager, "Retail Safety Trek", 25000);

        // ---- account path: exactly one invoice, carrying the account's GSTIN.
        UUID leadId = createLead(sales, agentId, "Account Booker", "+919700000066");
        confirm(sales, createBooking(sales, leadId, tripId, 1)).andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/{id}/invoices", agentId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ISSUED"))
                .andExpect(jsonPath("$[0].billingGstin").value("33AAACC1206D1ZN"));
        assertThat(payables(manager, agentId)).hasSize(1);

        // ---- retail path: same trip, same price, no account anywhere near it.
        UUID customerId = newCustomer("+919700000067");
        UUID retailId = createRetailBooking(sales, customerId, tripId);
        confirm(sales, retailId).andExpect(status().isOk());

        // The account's financial views are untouched by the retail booking...
        mockMvc.perform(get("/api/accounts/{id}/invoices", agentId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(jsonPath("$.length()").value(1));
        assertThat(payables(manager, agentId)).hasSize(1);

        // ...and the retail booking itself owns no financial records.
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from invoices where booking_id = ?", Integer.class, retailId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from account_commission_payables where booking_id = ?",
                Integer.class, retailId)).isZero();

        // Its account_id really is null, so this is policy and not coincidence:
        // the invoicing branch is guarded on the resolved account.
        assertThat(jdbcTemplate.queryForObject(
                "select account_id is null from bookings where id = ?", Boolean.class, retailId)).isTrue();
    }

    // ------------------------------------------------------------------ helpers

    /** The only payable whose net matches, which is how payables get matched. */
    private JsonNode payableWithNet(JsonNode payables, double net) {
        for (JsonNode row : payables) {
            if (row.get("netAmount").asDouble() == net) {
                return row;
            }
        }
        throw new AssertionError("no payable with net " + net + " in " + payables);
    }

    private UUID createPercentPlan(String token, String key, int rate) throws Exception {
        String resp = mockMvc.perform(post("/api/commission-plans")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "key", key, "label", "Plan " + key,
                                "basis", "NET", "method", "PERCENT", "ratePercent", rate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private org.springframework.test.web.servlet.ResultActions assign(
            String token, UUID accountId, UUID planId) throws Exception {
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

    private UUID createTrip(String token, String name, int baseCost) throws Exception {
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

    private UUID createLead(String token, UUID accountId, String name, String mobile) throws Exception {
        return createLead(token, name, mobile, accountId.toString());
    }

    private UUID createLead(String token, String name, String mobile) throws Exception {
        return createLead(token, name, mobile, null);
    }

    private UUID createLead(String token, String name, String mobile, String accountId) throws Exception {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("customerName", name);
        fields.put("mobileNumber", mobile);
        fields.put("source", "WEBSITE");
        fields.put("consentGiven", true);
        fields.put("consentScope", "ALL");
        if (accountId != null) {
            fields.put("accountId", accountId);
        }
        String resp = mockMvc.perform(post("/api/leads")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(fields)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createOpportunity(String token, String leadName, String mobile, String stageKey, int value)
            throws Exception {
        return createOpportunity(token, createLead(token, leadName, mobile), stageKey, value);
    }

    private UUID createOpportunity(String token, UUID leadId, String stageKey, int value) throws Exception {
        String resp = mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "leadId", leadId.toString(), "stageKey", stageKey,
                                "expectedValue", value, "expectedDate", "2026-11-30"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createBooking(String token, UUID leadId, UUID tripId, int travellers) throws Exception {
        String resp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "leadId", leadId.toString(), "tripId", tripId.toString(),
                                "travelDate", "2026-11-20", "numTravellers", travellers))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createLeadBooking(String token, UUID accountId, String name, String mobile,
                                   UUID tripId, BigDecimal discount) throws Exception {
        UUID leadId = createLead(token, accountId, name, mobile);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("leadId", leadId.toString());
        fields.put("tripId", tripId.toString());
        fields.put("travelDate", "2026-11-20");
        fields.put("numTravellers", 2);
        if (discount != null) {
            fields.put("discountAmount", discount);
        }
        String resp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(fields)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    /** A retail booking: a customer, a trip, a date and a party. No lead, no account. */
    private UUID createRetailBooking(String token, UUID customerId, UUID tripId) throws Exception {
        String resp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "customerId", customerId.toString(), "tripId", tripId.toString(),
                                "travelDate", "2026-12-20", "numTravellers", 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID newCustomer(String phone) {
        Customer360 customer = Customer360.fromLead("Walk-in Retail Customer", phone, phone, phone,
                "retail" + phone.substring(phone.length() - 4) + "@example.com", true,
                "retail enquiry");
        return customerRepository.save(customer).getId();
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String token, UUID bookingId)
            throws Exception {
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
        createUser("mgr.gate@securetravels.in", "Mgr Gate", Role.MANAGER, "manager123");
        return login("mgr.gate@securetravels.in", "manager123");
    }

    private String salesToken() throws Exception {
        createUser("sales.gate@securetravels.in", "Sales Gate", Role.SALES, "sales123");
        return login("sales.gate@securetravels.in", "sales123");
    }

    private String otherSalesToken() throws Exception {
        createUser("sales.other@securetravels.in", "Sales Other", Role.SALES, "sales123");
        return login("sales.other@securetravels.in", "sales123");
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode parse(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}