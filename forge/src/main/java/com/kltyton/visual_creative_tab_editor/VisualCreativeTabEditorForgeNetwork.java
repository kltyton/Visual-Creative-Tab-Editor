package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorForgeClient;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Forge 52 SimpleChannel transport for the loader-neutral payload records. */
public final class VisualCreativeTabEditorForgeNetwork {
    private static final int NETWORK_VERSION = 2;
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(
                    VisualCreativeTabEditorConstants.MOD_ID,
                    "main"
            ))
            .networkProtocolVersion(NETWORK_VERSION)
            .optional()
            .simpleChannel();
    public static boolean isRemotePresent(net.minecraft.network.Connection connection) {
        return CHANNEL.isRemotePresent(connection);
    }

    private static boolean initialized;

    private VisualCreativeTabEditorForgeNetwork() {
    }

    /** Registers all three packet directions exactly once. */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        CHANNEL.messageBuilder(SnapshotChunkPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .codec(SnapshotChunkPayload.STREAM_CODEC)
                .consumerMainThread(VisualCreativeTabEditorForgeNetwork::handleSnapshotChunk)
                .add();
        CHANNEL.messageBuilder(EditChunkPayload.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .codec(EditChunkPayload.STREAM_CODEC)
                .consumerMainThread(VisualCreativeTabEditorForgeNetwork::handleEditChunk)
                .add();
        CHANNEL.messageBuilder(EditResultPayload.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .codec(EditResultPayload.STREAM_CODEC)
                .consumerMainThread(VisualCreativeTabEditorForgeNetwork::handleEditResult)
                .add();
        CHANNEL.build();
        initialized = true;
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (CHANNEL.isRemotePresent(player.connection.getConnection())) {
            CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
        }
    }

    public static void sendToServer(CustomPacketPayload payload) {
        CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
    }

    private static void handleSnapshotChunk(
            SnapshotChunkPayload payload,
            CustomPayloadEvent.Context context
    ) {
        requireClientbound(context, payload);
        VisualCreativeTabEditorForgeClient.handleSnapshotChunk(payload);
    }

    private static void handleEditResult(
            EditResultPayload payload,
            CustomPayloadEvent.Context context
    ) {
        requireClientbound(context, payload);
        VisualCreativeTabEditorForgeClient.handleEditResult(payload);
    }

    private static void handleEditChunk(
            EditChunkPayload payload,
            CustomPayloadEvent.Context context
    ) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            throw new IllegalStateException("Serverbound creative-tab edit payload has no sender");
        }
        CreativeTabServerManager.handleEditChunk(player, payload);
    }

    private static void requireClientbound(
            CustomPayloadEvent.Context context,
            CustomPacketPayload payload
    ) {
        if (!context.isClientSide()) {
            throw new IllegalStateException(
                    "Clientbound payload " + payload.type().id() + " was delivered on the logical server"
            );
        }
    }
}
