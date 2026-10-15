package com.kltyton.visual_creative_tab_editor.data;

import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGroup;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
// @target if minecraft_version=1.20.1
// @target else
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
// @target end
import net.minecraft.core.HolderLookup;
// @target if minecraft_version=1.20.1
//|import net.minecraft.core.registries.BuiltInRegistries;
//|import net.minecraft.nbt.CompoundTag;
//|import net.minecraft.nbt.TagParser;
// @target else
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
// @target end
import net.minecraft.network.chat.Component;
// @target if minecraft_version=1.20.1
// @target else
import net.minecraft.network.chat.ComponentSerialization;
// @target end
import net.minecraft.resources.ResourceLocation;
// @target if minecraft_version=1.20.1
//|import net.minecraft.world.item.Item;
// @target else
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
// @target end
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// @target if minecraft_version=1.20.1
//|/** Gson document handling backed by the 1.20.1 component serializer and NBT item format. */
// @target else
/** Gson document handling backed by Minecraft's registry-aware value codecs. */
// @target end
public final class CreativeTabJsonCodec {
    public static final String DIRECTORY = "creative_tabs";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> FIELDS = Set.of(
            "format",
            "title",
            "icon",
            "items",
            "search_items",
            "item_groups",
            "hidden",
            "order",
            "type",
            "can_scroll",
            "show_title",
            "aligned_right",
            "background"
    );
// @target if minecraft_version=1.20.1
//|    private static final Set<String> ITEM_FIELDS = Set.of("id", "count", "tag");
// @target else
// @target end

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

// @target if minecraft_version=1.20.1
// @target else
        RegistryOps<JsonElement> ops = createRegistryOps(registries);
// @target end
        int format = object.has("format") ? readInt(object, "format") : CreativeTabPatch.CURRENT_FORMAT;
        try {
            return new CreativeTabPatch(
                    format,
// @target if minecraft_version=1.20.1
//|                    readComponentOptional(object, "title"),
//|                    readItemStackOptional(object, "icon"),
//|                    readItems(object, "items"),
//|                    readItems(object, "search_items"),
// @target else
                    readCodecOptional(object, "title", ComponentSerialization.CODEC, ops),
                    readCodecOptional(object, "icon", ItemStack.CODEC, ops),
                    readItems(object, "items", ops),
                    readItems(object, "search_items", ops),
// @target end
                    readBooleanOptional(object, "hidden"),
                    readIntOptional(object, "order"),
                    readType(object),
                    readBooleanOptional(object, "can_scroll"),
                    readBooleanOptional(object, "show_title"),
                    readBooleanOptional(object, "aligned_right"),
                    readResourceLocation(object, "background"),
                    readGrouping(object, registries)
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
// @target if minecraft_version=1.20.1
// @target else
        RegistryOps<JsonElement> ops = createRegistryOps(registries);
// @target end

        JsonObject object = new JsonObject();
        object.addProperty("format", patch.format());
// @target if minecraft_version=1.20.1
//|        patch.title().ifPresent(value -> object.add("title", encodeComponent("title", value)));
//|        patch.icon().ifPresent(value -> object.add("icon", encodeItemStack("icon", value)));
//|        patch.items().ifPresent(values -> object.add("items", encodeItems("items", values)));
//|        patch.searchItems().ifPresent(values -> object.add("search_items", encodeItems("search_items", values)));
// @target else
        patch.title().ifPresent(value -> object.add("title", encodeCodec("title", ComponentSerialization.CODEC, value, ops)));
        patch.icon().ifPresent(value -> object.add("icon", encodeCodec("icon", ItemStack.CODEC, value, ops)));
        patch.items().ifPresent(values -> object.add("items", encodeItems("items", values, ops)));
        patch.searchItems().ifPresent(values -> object.add("search_items", encodeItems("search_items", values, ops)));
// @target end
        patch.hidden().ifPresent(value -> object.addProperty("hidden", value));
        patch.order().ifPresent(value -> object.addProperty("order", value));
        patch.type().ifPresent(value -> object.addProperty("type", value.serializedName()));
        patch.canScroll().ifPresent(value -> object.addProperty("can_scroll", value));
        patch.showTitle().ifPresent(value -> object.addProperty("show_title", value));
        patch.alignedRight().ifPresent(value -> object.addProperty("aligned_right", value));
        patch.background().ifPresent(value -> object.addProperty("background", value.toString()));
        patch.grouping().ifPresent(value -> object.add("item_groups", encodeGrouping(value, registries)));
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
    public static ResourceLocation tabIdFromResourceFile(ResourceLocation resourceFile) {
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
// @target if minecraft_version=1.20.1
//|        return new ResourceLocation(resourceFile.getNamespace(), tabPath);
// @target else
        return ResourceLocation.fromNamespaceAndPath(resourceFile.getNamespace(), tabPath);
// @target end
    }

    /** Maps a tab identifier to its data-resource file identifier. */
    public static ResourceLocation resourceFileFromTabId(ResourceLocation tabId) {
        Objects.requireNonNull(tabId, "tabId");
// @target if minecraft_version=1.20.1
//|        return new ResourceLocation(tabId.getNamespace(), DIRECTORY + "/" + tabId.getPath() + ".json");
// @target else
        return ResourceLocation.fromNamespaceAndPath(tabId.getNamespace(), DIRECTORY + "/" + tabId.getPath() + ".json");
// @target end
    }

    private static Optional<List<ItemStack>> readItems(
            JsonObject object,
// @target if minecraft_version=1.20.1
//|            String field
// @target else
            String field,
            RegistryOps<JsonElement> ops
// @target end
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
// @target if minecraft_version=1.20.1
//|            items.add(decodeItemStack(field + "[" + index + "]", array.get(index)));
// @target else
            items.add(decodeCodec(field + "[" + index + "]", ItemStack.CODEC, array.get(index), ops));
// @target end
        }
        return Optional.of(List.copyOf(items));
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

// @target if minecraft_version=1.20.1
//|    private static JsonArray encodeItems(String field, List<ItemStack> items) {
// @target else
    private static JsonArray encodeItems(String field, List<ItemStack> items, RegistryOps<JsonElement> ops) {
// @target end
        JsonArray array = new JsonArray();
        for (int index = 0; index < items.size(); index++) {
            if (field.equals("items") && items.get(index).isEmpty()) {
                array.add(com.google.gson.JsonNull.INSTANCE);
                continue;
            }
// @target if minecraft_version=1.20.1
//|            array.add(encodeItemStack(field + "[" + index + "]", items.get(index)));
// @target else
            array.add(encodeCodec(field + "[" + index + "]", ItemStack.CODEC, items.get(index), ops));
// @target end
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

    private static Optional<ResourceLocation> readResourceLocation(JsonObject object, String field) {
        if (!object.has(field)) {
            return Optional.empty();
        }
        String value = readString(object, field);
        try {
// @target if minecraft_version=1.20.1
//|            return Optional.of(new ResourceLocation(value));
// @target else
            return Optional.of(ResourceLocation.parse(value));
// @target end
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

    private static int readInt(JsonObject object, String field) {
        JsonElement element = requireValue(object, field);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException(field + " must be an integer");
        }
        try {
            return element.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new JsonParseException(field + " must be a 32-bit integer", exception);
        }
    }

    private static String readString(JsonObject object, String field) {
        JsonElement element = requireValue(object, field);
        if (!element.isJsonPrimitive()) {
            throw new JsonParseException(field + " must be a string");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw new JsonParseException(field + " must be a string");
        }
        return primitive.getAsString();
    }

    private static JsonElement requireValue(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull()) {
            throw new JsonParseException(field + " must not be null");
        }
        return element;
    }

// @target if minecraft_version=1.20.1
//|    private static Optional<Component> readComponentOptional(JsonObject object, String field) {
//|        if (!object.has(field)) {
//|            return Optional.empty();
//|        }
//|        JsonElement input = requireValue(object, field);
//|        checkEncodedSize(field, input);
//|        Component component = Component.Serializer.fromJson(input);
//|        if (component == null) {
//|            throw new JsonParseException(field + " must be a valid text component");
//|        }
//|        return Optional.of(component);
//|    }
//|
//|    private static JsonElement encodeComponent(String field, Component component) {
//|        JsonElement encoded = Component.Serializer.toJsonTree(component);
//|        checkEncodedSize(field, encoded);
//|        return encoded;
//|    }
//|
//|    private static Optional<ItemStack> readItemStackOptional(JsonObject object, String field) {
// @target else
    private static <T> Optional<T> readCodecOptional(
            JsonObject object,
            String field,
            Codec<T> codec,
            RegistryOps<JsonElement> ops
    ) {
// @target end
        return object.has(field)
// @target if minecraft_version=1.20.1
//|                ? Optional.of(decodeItemStack(field, requireValue(object, field)))
// @target else
                ? Optional.of(decodeCodec(field, codec, requireValue(object, field), ops))
// @target end
                : Optional.empty();
    }

// @target if minecraft_version=1.20.1
//|    /**
//|     * 1.20.1 has no data-component ItemStack codec. Use an explicit, loader-neutral
//|     * document with a registry id, count, and optional SNBT tag string instead.
//|     */
//|    private static ItemStack decodeItemStack(String field, JsonElement input) {
//|        checkEncodedSize(field, input);
//|        if (!input.isJsonObject()) {
//|            throw new JsonParseException(field + " must be a JSON object");
// @target else
    private static <T> T decodeCodec(String field, Codec<T> codec, JsonElement input, RegistryOps<JsonElement> ops) {
        if (input.toString().length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
            throw new JsonParseException(field + " exceeds the encoded-value size limit");
// @target end
        }
// @target if minecraft_version=1.20.1
//|        JsonObject object = input.getAsJsonObject();
//|        for (String key : object.keySet()) {
//|            if (!ITEM_FIELDS.contains(key)) {
//|                throw new JsonParseException("Unknown " + field + " field: " + key);
//|            }
//|        }
//|
//|        String rawId = readString(object, "id");
//|        ResourceLocation id;
//|        try {
//|            id = new ResourceLocation(rawId);
//|        } catch (RuntimeException exception) {
//|            throw new JsonParseException(field + ".id is not a valid identifier: " + rawId, exception);
//|        }
//|        Item item = BuiltInRegistries.ITEM.getOptional(id)
//|                .orElseThrow(() -> new JsonParseException(field + ".id is not a registered item: " + id));
//|        int count = object.has("count") ? readInt(object, "count") : 1;
//|        if (count <= 0 || count > Byte.MAX_VALUE) {
//|            throw new JsonParseException(field + ".count must be in [1, " + Byte.MAX_VALUE + "]");
//|        }
//|
//|        ItemStack stack = new ItemStack(item, count);
//|        if (object.has("tag")) {
//|            String snbt = readString(object, "tag");
//|            if (snbt.length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
//|                throw new JsonParseException(field + ".tag exceeds the encoded-value size limit");
//|            }
//|            try {
//|                CompoundTag tag = TagParser.parseTag(snbt);
//|                if (!tag.isEmpty()) {
//|                    stack.setTag(tag);
//|                }
//|            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
//|                throw new JsonParseException(field + ".tag is not valid SNBT: " + exception.getMessage(), exception);
//|            }
//|        }
//|        return stack;
// @target else
        return codec.parse(ops, input).getOrThrow(message -> new JsonParseException(field + ": " + message));
// @target end
    }

// @target if minecraft_version=1.20.1
//|    private static JsonObject encodeItemStack(String field, ItemStack stack) {
//|        Objects.requireNonNull(stack, field);
//|        if (stack.isEmpty()) {
//|            throw new IllegalArgumentException(field + " must not be empty");
// @target else
    private static <T> JsonElement encodeCodec(String field, Codec<T> codec, T value, RegistryOps<JsonElement> ops) {
        JsonElement encoded = codec.encodeStart(ops, value)
                .getOrThrow(message -> new IllegalArgumentException(field + ": " + message));
        if (encoded.toString().length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
            throw new IllegalArgumentException(field + " exceeds the encoded-value size limit");
// @target end
        }
// @target if minecraft_version=1.20.1
//|        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
//|        if (id == null) {
//|            throw new IllegalArgumentException(field + " references an unregistered item");
//|        }
//|        JsonObject encoded = new JsonObject();
//|        encoded.addProperty("id", id.toString());
//|        encoded.addProperty("count", stack.getCount());
//|        CompoundTag tag = stack.getTag();
//|        if (tag != null && !tag.isEmpty()) {
//|            encoded.addProperty("tag", tag.toString());
//|        }
//|        checkEncodedSize(field, encoded);
// @target else
// @target end
        return encoded;
    }

// @target if minecraft_version=1.20.1
//|    private static void checkEncodedSize(String field, JsonElement encoded) {
//|        if (encoded.toString().length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
//|            throw new JsonParseException(field + " exceeds the encoded-value size limit");
// @target else
    /**
     * Builds registry ops that retain pending-tag lookups while serializing
     * holders against the registry that actually owns them. Fabric's data
     * reload lookup can be a {@link HolderLookup.RegistryLookup.Delegate}; its
     * getter returns parent-owned holders, so using the delegate itself as the
     * serialization owner makes an immediate decode/encode round trip fail.
     */
    private static RegistryOps<JsonElement> createRegistryOps(HolderLookup.Provider registries) {
        RegistryOps.RegistryInfoLookup lookup = new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> registryKey
            ) {
                return registries.lookup(registryKey).map(registry -> new RegistryOps.RegistryInfo<>(
                        serializationOwner(registry),
                        registry,
                        registry.registryLifecycle()
                ));
            }
        };
        return RegistryOps.create(JsonOps.INSTANCE, lookup);
    }

    @SuppressWarnings("unchecked")
    private static <T> HolderOwner<T> serializationOwner(HolderLookup.RegistryLookup<T> registry) {
        HolderLookup.RegistryLookup<T> current = registry;
        while (current instanceof HolderLookup.RegistryLookup.Delegate<?> delegate) {
            current = (HolderLookup.RegistryLookup<T>) delegate.parent();
// @target end
        }
// @target if minecraft_version=1.20.1
// @target else
        return current;
// @target end
    }

    private static void ensureDocumentSize(String json) {
        if (json.length() > CreativeTabValidation.MAX_DOCUMENT_JSON_LENGTH) {
            throw new IllegalArgumentException("Creative tab document exceeds the JSON size limit");
        }
    }
}
