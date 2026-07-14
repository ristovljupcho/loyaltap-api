package com.loyaltap.stamprequest.controller;

import com.loyaltap.stamprequest.dto.CreateStampRequest;
import com.loyaltap.stamprequest.dto.EmployeeStampRequestAction;
import com.loyaltap.stamprequest.dto.StampRequestResponse;
import com.loyaltap.stamprequest.dto.UserStampRequestAction;
import com.loyaltap.stamprequest.model.StampRequestStatus;
import com.loyaltap.stamprequest.service.StampRequestService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StampRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class StampRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StampRequestService stampRequestService;

    @Test
    void createsRequest() throws Exception {
        UUID membershipId = UUID.randomUUID();
        StampRequestResponse response = response(membershipId, StampRequestStatus.PENDING);
        CreateStampRequest request = new CreateStampRequest(membershipId, "user-1", "key-1");
        when(stampRequestService.createRequest("tag-1", request)).thenReturn(response);

        mockMvc.perform(post("/nfc-tags/{tagCode}/stamp-requests", "tag-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"membershipId":"%s","userId":"user-1","idempotencyKey":"key-1"}
                                """.formatted(membershipId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").value(membershipId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsMissingCreateFields() throws Exception {
        mockMvc.perform(post("/nfc-tags/{tagCode}/stamp-requests", "tag-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.membershipId").exists())
                .andExpect(jsonPath("$.fieldErrors.userId").exists())
                .andExpect(jsonPath("$.fieldErrors.idempotencyKey").exists());
    }

    @Test
    void getsAndCancelsRequest() throws Exception {
        StampRequestResponse response = response(UUID.randomUUID(), StampRequestStatus.PENDING);
        when(stampRequestService.getRequest(response.id(), "user-1")).thenReturn(response);
        when(stampRequestService.cancelRequest(response.id(), new UserStampRequestAction("user-1")))
                .thenReturn(response(response.membershipId(), StampRequestStatus.CANCELLED));

        mockMvc.perform(get("/stamp-requests/{requestId}", response.id()).param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
        mockMvc.perform(post("/stamp-requests/{requestId}/cancel", response.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsBlankGetUserAndMissingActionActors() throws Exception {
        UUID requestId = UUID.randomUUID();
        mockMvc.perform(get("/stamp-requests/{requestId}", requestId).param("userId", " "))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/stamp-requests/{requestId}/cancel", requestId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userId").exists());
        mockMvc.perform(post("/stamp-requests/{requestId}/approve", requestId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.employeeId").exists());
    }

    @Test
    void listsPendingRequests() throws Exception {
        UUID businessId = UUID.randomUUID();
        when(stampRequestService.listPendingRequests(businessId))
                .thenReturn(List.of(response(UUID.randomUUID(), StampRequestStatus.PENDING)));

        mockMvc.perform(get("/business/{businessId}/pending-stamp-requests", businessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void approvesAndRejectsRequests() throws Exception {
        UUID employeeId = UUID.randomUUID();
        StampRequestResponse pending = response(UUID.randomUUID(), StampRequestStatus.PENDING);
        when(stampRequestService.approveRequest(pending.id(), new EmployeeStampRequestAction(employeeId)))
                .thenReturn(response(pending.membershipId(), StampRequestStatus.APPROVED));
        UUID rejectedId = UUID.randomUUID();
        when(stampRequestService.rejectRequest(rejectedId, new EmployeeStampRequestAction(employeeId)))
                .thenReturn(response(pending.membershipId(), StampRequestStatus.REJECTED));

        String body = """
                {"employeeId":"%s"}
                """.formatted(employeeId);
        mockMvc.perform(post("/stamp-requests/{requestId}/approve", pending.id())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/stamp-requests/{requestId}/reject", rejectedId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    private StampRequestResponse response(UUID membershipId, StampRequestStatus status) {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        return new StampRequestResponse(
                UUID.randomUUID(), membershipId, UUID.randomUUID(), UUID.randomUUID(), null,
                1, status, "key-1", now, now.plusSeconds(300),
                status == StampRequestStatus.PENDING ? null : now.plusSeconds(10)
        );
    }
}
