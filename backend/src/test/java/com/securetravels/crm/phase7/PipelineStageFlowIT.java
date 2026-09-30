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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 Module 2 — configurable pipeline stages: seeded defaults, ordered
 * listing, validation (weights, key format, duplicate key/order), the
 * last-active-step guard, deactivation, and manager-only administration.
 * BaseIT restores the V20 seed rows before each test.
 */
class PipelineStageFlowIT extends BaseIT {

    @Test
    void seededDefaultsListedInOrder() throws Exception {
        String manager = managerToken();

        String list = mockMvc.perform(get("/api/pipeline-stages")
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].key").value("QUALIFIED"))
                .andExpect(jsonPath("$[1].key").value("QUOTATION_SENT"))
                .andExpect(jsonPath("$[2].key").value("NEGOTIATION"))
                .andReturn().getResponse().getContentAsString();

        JsonNode rows = parse(list);
        assertThat(rows.get(0).get("probabilityWeight").asDouble()).isEqualTo(20.0);
        assertThat(rows.get(1).get("probabilityWeight").asDouble()).isEqualTo(40.0);
        assertThat(rows.get(2).get("probabilityWeight").asDouble()).isEqualTo(70.0);
        assertThat(rows).allMatch(n -> n.get("active").asBoolean());
    }

    @Test
    void createInsertsStageIntoOrderedListAndCanDelete() throws Exception {
        String manager = managerToken();

        ResultActions created = performCreateStage(manager, Map.of(
                "key", "DEAL_STRUCTURING",
                "label", "Deal Structuring",
                "sortOrder", 15,
                "probabilityWeight", 55,
                "entryCondition", "Quotation accepted subject to structuring"));
        created.andExpect(status().isCreated());
        String resp = created.andReturn().getResponse().getContentAsString();
        assertThat(parse(resp).get("key").asText()).isEqualTo("DEAL_STRUCTURING");
        UUID id = UUID.fromString(parse(resp).get("id").asText());

        mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[1].key").value("DEAL_STRUCTURING"));

        mockMvc.perform(delete("/api/pipeline-stages/{id}", id)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void validationRejectsBadWeightDuplicateKeyAndDuplicateOrder() throws Exception {
        String manager = managerToken();

        performCreateStage(manager, Map.of(
                "key", "TOO_LIKELY", "label", "Too likely", "sortOrder", 40, "probabilityWeight", 150))
                .andExpect(status().isBadRequest());

        performCreateStage(manager, Map.of(
                "key", "qualified", "label", "Duplicate", "sortOrder", 40, "probabilityWeight", 10))
                .andExpect(status().isBadRequest());

        performCreateStage(manager, Map.of(
                "key", "QUALIFIED", "label", "Duplicate key", "sortOrder", 40, "probabilityWeight", 10))
                .andExpect(status().isConflict());

        performCreateStage(manager, Map.of(
                "key", "SAME_SLOT", "label", "Same slot", "sortOrder", 10, "probabilityWeight", 10))
                .andExpect(status().isConflict());

        performCreateStage(manager, Map.of(
                "key", "NO_LABEL", "label", "", "sortOrder", 40, "probabilityWeight", 10))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReordersAndDeactivatesAndGuardsLastActive() throws Exception {
        String manager = managerToken();
        UUID quoted = stageId(manager, "QUOTATION_SENT");

        mockMvc.perform(patch("/api/pipeline-stages/{id}", quoted)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("sortOrder", 5, "probabilityWeight", 35))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(5))
                .andExpect(jsonPath("$.probabilityWeight").value(35.00));

        mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("QUOTATION_SENT"));

        UUID negotiation = stageId(manager, "NEGOTIATION");
        UUID qualified = stageId(manager, "QUALIFIED");

        deactivate(manager, negotiation);
        deactivate(manager, quoted);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pipeline_stages WHERE is_active", Long.class)).isEqualTo(1L);

        // Deactivating the last active stage is refused.
        mockMvc.perform(patch("/api/pipeline-stages/{id}", qualified)
                        .header("Authorization", authHeader(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("active", false))))
                .andExpect(status().isConflict());

        // Deleting it is refused too; deleting an inactive stage is fine.
        mockMvc.perform(delete("/api/pipeline-stages/{id}", qualified)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/pipeline-stages/{id}", negotiation)
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNoContent());
    }

    @Test
    void inactiveStageExcludedFromDefaultListButVisibleToManagers() throws Exception {
        String manager = managerToken();
        performCreateStage(manager, Map.of(
                "key", "WON", "label", "Won", "sortOrder", 99, "probabilityWeight", 100, "active", false))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/pipeline-stages?includeInactive=true")
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        String sales = salesToken();
        mockMvc.perform(get("/api/pipeline-stages?includeInactive=true")
                        .header("Authorization", authHeader(sales)))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesCanReadButNotManageStages() throws Exception {
        String sales = salesToken();

        mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(sales)))
                .andExpect(status().isOk());

        performCreateStage(sales, Map.of(
                "key", "SNEAKY", "label", "Sneaky", "sortOrder", 40, "probabilityWeight", 10))
                .andExpect(status().isForbidden());

        UUID qualified = stageId(sales, "QUALIFIED");
        mockMvc.perform(patch("/api/pipeline-stages/{id}", qualified)
                        .header("Authorization", authHeader(sales))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("label", "Hacked"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/pipeline-stages/{id}", qualified)
                        .header("Authorization", authHeader(sales)))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingStageIsNotFound() throws Exception {
        String manager = managerToken();
        mockMvc.perform(get("/api/pipeline-stages/{id}", UUID.randomUUID())
                        .header("Authorization", authHeader(manager)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ helpers

    private void deactivate(String token, UUID id) throws Exception {
        mockMvc.perform(patch("/api/pipeline-stages/{id}", id)
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("active", false))))
                .andExpect(status().isOk());
    }

    private UUID stageId(String token, String key) throws Exception {
        String list = mockMvc.perform(get("/api/pipeline-stages").header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode rows = parse(list);
        for (JsonNode row : rows) {
            if (key.equals(row.get("key").asText())) {
                return UUID.fromString(row.get("id").asText());
            }
        }
        throw new AssertionError("seeded stage not listed: " + key);
    }

    private ResultActions performCreateStage(String token, Map<String, Object> fields) throws Exception {
        return mockMvc.perform(post("/api/pipeline-stages")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(fields)));
    }

    private String managerToken() throws Exception {
        createUser("mgr.pipe@securetravels.in", "Mgr Pipeline", Role.MANAGER, "manager123");
        return login("mgr.pipe@securetravels.in", "manager123");
    }

    private String salesToken() throws Exception {
        createUser("sales.pipe@securetravels.in", "Sales Pipeline", Role.SALES, "sales123");
        return login("sales.pipe@securetravels.in", "sales123");
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode parse(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }
}