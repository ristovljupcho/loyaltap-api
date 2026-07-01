package com.loyaltap.business.service;

import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.dto.CreateBusinessRequest;
import com.loyaltap.business.dto.UpdateBusinessRequest;

import java.util.List;
import java.util.UUID;

public interface BusinessService {

    BusinessResponse createBusiness(CreateBusinessRequest request);

    List<BusinessResponse> listActiveBusinesses();

    BusinessResponse getBusiness(UUID businessId);

    BusinessResponse updateBusiness(UUID businessId, UpdateBusinessRequest request);

    BusinessResponse deactivateBusiness(UUID businessId);
}
