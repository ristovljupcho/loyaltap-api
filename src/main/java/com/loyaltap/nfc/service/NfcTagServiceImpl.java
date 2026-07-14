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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class NfcTagServiceImpl implements NfcTagService {

    private final NfcTagRepository nfcTagRepository;
    private final BusinessRepository businessRepository;
    private final NfcTagMapper nfcTagMapper;

    @Override
    @Transactional
    public NfcTagResponse createTag(UUID businessId, @Valid CreateNfcTagRequest request) {
        Business business = findActiveBusiness(businessId);
        NfcTag tag = nfcTagMapper.toEntity(request, business, UUID.randomUUID().toString());
        return nfcTagMapper.toResponse(nfcTagRepository.save(tag));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NfcTagResponse> listTags(UUID businessId) {
        findBusiness(businessId);
        return nfcTagRepository.findAllByBusinessIdOrderByCreatedAtAsc(businessId)
                .stream()
                .map(nfcTagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NfcTagResponse getTag(UUID businessId, UUID tagId) {
        findBusiness(businessId);
        return nfcTagMapper.toResponse(findTag(businessId, tagId));
    }

    @Override
    @Transactional
    public NfcTagResponse updateTag(UUID businessId, UUID tagId, @Valid UpdateNfcTagRequest request) {
        findActiveBusiness(businessId);
        NfcTag tag = findTag(businessId, tagId);
        nfcTagMapper.updateEntity(tag, request);
        return nfcTagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public void disableTag(UUID businessId, UUID tagId) {
        findBusiness(businessId);
        NfcTag tag = findTag(businessId, tagId);
        if (tag.getStatus() != NfcTagStatus.DISABLED) {
            tag.setStatus(NfcTagStatus.DISABLED);
        }
    }

    private Business findActiveBusiness(UUID businessId) {
        Business business = findBusiness(businessId);
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new InvalidRequestException("Business is not active: " + businessId);
        }
        return business;
    }

    private Business findBusiness(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
    }

    private NfcTag findTag(UUID businessId, UUID tagId) {
        return nfcTagRepository.findByIdAndBusinessId(tagId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("NFC tag not found: " + tagId));
    }
}
