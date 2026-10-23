package com.dynamicform.api.common.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResult {

    private final boolean allowed;
    private final String policy;
    private final int limit;
    private final int remaining;
    private final long retryAfterSeconds;

    public static RateLimitResult allowed(String policy, int limit, int remaining) {
        return new RateLimitResult(true, policy, limit, Math.max(0, remaining), 0);
    }

    public static RateLimitResult denied(String policy, int limit, long retryAfterSeconds) {
        return new RateLimitResult(false, policy, limit, 0, Math.max(1, retryAfterSeconds));
    }
}
