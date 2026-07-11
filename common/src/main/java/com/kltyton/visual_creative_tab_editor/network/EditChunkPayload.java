package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One bounded chunk of a client edit transaction. */
public record EditChunkPayload(
        UUID sessionId,
        long baseRevision,
        int index,
        int total,
        byte[] chunk
) implements CustomPacketPayload {
    public static final Type<EditChunkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "edit_chunk")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EditChunkPayload> STREAM_CODEC =
            CustomPacketPayload.codec(EditChunkPayload::write, EditChunkPayload::decode);

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
    public Type<EditChunkPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeVarLong(baseRevision);
        buffer.writeVarInt(index);
        buffer.writeVarInt(total);
        buffer.writeByteArray(chunk);
    }

    private static EditChunkPayload decode(RegistryFriendlyByteBuf buffer) {
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
