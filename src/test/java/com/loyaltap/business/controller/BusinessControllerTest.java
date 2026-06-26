package com.loyaltap.business.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.dto.CreateBusinessRequest;
import com.loyaltap.business.dto.UpdateBusinessRequest;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.service.BusinessService;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.GlobalExceptionHandler;
import com.loyaltap.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BusinessControllerTest {

    private static final UUID BUSINESS_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessService businessService;

    @Test
    void createBusinessReturnsCreatedBusiness() throws Exception {
        CreateBusinessRequest request = createRequest("Cafe Aroma", null);
        when(businessService.createBusiness(any(CreateBusinessRequest.class))).thenReturn(response());

        mockMvc.perform(post("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(BUSINESS_ID.toString()))
                .andExpect(jsonPath("$.name").value("Cafe Aroma"))
                .andExpect(jsonPath("$.slug").value("cafe-aroma"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(businessService).createBusiness(any(CreateBusinessRequest.class));
    }

    @Test
    void createBusinessReturnsBadRequestForInvalidRequest() throws Exception {
        CreateBusinessRequest request = createRequest("", null);

        mockMvc.perform(post("/businesses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void listBusinessesReturnsActiveBusinesses() throws Exception {
        when(businessService.listActiveBusinesses()).thenReturn(List.of(response()));

        mockMvc.perform(get("/businesses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(BUSINESS_ID.toString()))
                .andExpect(jsonPath("$[0].slug").value("cafe-aroma"));
    }

    @Test
    void getBusinessReturnsNotFoundWhenMissing() throws Exception {
        when(businessService.getBusiness(BUSINESS_ID))
                .thenThrow(new ResourceNotFoundException("Business not found: " + BUSINESS_ID));

        mockMvc.perform(get("/businesses/{businessId}", BUSINESS_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString(BUSINESS_ID.toString())));
    }

    @Test
    void updateBusinessReturnsConflictForDuplicateSlug() throws Exception {
        UpdateBusinessRequest request = new UpdateBusinessRequest(
                null,
                "Cafe Aroma",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        when(businessService.updateBusiness(eq(BUSINESS_ID), any(UpdateBusinessRequest.class)))
                .thenThrow(new DuplicateResourceException("Business slug already exists: cafe-aroma"));

        mockMvc.perform(patch("/businesses/{businessId}", BUSINESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Business slug already exists: cafe-aroma"));
    }

    @Test
    void deactivateBusinessReturnsInactiveBusiness() throws Exception {
        when(businessService.deactivateBusiness(BUSINESS_ID)).thenReturn(inactiveResponse());

        mockMvc.perform(post("/businesses/{businessId}/deactivate", BUSINESS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    private CreateBusinessRequest createRequest(String name, String slug) {
        return new CreateBusinessRequest(
                name,
                slug,
                "A neighborhood cafe",
                "+38970000000",
                "owner@example.com",
                "https://example.com",
                "Main Street 1",
                null,
                "Skopje",
                null,
                "1000",
                "MK"
        );
    }

    private BusinessResponse response() {
        return new BusinessResponse(
                BUSINESS_ID,
                "Cafe Aroma",
                "cafe-aroma",
                "A neighborhood cafe",
                "+38970000000",
                "owner@example.com",
                "https://example.com",
                "Main Street 1",
                null,
                "Skopje",
                null,
                "1000",
                "MK",
                BusinessStatus.ACTIVE,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z")
        );
    }

    private BusinessResponse inactiveResponse() {
        BusinessResponse active = response();
        return new BusinessResponse(
                active.id(),
                active.name(),
                active.slug(),
                active.description(),
                active.phone(),
                active.email(),
                active.websiteUrl(),
                active.addressLine1(),
                active.addressLine2(),
                active.city(),
                active.state(),
                active.postalCode(),
                active.country(),
                BusinessStatus.INACTIVE,
                active.createdAt(),
                active.updatedAt()
        );
    }
}
