package com.longdnh.onward.identity.application.port;

/** Generates the raw secret that goes into an email link and the SHA-256 hex hash that is stored. */
public interface OneTimeTokenGenerator {

    String generate();

    String hash(String rawToken);
}
