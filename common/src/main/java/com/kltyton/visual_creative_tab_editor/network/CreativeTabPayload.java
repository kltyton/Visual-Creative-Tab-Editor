package com.kltyton.visual_creative_tab_editor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Loader-neutral 1.20.1 payload contract backed by a channel id and FriendlyByteBuf codec. */
public interface CreativeTabPayload {
    ResourceLocation id();

    void write(FriendlyByteBuf buffer);
}
