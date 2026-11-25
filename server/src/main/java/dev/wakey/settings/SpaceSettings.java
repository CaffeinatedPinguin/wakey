package dev.wakey.settings;

import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The settings port: the effective value of a space setting inside a plan, and the space overrides
 * accepted only within that plan. A caller passes the assigned plan name, possibly null for the
 * instance default; the port resolves and validates it.
 */
public interface SpaceSettings {

    /** Reads one effective value: a space override, a plan value, an instance default or the catalog default. */
    EffectiveSetting effective(UUID spaceId, @Nullable String planName, String key);

    /** Reads every catalog setting of a space, each with its source. */
    Map<String, EffectiveSetting> effectiveAll(UUID spaceId, @Nullable String planName);

    /** Stores a space override; a value outside the plan bound or a bad format is rejected. */
    void set(UUID spaceId, @Nullable String planName, String key, String value);

    /** Removes a space override, falling back to the plan, instance or catalog value. */
    void reset(UUID spaceId, String key);

    /** Checks that a plan exists and returns its name; an unknown name is a domain error. */
    String requireExisting(String planName);

    /** Resolves an assigned plan name; a null name means the instance default plan. */
    String resolvePlanName(@Nullable String planName);

    /** The resolved plan with its bounds; a null name means the instance default plan. */
    PlanProperties plan(@Nullable String planName);
}
