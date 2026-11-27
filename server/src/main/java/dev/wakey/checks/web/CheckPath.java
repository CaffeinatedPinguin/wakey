package dev.wakey.checks.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;

/** The path parameters of one check, bound as one object to stay within the parameter limit. */
@ParameterObject
record CheckPath(
        @Parameter(in = ParameterIn.PATH, required = true) String slug,
        @Parameter(in = ParameterIn.PATH, required = true) UUID id) {}
