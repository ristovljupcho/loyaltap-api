package com.loyaltap.business.controller;

import com.loyaltap.business.dto.BusinessRequest;
import com.loyaltap.business.dto.BusinessResponse;
import com.loyaltap.business.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/business")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> createBusiness(@Valid @RequestBody BusinessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessService.createBusiness(request));
    }

    @GetMapping
    public List<BusinessResponse> listBusinesses() {
        return businessService.listActiveBusinesses();
    }

    @GetMapping("/{businessId}")
    public BusinessResponse getBusiness(@PathVariable UUID businessId) {
        return businessService.getBusiness(businessId);
    }

    @PutMapping("/{businessId}")
    public BusinessResponse updateBusiness(
            @PathVariable UUID businessId,
            @Valid @RequestBody BusinessRequest request
    ) {
        return businessService.updateBusiness(businessId, request);
    }

    @PostMapping("/{businessId}/deactivate")
    public BusinessResponse deactivateBusiness(@PathVariable UUID businessId) {
        return businessService.deactivateBusiness(businessId);
    }
}
