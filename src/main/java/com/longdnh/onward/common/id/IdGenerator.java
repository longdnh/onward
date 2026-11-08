package com.longdnh.donot_get_left_behind.common.id;

import java.util.UUID;

/** Technical port shared by every module; implemented in common.id. */
public interface IdGenerator {

    /** Generates a new RFC 9562 UUIDv7 identifier. */
    UUID generate();
}
