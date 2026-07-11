package com.loyaltap.business.service;

import com.loyaltap.business.dto.BusinessRequest;
import com.loyaltap.business.dto.BusinessResponse;

import java.util.List;
import java.util.UUID;

public interface BusinessService {

    BusinessResponse createBusiness(BusinessRequest request);

    List<BusinessResponse> listActiveBusinesses();

    BusinessResponse getBusiness(UUID businessId);

    BusinessResponse updateBusiness(UUID businessId, BusinessRequest request);

    BusinessResponse deactivateBusiness(UUID businessId);
}
