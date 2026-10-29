package com.dynamicform.api.common.ratelimit;

import com.dynamicform.api.config.RateLimiterProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimiterProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitResult checkRateLimit(HttpServletRequest request, RateLimit rateLimit) {
        if (properties == null || !properties.isEnabled()) {
            return RateLimitResult.allowed("disabled", Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        String policy = resolvePolicy(rateLimit);
        RateLimiterProperties.PolicyConfig config = getPolicyConfig(policy);

        String clientIp = resolveClientIp(request);
        String bucketKey = clientIp + ":" + policy;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createNewBucket(config.getLimit(), config.getWindowSeconds()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitResult.allowed(policy, config.getLimit(), (int) probe.getRemainingTokens());
        } else {
            long nanos = probe.getNanosToWaitForRefill();
            long retryAfterSeconds = Math.max(1, (long) Math.ceil(nanos / 1_000_000_000.0));
            return RateLimitResult.denied(policy, config.getLimit(), retryAfterSeconds);
        }
    }

    public String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            if (ips.length > 0 && !ips[0].trim().isBlank()) {
                return ips[0].trim();
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }

    public void reset() {
        buckets.clear();
    }

    private Bucket createNewBucket(int limit, long windowSeconds) {
        int safeLimit = Math.max(1, limit);
        long safeWindowSeconds = Math.max(1L, windowSeconds);

        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(safeLimit)
                .refillIntervally(safeLimit, Duration.ofSeconds(safeWindowSeconds))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private String resolvePolicy(RateLimit rateLimit) {
        if (rateLimit != null && !rateLimit.value().isBlank()) {
            return rateLimit.value().trim().toLowerCase();
        }
        return "submission";
    }

    private RateLimiterProperties.PolicyConfig getPolicyConfig(String policy) {
        RateLimiterProperties.PolicyConfig config = switch (policy) {
            case "upload" -> properties.getUpload();
            case "login" -> properties.getLogin();
            default -> properties.getSubmission();
        };

        if (config == null || config.getLimit() <= 0) {
            RateLimiterProperties.PolicyConfig fallback = new RateLimiterProperties.PolicyConfig();
            fallback.setLimit(10);
            fallback.setWindowSeconds(60);
            return fallback;
        }

        return config;
    }
}
