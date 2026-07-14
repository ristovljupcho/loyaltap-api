package com.loyaltap.nfc.dto;

import com.loyaltap.nfc.model.NfcTagStatus;

import java.time.Instant;
import java.util.UUID;

public record NfcTagResponse(
        UUID id,
        UUID businessId,
        String tagCode,
        String locationName,
        NfcTagStatus status,
        Instant lastUsedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
