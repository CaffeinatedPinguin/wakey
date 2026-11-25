package dev.wakey.web;

import java.util.List;

/** A module's contribution to the error-code enum of the public contract. */
public interface ErrorCodes {

    List<? extends ErrorCode> codes();
}
