package com.longdnh.onward.identity.application.port;

import com.longdnh.onward.identity.application.dto.EmailMessage;

/**
 * Outbound port for email. Implementations must never be called inside a database transaction:
 * sending cannot be rolled back, so callers publish an event and send after commit.
 */
public interface EmailSender {

    void send(EmailMessage message);
}
