package com.longdnh.onward.identity.web;

import com.longdnh.onward.common.constants.ApiPaths;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.onward.identity.application.service.AccountService;
import com.longdnh.onward.identity.web.dto.AdminUserResponse;

import lombok.RequiredArgsConstructor;

/** Reachable only with ROLE_SYSTEM_ADMIN; see SecurityConfiguration. */
@RestController
@RequestMapping(path = ApiPaths.Admin.USERS, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminUserController {

    private final AccountService accountService;

    @GetMapping(ApiPaths.Admin.USER)
    public ResponseEntity<AdminUserResponse> get(@PathVariable UUID userId) {
        return ok(accountService.get(userId));
    }

    @PostMapping(ApiPaths.Admin.USER_DISABLE)
    public ResponseEntity<AdminUserResponse> disable(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        return ok(accountService.disableByAdmin(UUID.fromString(jwt.getSubject()), userId));
    }

    @PostMapping(ApiPaths.Admin.USER_ENABLE)
    public ResponseEntity<AdminUserResponse> enable(@PathVariable UUID userId) {
        return ok(accountService.enable(userId));
    }

    @PostMapping(ApiPaths.Admin.USER_PROMOTE)
    public ResponseEntity<AdminUserResponse> promote(@PathVariable UUID userId) {
        return ok(accountService.promoteToSystemAdmin(userId));
    }

    private static ResponseEntity<AdminUserResponse> ok(
            com.longdnh.onward.identity.domain.User user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(AdminUserResponse.from(user));
    }
}
