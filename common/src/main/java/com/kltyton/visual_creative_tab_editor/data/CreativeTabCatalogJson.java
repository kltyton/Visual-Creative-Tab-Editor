package com.kltyton.visual_creative_tab_editor.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

/** Wire/storage codec for a complete resolved creative-tab catalog. */
public final class CreativeTabCatalogJson {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private CreativeTabCatalogJson() {
    }

    public static String encode(CreativeTabCatalog catalog, HolderLookup.Provider registries) {
        JsonArray tabs = new JsonArray();
        for (CreativeTabDefinition definition : catalog.orderedDefinitions()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", definition.id().toString());
            entry.add("data", CreativeTabJsonCodec.encodeDefinition(definition, registries));
            tabs.add(entry);
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", CreativeTabPatch.CURRENT_FORMAT);
        root.add("tabs", tabs);
        return GSON.toJson(root);
    }

    public static CreativeTabCatalog decode(String json, HolderLookup.Provider registries) {
        Objects.requireNonNull(json, "json");
        if (json.length() > CreativeTabValidation.MAX_CATALOG_JSON_LENGTH) {
            throw new JsonParseException("Creative tab catalog exceeds 64 MiB");
        }
        JsonElement element = JsonParser.parseString(json);
        if (!element.isJsonObject()) {
            throw new JsonParseException("Creative tab catalog must be an object");
        }
        JsonObject root = element.getAsJsonObject();
        int format = root.has("format") ? root.get("format").getAsInt() : CreativeTabPatch.CURRENT_FORMAT;
        if (format != CreativeTabPatch.CURRENT_FORMAT) {
            throw new JsonParseException("Unsupported catalog format: " + format);
        }
        if (!root.has("tabs") || !root.get("tabs").isJsonArray()) {
            throw new JsonParseException("Creative tab catalog requires a tabs array");
        }
        JsonArray tabs = root.getAsJsonArray("tabs");
        if (tabs.size() > CreativeTabValidation.MAX_TABS) {
            throw new JsonParseException("Too many creative tabs");
        }
        List<CreativeTabDefinition> definitions = new ArrayList<>(tabs.size());
        for (int index = 0; index < tabs.size(); index++) {
            JsonObject entry = tabs.get(index).getAsJsonObject();
            if (!entry.has("id") || !entry.has("data") || entry.size() != 2) {
                throw new JsonParseException("tabs[" + index + "] requires exactly id and data");
            }
            Identifier id = Identifier.tryParse(entry.get("id").getAsString());
            if (id == null) {
                throw new JsonParseException("Invalid tab id at tabs[" + index + "]");
            }
            CreativeTabPatch patch = CreativeTabJsonCodec.decodePatch(entry.get("data"), registries);
            definitions.add(CreativeTabDefinition.resolve(id, List.of(patch)));
        }
        return definitions.isEmpty() ? CreativeTabCatalog.EMPTY : new CreativeTabCatalog(definitions);
    }
}
