package com.longdnh.onward.identity.web;

import com.longdnh.onward.common.constants.ApiPaths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.onward.common.constants.ClientMessages;
import com.longdnh.onward.identity.application.dto.LoginResult;
import com.longdnh.onward.identity.application.port.UserRepository;
import com.longdnh.onward.identity.application.service.LoginService;
import com.longdnh.onward.identity.application.service.AccountService;
import com.longdnh.onward.identity.application.service.EmailVerificationService;
import com.longdnh.onward.identity.application.service.LogoutService;
import com.longdnh.onward.identity.application.service.PasswordResetService;
import com.longdnh.onward.identity.application.service.RefreshService;
import com.longdnh.onward.identity.application.service.RegisterService;
import com.longdnh.onward.identity.web.dto.CsrfResponse;
import com.longdnh.onward.identity.web.dto.CurrentUserResponse;
import com.longdnh.onward.identity.web.dto.LoginRequest;
import com.longdnh.onward.identity.web.dto.LoginResponse;
import com.longdnh.onward.identity.web.dto.EmailRequest;
import com.longdnh.onward.identity.web.dto.RegisterRequest;
import com.longdnh.onward.identity.web.dto.ResetPasswordRequest;
import com.longdnh.onward.identity.web.dto.VerifyEmailRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.longdnh.onward.identity.web.dto.DisableAccountRequest;

