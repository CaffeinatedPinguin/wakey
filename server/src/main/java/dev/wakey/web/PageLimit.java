package dev.wakey.web;

/** The page size of a list request, resolved from the {@code limit} query parameter. */
public record PageLimit(int value) {}
