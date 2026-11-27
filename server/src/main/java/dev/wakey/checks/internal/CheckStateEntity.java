package dev.wakey.checks.internal;

import dev.wakey.checks.CheckState;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** The state column of a check, updated without touching the edit version of the aggregate. */
@Table("check")
public record CheckStateEntity(@Id UUID id, CheckState lastState) {}
