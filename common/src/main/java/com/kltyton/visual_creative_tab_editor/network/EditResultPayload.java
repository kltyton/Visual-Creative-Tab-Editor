package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server acknowledgement for one completed edit transaction. */
public record EditResultPayload(
        UUID sessionId,
        boolean success,
        long revision,
        boolean canEdit,
        Component message
) implements CustomPacketPayload {
    public static final Type<EditResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "edit_result")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EditResultPayload> STREAM_CODEC =
            CustomPacketPayload.codec(EditResultPayload::write, EditResultPayload::decode);

    public EditResultPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        Objects.requireNonNull(message, "message");
    }

    @Override
    public Type<EditResultPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeBoolean(success);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(canEdit);
        ComponentSerialization.STREAM_CODEC.encode(buffer, message);
    }

    private static EditResultPayload decode(RegistryFriendlyByteBuf buffer) {
        try {
            return new EditResultPayload(
                    buffer.readUUID(),
                    buffer.readBoolean(),
                    buffer.readVarLong(),
                    buffer.readBoolean(),
                    ComponentSerialization.STREAM_CODEC.decode(buffer)
            );
        } catch (DecoderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DecoderException("Invalid creative-tab edit result", exception);
        }
    }
}
