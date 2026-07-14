package com.loyaltap.stamprequest.mapper;

import com.loyaltap.membership.model.Membership;
import com.loyaltap.nfc.model.NfcTag;
import com.loyaltap.stamprequest.dto.CreateStampRequest;
import com.loyaltap.stamprequest.dto.StampRequestResponse;
import com.loyaltap.stamprequest.model.StampRequest;
import com.loyaltap.stamprequest.model.StampRequestStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class StampRequestMapper {

    public StampRequest toEntity(
            CreateStampRequest request,
            Membership membership,
            NfcTag nfcTag,
            Instant createdAt,
            Instant expiresAt
    ) {
        StampRequest stampRequest = new StampRequest();
        stampRequest.setMembership(membership);
        stampRequest.setBusiness(membership.getBusiness());
        stampRequest.setNfcTag(nfcTag);
        stampRequest.setPointsToAdd(1);
        stampRequest.setStatus(StampRequestStatus.PENDING);
        stampRequest.setIdempotencyKey(request.idempotencyKey());
        stampRequest.setCreatedAt(createdAt);
        stampRequest.setExpiresAt(expiresAt);
        return stampRequest;
    }

    public StampRequestResponse toResponse(StampRequest request) {
        return new StampRequestResponse(
                request.getId(),
                request.getMembership().getId(),
                request.getBusiness().getId(),
                request.getNfcTag() == null ? null : request.getNfcTag().getId(),
                request.getApprovedByEmployee() == null ? null : request.getApprovedByEmployee().getId(),
                request.getPointsToAdd(),
                request.getStatus(),
                request.getIdempotencyKey(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getProcessedAt()
        );
    }
}
