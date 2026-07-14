package com.loyaltap.nfc.mapper;

import com.loyaltap.business.model.Business;
import com.loyaltap.nfc.dto.CreateNfcTagRequest;
import com.loyaltap.nfc.dto.NfcTagResponse;
import com.loyaltap.nfc.dto.UpdateNfcTagRequest;
import com.loyaltap.nfc.model.NfcTag;
import com.loyaltap.nfc.model.NfcTagStatus;
import org.springframework.stereotype.Component;

@Component
public class NfcTagMapper {

    public NfcTag toEntity(CreateNfcTagRequest request, Business business, String tagCode) {
        NfcTag tag = new NfcTag();
        tag.setBusiness(business);
        tag.setTagCode(tagCode);
        tag.setLocationName(request.locationName());
        tag.setStatus(NfcTagStatus.ACTIVE);
        return tag;
    }

    public void updateEntity(NfcTag tag, UpdateNfcTagRequest request) {
        tag.setLocationName(request.locationName());
        tag.setStatus(request.status());
    }

    public NfcTagResponse toResponse(NfcTag tag) {
        return new NfcTagResponse(
                tag.getId(),
                tag.getBusiness().getId(),
                tag.getTagCode(),
                tag.getLocationName(),
                tag.getStatus(),
                tag.getLastUsedAt(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }
}
