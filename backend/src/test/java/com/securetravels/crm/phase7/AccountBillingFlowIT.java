package com.securetravels.crm.phase7;

import com.securetravels.crm.BaseIT;
import com.securetravels.crm.user.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 Module 1 — commission payables (flag ON). A travel-agent booking
 * accrues an OPEN payable at the configured flat rate on confirm, plus its
 * invoice; a second booking that is cancelled has its payable VOIDed, and a
 * manager can settle an OPEN payable.
 */
@TestPropertySource(properties = "app.feature-flags.partner-commissions=true")
class AccountBillingFlowIT extends BaseIT {

    @Test
    void travelAgentBookingAccruesAndSettlesCommission() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        UUID agentId = createTravelAgent(manager, "Wanderlust Travels", "33AAACC1206D1ZN");
        UUID tripId = createCustomFitTrip(manager, "Ladakh Trails", 20000);

        // ------------------------------------------------- booking 1: kept, paid
        UUID lead1 = createLead(sales, agentId, "Booked Traveller", "+919700000001");
        UUID booking1 = createBooking(sales, lead1, tripId, 1);   // net 20000

        mockMvc.perform(patch("/api/bookings/{id}/status", booking1)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());

        JsonNode payables = payables(manager, agentId);
        assertThat(payables).hasSize(1);
        JsonNode payable = payables.get(0);
        assertThat(payable.get("status").asText()).isEqualTo("OPEN");
        assertThat(payable.get("ratePercent").asDouble()).isEqualTo(10.0);
        assertThat(payable.get("netAmount").asDouble()).isEqualTo(20000.0);
        // 10% of net, rounded HALF_UP to 2dp.
        assertThat(payable.get("commissionAmount").asDouble()).isEqualTo(2000.0);

        mockMvc.perform(get("/api/accounts/{id}/invoices", agentId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ISSUED"))
                .andExpect(jsonPath("$[0].billingGstin").value("33AAACC1206D1ZN"));

        // Corporate accounts are never commissionable — only travel agents.
        UUID corpId = createCorporate(manager, "BigCorp India");
        UUID lead3 = createLead(sales, corpId, "Corporate Traveller", "+919700000003");
        UUID booking3 = createBooking(sales, lead3, tripId, 1);
        mockMvc.perform(patch("/api/bookings/{id}/status", booking3)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());
        assertThat(payables(manager, corpId)).isEmpty();

        // ----------------------------------------------------- settle booking 1
        UUID payableId = UUID.fromString(payable.get("id").asText());
        mockMvc.perform(post("/api/accounts/{id}/payables/{payableId}/settle", agentId, payableId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paidRef\":\"UTR-2026-991122\"}"))
                .andExpect(status().isOk());

        JsonNode paid = payables(manager, agentId).get(0);
        assertThat(paid.get("status").asText()).isEqualTo("PAID");
        assertThat(paid.get("paidRef").asText()).isEqualTo("UTR-2026-991122");

        // ---------------------------------------------- booking 2: confirm, cancel -> VOID
        UUID lead2 = createLead(sales, agentId, "Cancelled Traveller", "+919700000002");
        UUID booking2 = createBooking(sales, lead2, tripId, 1);
        mockMvc.perform(patch("/api/bookings/{id}/status", booking2)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());
        assertThat(payables(manager, agentId)).hasSize(2);

        mockMvc.perform(patch("/api/bookings/{id}/status", booking2)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        JsonNode afterCancel = payables(manager, agentId);
        assertThat(afterCancel).hasSize(2);
        // Order is payableAt desc, so the cancelled (later) booking is first.
        assertThat(afterCancel.get(0).get("status").asText()).isEqualTo("VOID");
        assertThat(afterCancel.get(1).get("status").asText()).isEqualTo("PAID");

        // The 360 aggregates match: one recognised booking kept, one payable paid.
        mockMvc.perform(get("/api/accounts/{id}/detail", agentId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.paidCommission").value(2000.0))
                .andExpect(jsonPath("$.stats.openCommission").value(0.0));
    }

    // ------------------------------------------------------------------ helpers

    private UUID createTravelAgent(String token, String name, String gstin) throws Exception {
        String resp = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "accountType", "TRAVEL_AGENT", "name", name,
                                "gstin", gstin, "billingName", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private UUID createCorporate(String token, String name) throws Exception {
        String resp = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("accountType", "CORPORATE", "name", name))))
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
        String resp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "leadId", leadId.toString(), "tripId", tripId.toString(),
                                "travelDate", "2026-10-15", "numTravellers", travellers))))
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
                                "durationDays", 5, "difficulty", "MODERATE"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private JsonNode payables(String token, UUID accountId) throws Exception {
        String resp = mockMvc.perform(get("/api/accounts/{id}/payables", accountId)
                        .header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return parse(resp);
    }

    private String managerToken() throws Exception {
        createUser("mgr.billing@securetravels.in", "Mgr Billing", Role.MANAGER, "manager123");
        return login("mgr.billing@securetravels.in", "manager123");
    }

    private String salesToken() throws Exception {
        createUser("sales.billing@securetravels.in", "Sales Billing", Role.SALES, "sales123");
        return login("sales.billing@securetravels.in", "sales123");
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode parse(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}