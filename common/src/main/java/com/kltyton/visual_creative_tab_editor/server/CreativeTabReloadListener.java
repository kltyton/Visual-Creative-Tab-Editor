package com.kltyton.visual_creative_tab_editor.server;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalog;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabJsonCodec;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabPatch;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabType;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import com.kltyton.visual_creative_tab_editor.pack.PinnedWorldPackSource;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Reads creative tab JSON resources in their real low-to-high pack-stack order. */
public class CreativeTabReloadListener extends SimplePreparableReloadListener<CreativeTabReloadListener.Prepared> {
    private final HolderLookup.Provider registries;

    public CreativeTabReloadListener(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        long sourceRevision = CreativeTabServerManager.revision();
        long sourceMutationEpoch = CreativeTabServerManager.packMutationEpoch();
        Map<ResourceLocation, List<Layer>> resources = new LinkedHashMap<>();
        boolean defaultResourceSeen = false;
        boolean defaultResourcesValid = true;
        List<Map.Entry<ResourceLocation, List<Resource>>> entries = new ArrayList<>(manager.listResourceStacks(
                CreativeTabJsonCodec.DIRECTORY,
                id -> id.getPath().endsWith(".json")
        ).entrySet());
        entries.sort(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)));

        for (Map.Entry<ResourceLocation, List<Resource>> entry : entries) {
            ResourceLocation tabId = CreativeTabJsonCodec.tabIdFromResourceFile(entry.getKey());
            List<Layer> layers = new ArrayList<>();
            for (Resource resource : entry.getValue()) {
                String source = resource.sourcePackId();
                boolean generatedDefault = source.equals(PinnedWorldPackSource.DEFAULT_PACK_ID);
                defaultResourceSeen |= generatedDefault;
                try (Reader reader = resource.openAsReader()) {
                    String json = readLimited(reader);
                    layers.add(new Layer(source, CreativeTabJsonCodec.decodePatch(json, this.registries)));
                } catch (Exception exception) {
                    if (generatedDefault) {
                        defaultResourcesValid = false;
                    }
                    if (isOwnedPack(source)) {
                        VisualCreativeTabEditorConstants.LOGGER.warn(
                                "Ignoring stale creative-tab resource {} from owned pack {} until it can be regenerated",
                                entry.getKey(),
                                source,
                                exception
                        );
                        continue;
                    }
                    throw new IllegalStateException(
                            "Invalid creative tab resource " + entry.getKey() + " from " + source,
                            exception
                    );
                }
            }
            if (!layers.isEmpty()) {
                resources.put(tabId, List.copyOf(layers));
            }
        }

        Map<ResourceLocation, ResolvedBase> baseDefinitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<Layer>> entry : resources.entrySet()) {
            List<Layer> baseLayers = entry.getValue().stream()
                    .filter(layer -> !layer.sourcePackId().equals(PinnedWorldPackSource.PLAYER_PACK_ID))
                    .toList();
            ResolvedBase definition = resolveBaseLayers(entry.getKey(), baseLayers);
            if (definition != null) {
                addBaseDefinition(baseDefinitions, entry.getKey(), definition);
            }
        }

        CreativeTabCatalog base = buildBaseCatalog(baseDefinitions);
        Map<ResourceLocation, CreativeTabDefinition> resolvedDefinitions = new LinkedHashMap<>(base.definitions());
        for (Map.Entry<ResourceLocation, List<Layer>> entry : resources.entrySet()) {
            List<Layer> playerLayers = entry.getValue().stream()
                    .filter(layer -> layer.sourcePackId().equals(PinnedWorldPackSource.PLAYER_PACK_ID))
                    .toList();
            if (playerLayers.isEmpty()) {
                continue;
            }

            CreativeTabDefinition current = resolvedDefinitions.get(entry.getKey());
            if (current == null) {
                CreativeTabPatch standalone = mergeOwnedLayers(entry.getKey(), playerLayers);
                if (standalone == null
                        || !isEditorCustomTab(entry.getKey())
                        || !isFullOwnedDefinition(standalone)) {
                    VisualCreativeTabEditorConstants.LOGGER.warn(
                            "Ignoring orphaned player creative-tab override {} because its lower-layer tab no longer exists",
                            entry.getKey()
                    );
                    continue;
                }
                current = CreativeTabDefinition.defaults(entry.getKey());
            }

            for (Layer layer : playerLayers) {
                CreativeTabDefinition candidate;
                try {
                    candidate = current.apply(layer.patch());
                } catch (RuntimeException exception) {
                    warnIgnoredOwnedLayer(entry.getKey(), layer, exception);
                    continue;
                }

                Map<ResourceLocation, CreativeTabDefinition> candidateDefinitions = new LinkedHashMap<>(resolvedDefinitions);
                candidateDefinitions.put(entry.getKey(), candidate);
                try {
                    catalogOf(candidateDefinitions);
                } catch (RuntimeException exception) {
                    warnIgnoredOwnedLayer(entry.getKey(), layer, exception);
                    continue;
                }
                current = candidate;
                resolvedDefinitions.put(entry.getKey(), candidate);
            }
        }

        CreativeTabCatalog resolved = catalogOf(resolvedDefinitions);

        Prepared prepared = new Prepared(
                base,
                resolved,
                defaultResourceSeen && defaultResourcesValid,
                this.registries,
                CreativeTabServerManager.prepareSnapshot(base, resolved, this.registries),
                sourceRevision,
                sourceMutationEpoch
        );
        return prepared;
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        CreativeTabServerManager.acceptReload(prepared);
    }

    private static String readLimited(Reader reader) throws IOException {
        char[] buffer = new char[8192];
        StringBuilder result = new StringBuilder();
        int count;
        while ((count = reader.read(buffer)) >= 0) {
            result.append(buffer, 0, count);
            if (result.length() > CreativeTabValidation.MAX_DOCUMENT_JSON_LENGTH) {
                throw new IOException("Creative tab JSON exceeds the document size limit");
            }
        }
        return result.toString();
    }

    private static boolean isOwnedPack(String sourcePackId) {
        return sourcePackId.equals(PinnedWorldPackSource.DEFAULT_PACK_ID)
                || sourcePackId.equals(PinnedWorldPackSource.PLAYER_PACK_ID);
    }

    private static boolean isFullOwnedDefinition(CreativeTabPatch patch) {
        return patch.title().isPresent()
                && patch.icon().isPresent()
                && patch.items().isPresent()
                && patch.searchItems().isPresent()
                && patch.hidden().isPresent()
                && patch.order().isPresent()
                && patch.type().filter(type -> type == CreativeTabType.CATEGORY).isPresent()
                && patch.canScroll().isPresent()
                && patch.showTitle().isPresent()
                && patch.alignedRight().isPresent()
                && patch.background().isPresent();
    }

    private static ResolvedBase resolveBaseLayers(ResourceLocation id, List<Layer> layers) {
        CreativeTabDefinition current = CreativeTabDefinition.defaults(id);
        boolean applied = false;
        boolean hasThirdParty = false;
        for (Layer layer : layers) {
            try {
                current = current.apply(layer.patch());
                applied = true;
                hasThirdParty |= !isOwnedPack(layer.sourcePackId());
            } catch (RuntimeException exception) {
                if (isOwnedPack(layer.sourcePackId())) {
                    warnIgnoredOwnedLayer(id, layer, exception);
                    continue;
                }
                throw new IllegalStateException(
                        "Invalid resolved creative tab " + id + " after applying third-party pack "
                                + layer.sourcePackId(),
                        exception
                );
            }
        }
        return applied ? new ResolvedBase(current, hasThirdParty) : null;
    }

    private static CreativeTabPatch mergeOwnedLayers(ResourceLocation id, List<Layer> layers) {
        CreativeTabPatch merged = CreativeTabPatch.empty();
        boolean applied = false;
        for (Layer layer : layers) {
            try {
                merged = merged.overlay(layer.patch());
                applied = true;
            } catch (RuntimeException exception) {
                warnIgnoredOwnedLayer(id, layer, exception);
            }
        }
        return applied ? merged : null;
    }

    private static void addBaseDefinition(
            Map<ResourceLocation, ResolvedBase> definitions,
            ResourceLocation id,
            ResolvedBase candidate
    ) {
        long candidateItems = itemCount(candidate.definition());
        if (!wouldExceedLimits(definitions, candidateItems)) {
            definitions.put(id, candidate);
            return;
        }

        if (!candidate.hasThirdParty()) {
            VisualCreativeTabEditorConstants.LOGGER.warn(
                    "Ignoring owned creative-tab definition {} because it exceeds the resolved catalog limits",
                    id
            );
            return;
        }

        while (wouldExceedLimits(definitions, candidateItems)) {
            ResourceLocation removable = null;
            for (Map.Entry<ResourceLocation, ResolvedBase> entry : definitions.entrySet()) {
                if (!entry.getValue().hasThirdParty()) {
                    removable = entry.getKey();
                }
            }
            if (removable == null) {
                throw new IllegalStateException(
                        "Third-party creative-tab definition " + id + " exceeds the resolved catalog limits"
                );
            }
            definitions.remove(removable);
            VisualCreativeTabEditorConstants.LOGGER.warn(
                    "Ignoring owned creative-tab definition {} to preserve strict third-party catalog data",
                    removable
            );
        }
        definitions.put(id, candidate);
    }

    private static boolean wouldExceedLimits(Map<ResourceLocation, ResolvedBase> definitions, long candidateItems) {
        if (definitions.size() + 1 > CreativeTabValidation.MAX_TABS) {
            return true;
        }
        long totalItems = candidateItems;
        for (ResolvedBase definition : definitions.values()) {
            totalItems += itemCount(definition.definition());
            if (totalItems > CreativeTabValidation.MAX_TOTAL_ITEMS) {
                return true;
            }
        }
        return false;
    }

    private static long itemCount(CreativeTabDefinition definition) {
        return (long) definition.itemCount() + definition.searchItemCount();
    }

    private static CreativeTabCatalog buildBaseCatalog(Map<ResourceLocation, ResolvedBase> definitions) {
        if (definitions.isEmpty()) {
            return CreativeTabCatalog.EMPTY;
        }
        try {
            return new CreativeTabCatalog(definitions.values().stream().map(ResolvedBase::definition).toList());
        } catch (RuntimeException exception) {
            boolean hasThirdParty = definitions.values().stream().anyMatch(ResolvedBase::hasThirdParty);
            if (hasThirdParty) {
                throw new IllegalStateException("Invalid third-party creative-tab catalog", exception);
            }
            VisualCreativeTabEditorConstants.LOGGER.warn(
                    "Ignoring invalid owned creative-tab catalog until the generated default pack can be rebuilt",
                    exception
            );
            return CreativeTabCatalog.EMPTY;
        }
    }

    private static CreativeTabCatalog catalogOf(Map<ResourceLocation, CreativeTabDefinition> definitions) {
        return definitions.isEmpty()
                ? CreativeTabCatalog.EMPTY
                : new CreativeTabCatalog(definitions.values());
    }

    private static void warnIgnoredOwnedLayer(ResourceLocation id, Layer layer, RuntimeException exception) {
        VisualCreativeTabEditorConstants.LOGGER.warn(
                "Ignoring invalid creative-tab layer {} from owned pack {} until it can be regenerated",
                id,
                layer.sourcePackId(),
                exception
        );
    }

    private static boolean isEditorCustomTab(ResourceLocation id) {
        return (id.getNamespace().equals(VisualCreativeTabEditorConstants.MOD_ID)
                || id.getNamespace().equals(VisualCreativeTabEditorConstants.LEGACY_MOD_ID))
                && id.getPath().startsWith("custom/");
    }

    private record Layer(String sourcePackId, CreativeTabPatch patch) {
    }

    private record ResolvedBase(CreativeTabDefinition definition, boolean hasThirdParty) {
    }

    public record Prepared(
            CreativeTabCatalog base,
            CreativeTabCatalog resolved,
            boolean defaultPresent,
            HolderLookup.Provider registries,
            CreativeTabServerManager.PreparedSnapshot snapshot,
            long sourceRevision,
            long sourceMutationEpoch
    ) {
    }
}
