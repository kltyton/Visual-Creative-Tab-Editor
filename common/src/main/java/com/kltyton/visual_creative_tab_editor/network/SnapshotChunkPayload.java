package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** One bounded chunk of a server-authoritative creative-tab snapshot. */
public record SnapshotChunkPayload(
        long revision,
        boolean canEdit,
        int index,
        int total,
        int uncompressedSize,
        byte[] chunk
) implements CreativeTabPayload {
    public static final ResourceLocation ID =
            new ResourceLocation(VisualCreativeTabEditorConstants.MOD_ID, "snapshot_chunk");

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
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarLong(revision);
        buffer.writeBoolean(canEdit);
        buffer.writeVarInt(index);
        buffer.writeVarInt(total);
        buffer.writeVarInt(uncompressedSize);
        buffer.writeByteArray(chunk);
    }

    public static SnapshotChunkPayload decode(FriendlyByteBuf buffer) {
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
