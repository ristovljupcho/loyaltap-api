package com.loyaltap.nfc.service;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.nfc.dto.CreateNfcTagRequest;
import com.loyaltap.nfc.dto.NfcTagResponse;
import com.loyaltap.nfc.dto.UpdateNfcTagRequest;
import com.loyaltap.nfc.mapper.NfcTagMapper;
import com.loyaltap.nfc.model.NfcTag;
import com.loyaltap.nfc.model.NfcTagStatus;
import com.loyaltap.nfc.repository.NfcTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfcTagServiceImplTest {

    @Mock
    private NfcTagRepository nfcTagRepository;
    @Mock
    private BusinessRepository businessRepository;

    private NfcTagServiceImpl nfcTagService;

    @BeforeEach
    void setUp() {
        nfcTagService = new NfcTagServiceImpl(nfcTagRepository, businessRepository, new NfcTagMapper());
    }

    @Test
    void createsActiveTagWithGeneratedCode() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId, BusinessStatus.ACTIVE)));
        when(nfcTagRepository.save(any(NfcTag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NfcTagResponse response = nfcTagService.createTag(businessId, new CreateNfcTagRequest("Front desk"));

        assertEquals(businessId, response.businessId());
        assertEquals("Front desk", response.locationName());
        assertEquals(NfcTagStatus.ACTIVE, response.status());
        assertNotNull(response.tagCode());
    }

    @Test
    void createRejectsMissingOrInactiveBusinessWithoutSaving() {
        UUID missingId = UUID.randomUUID();
        when(businessRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> nfcTagService.createTag(missingId, new CreateNfcTagRequest(null)));

        UUID inactiveId = UUID.randomUUID();
        when(businessRepository.findById(inactiveId)).thenReturn(Optional.of(business(inactiveId, BusinessStatus.INACTIVE)));
        assertThrows(InvalidRequestException.class,
                () -> nfcTagService.createTag(inactiveId, new CreateNfcTagRequest(null)));
        verify(nfcTagRepository, never()).save(any());
    }

    @Test
    void listsAndGetsTagsForInactiveBusiness() {
        UUID businessId = UUID.randomUUID();
        NfcTag tag = tag(businessId, NfcTagStatus.LOST);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId, BusinessStatus.INACTIVE)));
        when(nfcTagRepository.findAllByBusinessIdOrderByCreatedAtAsc(businessId)).thenReturn(List.of(tag));
        when(nfcTagRepository.findByIdAndBusinessId(tag.getId(), businessId)).thenReturn(Optional.of(tag));

        assertEquals(NfcTagStatus.LOST, nfcTagService.listTags(businessId).getFirst().status());
        assertEquals(tag.getId(), nfcTagService.getTag(businessId, tag.getId()).id());
    }

    @Test
    void getIsScopedToBusiness() {
        UUID businessId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId, BusinessStatus.ACTIVE)));
        when(nfcTagRepository.findByIdAndBusinessId(tagId, businessId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> nfcTagService.getTag(businessId, tagId));
    }

    @Test
    void updatesTagForActiveBusiness() {
        UUID businessId = UUID.randomUUID();
        NfcTag tag = tag(businessId, NfcTagStatus.ACTIVE);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId, BusinessStatus.ACTIVE)));
        when(nfcTagRepository.findByIdAndBusinessId(tag.getId(), businessId)).thenReturn(Optional.of(tag));

        NfcTagResponse response = nfcTagService.updateTag(
                businessId, tag.getId(), new UpdateNfcTagRequest("Bar", NfcTagStatus.REPLACED));

        assertEquals("Bar", response.locationName());
        assertEquals(NfcTagStatus.REPLACED, response.status());
    }

    @Test
    void updateRejectsInactiveBusinessBeforeTagLookup() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId, BusinessStatus.INACTIVE)));

        assertThrows(InvalidRequestException.class, () -> nfcTagService.updateTag(
                businessId, UUID.randomUUID(), new UpdateNfcTagRequest(null, NfcTagStatus.ACTIVE)));
        verify(nfcTagRepository, never()).findByIdAndBusinessId(any(), any());
    }

    @Test
    void disableIsIdempotentForInactiveBusiness() {
        UUID businessId = UUID.randomUUID();
        NfcTag tag = tag(businessId, NfcTagStatus.ACTIVE);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business(businessId, BusinessStatus.INACTIVE)));
        when(nfcTagRepository.findByIdAndBusinessId(tag.getId(), businessId)).thenReturn(Optional.of(tag));

        nfcTagService.disableTag(businessId, tag.getId());
        nfcTagService.disableTag(businessId, tag.getId());

        assertEquals(NfcTagStatus.DISABLED, tag.getStatus());
    }

    private Business business(UUID id, BusinessStatus status) {
        Business business = new Business();
        business.setId(id);
        business.setStatus(status);
        return business;
    }

    private NfcTag tag(UUID businessId, NfcTagStatus status) {
        NfcTag tag = new NfcTag();
        tag.setId(UUID.randomUUID());
        tag.setBusiness(business(businessId, BusinessStatus.ACTIVE));
        tag.setTagCode(UUID.randomUUID().toString());
        tag.setStatus(status);
        return tag;
    }
}
