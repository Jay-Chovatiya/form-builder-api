package com.dynamicform.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "formstudio.rate-limiting")
public class RateLimiterProperties {

    private boolean enabled = true;
    private PolicyConfig submission = new PolicyConfig();
    private PolicyConfig upload = new PolicyConfig();
    private PolicyConfig login = new PolicyConfig();

    @Getter
    @Setter
    public static class PolicyConfig {
        private int limit;
        private long windowSeconds;
    }
}
