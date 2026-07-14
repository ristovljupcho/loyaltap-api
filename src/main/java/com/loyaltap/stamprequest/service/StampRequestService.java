package com.loyaltap.stamprequest.service;

import com.loyaltap.stamprequest.dto.CreateStampRequest;
import com.loyaltap.stamprequest.dto.EmployeeStampRequestAction;
import com.loyaltap.stamprequest.dto.StampRequestResponse;
import com.loyaltap.stamprequest.dto.UserStampRequestAction;

import java.util.List;
import java.util.UUID;

public interface StampRequestService {

    StampRequestResponse createRequest(String tagCode, CreateStampRequest request);

    StampRequestResponse getRequest(UUID requestId, String userId);

    StampRequestResponse cancelRequest(UUID requestId, UserStampRequestAction action);

    List<StampRequestResponse> listPendingRequests(UUID businessId);

    StampRequestResponse approveRequest(UUID requestId, EmployeeStampRequestAction action);

    StampRequestResponse rejectRequest(UUID requestId, EmployeeStampRequestAction action);

    int expirePendingRequests();
}
