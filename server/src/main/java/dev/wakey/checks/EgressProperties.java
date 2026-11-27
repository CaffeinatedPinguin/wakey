package dev.wakey.checks;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The instance egress switches from {@code wakey.egress.*}. The default lives in
 * {@code application.yml}; this record only binds what the operator wrote.
 */
@ConfigurationProperties(prefix = "wakey.egress")
public record EgressProperties(boolean allowPrivateNetworks) {}
