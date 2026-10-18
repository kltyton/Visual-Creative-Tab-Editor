package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;

/** Server acknowledgement for one completed edit transaction. */
public record EditResultPayload(
        UUID sessionId,
        boolean success,
        long revision,
        boolean canEdit,
        Component message
) implements CreativeTabPayload {
    public static final ResourceId ID =
            ResourceId.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "edit_result_v4");

    public EditResultPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        Objects.requireNonNull(message, "message");
    }

    @Override
    public ResourceId payloadId() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeBoolean(success);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(canEdit);
        NativeComponentBuffer.write(buffer, message);
    }

    public static EditResultPayload decode(FriendlyByteBuf buffer) {
        try {
            return new EditResultPayload(
                    buffer.readUUID(),
                    buffer.readBoolean(),
                    buffer.readVarLong(),
                    buffer.readBoolean(),
                    NativeComponentBuffer.read(buffer)
            );
        } catch (DecoderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DecoderException("Invalid creative-tab edit result", exception);
        }
    }
}
