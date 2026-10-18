package com.kltyton.visual_creative_tab_editor.data;

import com.kltyton.visual_creative_tab_editor.data.codec.NativeValueCodec;
import static com.kltyton.visual_creative_tab_editor.data.codec.JsonFields.*;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGroup;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping;
import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategory;
import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategories;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Gson document handling backed by Minecraft's registry-aware value codecs. */
public final class CreativeTabJsonCodec {
    private static <T> Optional<T> readValueOptional(JsonObject object, String field,
            java.util.function.BiFunction<String, JsonElement, T> decoder) {
        return object.has(field) ? Optional.of(decoder.apply(field, requireValue(object, field))) : Optional.empty();
    }

    public static final String DIRECTORY = "creative_tabs";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> FIELDS = Set.of(
            "format",
            "title",
            "icon",
            "items",
            "search_items",
            "item_groups",
            "categories",
            "hidden",
            "order",
            "type",
            "can_scroll",
            "show_title",
            "aligned_right",
            "background"
    );

    private CreativeTabJsonCodec() {
    }

    /** Decodes one partial tab document. Missing {@code format} defaults to 1. */
    public static CreativeTabPatch decodePatch(String json, HolderLookup.Provider registries) {
        Objects.requireNonNull(json, "json");
        if (json.length() > CreativeTabValidation.MAX_DOCUMENT_JSON_LENGTH) {
            throw new JsonParseException("Creative tab document exceeds the JSON size limit");
        }
        try {
            return decodePatch(JsonParser.parseString(json), registries);
        } catch (JsonParseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new JsonParseException("Invalid creative tab document: " + exception.getMessage(), exception);
        }
    }

    /** Decodes one partial tab JSON value. */
    public static CreativeTabPatch decodePatch(JsonElement json, HolderLookup.Provider registries) {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(registries, "registries");
        if (json.toString().length() > CreativeTabValidation.MAX_DOCUMENT_JSON_LENGTH) {
            throw new JsonParseException("Creative tab document exceeds the JSON size limit");
        }
        if (!json.isJsonObject()) {
            throw new JsonParseException("Creative tab document must be a JSON object");
        }

        JsonObject object = json.getAsJsonObject();
        for (String key : object.keySet()) {
            if (!FIELDS.contains(key)) {
                throw new JsonParseException("Unknown creative tab field: " + key);
            }
        }

        NativeValueCodec ops = new NativeValueCodec(registries);
        int format = object.has("format") ? readInt(object, "format") : CreativeTabPatch.CURRENT_FORMAT;
        try {
            return new CreativeTabPatch(
                    format,
                    readValueOptional(object, "title", ops::decodeComponent),
                    readValueOptional(object, "icon", ops::decodeItem),
                    readItems(object, "items", ops),
                    readItems(object, "search_items", ops),
                    readBooleanOptional(object, "hidden"),
                    readIntOptional(object, "order"),
                    readType(object),
                    readBooleanOptional(object, "can_scroll"),
                    readBooleanOptional(object, "show_title"),
                    readBooleanOptional(object, "aligned_right"),
                    readResourceLocation(object, "background"),
                    readGrouping(object, registries),
                    readCategories(object, ops)
            );
        } catch (JsonParseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new JsonParseException("Invalid creative tab document: " + exception.getMessage(), exception);
        }
    }

    /** Encodes one partial tab document. */
    public static JsonObject encodePatch(CreativeTabPatch patch, HolderLookup.Provider registries) {
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(registries, "registries");
        NativeValueCodec ops = new NativeValueCodec(registries);

        JsonObject object = new JsonObject();
        object.addProperty("format", patch.format());
        patch.title().ifPresent(value -> object.add("title", ops.encodeComponent("title", value)));
        patch.icon().ifPresent(value -> object.add("icon", ops.encodeItem("icon", value)));
        patch.items().ifPresent(values -> object.add("items", encodeItems("items", values, ops)));
        patch.searchItems().ifPresent(values -> object.add("search_items", encodeItems("search_items", values, ops)));
        patch.hidden().ifPresent(value -> object.addProperty("hidden", value));
        patch.order().ifPresent(value -> object.addProperty("order", value));
        patch.type().ifPresent(value -> object.addProperty("type", value.serializedName()));
        patch.canScroll().ifPresent(value -> object.addProperty("can_scroll", value));
        patch.showTitle().ifPresent(value -> object.addProperty("show_title", value));
        patch.alignedRight().ifPresent(value -> object.addProperty("aligned_right", value));
        patch.background().ifPresent(value -> object.addProperty("background", value.toString()));
        patch.grouping().ifPresent(value -> object.add("item_groups", encodeGrouping(value, registries)));
        patch.categories().ifPresent(value -> object.add("categories", encodeCategories(value, ops)));
        ensureDocumentSize(object.toString());
        return object;
    }

