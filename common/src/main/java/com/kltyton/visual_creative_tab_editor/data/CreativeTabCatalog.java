package com.kltyton.visual_creative_tab_editor.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Immutable, fully resolved creative-tab catalog for one resource-pack view. */
public final class CreativeTabCatalog {
    public static final CreativeTabCatalog EMPTY = new CreativeTabCatalog(List.of(), false);

    private final Map<ResourceLocation, CreativeTabDefinition> definitions;
    private final List<CreativeTabDefinition> ordered;

    public CreativeTabCatalog(Collection<CreativeTabDefinition> definitions) {
        this(definitions, true);
    }

    private CreativeTabCatalog(Collection<CreativeTabDefinition> definitions, boolean requireVisibleCategory) {
        Objects.requireNonNull(definitions, "definitions");
        Map<ResourceLocation, CreativeTabDefinition> byId = new LinkedHashMap<>();
        for (CreativeTabDefinition definition : definitions) {
            CreativeTabDefinition previous = byId.put(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate creative tab id: " + definition.id());
            }
        }
        List<CreativeTabDefinition> sorted = new ArrayList<>(byId.values());
        sorted.sort(Comparator.comparingInt(CreativeTabDefinition::order)
                .thenComparing(definition -> definition.id().toString()));
        if (requireVisibleCategory) {
            CreativeTabValidation.validateAll(sorted);
        }
        this.definitions = Map.copyOf(byId);
        this.ordered = List.copyOf(sorted);
    }

    public Optional<CreativeTabDefinition> definition(ResourceLocation id) {
        return Optional.ofNullable(this.definitions.get(id));
    }

    public List<CreativeTabDefinition> orderedDefinitions() {
        return this.ordered;
    }

    public Map<ResourceLocation, CreativeTabDefinition> definitions() {
        return this.definitions;
    }

    public boolean isEmpty() {
        return this.definitions.isEmpty();
    }
}
