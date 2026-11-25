package dev.wakey.checks.internal;

/** The one ordering that defines the newest result of a check. */
final class LatestResultOrder {

    static final String NEWEST_FIRST = "checked_at DESC, id DESC";

    private LatestResultOrder() {}
}
