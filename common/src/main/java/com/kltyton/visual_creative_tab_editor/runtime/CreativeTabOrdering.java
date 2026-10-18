package com.kltyton.visual_creative_tab_editor.runtime;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.CreativeModeTab;

/** Stable configured order with the original native order as the tie breaker. */
public final class CreativeTabOrdering {
    private CreativeTabOrdering() {}


    public static List<CreativeModeTab> ordered(List<CreativeModeTab> input) {
        if (CreativeTabRuntime.catalog().isEmpty() || input.size() < 2) {
            return input;
        }
        List<CreativeModeTab> ordered = new ArrayList<>(input);
        Map<CreativeModeTab, Integer> nativeOrder = new HashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            nativeOrder.put(ordered.get(index), index);
        }
        Map<ResourceId, Integer> configuredOrder = new HashMap<>();
        var definitions = CreativeTabRuntime.catalog().orderedDefinitions();
        for (int index = 0; index < definitions.size(); index++) {
            configuredOrder.put(definitions.get(index).id(), index);
        }
        ordered.sort(java.util.Comparator
                .comparingInt((CreativeModeTab tab) -> CreativeTabRuntime.id(tab)
                        .map(id -> configuredOrder.getOrDefault(id, Integer.MAX_VALUE))
                        .orElse(Integer.MAX_VALUE))
                .thenComparingInt(tab -> nativeOrder.getOrDefault(tab, Integer.MAX_VALUE)));
        return List.copyOf(ordered);
    }
    public static Optional<CreativeModeTab> defaultTab(Supplier<List<CreativeModeTab>> source) {
        if (CreativeTabRuntime.catalog().isEmpty()) { return Optional.empty(); }
        List<CreativeModeTab> visible = source.get();
        return visible.isEmpty() ? Optional.empty() : Optional.of(visible.get(0));
    }
}
