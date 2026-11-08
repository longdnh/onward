package com.longdnh.onward.identity.application.port;

public interface  RefreshTokenGenerator {
    String generate();

    String hash(String rawToken);
}
