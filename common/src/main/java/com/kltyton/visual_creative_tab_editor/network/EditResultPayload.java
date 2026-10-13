package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import io.netty.handler.codec.DecoderException;
import java.util.Objects;
import java.util.UUID;
// @target if minecraft_version=1.20.1
//|import net.minecraft.network.FriendlyByteBuf;
// @target else
import net.minecraft.network.RegistryFriendlyByteBuf;
// @target end
import net.minecraft.network.chat.Component;
// @target if minecraft_version=1.20.1
// @target else
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
// @target end
import net.minecraft.resources.ResourceLocation;

/** Server acknowledgement for one completed edit transaction. */
public record EditResultPayload(
        UUID sessionId,
        boolean success,
        long revision,
        boolean canEdit,
        Component message
// @target if minecraft_version=1.20.1
//|) implements CreativeTabPayload {
//|    public static final ResourceLocation ID =
//|            new ResourceLocation(VisualCreativeTabEditorConstants.MOD_ID, "edit_result");
// @target else
) implements CustomPacketPayload {
    public static final Type<EditResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "edit_result")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EditResultPayload> STREAM_CODEC =
            CustomPacketPayload.codec(EditResultPayload::write, EditResultPayload::decode);
// @target end

    public EditResultPayload {
        Objects.requireNonNull(sessionId, "sessionId");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        Objects.requireNonNull(message, "message");
    }

    @Override
// @target if minecraft_version=1.20.1
//|    public ResourceLocation id() {
//|        return ID;
// @target else
    public Type<EditResultPayload> type() {
        return TYPE;
// @target end
    }

// @target if minecraft_version=1.20.1
//|    @Override
//|    public void write(FriendlyByteBuf buffer) {
// @target else
    private void write(RegistryFriendlyByteBuf buffer) {
// @target end
        buffer.writeUUID(sessionId);
        buffer.writeBoolean(success);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(canEdit);
// @target if minecraft_version=1.20.1
//|        buffer.writeComponent(message);
// @target else
        ComponentSerialization.STREAM_CODEC.encode(buffer, message);
// @target end
    }

// @target if minecraft_version=1.20.1
//|    public static EditResultPayload decode(FriendlyByteBuf buffer) {
// @target else
    private static EditResultPayload decode(RegistryFriendlyByteBuf buffer) {
// @target end
        try {
            return new EditResultPayload(
                    buffer.readUUID(),
                    buffer.readBoolean(),
                    buffer.readVarLong(),
                    buffer.readBoolean(),
// @target if minecraft_version=1.20.1
//|                    buffer.readComponent()
// @target else
                    ComponentSerialization.STREAM_CODEC.decode(buffer)
// @target end
            );
        } catch (DecoderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DecoderException("Invalid creative-tab edit result", exception);
        }
    }
}
