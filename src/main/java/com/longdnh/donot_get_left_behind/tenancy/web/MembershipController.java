package com.longdnh.donot_get_left_behind.tenancy.web;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.tenancy.application.service.MembershipService;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.AddMemberRequest;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.MembershipResponse;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.PageResponse;

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

    private static UUID callerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
