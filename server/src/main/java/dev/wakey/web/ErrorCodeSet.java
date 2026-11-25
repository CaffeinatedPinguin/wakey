package dev.wakey.web;

import java.util.List;

/** One module's contribution to the error codes of the public contract. */
public record ErrorCodeSet(List<? extends ErrorCode> codes) {}
