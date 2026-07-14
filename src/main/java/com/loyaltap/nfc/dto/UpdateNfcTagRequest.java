package com.loyaltap.nfc.dto;

import com.loyaltap.nfc.model.NfcTagStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateNfcTagRequest(
        String locationName,
        @NotNull NfcTagStatus status
) {
}
