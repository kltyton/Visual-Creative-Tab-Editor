package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Registers editor DTOs with native payload types and registry-aware codecs. */
public final class NativePayloadTypes {
    private NativePayloadTypes() {}
    public static final CustomPacketPayload.Type<EditChunkPayload> EDIT_CHUNK =
            new CustomPacketPayload.Type<>(NativeIds.unwrap(EditChunkPayload.ID));
    public static final CustomPacketPayload.Type<SnapshotChunkPayload> SNAPSHOT_CHUNK =
            new CustomPacketPayload.Type<>(NativeIds.unwrap(SnapshotChunkPayload.ID));
    public static final CustomPacketPayload.Type<EditResultPayload> EDIT_RESULT =
            new CustomPacketPayload.Type<>(NativeIds.unwrap(EditResultPayload.ID));
    public static final StreamCodec<RegistryFriendlyByteBuf, EditChunkPayload> EDIT_CHUNK_CODEC =
            CustomPacketPayload.codec(EditChunkPayload::write, EditChunkPayload::decode);
    public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotChunkPayload> SNAPSHOT_CHUNK_CODEC =
            CustomPacketPayload.codec(SnapshotChunkPayload::write, SnapshotChunkPayload::decode);
    public static final StreamCodec<RegistryFriendlyByteBuf, EditResultPayload> EDIT_RESULT_CODEC =
            CustomPacketPayload.codec(EditResultPayload::write, EditResultPayload::decode);
}
