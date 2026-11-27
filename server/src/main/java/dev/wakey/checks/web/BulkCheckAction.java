package dev.wakey.checks.web;

import dev.wakey.checks.CheckBulkAction;

/** The bulk action as the API names it; it mirrors the domain {@link CheckBulkAction}. */
public enum BulkCheckAction {
    PAUSE,
    RESUME,
    DELETE;

    CheckBulkAction toDomain() {
        return CheckBulkAction.valueOf(name());
    }
}
