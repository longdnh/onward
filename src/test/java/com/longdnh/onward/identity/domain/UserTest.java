package com.longdnh.donot_get_left_behind.identity.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserTest {

    private static final UUID ID = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-10-20T00:00:00Z");

    @Test
    void canonicalizesEmailWithoutChangingTheHashOrExposingItInLogs() {
        User user = User.create(ID, "  Alice.Smith+Research@Example.test  ", "Encoded-HASH", NOW);

        assertEquals("alice.smith+research@example.test", user.getEmail());
        assertEquals("Encoded-HASH", user.getPasswordHash());
        assertTrue(user.isActive());
        assertFalse(user.toString().contains("Encoded-HASH"));
    }

    @Test
    void enforcesEmailLengthAfterCanonicalization() {
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63)
                + "." + "d".repeat(56) + ".test";
        assertEquals(254, email.length());
        User user = User.create(ID, "  " + email.toUpperCase(Locale.ROOT) + "  ", "hash", NOW);
        assertEquals(email, user.getEmail());
        assertThrows(IllegalArgumentException.class, () -> User.create(ID, email + "x", "hash", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new User(ID, email + "x", "hash", User.UserStatus.ACTIVE, NOW, NOW));

        // Lowercasing U+0130 produces two code points, so the normalized value exceeds the limit.
        String expandsWhenLowercased = "\u0130" + email.substring(1);
        assertEquals(254, expandsWhenLowercased.codePointCount(0, expandsWhenLowercased.length()));
        assertThrows(IllegalArgumentException.class,
                () -> User.create(ID, expandsWhenLowercased, "hash", NOW));
    }

    @Test
    void rejectsWrongUuidVersionAndVariant() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create(UUID.fromString("019a0000-0000-4000-8000-000000000001"), "a@b.test", "hash", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> User.create(UUID.fromString("019a0000-0000-7000-0000-000000000001"), "a@b.test", "hash", NOW));
    }

    @Test
    void rejectsBlankCredentialsAndReversedTimestamps() {
        assertThrows(IllegalArgumentException.class, () -> User.create(ID, " \t ", "hash", NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create(ID, "a@b.test", "\t", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new User(ID, "a@b.test", "hash", User.UserStatus.ACTIVE, NOW, NOW.minusSeconds(1)));
    }
}
