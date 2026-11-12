package com.sms.common.web;

/** A downstream dependency (another service, blob storage) failed. Mapped to 502. */
public class UpstreamException extends RuntimeException {
    public UpstreamException(String message) {
        super(message);
    }

    public UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
