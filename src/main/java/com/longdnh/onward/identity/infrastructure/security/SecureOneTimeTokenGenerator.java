package com.longdnh.onward.identity.infrastructure.security;

import org.springframework.stereotype.Component;

import com.longdnh.onward.identity.application.port.OneTimeTokenGenerator;

@Component
public final class SecureOneTimeTokenGenerator implements OneTimeTokenGenerator {

    @Override
    public String generate() {
        return SecureTokens.generate();
    }

    @Override
    public String hash(String rawToken) {
        return SecureTokens.sha256Hex(rawToken);
    }
}
