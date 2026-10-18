package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The editor payload stream carried by Minecraft's typed custom payload protocol. */
public sealed interface CreativeTabPayload extends CustomPacketPayload
        permits EditChunkPayload, SnapshotChunkPayload, EditResultPayload {
    ResourceId payloadId();
    void write(FriendlyByteBuf buffer);
    @Override
    default Type<? extends CustomPacketPayload> type() {
        return switch (this) {
            case EditChunkPayload payload -> NativePayloadTypes.EDIT_CHUNK;
            case SnapshotChunkPayload payload -> NativePayloadTypes.SNAPSHOT_CHUNK;
            case EditResultPayload payload -> NativePayloadTypes.EDIT_RESULT;
        };
    }
}
