package com.loyaltap.business.mapper;

import com.loyaltap.business.dto.BusinessRequest;
import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import org.springframework.stereotype.Component;

@Component
public class BusinessMapper {

    public Business toEntity(BusinessRequest request, String slug) {
        Business business = new Business();
        business.setName(request.name().strip());
        business.setSlug(slug);
        business.setDescription(trimToNull(request.description()));
        business.setPhone(trimToNull(request.phone()));
        business.setEmail(trimToNull(request.email()));
        business.setWebsiteUrl(trimToNull(request.websiteUrl()));
        business.setAddress(trimToNull(request.address()));
        business.setCity(trimToNull(request.city()));
        business.setStatus(BusinessStatus.ACTIVE);
        return business;
    }

    public void updateEntity(Business business, BusinessRequest request, String slug) {
        business.setName(request.name().strip());
        business.setSlug(slug);
        business.setDescription(trimToNull(request.description()));
        business.setPhone(trimToNull(request.phone()));
        business.setEmail(trimToNull(request.email()));
        business.setWebsiteUrl(trimToNull(request.websiteUrl()));
        business.setAddress(trimToNull(request.address()));
        business.setCity(trimToNull(request.city()));
    }

    public BusinessResponse toResponse(Business business) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getSlug(),
                business.getDescription(),
                business.getPhone(),
                business.getEmail(),
                business.getWebsiteUrl(),
                business.getAddress(),
                business.getCity(),
                business.getStatus()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
