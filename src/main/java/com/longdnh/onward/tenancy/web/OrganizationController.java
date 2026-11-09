package com.longdnh.onward.tenancy.web;

import com.longdnh.onward.common.constants.ApiPaths;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.onward.common.pagination.PageRequest;
import com.longdnh.onward.tenancy.application.dto.CreateOrganizationResult;
import com.longdnh.onward.tenancy.application.service.OrganizationService;
import com.longdnh.onward.tenancy.web.dto.CreateOrganizationRequest;
import com.longdnh.onward.tenancy.web.dto.CreateOrganizationResponse;
import com.longdnh.onward.tenancy.web.dto.MembershipResponse;
import com.longdnh.onward.tenancy.web.dto.OrganizationResponse;
import com.longdnh.onward.tenancy.web.dto.OrganizationSummaryResponse;
import com.longdnh.onward.tenancy.web.dto.PageResponse;
import com.longdnh.onward.tenancy.web.dto.UpdateOrganizationRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = ApiPaths.Organizations.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateOrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // The owner always comes from the authenticated token, never from the request body.
        UUID ownerUserId = UUID.fromString(jwt.getSubject());

        CreateOrganizationResult result = organizationService.create(ownerUserId, request.name());

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(new CreateOrganizationResponse(
                        OrganizationResponse.from(result.organization()),
                        MembershipResponse.from(result.ownerMembership())));
    }

    @GetMapping(ApiPaths.Organizations.ORGANIZATION)
    public ResponseEntity<OrganizationResponse> getProfile(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {

        // organizationId is untrusted route input; the service verifies the caller's membership in it.
        UUID callerUserId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(OrganizationResponse.from(organizationService.getProfile(organizationId, callerUserId)));
    }

    /** Constraint violations on the params are turned into 400 by Spring's built-in method validation. */
    @GetMapping
    public ResponseEntity<PageResponse<OrganizationSummaryResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "" + PageRequest.DEFAULT_SIZE) @Min(1) @Max(PageRequest.MAX_SIZE) int size,
            @AuthenticationPrincipal Jwt jwt) {

        UUID callerUserId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(PageResponse.from(
                        organizationService.listForUser(callerUserId, new PageRequest(page, size)),
                        OrganizationSummaryResponse::from));
    }

    @PatchMapping(path = ApiPaths.Organizations.ORGANIZATION, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrganizationResponse> rename(
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID callerUserId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(OrganizationResponse.from(
                        organizationService.rename(organizationId, callerUserId, request.name())));
    }
}
