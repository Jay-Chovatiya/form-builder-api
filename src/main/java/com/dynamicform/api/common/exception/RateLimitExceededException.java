package com.dynamicform.api.common.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final int limit;

    public RateLimitExceededException(long retryAfterSeconds, int limit, String message) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.limit = limit;
    }
}
