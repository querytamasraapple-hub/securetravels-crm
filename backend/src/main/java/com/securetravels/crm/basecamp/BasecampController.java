package com.securetravels.crm.basecamp;

import com.securetravels.crm.common.security.CurrentUser;
import com.securetravels.crm.user.UserPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Basecamp OS admin modules — HR, inventory, visitors, safe company settings.
 * All mutations are role-gated; secrets are never accepted via settings.
 */
@RestController
@RequestMapping("/api/basecamp")
@SecurityRequirement(name = "bearerAuth")
public class BasecampController {

    private final BasecampService basecamp;

    public BasecampController(BasecampService basecamp) {
        this.basecamp = basecamp;
    }

    @GetMapping(value = "/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SALES','OPS','MANAGER','ADMIN','CEO')")
    public List<Map<String, Object>> attendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser UserPrincipal caller) {
        return basecamp.listAttendance(caller, from, to);
    }

    @PostMapping(value = "/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SALES','OPS','MANAGER','ADMIN','CEO')")
    public Map<String, Object> upsertAttendance(@RequestBody Map<String, Object> body,
                                                @CurrentUser UserPrincipal caller) {
        UUID userId = body.get("userId") != null ? UUID.fromString(body.get("userId").toString()) : null;
        LocalDate date = body.get("workDate") != null ? LocalDate.parse(body.get("workDate").toString()) : null;
        StaffAttendance.Status status = body.get("status") != null
                ? StaffAttendance.Status.valueOf(body.get("status").toString()) : null;
        return basecamp.upsertAttendance(caller, userId, date, status,
                body.get("notes") != null ? body.get("notes").toString() : null,
                Boolean.TRUE.equals(body.get("checkIn")),
                Boolean.TRUE.equals(body.get("checkOut")));
    }

    @GetMapping(value = "/leave", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SALES','OPS','MANAGER','ADMIN','CEO')")
    public List<Map<String, Object>> leave(@CurrentUser UserPrincipal caller) {
        return basecamp.listLeave(caller);
    }

    @PostMapping(value = "/leave", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SALES','OPS','MANAGER','ADMIN','CEO')")
    public Map<String, Object> createLeave(@RequestBody Map<String, Object> body,
                                           @CurrentUser UserPrincipal caller) {
        return basecamp.createLeave(caller,
                StaffLeaveRequest.LeaveType.valueOf(body.get("leaveType").toString()),
                LocalDate.parse(body.get("fromDate").toString()),
                LocalDate.parse(body.get("toDate").toString()),
                body.get("reason") != null ? body.get("reason").toString() : null);
    }

    @PostMapping(value = "/leave/{id}/review", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','CEO')")
    public Map<String, Object> reviewLeave(@PathVariable UUID id, @RequestBody Map<String, String> body,
                                           @CurrentUser UserPrincipal caller) {
        return basecamp.reviewLeave(id, StaffLeaveRequest.Status.valueOf(body.get("status")),
                body.get("note"), caller);
    }

    @GetMapping(value = "/payroll", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','CEO','FINANCE')")
    public List<Map<String, Object>> payroll(@RequestParam(required = false) String month,
                                             @CurrentUser UserPrincipal caller) {
        return basecamp.listPayroll(caller, month);
    }

    @PostMapping(value = "/payroll", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','CEO')")
    public Map<String, Object> upsertPayroll(@RequestBody Map<String, Object> body,
                                             @CurrentUser UserPrincipal caller) {
        return basecamp.upsertPayroll(caller,
                UUID.fromString(body.get("userId").toString()),
                body.get("periodMonth").toString(),
                num(body.get("basicPay")), num(body.get("allowances")), num(body.get("deductions")),
                body.get("status") != null ? StaffPayroll.Status.valueOf(body.get("status").toString()) : null,
                body.get("notes") != null ? body.get("notes").toString() : null);
    }

    @GetMapping(value = "/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OPS','MANAGER','ADMIN','CEO')")
    public List<Map<String, Object>> inventory(@CurrentUser UserPrincipal caller) {
        return basecamp.listInventory(caller);
    }

    @PostMapping(value = "/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPS','MANAGER','ADMIN','CEO')")
    public Map<String, Object> createInventory(@RequestBody Map<String, Object> body,
                                               @CurrentUser UserPrincipal caller) {
        return basecamp.createInventory(caller,
                body.get("sku").toString(), body.get("name").toString(),
                body.get("category") != null ? InventoryItem.Category.valueOf(body.get("category").toString()) : null,
                num(body.get("quantity")),
                body.get("unit") != null ? body.get("unit").toString() : null,
                num(body.get("reorderLevel")),
                body.get("location") != null ? body.get("location").toString() : null,
                body.get("notes") != null ? body.get("notes").toString() : null);
    }

    @PostMapping(value = "/inventory/{id}/quantity", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OPS','MANAGER','ADMIN','CEO')")
    public Map<String, Object> adjustInventory(@PathVariable UUID id, @RequestBody Map<String, Object> body,
                                               @CurrentUser UserPrincipal caller) {
        return basecamp.adjustInventory(id, num(body.get("quantity")), caller);
    }

    @GetMapping(value = "/visitors", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OPS','MANAGER','ADMIN','CEO')")
    public List<Map<String, Object>> visitors(@CurrentUser UserPrincipal caller) {
        return basecamp.listVisitors(caller);
    }

    @PostMapping(value = "/visitors", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPS','MANAGER','ADMIN','CEO')")
    public Map<String, Object> checkIn(@RequestBody Map<String, Object> body,
                                       @CurrentUser UserPrincipal caller) {
        UUID host = body.get("hostUserId") != null ? UUID.fromString(body.get("hostUserId").toString()) : null;
        return basecamp.checkInVisitor(caller,
                body.get("visitorName").toString(),
                body.get("phone") != null ? body.get("phone").toString() : null,
                body.get("purpose").toString(), host,
                body.get("notes") != null ? body.get("notes").toString() : null);
    }

    @PostMapping(value = "/visitors/{id}/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OPS','MANAGER','ADMIN','CEO')")
    public Map<String, Object> checkOut(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return basecamp.checkOutVisitor(id, caller);
    }

    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SALES','OPS','MANAGER','ADMIN','CEO')")
    public Map<String, String> settings(@CurrentUser UserPrincipal caller) {
        return basecamp.getSettings(caller);
    }

    @PostMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','CEO')")
    public Map<String, String> updateSetting(@RequestBody Map<String, String> body,
                                             @CurrentUser UserPrincipal caller) {
        return basecamp.updateSetting(caller, body.get("key"), body.get("value"));
    }

    private static BigDecimal num(Object v) {
        if (v == null) return null;
        return new BigDecimal(v.toString());
    }
}
