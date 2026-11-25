package dev.wakey.settings;

import java.util.Map;
import java.util.UUID;

/**
 * The settings port: the effective value of a space setting inside a plan, and the space overrides
 * accepted only within that plan. The assignment of a plan to a space is a separate concern; a
 * caller passes the plan name explicitly.
 */
public interface SpaceSettings {

    /** Reads one effective value: a space override, a plan value, an instance default or the catalog default. */
    EffectiveSetting effective(UUID spaceId, String planName, String key);

    /** Reads every catalog setting of a space, each with its source. */
    Map<String, EffectiveSetting> effectiveAll(UUID spaceId, String planName);

    /** Stores a space override; a value outside the plan bound or a bad format is rejected. */
    void set(UUID spaceId, String planName, String key, String value);

    /** Removes a space override, falling back to the plan, instance or catalog value. */
    void reset(UUID spaceId, String key);
}
