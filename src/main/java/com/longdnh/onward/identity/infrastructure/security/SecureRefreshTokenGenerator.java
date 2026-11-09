package com.longdnh.onward.identity.infrastructure.security;

import org.springframework.stereotype.Component;

import com.longdnh.onward.identity.application.port.RefreshTokenGenerator;

@Component
public final class SecureRefreshTokenGenerator implements RefreshTokenGenerator {

    @Override
    public String generate() {
        return SecureTokens.generate();
    }

    @Override
    public String hash(String rawToken) {
        return SecureTokens.sha256Hex(rawToken);
    }
}
