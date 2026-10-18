package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

/** Native legacy channels retain the editor's protocol identifier and field stream. */
public interface CreativeTabPayload {
    ResourceId payloadId();
    default ResourceLocation id() { return NativeIds.unwrap(payloadId()); }
    void write(FriendlyByteBuf buffer);
}
