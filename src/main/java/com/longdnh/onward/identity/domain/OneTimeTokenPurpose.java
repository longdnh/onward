package com.longdnh.onward.identity.domain;

/** What a one-time token may be exchanged for. A token is only valid for the purpose it was issued with. */
public enum OneTimeTokenPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET
}
