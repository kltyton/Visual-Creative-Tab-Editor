package com.kltyton.visual_creative_tab_editor.data.codec;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import static com.kltyton.visual_creative_tab_editor.data.codec.JsonFields.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Native component serializer and bounded id/count/SNBT item format for 1.20.1. */
public final class NativeValueCodec {
    private static final Set<String> ITEM_FIELDS = Set.of("id", "count", "tag");
    public NativeValueCodec(HolderLookup.Provider registries) { Objects.requireNonNull(registries, "registries"); }
    public Component decodeComponent(String field, JsonElement input) {
        checkEncodedSize(field, input);
        Component component = Component.Serializer.fromJson(input);
        if (component == null) { throw new JsonParseException(field + " must be a valid text component"); }
        return component;
    }
    public ItemStack decodeItem(String field, JsonElement input) { return decodeItemStack(field, input); }
    public JsonElement encodeItem(String field, ItemStack value) { return encodeItemStack(field, value); }


    public JsonElement encodeComponent(String field, Component component) {
        JsonElement encoded = Component.Serializer.toJsonTree(component);
        checkEncodedSize(field, encoded);
        return encoded;
    }


    /**
     * 1.20.1 has no data-component ItemStack codec. Use an explicit, loader-neutral
     * document with a registry id, count, and optional SNBT tag string instead.
     */
    private static ItemStack decodeItemStack(String field, JsonElement input) {
        checkEncodedSize(field, input);
        if (!input.isJsonObject()) {
            throw new JsonParseException(field + " must be a JSON object");
        }
        JsonObject object = input.getAsJsonObject();
        for (String key : object.keySet()) {
            if (!ITEM_FIELDS.contains(key)) {
                throw new JsonParseException("Unknown " + field + " field: " + key);
            }
        }

        String rawId = readString(object, "id");
        ResourceLocation id;
        try {
            id = new ResourceLocation(rawId);
        } catch (RuntimeException exception) {
            throw new JsonParseException(field + ".id is not a valid identifier: " + rawId, exception);
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new JsonParseException(field + ".id is not a registered item: " + id));
        int count = object.has("count") ? readInt(object, "count") : 1;
        if (count <= 0 || count > Byte.MAX_VALUE) {
            throw new JsonParseException(field + ".count must be in [1, " + Byte.MAX_VALUE + "]");
        }

        ItemStack stack = new ItemStack(item, count);
        if (object.has("tag")) {
            String snbt = readString(object, "tag");
            if (snbt.length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
                throw new JsonParseException(field + ".tag exceeds the encoded-value size limit");
            }
            try {
                CompoundTag tag = TagParser.parseTag(snbt);
                if (!tag.isEmpty()) {
                    stack.setTag(tag);
                }
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                throw new JsonParseException(field + ".tag is not valid SNBT: " + exception.getMessage(), exception);
            }
        }
        return stack;
    }

    private static JsonObject encodeItemStack(String field, ItemStack stack) {
        Objects.requireNonNull(stack, field);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            throw new IllegalArgumentException(field + " references an unregistered item");
        }
        JsonObject encoded = new JsonObject();
        encoded.addProperty("id", id.toString());
        encoded.addProperty("count", stack.getCount());
        CompoundTag tag = stack.getTag();
        if (tag != null && !tag.isEmpty()) {
            encoded.addProperty("tag", tag.toString());
        }
        checkEncodedSize(field, encoded);
        return encoded;
    }

    private static void checkEncodedSize(String field, JsonElement encoded) {
        if (encoded.toString().length() > CreativeTabValidation.MAX_CODEC_JSON_LENGTH) {
            throw new JsonParseException(field + " exceeds the encoded-value size limit");
        }
    }
}
