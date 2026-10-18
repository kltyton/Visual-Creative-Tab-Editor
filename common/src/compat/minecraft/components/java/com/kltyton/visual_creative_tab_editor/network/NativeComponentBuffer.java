package com.kltyton.visual_creative_tab_editor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/** NativePayloadTypes codecs supply registry-aware buffers for component messages. */
public final class NativeComponentBuffer {
    private NativeComponentBuffer() {}

    public static void write(FriendlyByteBuf buffer, Component message) {
        ComponentSerialization.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, message);
    }

    public static Component read(FriendlyByteBuf buffer) {
        return ComponentSerialization.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    }
}
