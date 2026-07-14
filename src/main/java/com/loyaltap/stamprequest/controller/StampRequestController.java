package com.loyaltap.stamprequest.controller;

import com.loyaltap.stamprequest.dto.CreateStampRequest;
import com.loyaltap.stamprequest.dto.EmployeeStampRequestAction;
import com.loyaltap.stamprequest.dto.StampRequestResponse;
import com.loyaltap.stamprequest.dto.UserStampRequestAction;
import com.loyaltap.stamprequest.service.StampRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
public class StampRequestController {

    private final StampRequestService stampRequestService;

    @PostMapping("/nfc-tags/{tagCode}/stamp-requests")
    public ResponseEntity<StampRequestResponse> createRequest(
            @PathVariable String tagCode,
            @Valid @RequestBody CreateStampRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stampRequestService.createRequest(tagCode, request));
    }

    @GetMapping("/stamp-requests/{requestId}")
    public StampRequestResponse getRequest(
            @PathVariable UUID requestId,
            @RequestParam @NotBlank String userId
    ) {
        return stampRequestService.getRequest(requestId, userId);
    }

    @PostMapping("/stamp-requests/{requestId}/cancel")
    public StampRequestResponse cancelRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody UserStampRequestAction action
    ) {
        return stampRequestService.cancelRequest(requestId, action);
    }

    @GetMapping("/business/{businessId}/pending-stamp-requests")
    public List<StampRequestResponse> listPendingRequests(@PathVariable UUID businessId) {
        return stampRequestService.listPendingRequests(businessId);
    }

    @PostMapping("/stamp-requests/{requestId}/approve")
    public StampRequestResponse approveRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody EmployeeStampRequestAction action
    ) {
        return stampRequestService.approveRequest(requestId, action);
    }

    @PostMapping("/stamp-requests/{requestId}/reject")
    public StampRequestResponse rejectRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody EmployeeStampRequestAction action
    ) {
        return stampRequestService.rejectRequest(requestId, action);
    }
}
