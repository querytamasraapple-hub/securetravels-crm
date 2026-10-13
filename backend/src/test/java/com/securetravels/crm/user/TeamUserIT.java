package com.securetravels.crm.user;

import com.securetravels.crm.BaseIT;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamUserIT extends BaseIT {

    @Test
    void salesCannotListTeam() throws Exception {
        createUser("sales@toh.in", "Ravi", Role.SALES, "sales12345");
        String token = login("sales@toh.in", "sales12345");

        mockMvc.perform(get("/api/users").header("Authorization", authHeader(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCreatesSalesAndCannotCreateAdmin() throws Exception {
        createUser("mgr@toh.in", "Manager", Role.MANAGER, "manager123");
        String token = login("mgr@toh.in", "manager123");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", authHeader(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "new.sales@toh.in",
                                "fullName", "New Sales",
                                "role", "SALES",
                                "password", "password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.sales@toh.in"))
                .andExpect(jsonPath("$.role").value("SALES"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", authHeader(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "sneaky@toh.in",
                                "fullName", "Sneaky Admin",
                                "role", "ADMIN",
                                "password", "password1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanChangeRoleAndDeactivate() throws Exception {
        createUser("admin@toh.in", "Admin", Role.ADMIN, "admin12345");
        UUID salesId = createUser("sales@toh.in", "Ravi", Role.SALES, "sales12345");
        String token = login("admin@toh.in", "admin12345");

        mockMvc.perform(patch("/api/users/" + salesId)
                        .header("Authorization", authHeader(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "role", "OPS",
                                "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OPS"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/users").header("Authorization", authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
