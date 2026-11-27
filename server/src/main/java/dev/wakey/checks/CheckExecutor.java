package dev.wakey.checks;

/**
 * One protocol the server can measure. A separate bean exists for every check type, and the registry
 * finds it by {@link CheckType} without a switch. The same executor describes the fields its type
 * needs in the check form.
 */
public interface CheckExecutor {

    /** The check type this executor handles. */
    CheckType type();

    /** Runs one measurement of the check. The caller owns scheduling and storing the result. */
    CheckResult execute(Check check);

    /** The fields a frontend renders for this check type. */
    CheckFormDescriptor formDescriptor();
}
