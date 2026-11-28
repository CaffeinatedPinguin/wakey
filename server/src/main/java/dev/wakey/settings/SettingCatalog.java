package dev.wakey.settings;

import dev.wakey.web.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The single catalog table, assembled from the definitions every module contributes. */
public final class SettingCatalog {

    private final List<SettingDefinition> definitions;

    private final Map<String, SettingDefinition> definitionsByKey;

    public SettingCatalog(List<SettingDefinitionProvider> providers) {
        this.definitions = providers.stream()
                .flatMap(provider -> provider.definitions().stream())
                .toList();
        this.definitionsByKey = indexDefinitions(definitions);
    }

    public List<SettingDefinition> all() {
        return definitions;
    }

    /** Looks up a definition; an unknown key is a domain error, never a silent fallback. */
    public SettingDefinition require(String key) {
        SettingDefinition definition = definitionsByKey.get(key);
        if (definition == null) {
            throw ApiException.invalidField(SettingError.INVALID_KEY, "/key");
        }
        return definition;
    }

    private static Map<String, SettingDefinition> indexDefinitions(List<SettingDefinition> definitions) {
        Map<String, SettingDefinition> indexed = new LinkedHashMap<>();
        for (SettingDefinition definition : definitions) {
            SettingKey key = definition.key();
            indexed.put(key.key(), definition);
        }
        return Map.copyOf(indexed);
    }
}
