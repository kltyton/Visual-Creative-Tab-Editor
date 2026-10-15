package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
// @target if minecraft_version=1.20.1
//|import net.minecraft.network.FriendlyByteBuf;
// @target else
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
// @target end
import net.minecraft.resources.ResourceLocation;

/** One bounded chunk of a server-authoritative creative-tab snapshot. */
public record SnapshotChunkPayload(
        long revision,
        boolean canEdit,
        int index,
        int total,
        int uncompressedSize,
        byte[] chunk
// @target if minecraft_version=1.20.1
//|) implements CreativeTabPayload {
//|    public static final ResourceLocation ID =
//|            new ResourceLocation(VisualCreativeTabEditorConstants.MOD_ID, "snapshot_chunk_v3");
// @target else
) implements CustomPacketPayload {
    public static final Type<SnapshotChunkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "snapshot_chunk_v3")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotChunkPayload> STREAM_CODEC =
            CustomPacketPayload.codec(SnapshotChunkPayload::write, SnapshotChunkPayload::decode);
// @target end

    public SnapshotChunkPayload {
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        PayloadChunks.validateChunkMetadata(index, total);
        PayloadChunks.checkUncompressedSize(uncompressedSize);
        chunk = PayloadChunks.copyAndValidateChunk(chunk);
    }

    @Override
    public byte[] chunk() {
        return chunk.clone();
    }

    @Override
// @target if minecraft_version=1.20.1
//|    public ResourceLocation id() {
//|        return ID;
// @target else
    public Type<SnapshotChunkPayload> type() {
        return TYPE;
// @target end
    }

// @target if minecraft_version=1.20.1
//|    @Override
//|    public void write(FriendlyByteBuf buffer) {
// @target else
    private void write(RegistryFriendlyByteBuf buffer) {
// @target end
        buffer.writeVarLong(revision);
        buffer.writeBoolean(canEdit);
        buffer.writeVarInt(index);
        buffer.writeVarInt(total);
        buffer.writeVarInt(uncompressedSize);
        buffer.writeByteArray(chunk);
    }

// @target if minecraft_version=1.20.1
//|    public static SnapshotChunkPayload decode(FriendlyByteBuf buffer) {
// @target else
    private static SnapshotChunkPayload decode(RegistryFriendlyByteBuf buffer) {
// @target end
        try {
            return new SnapshotChunkPayload(
                    buffer.readVarLong(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readByteArray(PayloadChunks.MAX_CHUNK_BYTES)
            );
        } catch (DecoderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DecoderException("Invalid creative-tab snapshot chunk", exception);
        }
    }
}
