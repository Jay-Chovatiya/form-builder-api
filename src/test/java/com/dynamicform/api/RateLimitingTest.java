package com.dynamicform.api;

import com.dynamicform.api.common.exception.RateLimitExceededException;
import com.dynamicform.api.common.ratelimit.RateLimit;
import com.dynamicform.api.common.ratelimit.RateLimitInterceptor;
import com.dynamicform.api.common.ratelimit.RateLimiterService;
import com.dynamicform.api.config.RateLimiterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingTest {

    private RateLimiterProperties properties;
    private RateLimiterService rateLimiterService;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new RateLimiterProperties();
        properties.setEnabled(true);
        properties.getSubmission().setLimit(5);
        properties.getSubmission().setWindowSeconds(60);
        properties.getUpload().setLimit(10);
        properties.getUpload().setWindowSeconds(60);
        properties.getLogin().setLimit(10);
        properties.getLogin().setWindowSeconds(60);

        rateLimiterService = new RateLimiterService(properties);
        rateLimiterService.reset();
        interceptor = new RateLimitInterceptor(rateLimiterService);
    }

    static class DummyController {
        @RateLimit("submission")
        public void submit() {}

        @RateLimit("upload")
        public void upload() {}
    }

    @Test
    @DisplayName("Submission rate limit permits 5 requests then blocks the 6th")
    void testSubmissionLimitBlocksAfter5Requests() throws Exception {
        Method method = DummyController.class.getMethod("submit");
        HandlerMethod handler = new HandlerMethod(new DummyController(), method);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        // First 5 requests should pass
        for (int i = 1; i <= 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            boolean allowed = interceptor.preHandle(request, response, handler);
            assertTrue(allowed, "Request " + i + " should be permitted");
            assertEquals("5", response.getHeader("X-RateLimit-Limit"));
            assertEquals(String.valueOf(5 - i), response.getHeader("X-RateLimit-Remaining"));
        }

        // 6th request should throw RateLimitExceededException
        MockHttpServletResponse response = new MockHttpServletResponse();
        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, handler)
        );

        assertTrue(ex.getRetryAfterSeconds() > 0);
        assertEquals(5, ex.getLimit());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertNotNull(response.getHeader("Retry-After"));
    }

    @Test
    @DisplayName("Different IP addresses maintain separate rate limit buckets")
    void testDifferentIpsHaveIsolatedBuckets() throws Exception {
        Method method = DummyController.class.getMethod("submit");
        HandlerMethod handler = new HandlerMethod(new DummyController(), method);

        MockHttpServletRequest ip1Request = new MockHttpServletRequest();
        ip1Request.setRemoteAddr("10.0.0.1");

        MockHttpServletRequest ip2Request = new MockHttpServletRequest();
        ip2Request.setRemoteAddr("10.0.0.2");

        // IP 1 uses all 5 permits
        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(ip1Request, new MockHttpServletResponse(), handler));
        }

        // IP 1 is now blocked
        assertThrows(RateLimitExceededException.class,
                () -> interceptor.preHandle(ip1Request, new MockHttpServletResponse(), handler));

        // IP 2 must still be allowed because it has a separate bucket
        assertTrue(interceptor.preHandle(ip2Request, new MockHttpServletResponse(), handler));
    }

    @Test
    @DisplayName("Client IP is properly resolved from X-Forwarded-For header")
    void testResolveIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");
        request.setRemoteAddr("127.0.0.1");

        String resolvedIp = rateLimiterService.resolveClientIp(request);
        assertEquals("203.0.113.195", resolvedIp);
    }

    @Test
    @DisplayName("When rate limiting is disabled, all requests are allowed")
    void testDisabledRateLimitingAllowsAll() throws Exception {
        properties.setEnabled(false);

        Method method = DummyController.class.getMethod("submit");
        HandlerMethod handler = new HandlerMethod(new DummyController(), method);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");

        // Even after 10 requests, none should be blocked
        for (int i = 0; i < 10; i++) {
            assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
        }
    }
}
