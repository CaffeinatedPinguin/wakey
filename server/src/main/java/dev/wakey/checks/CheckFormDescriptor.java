package dev.wakey.checks;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The form a frontend renders for one check type. A field names the check property, the control
 * that edits it and the bounds the server applies.
 */
public record CheckFormDescriptor(CheckType type, List<Field> fields) {

    /** The control a form renders for a field. */
    public enum FieldType {
        URL,
        INTEGER
    }

    /** One editable field: its check property, control, whether it is required and its bounds. */
    public record Field(
            String name,
            FieldType type,
            boolean required,
            @Nullable Long min,
            @Nullable Long max,
            @Nullable String unit) {}
}