@RestController
@RequestMapping(path = ApiPaths.Auth.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

        private static final String REFRESH_COOKIE_NAME = "refresh_token";
        private static final String REFRESH_COOKIE_PATH = ApiPaths.Auth.BASE;
        
        private final RegisterService registerService;
        private final LoginService loginService;
        private final UserRepository userRepository;
        private final CsrfTokenRepository csrfTokenRepository;
        private final Clock clock;
        private final boolean secureRefreshCookie;
        private final RefreshService refreshService;
        private final LogoutService logoutService;
        private final AccountService accountService;
        private final EmailVerificationService emailVerificationService;
        private final PasswordResetService passwordResetService;

        public AuthController(
                        RegisterService registerService,
                        LoginService loginService,
                        UserRepository userRepository,
                        CsrfTokenRepository csrfTokenRepository,
                        Clock clock,
                        @Value("${app.security.refresh.secure-cookie:true}") boolean secureRefreshCookie,
                        RefreshService refreshService, 
                        LogoutService logoutService,
                        AccountService accountService,
                        EmailVerificationService emailVerificationService,
                        PasswordResetService passwordResetService) {

                this.registerService = registerService;
                this.loginService = loginService;
                this.userRepository = userRepository;
                this.csrfTokenRepository = csrfTokenRepository;
                this.clock = clock;
                this.secureRefreshCookie = secureRefreshCookie;
                this.refreshService = refreshService;
                this.logoutService = logoutService;
                this.accountService = accountService;
                this.emailVerificationService = emailVerificationService;
                this.passwordResetService = passwordResetService;
        }

        @GetMapping(ApiPaths.Auth.CSRF)
        public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
                return ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .body(new CsrfResponse(
                                                token.getHeaderName(),
                                                token.getToken()));
        }

        /**
         * Always 202: the account may have been created, may already exist, or may be unverified and get its
         * mail again. The body never says which, so the endpoint cannot be used to enumerate emails.
         */
        @PostMapping(path = ApiPaths.Auth.REGISTER, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
                registerService.register(request.email(), request.password());
                return accepted();
        }

        @PostMapping(path = ApiPaths.Auth.VERIFY_EMAIL, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
                emailVerificationService.verify(request.token());
                return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }

        @PostMapping(path = ApiPaths.Auth.RESEND_VERIFICATION, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest request) {
                emailVerificationService.resend(request.email());
                return accepted();
        }

        @PostMapping(path = ApiPaths.Auth.FORGOT_PASSWORD, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
                passwordResetService.requestReset(request.email());
                return accepted();
        }

        /** The reset revokes every session of the account; the client must sign in again. */
        @PostMapping(path = ApiPaths.Auth.RESET_PASSWORD, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                passwordResetService.reset(request.token(), request.newPassword());
                return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }

        @PostMapping(path = ApiPaths.Auth.LOGIN, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<LoginResponse> login(
                        @Valid @RequestBody LoginRequest request,
                        HttpServletRequest servletRequest,
                        HttpServletResponse servletResponse) {

                LoginResult result = loginService.login(
                                request.email(),
                                request.password());

                Instant now = clock.instant();

                ResponseCookie cookie = buildRefreshCookie(result.refreshToken(), result.refreshTokenExpiresAt(), now);

                servletResponse.addHeader(
                                HttpHeaders.SET_COOKIE,
                                cookie.toString());

                csrfTokenRepository.saveToken(
                                null,
                                servletRequest,
                                servletResponse);

                long expiresIn = Math.max(
                                0,
                                Duration.between(
                                                now,
                                                result.accessTokenExpiresAt()).getSeconds());

                return ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .body(new LoginResponse(
                                                result.accessToken(),
                                                "Bearer",
                                                expiresIn));
        }

        @GetMapping(ApiPaths.Auth.ME)
        public ResponseEntity<CurrentUserResponse> me(
                        @AuthenticationPrincipal Jwt jwt) {

                UUID userId = UUID.fromString(jwt.getSubject());

                var user = userRepository.findById(userId)
                                .filter(candidate -> candidate.canAuthenticate())
                                .orElseThrow(() -> new BadCredentialsException(
                                                ClientMessages.AUTHENTICATION_REQUIRED));

                return ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .body(new CurrentUserResponse(
                                                user.getId(),
                                                user.getEmail()));
        }

        @PostMapping(ApiPaths.Auth.REFRESH)
        public ResponseEntity<LoginResponse> getRefreshToken(
                        HttpServletResponse response,
                        @CookieValue(name = REFRESH_COOKIE_NAME) String rawRefreshToken) {

                var result = refreshService.refresh(rawRefreshToken);
                Instant now = clock.instant();

                ResponseCookie cookie = buildRefreshCookie(result.refreshToken(), result.refreshTokenExpiresAt(), now);
                response.addHeader(
                                HttpHeaders.SET_COOKIE,
                                cookie.toString());

                long expiresIn = Math.max(
                                0,
                                Duration.between(
                                                now,
                                                result.accessTokenExpiresAt()).getSeconds());
                return ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .body(new LoginResponse(
                                                result.accessToken(),
                                                "Bearer",
                                                expiresIn));
        }

        @PostMapping(ApiPaths.Auth.LOGOUT)
        public ResponseEntity<Void> logout(
                        @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken,
                        HttpServletRequest servletRequest,
                        HttpServletResponse servletResponse) {

                logoutService.logout(rawRefreshToken);

                servletResponse.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString());
                csrfTokenRepository.saveToken(null, servletRequest, servletResponse);

                return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }

        /** Self-service disable: requires the password again; all sessions are revoked by the service. */
        @PostMapping(path = ApiPaths.Auth.DISABLE_ACCOUNT, consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Void> disableAccount(
                        @Valid @RequestBody DisableAccountRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                accountService.disableSelf(UUID.fromString(jwt.getSubject()), request.password());
                return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }

        private static ResponseEntity<Void> accepted() {
                return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
        }

        private ResponseCookie buildRefreshCookie(String rawToken, Instant expiresAt, Instant now) {
                long refreshMaxAge = Math.max(
                                0,
                                Duration.between(now, expiresAt).getSeconds());

                return ResponseCookie
                                .from(REFRESH_COOKIE_NAME, rawToken)
                                .httpOnly(true)
                                .secure(secureRefreshCookie)
                                .sameSite("Lax")
                                .path(REFRESH_COOKIE_PATH)
                                .maxAge(refreshMaxAge)
                                .build();

        }

        private ResponseCookie expiredRefreshCookie() {
                return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                                .httpOnly(true)
                                .secure(secureRefreshCookie)
                                .sameSite("Lax")
                                .path(REFRESH_COOKIE_PATH)
                                .maxAge(0)
                                .build();
        }
}