    /** Encodes every field of a resolved definition. */
    public static JsonObject encodeDefinition(CreativeTabDefinition definition, HolderLookup.Provider registries) {
        return encodePatch(CreativeTabPatch.full(definition), registries);
    }

    /** Encodes only fields changed between two definitions. */
    public static JsonObject encodeDiff(
            CreativeTabDefinition base,
            CreativeTabDefinition target,
            HolderLookup.Provider registries
    ) {
        return encodePatch(CreativeTabPatch.diff(base, target), registries);
    }

    /** Pretty-prints one patch using the stable field order of the encoder. */
    public static String encodePatchToString(CreativeTabPatch patch, HolderLookup.Provider registries) {
        String json = GSON.toJson(encodePatch(patch, registries));
        ensureDocumentSize(json);
        return json;
    }

    /** Maps {@code data/<namespace>/creative_tabs/<path>.json} to {@code namespace:path}. */
    public static ResourceId tabIdFromResourceFile(ResourceId resourceFile) {
        Objects.requireNonNull(resourceFile, "resourceFile");
        String prefix = DIRECTORY + "/";
        String path = resourceFile.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("Not a creative tab JSON resource: " + resourceFile);
        }
        String tabPath = path.substring(prefix.length(), path.length() - ".json".length());
        if (tabPath.isEmpty()) {
            throw new IllegalArgumentException("Creative tab resource has an empty path: " + resourceFile);
        }
        return ResourceId.fromNamespaceAndPath(resourceFile.getNamespace(), tabPath);
    }

    /** Maps a tab identifier to its data-resource file identifier. */
    public static ResourceId resourceFileFromTabId(ResourceId tabId) {
        Objects.requireNonNull(tabId, "tabId");
        return ResourceId.fromNamespaceAndPath(tabId.getNamespace(), DIRECTORY + "/" + tabId.getPath() + ".json");
    }

    private static Optional<List<ItemStack>> readItems(
            JsonObject object,
            String field,
            NativeValueCodec ops
    ) {
        if (!object.has(field)) {
            return Optional.empty();
        }
        JsonElement element = requireValue(object, field);
        if (!element.isJsonArray()) {
            throw new JsonParseException(field + " must be a JSON array");
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > CreativeTabValidation.MAX_ITEMS_PER_TAB) {
            throw new JsonParseException(field + " exceeds the per-tab item limit");
        }
        List<ItemStack> items = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            if (field.equals("items") && array.get(index).isJsonNull()) {
                items.add(ItemStack.EMPTY);
                continue;
            }
            items.add(ops.decodeItem(field + "[" + index + "]", array.get(index)));
        }
        return Optional.of(List.copyOf(items));
    }

    private static Optional<CreativeTabCategories> readCategories(JsonObject object, NativeValueCodec ops) {
        if (!object.has("categories")) { return Optional.empty(); }
        JsonArray entries = requireValue(object, "categories").getAsJsonArray();
        if (entries.size() > CreativeTabValidation.MAX_TABS) {
            throw new JsonParseException("Too many creative categories");
        }
        List<CreativeTabCategory> categories = new ArrayList<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            for (String field : entry.keySet()) {
                if (!Set.of("id", "title", "icon", "items").contains(field)) {
                    throw new JsonParseException("Unknown creative category field: " + field);
                }
            }
            categories.add(new CreativeTabCategory(readString(entry, "id"),
                    ops.decodeComponent("category.title", requireValue(entry, "title")),
                    ops.decodeItem("category.icon", requireValue(entry, "icon")),
                    readItems(entry, "items", ops).orElseThrow(() -> new JsonParseException("Category items are required"))));
        }
        return Optional.of(new CreativeTabCategories(categories));
    }

    private static JsonArray encodeCategories(CreativeTabCategories categories, NativeValueCodec ops) {
        JsonArray entries = new JsonArray();
        for (CreativeTabCategory category : categories.entries()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", category.id());
            entry.add("title", ops.encodeComponent("category.title", category.title()));
            entry.add("icon", ops.encodeItem("category.icon", category.icon()));
            entry.add("items", encodeItems("category.items", category.items(), ops));
            entries.add(entry);
        }
        return entries;
    }

    private static Optional<CreativeItemGrouping> readGrouping(JsonObject object, HolderLookup.Provider registries) {
        if (!object.has("item_groups")) { return Optional.empty(); }
        JsonObject grouping = requireValue(object, "item_groups").getAsJsonObject();
        for (String field : grouping.keySet()) {
            if (!field.equals("groups") && !field.equals("ungrouped")) {
                throw new JsonParseException("Unknown item grouping field: " + field);
            }
        }
        List<CreativeItemGroup> groups = new ArrayList<>();
        if (grouping.has("groups")) {
            JsonArray entries = requireValue(grouping, "groups").getAsJsonArray();
            if (entries.size() > CreativeTabValidation.MAX_ITEMS_PER_TAB) {
                throw new JsonParseException("Too many item groups");
            }
            for (JsonElement element : entries) {
                JsonObject entry = element.getAsJsonObject();
                for (String field : entry.keySet()) {
                    if (!Set.of("id", "title", "icon", "items").contains(field)) {
                        throw new JsonParseException("Unknown item group field: " + field);
                    }
                }
                String id = readString(entry, "id");
                JsonObject values = entry.deepCopy();
                values.remove("id");
                CreativeTabPatch decoded = decodePatch(values, registries);
                groups.add(new CreativeItemGroup(id,
                        decoded.title().orElseThrow(() -> new JsonParseException("Item group title is required")),
                        decoded.icon().orElseThrow(() -> new JsonParseException("Item group icon is required")),
                        decoded.items().orElseThrow(() -> new JsonParseException("Item group items are required"))));
            }
        }
        List<ItemStack> ungrouped = List.of();
        if (grouping.has("ungrouped")) {
            JsonObject values = new JsonObject();
            values.add("items", requireValue(grouping, "ungrouped"));
            ungrouped = decodePatch(values, registries).items().orElseThrow();
        }
        return Optional.of(new CreativeItemGrouping(groups, ungrouped));
    }

    private static JsonObject encodeGrouping(CreativeItemGrouping grouping, HolderLookup.Provider registries) {
        JsonObject result = new JsonObject();
        JsonArray entries = new JsonArray();
        for (CreativeItemGroup group : grouping.groups()) {
            JsonObject values = encodePatch(itemValuePatch(Optional.of(group.title()), Optional.of(group.icon()), group.items()), registries);
            values.remove("format");
            JsonObject entry = new JsonObject();
            entry.addProperty("id", group.id());
            values.entrySet().forEach(field -> entry.add(field.getKey(), field.getValue()));
            entries.add(entry);
        }
        result.add("groups", entries);
        result.add("ungrouped", encodePatch(itemValuePatch(Optional.empty(), Optional.empty(), grouping.ungrouped()), registries).get("items"));
        return result;
    }

    private static CreativeTabPatch itemValuePatch(Optional<Component> title, Optional<ItemStack> icon, List<ItemStack> items) {
        return new CreativeTabPatch(CreativeTabPatch.CURRENT_FORMAT, title, icon, Optional.of(items),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static JsonArray encodeItems(String field, List<ItemStack> items, NativeValueCodec ops) {
        JsonArray array = new JsonArray();
        for (int index = 0; index < items.size(); index++) {
            if (field.equals("items") && items.get(index).isEmpty()) {
                array.add(com.google.gson.JsonNull.INSTANCE);
                continue;
            }
            array.add(ops.encodeItem(field + "[" + index + "]", items.get(index)));
        }
        return array;
    }

    private static Optional<CreativeTabType> readType(JsonObject object) {
        if (!object.has("type")) {
            return Optional.empty();
        }
        String value = readString(object, "type");
        try {
            return Optional.of(CreativeTabType.parse(value));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(exception.getMessage(), exception);
        }
    }

    private static Optional<ResourceId> readResourceLocation(JsonObject object, String field) {
        if (!object.has(field)) {
            return Optional.empty();
        }
        String value = readString(object, field);
        try {
            return Optional.of(ResourceId.parse(value));
        } catch (RuntimeException exception) {
            throw new JsonParseException(field + " is not a valid identifier: " + value, exception);
        }
    }

    private static Optional<Boolean> readBooleanOptional(JsonObject object, String field) {
        if (!object.has(field)) {
            return Optional.empty();
        }
        JsonElement element = requireValue(object, field);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException(field + " must be a boolean");
        }
        return Optional.of(element.getAsBoolean());
    }

    private static Optional<Integer> readIntOptional(JsonObject object, String field) {
        return object.has(field) ? Optional.of(readInt(object, field)) : Optional.empty();
    }

    private static void ensureDocumentSize(String json) {
        if (json.length() > CreativeTabValidation.MAX_DOCUMENT_JSON_LENGTH) {
            throw new IllegalArgumentException("Creative tab document exceeds the JSON size limit");
        }
    }
}
