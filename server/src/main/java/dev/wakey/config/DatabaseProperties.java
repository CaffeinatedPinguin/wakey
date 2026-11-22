package dev.wakey.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wakey.database")
// postgres-only: both values are PostgreSQL server settings.
record DatabaseProperties(Duration idleInTransactionTimeout, Duration lockTimeout) {}
