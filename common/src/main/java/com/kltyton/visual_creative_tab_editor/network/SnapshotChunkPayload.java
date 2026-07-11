package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One bounded chunk of a server-authoritative creative-tab snapshot. */
public record SnapshotChunkPayload(
        long revision,
        boolean canEdit,
        int index,
        int total,
        int uncompressedSize,
        byte[] chunk
) implements CustomPacketPayload {
    public static final Type<SnapshotChunkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "snapshot_chunk")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotChunkPayload> STREAM_CODEC =
            CustomPacketPayload.codec(SnapshotChunkPayload::write, SnapshotChunkPayload::decode);

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
    public Type<SnapshotChunkPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarLong(revision);
        buffer.writeBoolean(canEdit);
        buffer.writeVarInt(index);
        buffer.writeVarInt(total);
        buffer.writeVarInt(uncompressedSize);
        buffer.writeByteArray(chunk);
    }

    private static SnapshotChunkPayload decode(RegistryFriendlyByteBuf buffer) {
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
