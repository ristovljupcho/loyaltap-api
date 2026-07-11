package com.loyaltap.business.service;

import com.loyaltap.business.dto.BusinessRequest;
import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.mapper.BusinessMapper;
import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.business.utils.BusinessSlugUtils;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;

    @Override
    @Transactional
    public BusinessResponse createBusiness(@Valid BusinessRequest request) {
        String slug = normalizeRequiredSlug(request.slug());
        ensureSlugAvailable(slug, null);
        Business business = businessMapper.toEntity(request, slug);

        return businessMapper.toResponse(businessRepository.save(business));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessResponse> listActiveBusinesses() {
        return businessRepository.findAllByStatusOrderByNameAsc(BusinessStatus.ACTIVE)
                .stream()
                .map(businessMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessResponse getBusiness(UUID businessId) {
        return businessMapper.toResponse(findBusiness(businessId));
    }

    @Override
    @Transactional
    public BusinessResponse updateBusiness(UUID businessId, @Valid BusinessRequest request) {
        Business business = findBusiness(businessId);
        String slug = normalizeRequiredSlug(request.slug());
        if (!slug.equals(business.getSlug())) {
            ensureSlugAvailable(slug, businessId);
        }
        businessMapper.updateEntity(business, request, slug);

        return businessMapper.toResponse(business);
    }

    @Override
    @Transactional
    public BusinessResponse deactivateBusiness(UUID businessId) {
        Business business = findBusiness(businessId);
        if (business.getStatus() != BusinessStatus.INACTIVE) {
            business.setStatus(BusinessStatus.INACTIVE);
        }
        return businessMapper.toResponse(business);
    }

    private Business findBusiness(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
    }

    private void ensureSlugAvailable(String slug, UUID currentBusinessId) {
        boolean slugExists = currentBusinessId == null
                ? businessRepository.existsBySlug(slug)
                : businessRepository.existsBySlugAndIdNot(slug, currentBusinessId);

        if (slugExists) {
            throw new DuplicateResourceException("Business slug already exists: " + slug);
        }
    }

    private String normalizeRequiredSlug(String value) {
        String slug = BusinessSlugUtils.normalize(value);
        if (!StringUtils.hasText(slug)) {
            throw new InvalidRequestException("Business slug must contain at least one letter or number");
        }
        return slug;
    }
}
