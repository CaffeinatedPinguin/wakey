package dev.wakey.web;

import java.util.Map;

/** One entry of the {@code errors[]} array: a pointer to the field and the rule it broke. */
public record ApiFieldError(String pointer, String code, Map<String, Object> params) {}
