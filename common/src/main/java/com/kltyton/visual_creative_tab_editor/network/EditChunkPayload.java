package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;

/** One bounded chunk of a client edit transaction. */
public record EditChunkPayload(
        UUID sessionId,
        long baseRevision,
        int index,
        int total,
        byte[] chunk
) implements CreativeTabPayload {
    public static final ResourceId ID =
            ResourceId.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "edit_chunk_v4");

    public EditChunkPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        if (baseRevision < 0) {
            throw new IllegalArgumentException("baseRevision must be non-negative");
        }
        PayloadChunks.validateChunkMetadata(index, total);
        chunk = PayloadChunks.copyAndValidateChunk(chunk);
    }

    @Override
    public byte[] chunk() {
        return chunk.clone();
    }

    @Override
    public ResourceId payloadId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeVarLong(baseRevision);
        buffer.writeVarInt(index);
        buffer.writeVarInt(total);
        buffer.writeByteArray(chunk);
    }

    public static EditChunkPayload decode(FriendlyByteBuf buffer) {
        try {
            return new EditChunkPayload(
                    buffer.readUUID(),
                    buffer.readVarLong(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readByteArray(PayloadChunks.MAX_CHUNK_BYTES)
            );
        } catch (DecoderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DecoderException("Invalid creative-tab edit chunk", exception);
        }
    }
}
