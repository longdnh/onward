package com.longdnh.onward.identity.application.dto;

import java.util.Objects;

/** Plain-text email. toString omits the body because it may carry a secret link. */
public record EmailMessage(String to, String subject, String textBody) {

    public EmailMessage {
        Objects.requireNonNull(to, "to is required");
        Objects.requireNonNull(subject, "subject is required");
        Objects.requireNonNull(textBody, "textBody is required");
    }

    @Override
    public String toString() {
        return "EmailMessage[to=" + to + ", subject=" + subject + "]";
    }
}
