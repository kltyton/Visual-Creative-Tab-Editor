package com.kltyton.visual_creative_tab_editor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Legacy component messages use the native bounded JSON representation. */
public final class NativeComponentBuffer {
    private NativeComponentBuffer() {}

    public static void write(FriendlyByteBuf buffer, Component message) {
        buffer.writeComponent(message);
    }

    public static Component read(FriendlyByteBuf buffer) {
        return buffer.readComponent();
    }
}
