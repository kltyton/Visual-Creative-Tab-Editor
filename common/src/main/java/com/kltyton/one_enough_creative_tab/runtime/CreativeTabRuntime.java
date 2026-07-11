package com.kltyton.one_enough_creative_tab.runtime;

import com.kltyton.one_enough_creative_tab.data.CreativeTabCatalog;
import com.kltyton.one_enough_creative_tab.data.CreativeTabDefinition;
import com.kltyton.one_enough_creative_tab.data.CreativeTabType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/** Thread-safe runtime projection of the current server-authoritative tab catalog. */
public final class CreativeTabRuntime {
    private static final AtomicReference<State> ACTIVE = new AtomicReference<>(State.empty());
    private static final AtomicReference<Map<Identifier, CreativeModeTab>> PREVIEW_HANDOFF = new AtomicReference<>();
    private static final ThreadLocal<State> PREVIEW = new ThreadLocal<>();
    private static final ThreadLocal<Integer> NATIVE_BYPASS = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<CreativeModeTab> FORCED_VISIBLE_TAB = new ThreadLocal<>();

    private CreativeTabRuntime() {
    }

    public static void install(CreativeTabCatalog catalog) {
        Map<Identifier, CreativeModeTab> handoff = PREVIEW_HANDOFF.getAndSet(null);
        ACTIVE.updateAndGet(previous -> State.create(catalog, previous, handoff));
        CreativeModeTabs.CACHED_PARAMETERS = null;
    }

    public static void clear() {
        PREVIEW.remove();
        PREVIEW_HANDOFF.set(null);
        ACTIVE.set(State.empty());
        CreativeModeTabs.CACHED_PARAMETERS = null;
    }

    public static CreativeTabCatalog catalog() {
        State preview = PREVIEW.get();
        return preview != null ? preview.catalog : ACTIVE.get().catalog;
    }

    public static void setPreview(CreativeTabCatalog preview) {
        State previous = PREVIEW.get();
        PREVIEW.set(State.create(Objects.requireNonNull(preview, "preview"), previous != null ? previous : ACTIVE.get()));
        CreativeModeTabs.CACHED_PARAMETERS = null;
    }

    public static void clearPreview() {
        PREVIEW.remove();
        PREVIEW_HANDOFF.set(null);
        CreativeModeTabs.CACHED_PARAMETERS = null;
    }

    /** Makes custom preview-tab identities available to the next server snapshot. */
    public static void preparePreviewHandoff() {
        State preview = PREVIEW.get();
        PREVIEW_HANDOFF.set(preview == null ? null : preview.runtimeTabs);
    }

    /** Drops identities prepared for a save that the server rejected. */
    public static void discardPreviewHandoff() {
        PREVIEW_HANDOFF.set(null);
    }

    public static boolean isNativeBypass() {
        return NATIVE_BYPASS.get() > 0;
    }

    public static boolean isForcedVisible(CreativeModeTab tab) {
        return FORCED_VISIBLE_TAB.get() == tab;
    }

    public static void withForcedVisibility(CreativeModeTab tab, Runnable action) {
        CreativeModeTab previous = FORCED_VISIBLE_TAB.get();
        FORCED_VISIBLE_TAB.set(Objects.requireNonNull(tab, "tab"));
        try {
            action.run();
        } finally {
            if (previous == null) {
                FORCED_VISIBLE_TAB.remove();
            } else {
                FORCED_VISIBLE_TAB.set(previous);
            }
        }
    }

    public static <T> T withNativeBypass(Supplier<T> action) {
        NATIVE_BYPASS.set(NATIVE_BYPASS.get() + 1);
        try {
            return action.get();
        } finally {
            int depth = NATIVE_BYPASS.get() - 1;
            if (depth == 0) {
                NATIVE_BYPASS.remove();
            } else {
                NATIVE_BYPASS.set(depth);
            }
        }
    }

    public static Optional<Identifier> id(CreativeModeTab tab) {
        Identifier registered = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (registered != null) {
            return Optional.of(registered);
        }
        State preview = PREVIEW.get();
        Identifier previewId = preview == null ? null : preview.idsByRuntimeTab.get(tab);
        return Optional.ofNullable(previewId != null ? previewId : ACTIVE.get().idsByRuntimeTab.get(tab));
    }

