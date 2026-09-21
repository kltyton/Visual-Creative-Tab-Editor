package com.kltyton.visual_creative_tab_editor.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

/** Local default template and per-server preferences, stored as patches against native tabs. */
public final class CreativeTabPreferences {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static Path directory;

    private CreativeTabPreferences() {
    }

    public static void configure(Path configDirectory) {
        directory = Objects.requireNonNull(configDirectory).resolve(VisualCreativeTabEditorConstants.MOD_ID);
    }

    public static Path globalFile() {
        return Objects.requireNonNull(directory, "Preferences directory is not configured").resolve("global.json");
    }

    public static Path serverFile(String address) {
        String key = java.util.UUID.nameUUIDFromBytes(address.getBytes(StandardCharsets.UTF_8)).toString();
        return Objects.requireNonNull(directory).resolve("servers").resolve(key + ".json");
    }

    public static CreativeTabCatalog load(Path file, CreativeTabCatalog baseline, HolderLookup.Provider registries)
            throws IOException {
        if (!Files.isRegularFile(file)) {
            return baseline;
        }
        byte[] bytes;
        try (var input = Files.newInputStream(file)) {
            bytes = input.readNBytes(CreativeTabValidation.MAX_CATALOG_JSON_LENGTH + 1);
        }
        if (bytes.length > CreativeTabValidation.MAX_CATALOG_JSON_LENGTH) {
            throw new IOException("Creative tab preferences exceed the file size limit");
        }
        try {
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("format").getAsInt() != CreativeTabPatch.CURRENT_FORMAT) {
                throw new IllegalArgumentException("Unsupported creative tab preferences format");
            }
            JsonArray entries = root.getAsJsonArray("tabs");
            if (entries.size() > CreativeTabValidation.MAX_TABS) {
                throw new IllegalArgumentException("Too many creative tab preference entries");
            }
            Map<Identifier, CreativeTabDefinition> definitions = new LinkedHashMap<>(baseline.definitions());
            java.util.Set<Identifier> seen = new java.util.HashSet<>();
            for (var element : entries) {
                JsonObject entry = element.getAsJsonObject();
                Identifier id = Identifier.tryParse(entry.get("id").getAsString());
                if (id == null || !seen.add(id)) {
                    throw new IllegalArgumentException("Invalid or duplicate creative tab preference id");
                }
                CreativeTabDefinition original = definitions.get(id);
                if (original == null && !id.getNamespace().equals(VisualCreativeTabEditorConstants.MOD_ID)
                        && !id.getNamespace().equals(VisualCreativeTabEditorConstants.LEGACY_MOD_ID)) {
                    continue;
                }
                try {
                    CreativeTabPatch patch = CreativeTabJsonCodec.decodePatch(entry.get("data"), registries);
                    CreativeTabDefinition updated = (original == null ? CreativeTabDefinition.defaults(id) : original).apply(patch);
                    if (original != null && original.type() != updated.type()) {
                        throw new IllegalArgumentException("Preferences cannot change a native tab type");
                    }
                    if (original == null && updated.type() != CreativeTabType.CATEGORY) {
                        throw new IllegalArgumentException("Custom preference tabs must be categories");
                    }
                    definitions.put(id, updated);
                } catch (RuntimeException exception) {
                    // Keep the file intact so a temporarily absent mod or registry entry can return later.
                    VisualCreativeTabEditorConstants.LOGGER.warn("Could not apply local creative tab preference {}", id, exception);
                }
            }
            return new CreativeTabCatalog(definitions.values());
        } catch (RuntimeException exception) {
            throw new IOException("Invalid creative tab preferences: " + file.getFileName(), exception);
        }
    }

    public static void save(Path file, CreativeTabCatalog baseline, CreativeTabCatalog target,
                            HolderLookup.Provider registries) throws IOException {
        CreativeTabValidation.validateAll(target.orderedDefinitions());
        JsonArray entries = new JsonArray();
        for (CreativeTabDefinition definition : target.orderedDefinitions()) {
            CreativeTabDefinition original = baseline.definition(definition.id()).orElse(null);
            CreativeTabPatch patch = original == null
                    ? CreativeTabPatch.full(definition) : CreativeTabPatch.diff(original, definition);
            if (!patch.isEmpty()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", definition.id().toString());
                entry.add("data", CreativeTabJsonCodec.encodePatch(patch, registries));
                entries.add(entry);
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", CreativeTabPatch.CURRENT_FORMAT);
        root.add("tabs", entries);
        byte[] bytes = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > CreativeTabValidation.MAX_CATALOG_JSON_LENGTH) {
            throw new IOException("Creative tab preferences exceed the file size limit");
        }
        Files.createDirectories(file.getParent());
        Path pending = Files.createTempFile(file.getParent(), "creative-tabs-", ".pending");
        try {
            Files.write(pending, bytes);
            try {
                Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }
}
