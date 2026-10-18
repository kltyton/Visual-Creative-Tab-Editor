package com.kltyton.visual_creative_tab_editor.data.codec;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

/** Registry-aware native values retain pending tags and the actual holder owner. */
public final class NativeValueCodec {
    private final RegistryOps<JsonElement> ops;
    public NativeValueCodec(HolderLookup.Provider registries) { this.ops = createRegistryOps(registries); }
    public Component decodeComponent(String field, JsonElement input) { return decodeCodec(field, ComponentSerialization.CODEC, input, ops); }
    public ItemStack decodeItem(String field, JsonElement input) { return decodeCodec(field, ItemStack.CODEC, input, ops); }
    public JsonElement encodeComponent(String field, Component value) { return encodeCodec(field, ComponentSerialization.CODEC, value, ops); }
    public JsonElement encodeItem(String field, ItemStack value) { return encodeCodec(field, ItemStack.CODEC, value, ops); }


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
}
