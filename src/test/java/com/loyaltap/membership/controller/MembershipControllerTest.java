package com.loyaltap.membership.controller;

import com.loyaltap.membership.dto.CreateMembershipRequest;
import com.loyaltap.membership.dto.MembershipResponse;
import com.loyaltap.membership.model.MembershipStatus;
import com.loyaltap.membership.service.MembershipService;
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

@WebMvcTest(MembershipController.class)
@AutoConfigureMockMvc(addFilters = false)
class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipService membershipService;

    @Test
    void createsMembership() throws Exception {
        MembershipResponse response = response();
        CreateMembershipRequest request = new CreateMembershipRequest(response.userId(), response.businessId());
        when(membershipService.createMembership(request)).thenReturn(response);

        mockMvc.perform(post("/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1","businessId":"%s"}
                                """.formatted(response.businessId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.businessId").value(response.businessId().toString()))
                .andExpect(jsonPath("$.pointsBalance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsMissingOrBlankCreateFields() throws Exception {
        mockMvc.perform(post("/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userId").exists())
                .andExpect(jsonPath("$.fieldErrors.businessId").exists());

        mockMvc.perform(post("/memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"   ","businessId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userId").exists());
    }

    @Test
    void listsActiveMembershipsForRequiredUser() throws Exception {
        MembershipResponse response = response();
        when(membershipService.listActiveMemberships(response.userId())).thenReturn(List.of(response));

        mockMvc.perform(get("/memberships").param("userId", response.userId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.id().toString()));

        mockMvc.perform(get("/memberships"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsMembership() throws Exception {
        MembershipResponse response = response();
        when(membershipService.getMembership(response.id())).thenReturn(response);

        mockMvc.perform(get("/memberships/{membershipId}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(response.userId()));
    }

    private MembershipResponse response() {
        return new MembershipResponse(
                UUID.randomUUID(),
                "user-1",
                UUID.randomUUID(),
                0,
                0,
                0,
                0,
                0,
                MembershipStatus.ACTIVE,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
