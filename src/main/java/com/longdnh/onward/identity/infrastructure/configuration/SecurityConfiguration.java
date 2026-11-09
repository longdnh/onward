package com.longdnh.onward.identity.infrastructure.configuration;

import com.longdnh.onward.common.constants.ApiPaths;
import com.longdnh.onward.common.constants.ApiPaths.Auth;
import com.longdnh.onward.common.Exception.ApiErrorWriter;
import com.longdnh.onward.common.constants.ApiErrorCodes;
import com.longdnh.onward.common.constants.ClientMessages;
import com.longdnh.onward.identity.infrastructure.security.PublicAuthRateLimitFilter;
import com.longdnh.onward.identity.infrastructure.security.SessionJwtAuthenticationConverter;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    /** Token-driven flows: no credentials yet, so they must be reachable without a JWT. */
    private static final String[] PUBLIC_EMAIL_FLOW_PATHS = {
            Auth.BASE + Auth.VERIFY_EMAIL,
            Auth.BASE + Auth.RESEND_VERIFICATION,
            Auth.BASE + Auth.FORGOT_PASSWORD,
            Auth.BASE + Auth.RESET_PASSWORD,
    };

    /** The public endpoints that create rows or send mail; verify/reset consume a secret and need no limit. */
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            Auth.BASE + Auth.REGISTER,
            Auth.BASE + Auth.RESEND_VERIFICATION,
            Auth.BASE + Auth.FORGOT_PASSWORD);

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ApiErrorWriter errorWriter, Clock clock,
            @Value("${app.security.csrf.secure-cookie:true}") boolean secureCookie,
            CsrfTokenRepository csrfTokenRepository,
            @Value("${app.security.registration.max-attempts:10}") int maxAttempts,
            @Value("${app.security.registration.window-seconds:3600}") long windowSeconds, JwtDecoder jwtDecoder,
            SessionJwtAuthenticationConverter sessionJwtAuthenticationConverter)
            throws Exception {

        return http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        authorize -> authorize.dispatcherTypeMatchers(DispatcherType.ERROR)
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, Auth.BASE + Auth.CSRF)
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, Auth.BASE + Auth.REGISTER)
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, PUBLIC_EMAIL_FLOW_PATHS)
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, Auth.BASE + Auth.LOGIN)
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, Auth.BASE + Auth.REFRESH)
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, Auth.BASE + Auth.LOGOUT)
                                .permitAll()
                                // System-wide role: the filter chain has all the information it needs,
                                // unlike tenant roles, which depend on the organization in the route.
                                .requestMatchers(ApiPaths.Admin.PATTERN)
                                .hasRole("SYSTEM_ADMIN")
                                .anyRequest()
                                .authenticated())
                .exceptionHandling(
                        errors -> errors
                                .authenticationEntryPoint(
                                        (request, response, exception) -> errorWriter.write(
                                                response, 401, ApiErrorCodes.UNAUTHENTICATED,
                                                ClientMessages.AUTHENTICATION_REQUIRED))
                                .accessDeniedHandler((request, response, exception) -> {
                                    if (exception instanceof CsrfException) {
                                        errorWriter.write(response, 403,
                                                ApiErrorCodes.CSRF_INVALID,
                                                ClientMessages.CSRF_TOKEN_REQUIRED);
                                    } else {
                                        errorWriter.write(response, 403, ApiErrorCodes.FORBIDDEN,
                                                ClientMessages.ACCESS_DENIED);
                                    }
                                }))
                .addFilterAfter(
                        new PublicAuthRateLimitFilter(clock, errorWriter, RATE_LIMITED_PATHS, maxAttempts,
                                Duration.ofSeconds(windowSeconds)),
                        CsrfFilter.class)
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .jwt(jwt -> jwt.decoder(jwtDecoder)
                                        .jwtAuthenticationConverter(
                                                sessionJwtAuthenticationConverter))
                                .authenticationEntryPoint((request, response, exception) -> {
                                    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

                                    errorWriter.write(response, 401,
                                            ApiErrorCodes.UNAUTHENTICATED,
                                            ClientMessages.AUTHENTICATION_REQUIRED);
                                })
                                .accessDeniedHandler(
                                        (request, response, exception) -> errorWriter.write(response, 403,
                                                ApiErrorCodes.FORBIDDEN,
                                                ClientMessages.ACCESS_DENIED)))

                .build();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository(
            @Value("${app.security.csrf.secure-cookie:true}") boolean secureCookie) {

        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();

        repository.setHeaderName("X-CSRF-TOKEN");

        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/"));

        return repository;
    }
}
