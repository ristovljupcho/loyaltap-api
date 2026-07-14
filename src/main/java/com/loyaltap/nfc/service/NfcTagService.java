package com.loyaltap.nfc.service;

import com.loyaltap.nfc.dto.CreateNfcTagRequest;
import com.loyaltap.nfc.dto.NfcTagResponse;
import com.loyaltap.nfc.dto.UpdateNfcTagRequest;

import java.util.List;
import java.util.UUID;

public interface NfcTagService {

    NfcTagResponse createTag(UUID businessId, CreateNfcTagRequest request);

    List<NfcTagResponse> listTags(UUID businessId);

    NfcTagResponse getTag(UUID businessId, UUID tagId);

    NfcTagResponse updateTag(UUID businessId, UUID tagId, UpdateNfcTagRequest request);

    void disableTag(UUID businessId, UUID tagId);
}
