package com.securetravels.crm.phase7;

import com.securetravels.crm.BaseIT;
import com.securetravels.crm.user.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 Module 3 — opportunities: creation from a lead, one-per-lead,
 * ownership scoping (SALES own-only, MANAGER sees all), stage moves,
 * terminal WON/LOST closure, and the live expected/best/won forecast with
 * stage + month bucketing.
 */
class OpportunityFlowIT extends BaseIT {

    @Test
    void createMoveCloseAndForecastMath() throws Exception {
        String manager = managerToken();
        String salesA = salesToken("a");

        UUID leadA = createLead(salesA, "Alpha Buyer", "9810000001");
        UUID leadB = createLead(salesA, "Beta Buyer", "9810000002");

        String created = performCreateOpportunity(salesA, Map.of(
                "leadId", leadA.toString(), "expectedValue", 20000,
                "expectedDate", "2026-10-15"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stageKey").value("QUALIFIED"))
                .andReturn().getResponse().getContentAsString();
        UUID oppA = UUID.fromString(parse(created).get("id").asText());

        String created2 = performCreateOpportunity(salesA, Map.of(
                "leadId", leadB.toString(), "expectedValue", 50000,
                "stageKey", "NEGOTIATION", "expectedDate", "2026-11-05"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID oppB = UUID.fromString(parse(created2).get("id").asText());

        // Move oppB back a stage: any active stage is eligible.
        String moved = performMove(salesA, oppB, "QUOTATION_SENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probabilityWeight").value(40.0))
                .andReturn().getResponse().getContentAsString();
        assertThat(parse(moved).get("status").asText()).isEqualTo("OPEN");

        // expected = 20000*20% + 50000*40% = 24000; best = 70000; won = 0.
        JsonNode forecast = parse(performForecast(manager, "2026-10-01", "2026-12-01"));
        assertThat(forecast.get("expected").asDouble()).isEqualTo(24000.0);
        assertThat(forecast.get("best").asDouble()).isEqualTo(70000.0);
        assertThat(forecast.get("won").asDouble()).isEqualTo(0.0);
        assertThat(forecast.get("openCount").asLong()).isEqualTo(2L);

        // Close oppA as WON (owner may close) -> won 20000, expected 24000-4000 = 20000.
        performClose(salesA, oppA, "WON", "signed")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        JsonNode afterClose = parse(performForecast(manager, "2026-10-01", "2026-12-01"));
        assertThat(afterClose.get("won").asDouble()).isEqualTo(20000.0);
        assertThat(afterClose.get("expected").asDouble()).isEqualTo(20000.0);
        assertThat(afterClose.get("openCount").asLong()).isEqualTo(1L);

        // A sales user's forecast is scoped to their own pipeline.
        JsonNode salesView = parse(performForecast(salesA, "2026-10-01", "2026-12-01"));
        assertThat(salesView.get("won").asDouble()).isEqualTo(20000.0);
    }

    @Test
    void salesCannotSeeOrActOnOthersOpportunities() throws Exception {
        String salesA = salesToken("a");
        String salesB = salesToken("b");
        String manager = managerToken();

        UUID leadB = createLead(salesB, "Beta Buyer", "9810000010");
        String oppB = performCreateOpportunity(salesB, Map.of(
                "leadId", leadB.toString(), "expectedValue", 90000,
                "expectedDate", "2026-10-30"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID oppBId = UUID.fromString(parse(oppB).get("id").asText());

        mockMvc.perform(get("/api/opportunities").header("Authorization", authHeader(salesA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/opportunities/{id}", oppBId)
                        .header("Authorization", authHeader(salesA)))
                .andExpect(status().isForbidden());
        performMove(salesA, oppBId, "NEGOTIATION").andExpect(status().isForbidden());
        performClose(salesA, oppBId, "WON", null).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/opportunities").header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Manager can close the sales B opportunity.
        performClose(manager, oppBId, "WON", "signed by manager")
                .andExpect(status().isOk());
    }

    @Test
    void oneOpportunityPerLeadAndLostLeadRejected() throws Exception {
        String salesA = salesToken("a");
        UUID lead = createLead(salesA, "Gamma Buyer", "9810000020");

        performCreateOpportunity(salesA, Map.of(
                "leadId", lead.toString(), "expectedValue", 10000, "expectedDate", "2026-10-15"))
                .andExpect(status().isCreated());
        performCreateOpportunity(salesA, Map.of(
                "leadId", lead.toString(), "expectedValue", 20000, "expectedDate", "2026-10-15"))
                .andExpect(status().isConflict());

        // Lost lead -> no opportunity.
        UUID lead2 = createLead(salesA, "Delta Buyer", "9810000021");
        mockMvc.perform(patch("/api/leads/{id}/status", lead2)
                        .header("Authorization", authHeader(salesA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("status", "LOST", "lostReason", "PRICE_TOO_HIGH"))))
                .andExpect(status().isOk());
        performCreateOpportunity(salesA, Map.of(
                "leadId", lead2.toString(), "expectedValue", 10000, "expectedDate", "2026-10-15"))
                .andExpect(status().isConflict());

        // OPS cannot create opportunities.
        String ops = opsToken();
        UUID lead3 = createLead(salesA, "Epsilon Buyer", "9810000022");
        performCreateOpportunity(ops, Map.of(
                "leadId", lead3.toString(), "expectedValue", 10000, "expectedDate", "2026-10-15"))
                .andExpect(status().isForbidden());
    }

    @Test
    void stageTransitionAndForecastWindowValidation() throws Exception {
        String manager = managerToken();
        String salesA = salesToken("a");
        UUID lead = createLead(salesA, "Zeta Buyer", "9810000030");

        String created = performCreateOpportunity(salesA, Map.of(
                "leadId", lead.toString(), "expectedValue", 10000, "expectedDate", "2026-10-15"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID opp = UUID.fromString(parse(created).get("id").asText());

        // Terminal states are irreversible.
        performClose(salesA, opp, "LOST", "pricing").andExpect(status().isOk());
        performMove(salesA, opp, "NEGOTIATION").andExpect(status().isConflict());
        performClose(salesA, opp, "WON", null).andExpect(status().isConflict());

        // Unknown stage -> 404; inactive stage -> 409; invalid window -> 400.
        UUID lead2 = createLead(salesA, "Eta Buyer", "9810000031");
        String opp2Resp = performCreateOpportunity(salesA, Map.of(
                "leadId", lead2.toString(), "expectedValue", 10000, "expectedDate", "2026-10-15"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID opp2 = UUID.fromString(parse(opp2Resp).get("id").asText());
        performMove(salesA, opp2, "GARBAGE").andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/pipeline-stages/{id}", stageId(manager, "NEGOTIATION"))
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("active", false))))
                .andExpect(status().isOk());
        performMove(salesA, opp2, "NEGOTIATION").andExpect(status().isConflict());

        mockMvc.perform(get("/api/forecast")
                        .header("Authorization", authHeader(manager))
                        .param("from", "2026-12-01").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forecastGroupsByStageAndMonth() throws Exception {
        String manager = managerToken();
        String salesA = salesToken("a");

        UUID lead1 = createLead(salesA, "Theta Buyer", "9810000040");
        UUID lead2 = createLead(salesA, "Iota Buyer", "9810000041");
        performCreateOpportunity(salesA, Map.of(
                "leadId", lead1.toString(), "expectedValue", 20000,
                "stageKey", "QUOTATION_SENT", "expectedDate", "2026-10-15"))
                .andExpect(status().isCreated());
        performCreateOpportunity(salesA, Map.of(
                "leadId", lead2.toString(), "expectedValue", 50000,
                "stageKey", "NEGOTIATION", "expectedDate", "2026-10-20"))
                .andExpect(status().isCreated());
        // A won deal in a different month.
        UUID lead3 = createLead(salesA, "Kappa Buyer", "9810000042");
        String opp3Resp = performCreateOpportunity(salesA, Map.of(
                "leadId", lead3.toString(), "expectedValue", 8000,
                "expectedDate", "2026-11-01"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID opp3 = UUID.fromString(parse(opp3Resp).get("id").asText());
        performClose(salesA, opp3, "WON", "paid").andExpect(status().isOk());

        JsonNode forecast = parse(performForecast(manager, "2026-10-01", "2026-12-01"));
        // expected = 20000*40% + 50000*70% = 8000 + 35000 = 43000; best = 70000; won = 8000.
        assertThat(forecast.get("expected").asDouble()).isEqualTo(43000.0);
        assertThat(forecast.get("best").asDouble()).isEqualTo(70000.0);
        assertThat(forecast.get("won").asDouble()).isEqualTo(8000.0);

        JsonNode byStage = forecast.get("byStage");
        assertThat(byStage).hasSize(2);
        JsonNode quoted = findStage(byStage, "QUOTATION_SENT");
        assertThat(quoted.get("count").asLong()).isEqualTo(1L);
        assertThat(quoted.get("expected").asDouble()).isEqualTo(8000.0);
        assertThat(quoted.get("best").asDouble()).isEqualTo(20000.0);
        JsonNode neg = findStage(byStage, "NEGOTIATION");
        assertThat(neg.get("expected").asDouble()).isEqualTo(35000.0);

        JsonNode byMonth = forecast.get("byMonth");
        assertThat(byMonth).hasSize(2);
        JsonNode oct = findMonth(byMonth, "2026-10");
        assertThat(oct.get("expected").asDouble()).isEqualTo(43000.0);
        assertThat(oct.get("best").asDouble()).isEqualTo(70000.0);
        assertThat(oct.get("won").asDouble()).isEqualTo(0.0);
        JsonNode nov = findMonth(byMonth, "2026-11");
        assertThat(nov.get("won").asDouble()).isEqualTo(8000.0);
        assertThat(nov.get("expected").asDouble()).isEqualTo(0.0);
    }

    // ------------------------------------------------------------------ helpers

    private UUID createLead(String token, String name, String mobile) throws Exception {
        String resp = mockMvc.perform(post("/api/leads")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "customerName", name, "mobileNumber", mobile,
                                "source", "WEBSITE", "destination", "Rishikesh",
                                "consentGiven", true, "consentScope", "ALL"))))
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

    private org.springframework.test.web.servlet.ResultActions performCreateOpportunity(
            String token, Map<String, Object> fields) throws Exception {
        return mockMvc.perform(post("/api/opportunities")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(fields)));
    }

    private org.springframework.test.web.servlet.ResultActions performMove(String token, UUID id, String stageKey)
            throws Exception {
        return mockMvc.perform(patch("/api/opportunities/{id}/stage", id)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("stageKey", stageKey))));
    }

    private org.springframework.test.web.servlet.ResultActions performClose(String token, UUID id,
                                                                             String outcome, String note)
            throws Exception {
        Map<String, Object> fields = note == null ? Map.of("outcome", outcome) : Map.of("outcome", outcome, "note", note);
        return mockMvc.perform(post("/api/opportunities/{id}/close", id)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(fields)));
    }

    private String performForecast(String token, String from, String to) throws Exception {
        return mockMvc.perform(get("/api/forecast")
                        .header("Authorization", authHeader(token))
                        .param("from", from).param("to", to))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static JsonNode findStage(JsonNode array, String key) {
        for (JsonNode node : array) {
            if (key.equals(node.get("stageKey").asText())) return node;
        }
        throw new AssertionError("stage bucket not found: " + key);
    }

    private static JsonNode findMonth(JsonNode array, String month) {
        for (JsonNode node : array) {
            if (month.equals(node.get("month").asText())) return node;
        }
        throw new AssertionError("month bucket not found: " + month);
    }

    private String managerToken() throws Exception {
        createUser("mgr.opp@securetravels.in", "Mgr Opp", Role.MANAGER, "manager123");
        return login("mgr.opp@securetravels.in", "manager123");
    }

    private String salesToken(String suffix) throws Exception {
        String email = "sales.opp" + suffix + "@securetravels.in";
        createUser(email, "Sales Opp " + suffix, Role.SALES, "sales123");
        return login(email, "sales123");
    }

    private String opsToken() throws Exception {
        createUser("ops.opp@securetravels.in", "Ops Opp", Role.OPS, "ops123");
        return login("ops.opp@securetravels.in", "ops123");
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode parse(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}
