package com.loyaltap.business.service;

import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.dto.CreateBusinessRequest;
import com.loyaltap.business.dto.UpdateBusinessRequest;
import com.loyaltap.business.mapper.BusinessMapper;
import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.business.utils.BusinessSlugUtils;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Validated
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;

    public BusinessServiceImpl(BusinessRepository businessRepository, BusinessMapper businessMapper) {
        this.businessRepository = businessRepository;
        this.businessMapper = businessMapper;
    }

    @Override
    @Transactional
    public BusinessResponse createBusiness(@Valid CreateBusinessRequest request) {
        String name = requireNonBlank(request.name(), "Business name is required");
        String slugSource = StringUtils.hasText(request.slug()) ? request.slug() : name;
        String slug = normalizeRequiredSlug(slugSource);
        ensureSlugAvailable(slug, null);

        Business business = businessMapper.toEntity(request, name, slug);

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
    public BusinessResponse updateBusiness(UUID businessId, @Valid UpdateBusinessRequest request) {
        Business business = findBusiness(businessId);

        if (request.name() != null) {
            business.setName(requireNonBlank(request.name(), "Business name cannot be blank"));
        }
        if (request.slug() != null) {
            String slug = normalizeRequiredSlug(request.slug());
            if (!slug.equals(business.getSlug())) {
                ensureSlugAvailable(slug, business.getId());
                business.setSlug(slug);
            }
        }
        applyIfPresent(request.description(), business::setDescription);
        applyIfPresent(request.phone(), business::setPhone);
        applyIfPresent(request.email(), business::setEmail);
        applyIfPresent(request.websiteUrl(), business::setWebsiteUrl);
        applyIfPresent(request.address(), business::setAddress);
        applyIfPresent(request.city(), business::setCity);

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

    private String requireNonBlank(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidRequestException(message);
        }
        return value.trim();
    }

    private void applyIfPresent(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(trimToNull(value));
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
