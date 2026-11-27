package dev.wakey.settings;

/** A space's plan after resolving the instance default: the name and the configured bounds. */
public record SpacePlan(String name, PlanProperties properties) {}
