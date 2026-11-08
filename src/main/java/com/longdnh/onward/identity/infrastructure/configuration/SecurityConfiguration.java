package com.longdnh.donot_get_left_behind.identity.infrastructure.configuration;

import com.longdnh.donot_get_left_behind.common.Exception.ApiErrorWriter;
import com.longdnh.donot_get_left_behind.common.constants.ApiErrorCodes;
import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;
import com.longdnh.donot_get_left_behind.identity.infrastructure.security.RegistrationRateLimitFilter;
import com.longdnh.donot_get_left_behind.identity.infrastructure.security.SessionJwtAuthenticationConverter;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.time.Duration;
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
                                .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/register")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/login")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/refresh")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                                .permitAll()
                                // System-wide role: the filter chain has all the information it needs,
                                // unlike tenant roles, which depend on the organization in the route.
                                .requestMatchers("/api/admin/**")
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
                        new RegistrationRateLimitFilter(clock, errorWriter, maxAttempts,
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
