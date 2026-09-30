package com.securetravels.crm.phase7;

import com.securetravels.crm.BaseIT;
import com.securetravels.crm.user.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 Module 1 — account lifecycle (flag default OFF): CRUD + RBAC,
 * GSTIN validation, lead/booking linking, the account invoice on confirm
 * and its void on cancel, and the account 360 detail.
 */
class AccountFlowIT extends BaseIT {

    @Test
    void accountLifecycleFromCreateThroughCancel() throws Exception {
        String manager = managerToken();
        String sales = salesToken();

        // ------------------------------------------------------------ create
        String corpResp = performCreateAccount(manager, "CORPORATE", "3299 Acme Travels Pvt Ltd",
                "27AAPFU0939F1ZV", "Acme Holidays Pvt Ltd")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID corpId = UUID.fromString(parse(corpResp).get("id").asText());
        // GSTIN is normalized to uppercase and the checksum accepted.
        assertThat(parse(corpResp).get("gstin").asText()).isEqualTo("27AAPFU0939F1ZV");

        // -------------------------------------------------- validation + RBAC
        performCreateAccount(manager, "CORPORATE", "Duplicate GSTIN", "27AAPFU0939F1ZV", null)
                .andExpect(status().isConflict());

        performCreateAccount(manager, "CORPORATE", "Bad checksum", "27AAPFU0939F1ZW", null)
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("accountType", "TRAVEL_AGENT", "name", "Sales-made Account"))))
                .andExpect(status().isForbidden());

        // ------------------------------------------------- reads (any authn)
        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", authHeader(sales)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("3299 Acme Travels Pvt Ltd"));

        mockMvc.perform(get("/api/accounts/{id}", corpId)
                        .header("Authorization", authHeader(sales)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gstin").value("27AAPFU0939F1ZV"));

        // --------------------------------------------- 360 is manager-gated
        mockMvc.perform(get("/api/accounts/{id}/detail", corpId)
                        .header("Authorization", authHeader(sales)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/accounts/{id}/detail", corpId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.leads").value(0));

        // ------------------------------------------- lead + booking linking
        UUID leadId = createLead(sales, corpId, "Ananya Desai", "+919812345678");
        mockMvc.perform(get("/api/leads/{id}", leadId)
                        .header("Authorization", authHeader(sales)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(corpId.toString()));

        UUID tripId = createCustomFitTrip(manager, "Corporate Offsite 2026", 12000);
        String bookingResp = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "leadId", leadId.toString(),
                                "tripId", tripId.toString(),
                                "travelDate", "2026-11-20",
                                "numTravellers", 2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID bookingId = UUID.fromString(parse(bookingResp).get("id").asText());
        // The booking inherits the lead's account (none passed explicitly).
        assertThat(parse(bookingResp).get("accountId").asText()).isEqualTo(corpId.toString());

        // ------------------------------------------- confirm -> invoice, no payable
        mockMvc.perform(patch("/api/bookings/{id}/status", bookingId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());

        String invResp = mockMvc.perform(get("/api/accounts/{id}/invoices", corpId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(parse(invResp)).hasSize(1);
        assertThat(parse(invResp).get(0).get("status").asText()).isEqualTo("ISSUED");
        // Billed to the account's billing identity, invoice ref shaped INV-YYYY-####.
        assertThat(parse(invResp).get(0).get("billingName").asText()).isEqualTo("Acme Holidays Pvt Ltd");
        assertThat(parse(invResp).get(0).get("invoiceRef").asText()).matches("INV-\\d{4}-\\d{4}");

        // Flag OFF: no commission payable is accrued for travel agents or anyone.
        mockMvc.perform(get("/api/accounts/{id}/payables", corpId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Booking response carries the account.
        mockMvc.perform(get("/api/bookings/{id}", bookingId)
                        .header("Authorization", authHeader(sales)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(corpId.toString()));

        // ------------------------------------------------------ 360 after the chain
        mockMvc.perform(get("/api/accounts/{id}/detail", corpId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.leads").value(1))
                .andExpect(jsonPath("$.stats.bookings").value(1))
                .andExpect(jsonPath("$.stats.confirmedBookings").value(1));

        // --------------------------------------------------- cancel -> void everything
        mockMvc.perform(patch("/api/bookings/{id}/status", bookingId)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());

        String voided = mockMvc.perform(get("/api/accounts/{id}/invoices", corpId)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(parse(voided).get(0).get("status").asText()).isEqualTo("VOID");

        // Deactivate via PATCH, then a new lead link must be refused.
        mockMvc.perform(patch("/api/accounts/{id}", corpId)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/leads")
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "customerName", "Refused Person", "mobileNumber", "+919800000000",
                                "source", "WEBSITE", "consentGiven", true,
                                "accountId", corpId.toString()))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions performCreateAccount(String token, String type, String name, String gstin, String billingName)
            throws Exception {
        return mockMvc.perform(post("/api/accounts")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "accountType", type, "name", name,
                                "gstin", gstin == null ? "" : gstin,
                                "billingName", billingName == null ? "" : billingName))));
    }

    private UUID createLead(String token, UUID accountId, String name, String mobile) throws Exception {
        String resp = mockMvc.perform(post("/api/leads")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "customerName", name, "mobileNumber", mobile,
                                "source", "WEBSITE", "destination", "Rishikesh",
                                "consentGiven", true, "consentScope", "ALL",
                                "accountId", accountId.toString()))))
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
                                "durationDays", 3, "difficulty", "MODERATE"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(parse(resp).get("id").asText());
    }

    private String managerToken() throws Exception {
        createUser("mgr.account@securetravels.in", "Mgr Accounts", Role.MANAGER, "manager123");
        return login("mgr.account@securetravels.in", "manager123");
    }

    private String salesToken() throws Exception {
        createUser("sales.account@securetravels.in", "Sales Accounts", Role.SALES, "sales123");
        return login("sales.account@securetravels.in", "sales123");
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode parse(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}