package dev.wakey.checks;

/** One action a bulk request applies to every listed check of a space. */
public enum CheckBulkAction {
    PAUSE,
    RESUME,
    DELETE
}
