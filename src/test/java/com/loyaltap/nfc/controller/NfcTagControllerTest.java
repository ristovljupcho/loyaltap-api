package com.loyaltap.nfc.controller;

import com.loyaltap.nfc.dto.CreateNfcTagRequest;
import com.loyaltap.nfc.dto.NfcTagResponse;
import com.loyaltap.nfc.dto.UpdateNfcTagRequest;
import com.loyaltap.nfc.model.NfcTagStatus;
import com.loyaltap.nfc.service.NfcTagService;
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

@WebMvcTest(NfcTagController.class)
@AutoConfigureMockMvc(addFilters = false)
class NfcTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NfcTagService nfcTagService;

    @Test
    void createsTag() throws Exception {
        UUID businessId = UUID.randomUUID();
        NfcTagResponse response = response(businessId, NfcTagStatus.ACTIVE);
        when(nfcTagService.createTag(businessId, new CreateNfcTagRequest("Front desk"))).thenReturn(response);

        mockMvc.perform(post("/business/{businessId}/nfc-tags", businessId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locationName":"Front desk"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessId").value(businessId.toString()))
                .andExpect(jsonPath("$.tagCode").value(response.tagCode()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listsAndGetsTags() throws Exception {
        UUID businessId = UUID.randomUUID();
        NfcTagResponse response = response(businessId, NfcTagStatus.LOST);
        when(nfcTagService.listTags(businessId)).thenReturn(List.of(response));
        when(nfcTagService.getTag(businessId, response.id())).thenReturn(response);

        mockMvc.perform(get("/business/{businessId}/nfc-tags", businessId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("LOST"));
        mockMvc.perform(get("/business/{businessId}/nfc-tags/{tagId}", businessId, response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void updatesTag() throws Exception {
        UUID businessId = UUID.randomUUID();
        NfcTagResponse response = response(businessId, NfcTagStatus.DISABLED);
        when(nfcTagService.updateTag(
                businessId, response.id(), new UpdateNfcTagRequest("Bar", NfcTagStatus.DISABLED)))
                .thenReturn(response);

        mockMvc.perform(put("/business/{businessId}/nfc-tags/{tagId}", businessId, response.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locationName":"Bar","status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void rejectsMissingUpdateStatus() throws Exception {
        mockMvc.perform(put("/business/{businessId}/nfc-tags/{tagId}", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void disablesTag() throws Exception {
        UUID businessId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        mockMvc.perform(delete("/business/{businessId}/nfc-tags/{tagId}", businessId, tagId))
                .andExpect(status().isNoContent());
        verify(nfcTagService).disableTag(businessId, tagId);
    }

    private NfcTagResponse response(UUID businessId, NfcTagStatus status) {
        return new NfcTagResponse(
                UUID.randomUUID(),
                businessId,
                UUID.randomUUID().toString(),
                status == NfcTagStatus.DISABLED ? "Bar" : "Front desk",
                status,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
