package dev.wakey.checks;

/** One write of a check as the audit log sees it. */
record CheckChange(String action, String details) {}
