package com.longdnh.donot_get_left_behind.identity.application.port;

public interface  RefreshTokenGenerator {
    String generate();

    String hash(String rawToken);
}
