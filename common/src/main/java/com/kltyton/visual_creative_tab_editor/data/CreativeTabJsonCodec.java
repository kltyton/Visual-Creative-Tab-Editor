package com.kltyton.visual_creative_tab_editor.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Gson document handling backed by Minecraft's registry-aware value codecs. */
public final class CreativeTabJsonCodec {
    public static final String DIRECTORY = "creative_tabs";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> FIELDS = Set.of(
            "format",
            "title",
            "icon",
            "items",
            "search_items",
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

        RegistryOps<JsonElement> ops = createRegistryOps(registries);
        int format = object.has("format") ? readInt(object, "format") : CreativeTabPatch.CURRENT_FORMAT;
        try {
            return new CreativeTabPatch(
                    format,
                    readCodecOptional(object, "title", ComponentSerialization.CODEC, ops),
                    readCodecOptional(object, "icon", ItemStack.CODEC, ops),
                    readItems(object, "items", ops),
                    readItems(object, "search_items", ops),
                    readBooleanOptional(object, "hidden"),
                    readIntOptional(object, "order"),
                    readType(object),
                    readBooleanOptional(object, "can_scroll"),
                    readBooleanOptional(object, "show_title"),
                    readBooleanOptional(object, "aligned_right"),
                    readResourceLocation(object, "background")
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
        RegistryOps<JsonElement> ops = createRegistryOps(registries);

        JsonObject object = new JsonObject();
        object.addProperty("format", patch.format());
        patch.title().ifPresent(value -> object.add("title", encodeCodec("title", ComponentSerialization.CODEC, value, ops)));
        patch.icon().ifPresent(value -> object.add("icon", encodeCodec("icon", ItemStack.CODEC, value, ops)));
        patch.items().ifPresent(values -> object.add("items", encodeItems("items", values, ops)));
        patch.searchItems().ifPresent(values -> object.add("search_items", encodeItems("search_items", values, ops)));
        patch.hidden().ifPresent(value -> object.addProperty("hidden", value));
        patch.order().ifPresent(value -> object.addProperty("order", value));
        patch.type().ifPresent(value -> object.addProperty("type", value.serializedName()));
        patch.canScroll().ifPresent(value -> object.addProperty("can_scroll", value));
        patch.showTitle().ifPresent(value -> object.addProperty("show_title", value));
        patch.alignedRight().ifPresent(value -> object.addProperty("aligned_right", value));
        patch.background().ifPresent(value -> object.addProperty("background", value.toString()));
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
        return ResourceLocation.fromNamespaceAndPath(resourceFile.getNamespace(), tabPath);
    }

    /** Maps a tab identifier to its data-resource file identifier. */
    public static ResourceLocation resourceFileFromTabId(ResourceLocation tabId) {
        Objects.requireNonNull(tabId, "tabId");
        return ResourceLocation.fromNamespaceAndPath(tabId.getNamespace(), DIRECTORY + "/" + tabId.getPath() + ".json");
    }

    private static Optional<List<ItemStack>> readItems(
            JsonObject object,
            String field,
            RegistryOps<JsonElement> ops
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
            items.add(decodeCodec(field + "[" + index + "]", ItemStack.CODEC, array.get(index), ops));
        }
        return Optional.of(List.copyOf(items));
    }

    private static JsonArray encodeItems(String field, List<ItemStack> items, RegistryOps<JsonElement> ops) {
        JsonArray array = new JsonArray();
        for (int index = 0; index < items.size(); index++) {
            array.add(encodeCodec(field + "[" + index + "]", ItemStack.CODEC, items.get(index), ops));
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
            return Optional.of(ResourceLocation.parse(value));
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

    private static <T> Optional<T> readCodecOptional(
            JsonObject object,
            String field,
            Codec<T> codec,
            RegistryOps<JsonElement> ops
    ) {
        return object.has(field)
                ? Optional.of(decodeCodec(field, codec, requireValue(object, field), ops))
                : Optional.empty();
    }

    private static <T> T decodeCodec(String field, Codec<T> codec, JsonElement input, RegistryOps<JsonElement> ops) {
        if (input.toString().length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
            throw new JsonParseException(field + " exceeds the encoded-value size limit");
        }
        return codec.parse(ops, input).getOrThrow(message -> new JsonParseException(field + ": " + message));
    }

    private static <T> JsonElement encodeCodec(String field, Codec<T> codec, T value, RegistryOps<JsonElement> ops) {
        JsonElement encoded = codec.encodeStart(ops, value)
                .getOrThrow(message -> new IllegalArgumentException(field + ": " + message));
        if (encoded.toString().length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
            throw new IllegalArgumentException(field + " exceeds the encoded-value size limit");
        }
        return encoded;
    }

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
        }
        return current;
    }

    private static void ensureDocumentSize(String json) {
        if (json.length() > CreativeTabValidation.MAX_DOCUMENT_JSON_LENGTH) {
            throw new IllegalArgumentException("Creative tab document exceeds the JSON size limit");
        }
    }
}
