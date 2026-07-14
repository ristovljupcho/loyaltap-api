package com.loyaltap.nfc.controller;

import com.loyaltap.nfc.dto.CreateNfcTagRequest;
import com.loyaltap.nfc.dto.NfcTagResponse;
import com.loyaltap.nfc.dto.UpdateNfcTagRequest;
import com.loyaltap.nfc.service.NfcTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/business/{businessId}/nfc-tags")
@RequiredArgsConstructor
public class NfcTagController {

    private final NfcTagService nfcTagService;

    @PostMapping
    public ResponseEntity<NfcTagResponse> createTag(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreateNfcTagRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nfcTagService.createTag(businessId, request));
    }

    @GetMapping
    public List<NfcTagResponse> listTags(@PathVariable UUID businessId) {
        return nfcTagService.listTags(businessId);
    }

    @GetMapping("/{tagId}")
    public NfcTagResponse getTag(@PathVariable UUID businessId, @PathVariable UUID tagId) {
        return nfcTagService.getTag(businessId, tagId);
    }

    @PutMapping("/{tagId}")
    public NfcTagResponse updateTag(
            @PathVariable UUID businessId,
            @PathVariable UUID tagId,
            @Valid @RequestBody UpdateNfcTagRequest request
    ) {
        return nfcTagService.updateTag(businessId, tagId, request);
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> disableTag(@PathVariable UUID businessId, @PathVariable UUID tagId) {
        nfcTagService.disableTag(businessId, tagId);
        return ResponseEntity.noContent().build();
    }
}
