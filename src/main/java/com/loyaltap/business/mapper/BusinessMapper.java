package com.loyaltap.business.mapper;

import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.model.Business;
import org.springframework.stereotype.Component;

@Component
public class BusinessMapper {

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
}
