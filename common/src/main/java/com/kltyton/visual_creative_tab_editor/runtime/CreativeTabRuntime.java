package com.kltyton.visual_creative_tab_editor.runtime;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalog;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.HashSet;
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
    private static final AtomicReference<Boolean> LAST_HAS_PERMISSIONS = new AtomicReference<>();

    private CreativeTabRuntime() {
    }

    public static void install(CreativeTabCatalog catalog) {
        Map<Identifier, CreativeModeTab> handoff = PREVIEW_HANDOFF.getAndSet(null);
        ACTIVE.updateAndGet(previous -> State.create(catalog, previous, handoff));
        CreativeModeTabs.CACHED_PARAMETERS = null;
        refreshKnownContents();
    }

    public static void clear() {
        PREVIEW.remove();
        PREVIEW_HANDOFF.set(null);
        ACTIVE.set(State.empty());
        LAST_HAS_PERMISSIONS.set(null);
        CreativeModeTabs.CACHED_PARAMETERS = null;
    }

    public static CreativeTabCatalog catalog() {
        State preview = PREVIEW.get();
        return preview != null ? preview.catalog : ACTIVE.get().catalog;
    }

    public static void setPreview(CreativeTabCatalog preview) {
        State previous = PREVIEW.get();
        State baseline = previous != null ? previous : ACTIVE.get();
        PREVIEW.set(State.create(Objects.requireNonNull(preview, "preview"), baseline));
        refreshKnownContents(baseline);
    }

    public static void clearPreview() {
        State previous = PREVIEW.get();
        PREVIEW.remove();
        PREVIEW_HANDOFF.set(null);
        refreshKnownContents(previous);
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

    /** Records the client screen's exact operator-tab visibility decision across render/reload threads. */
    public static boolean hasClientPermissions() {
        return Boolean.TRUE.equals(LAST_HAS_PERMISSIONS.get());
    }

    public static void rememberClientPermissions(boolean hasPermissions) {
        Boolean previous = LAST_HAS_PERMISSIONS.getAndSet(hasPermissions);
        if (previous != null && previous != hasPermissions) {
            CreativeModeTabs.CACHED_PARAMETERS = null;
        }
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
            for (int index = 0; index < definition.itemCount(); index++) {
                ItemStack preferred = definition.itemAt(index);
                ItemStack present = aggregate.remove(StackKey.of(preferred));
                if (present != null) {
                    ordered.add(present);
                }
            }
        });
        ordered.addAll(aggregate.values());
        ((CreativeTabContents) search).visualCreativeTabEditor$replaceContents(ordered, ordered);
    }

    /**
     * Projects the active catalog directly into vanilla tab collections.
     * This deliberately bypasses {@link CreativeModeTab#buildContents} because
     * performance mods may memoize that complete method and skip Mixin hooks.
     */
    public static void refreshContents(CreativeModeTab.ItemDisplayParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        applyCatalogContents(parameters.hasPermissions());
        rebuildSearchContents();
    }

    private static void refreshKnownContents() {
        refreshKnownContents(null);
    }

    private static void refreshKnownContents(State previous) {
        Boolean hasPermissions = LAST_HAS_PERMISSIONS.get();
        State current = currentState();
        if (hasPermissions == null || isNativeBypass() || current.catalog.isEmpty()) {
            return;
        }
        if (previous == null) {
            applyCatalogContents(hasPermissions, null);
            rebuildSearchContents();
            return;
        }

        Set<Identifier> changedContents = new HashSet<>();
        boolean searchChanged = false;
        for (CreativeTabDefinition definition : current.catalog.orderedDefinitions()) {
            CreativeTabDefinition before = previous.catalog.definitions().get(definition.id());
            if (before == definition) {
                continue;
            }
            if (definition.type() == CreativeTabType.CATEGORY
                    && (before == null
                    || before.type() != CreativeTabType.CATEGORY
                    || !before.hasSameItems(definition)
                    || !before.hasSameSearchItems(definition))) {
                changedContents.add(definition.id());
            }
            searchChanged |= changesSearchProjection(before, definition);
        }
        for (CreativeTabDefinition definition : previous.catalog.orderedDefinitions()) {
            if (!current.catalog.definitions().containsKey(definition.id())
                    && (definition.type() == CreativeTabType.CATEGORY
                    || definition.type() == CreativeTabType.SEARCH)) {
                searchChanged = true;
            }
        }
        if (!changedContents.isEmpty()) {
            applyCatalogContents(hasPermissions, changedContents);
        }
        if (searchChanged) {
            CreativeModeTabs.CACHED_PARAMETERS = null;
            rebuildSearchContents();
        }
    }

    private static void applyCatalogContents(boolean hasPermissions) {
        applyCatalogContents(hasPermissions, null);
    }

    private static void applyCatalogContents(boolean hasPermissions, Set<Identifier> changedContents) {
        effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream()).forEach(tab -> {
            CreativeTabDefinition definition = definition(tab).orElse(null);
            if (definition == null || definition.type() != CreativeTabType.CATEGORY) {
                return;
            }
            if (changedContents != null && !changedContents.contains(definition.id())) {
                return;
            }
            Collection<ItemStack> display = new ArrayList<>();
            Collection<ItemStack> searchable = new ArrayList<>();
            if (id(tab).filter(id -> id.equals(Identifier.withDefaultNamespace("op_blocks"))).isPresent()
                    && !hasPermissions) {
                ((CreativeTabContents) tab).visualCreativeTabEditor$replaceContents(display, searchable);
                return;
            }
            definition.copyItemsTo(display);
            definition.copySearchItemsTo(searchable);
            ((CreativeTabContents) tab).visualCreativeTabEditor$replaceContents(display, searchable);
        });
    }

    private static boolean changesSearchProjection(
            CreativeTabDefinition before,
            CreativeTabDefinition after
    ) {
        if (before == null) {
            return after.type() == CreativeTabType.CATEGORY || after.type() == CreativeTabType.SEARCH;
        }
        if (before.type() != after.type()) {
            return before.type() == CreativeTabType.CATEGORY
                    || before.type() == CreativeTabType.SEARCH
                    || after.type() == CreativeTabType.CATEGORY
                    || after.type() == CreativeTabType.SEARCH;
        }
        if (after.type() == CreativeTabType.CATEGORY) {
            return before.hidden() != after.hidden()
                    || before.order() != after.order()
                    || !before.hasSameSearchItems(after);
        }
        return after.type() == CreativeTabType.SEARCH && !before.hasSameItems(after);
    }

    private static State currentState() {
        State preview = PREVIEW.get();
        return preview != null ? preview : ACTIVE.get();
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
