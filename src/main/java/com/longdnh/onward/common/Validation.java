package com.longdnh.donot_get_left_behind.common;

import java.util.Objects;
import java.util.UUID;

/** Shared value checks with no dependencies on application modules or frameworks. */
public final class Validation {

    private Validation() {
    }

    public static UUID requireUuidV7(UUID id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " is required");
        if (id.version() != 7 || id.variant() != 2) {
            throw new IllegalArgumentException(fieldName + " must be an RFC 9562 UUIDv7");
        }
        return id;
    }

    /** Validates without trimming or otherwise transforming the value. */
    public static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
