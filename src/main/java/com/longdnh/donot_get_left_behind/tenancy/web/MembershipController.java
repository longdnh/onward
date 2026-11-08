package com.longdnh.donot_get_left_behind.tenancy.web;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.tenancy.application.service.MembershipService;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.AddMemberRequest;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.ChangeRoleRequest;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.MembershipResponse;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.OwnershipTransferResponse;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.PageResponse;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.TransferOwnershipRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/organizations/{organizationId}/memberships", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/me")
    public ResponseEntity<MembershipResponse> getOwn(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {

        return ok(MembershipResponse.from(membershipService.getOwn(organizationId, callerId(jwt))));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MembershipResponse>> list(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + PageRequest.DEFAULT_SIZE) @Min(1) @Max(PageRequest.MAX_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt) {

        return ok(PageResponse.from(
                membershipService.list(organizationId, callerId(jwt), new PageRequest(page, size)),
                MembershipResponse::from));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MembershipResponse> add(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // The organization comes from the route and the actor from the token; only the target user and role come from the body.
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(MembershipResponse.from(
                        membershipService.add(organizationId, callerId(jwt), request.userId(), request.role())));
    }


    @PatchMapping(path = "/{membershipId}/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MembershipResponse> changeRole(
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ok(MembershipResponse.from(
                membershipService.changeRole(organizationId, callerId(jwt), membershipId, request.role())));
    }

    @DeleteMapping("/{membershipId}")
    public ResponseEntity<Void> remove(
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            @AuthenticationPrincipal Jwt jwt) {

        membershipService.remove(organizationId, callerId(jwt), membershipId);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    /** Mapped under /memberships because it is a membership mutation; the body names the receiving membership. */
    @PostMapping(path = "/ownership-transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OwnershipTransferResponse> transferOwnership(
            @PathVariable UUID organizationId,
            @Valid @RequestBody TransferOwnershipRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ok(OwnershipTransferResponse.from(
                membershipService.transferOwnership(organizationId, callerId(jwt), request.membershipId())));
    }

    private static UUID callerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
