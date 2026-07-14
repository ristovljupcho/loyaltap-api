package com.loyaltap.employee.controller;

import com.loyaltap.employee.dto.EmployeeResponse;
import com.loyaltap.employee.dto.CreateEmployeeRequest;
import com.loyaltap.employee.dto.UpdateEmployeeRequest;
import com.loyaltap.employee.model.EmployeeRole;
import com.loyaltap.employee.model.EmployeeStatus;
import com.loyaltap.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void createsEmployee() throws Exception {
        UUID businessId = UUID.randomUUID();
        EmployeeResponse response = response(businessId, EmployeeStatus.ACTIVE);
        when(employeeService.createEmployee(
                businessId,
                new CreateEmployeeRequest("user-1", EmployeeRole.MANAGER)
        )).thenReturn(response);

        mockMvc.perform(post("/business/{businessId}/employees", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1","role":"MANAGER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessId").value(businessId.toString()))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsMissingCreateFields() throws Exception {
        mockMvc.perform(post("/business/{businessId}/employees", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userId").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    @Test
    void listsEmployees() throws Exception {
        UUID businessId = UUID.randomUUID();
        when(employeeService.listEmployees(businessId)).thenReturn(List.of(response(businessId, EmployeeStatus.ACTIVE)));

        mockMvc.perform(get("/business/{businessId}/employees", businessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user-1"));
    }

    @Test
    void getsEmployee() throws Exception {
        UUID businessId = UUID.randomUUID();
        EmployeeResponse response = response(businessId, EmployeeStatus.ACTIVE);
        when(employeeService.getEmployee(businessId, response.id())).thenReturn(response);

        mockMvc.perform(get("/business/{businessId}/employees/{employeeId}", businessId, response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void updatesEmployee() throws Exception {
        UUID businessId = UUID.randomUUID();
        EmployeeResponse response = response(businessId, EmployeeStatus.DISABLED);
        when(employeeService.updateEmployee(
                businessId,
                response.id(),
                new UpdateEmployeeRequest(EmployeeRole.EMPLOYEE, EmployeeStatus.DISABLED)
        )).thenReturn(response);

        mockMvc.perform(put("/business/{businessId}/employees/{employeeId}", businessId, response.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"EMPLOYEE","status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void rejectsMissingUpdateFields() throws Exception {
        mockMvc.perform(put("/business/{businessId}/employees/{employeeId}",
                        UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.role").exists())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void removesEmployee() throws Exception {
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(delete("/business/{businessId}/employees/{employeeId}", businessId, employeeId))
                .andExpect(status().isNoContent());
        verify(employeeService).removeEmployee(businessId, employeeId);
    }

    private EmployeeResponse response(UUID businessId, EmployeeStatus status) {
        return new EmployeeResponse(
                UUID.randomUUID(),
                businessId,
                "user-1",
                status == EmployeeStatus.DISABLED ? EmployeeRole.EMPLOYEE : EmployeeRole.MANAGER,
                status,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
