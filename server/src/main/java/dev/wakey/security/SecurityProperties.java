package dev.wakey.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Instance settings for the Admin role mapping, the post-login redirect and the absolute session limit. */
@ConfigurationProperties(prefix = "wakey.security")
public record SecurityProperties(String adminClaim, String adminGroup, String frontendUrl, Duration sessionMaxAge) {}
