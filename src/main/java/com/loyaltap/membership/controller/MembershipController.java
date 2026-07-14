package com.loyaltap.membership.controller;

import com.loyaltap.membership.dto.CreateMembershipRequest;
import com.loyaltap.membership.dto.MembershipResponse;
import com.loyaltap.membership.service.MembershipService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/memberships")
@RequiredArgsConstructor
@Validated
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping
    public ResponseEntity<MembershipResponse> createMembership(
            @Valid @RequestBody CreateMembershipRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membershipService.createMembership(request));
    }

    @GetMapping
    public List<MembershipResponse> listActiveMemberships(@RequestParam @NotBlank String userId) {
        return membershipService.listActiveMemberships(userId);
    }

    @GetMapping("/{membershipId}")
    public MembershipResponse getMembership(@PathVariable UUID membershipId) {
        return membershipService.getMembership(membershipId);
    }
}