    public static Optional<CreativeTabDefinition> definition(CreativeModeTab tab) {
        if (isNativeBypass()) {
            return Optional.empty();
        }
        return id(tab).flatMap(catalog()::definition);
    }

    public static Stream<CreativeModeTab> effectiveTabs(Stream<CreativeModeTab> nativeTabs) {
        if (isNativeBypass() || catalog().isEmpty()) {
            return nativeTabs;
        }
        List<CreativeModeTab> nativeList = nativeTabs.toList();
        Map<Identifier, CreativeModeTab> byId = new LinkedHashMap<>();
        for (CreativeModeTab tab : nativeList) {
            Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null) {
                byId.put(id, tab);
            }
        }
        State preview = PREVIEW.get();
        byId.putAll((preview != null ? preview : ACTIVE.get()).runtimeTabs);

        List<CreativeModeTab> result = new ArrayList<>();
        for (CreativeTabDefinition definition : catalog().orderedDefinitions()) {
            CreativeModeTab tab = byId.remove(definition.id());
            if (tab != null) {
                result.add(tab);
            }
        }
        result.addAll(byId.values());
        return result.stream();
    }

    public static void rebuildSearchContents() {
        if (isNativeBypass() || catalog().isEmpty()) {
            return;
        }
        CreativeModeTab search = CreativeModeTabs.searchTab();
        Collection<ItemStack> display = search.getDisplayItems();
        Collection<ItemStack> searchable = search.getSearchTabDisplayItems();
        display.clear();
        searchable.clear();
        Map<StackKey, ItemStack> aggregate = new LinkedHashMap<>();
        effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream()).forEach(tab -> {
            if (tab != search && definition(tab).map(definition -> !definition.hidden()).orElse(true)) {
                for (ItemStack stack : tab.getSearchTabDisplayItems()) {
                    aggregate.putIfAbsent(StackKey.of(stack), stack);
                }
            }
        });
        List<ItemStack> ordered = new ArrayList<>(aggregate.size());
        definition(search).ifPresent(definition -> {
            for (ItemStack preferred : definition.items()) {
                ItemStack present = aggregate.remove(StackKey.of(preferred));
                if (present != null) {
                    ordered.add(present);
                }
            }
        });
        ordered.addAll(aggregate.values());
        display.addAll(ordered);
        searchable.addAll(ordered);
    }

    private record State(
            CreativeTabCatalog catalog,
            Map<Identifier, CreativeModeTab> runtimeTabs,
            Map<CreativeModeTab, Identifier> idsByRuntimeTab
    ) {
        private static State empty() {
            return new State(CreativeTabCatalog.EMPTY, Map.of(), Map.of());
        }

        @SuppressWarnings("deprecation")
        private static State create(CreativeTabCatalog catalog, State previous) {
            return create(catalog, previous, null);
        }

        private static State create(
                CreativeTabCatalog catalog,
                State previous,
                Map<Identifier, CreativeModeTab> handoff
        ) {
            Map<Identifier, CreativeModeTab> runtime = new LinkedHashMap<>();
            Map<CreativeModeTab, Identifier> reverse = new IdentityHashMap<>();
            for (CreativeTabDefinition definition : catalog.orderedDefinitions()) {
                if (BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(definition.id()).isPresent()
                        || definition.type() != CreativeTabType.CATEGORY) {
                    continue;
                }
                CreativeModeTab tab = previous.runtimeTabs.get(definition.id());
                if (tab == null && handoff != null) {
                    tab = handoff.get(definition.id());
                }
                if (tab == null) {
                    tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .title(definition.title())
                            .icon(definition::icon)
                            .build();
                }
                runtime.put(definition.id(), tab);
                reverse.put(tab, definition.id());
            }
            return new State(catalog, Map.copyOf(runtime), Map.copyOf(reverse));
        }
    }

    private record StackKey(Item item, DataComponentMap components) {
        private static StackKey of(ItemStack stack) {
            return new StackKey(stack.getItem(), stack.getComponents());
        }
    }
}
