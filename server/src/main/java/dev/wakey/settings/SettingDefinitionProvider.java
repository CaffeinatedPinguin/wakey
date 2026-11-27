package dev.wakey.settings;

import java.util.List;

/** A module that contributes its settings to the one catalog; the catalog collects every provider. */
public interface SettingDefinitionProvider {

    /** The definitions owned by one module. */
    List<SettingDefinition> definitions();
}
