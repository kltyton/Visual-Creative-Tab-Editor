package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorForgeClient;
// @target if minecraft_version=1.20.1
//|import com.kltyton.visual_creative_tab_editor.network.CreativeTabPayload;
// @target else
// @target end
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
// @target if minecraft_version=1.20.1
//|import java.util.Optional;
//|import java.util.function.Supplier;
// @target else
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
// @target end
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
// @target if minecraft_version=1.20.1
// @target else
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
// @target end
import net.minecraftforge.network.NetworkDirection;
// @target if minecraft_version=1.20.1
//|import net.minecraftforge.network.NetworkEvent;
//|import net.minecraftforge.network.NetworkRegistry;
// @target else
// @target end
import net.minecraftforge.network.PacketDistributor;
// @target if minecraft_version=1.20.1
//|import net.minecraftforge.network.simple.SimpleChannel;
// @target else
import net.minecraftforge.network.SimpleChannel;
// @target end

// @target if minecraft_version=1.20.1
//|/** Forge 47 SimpleChannel transport for the loader-neutral payload records. */
// @target else
/** Forge 52 SimpleChannel transport for the loader-neutral payload records. */
// @target end
public final class VisualCreativeTabEditorForgeNetwork {
// @target if minecraft_version=1.20.1
//|    private static final String NETWORK_VERSION = "2";
//|    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
//|            new ResourceLocation(VisualCreativeTabEditorConstants.MOD_ID, "main"),
//|            () -> NETWORK_VERSION,
//|            version -> NETWORK_VERSION.equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version),
//|            version -> NETWORK_VERSION.equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version)
//|    );
// @target else
    private static final int NETWORK_VERSION = 2;
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(
                    VisualCreativeTabEditorConstants.MOD_ID,
                    "main"
            ))
            .networkProtocolVersion(NETWORK_VERSION)
            .optional()
            .simpleChannel();
// @target end
    public static boolean isRemotePresent(net.minecraft.network.Connection connection) {
        return CHANNEL.isRemotePresent(connection);
    }

    private static boolean initialized;

    private VisualCreativeTabEditorForgeNetwork() {
    }

// @target if minecraft_version=1.20.1
//|    /** Registers the three bounded payloads exactly once. */
// @target else
    /** Registers all three packet directions exactly once. */
// @target end
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
// @target if minecraft_version=1.20.1
//|        CHANNEL.registerMessage(
//|                0,
//|                SnapshotChunkPayload.class,
//|                SnapshotChunkPayload::write,
//|                SnapshotChunkPayload::decode,
//|                VisualCreativeTabEditorForgeNetwork::handleSnapshotChunk,
//|                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
//|        );
//|        CHANNEL.registerMessage(
//|                1,
//|                EditChunkPayload.class,
//|                EditChunkPayload::write,
//|                EditChunkPayload::decode,
//|                VisualCreativeTabEditorForgeNetwork::handleEditChunk,
//|                Optional.of(NetworkDirection.PLAY_TO_SERVER)
//|        );
//|        CHANNEL.registerMessage(
//|                2,
//|                EditResultPayload.class,
//|                EditResultPayload::write,
//|                EditResultPayload::decode,
//|                VisualCreativeTabEditorForgeNetwork::handleEditResult,
//|                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
//|        );
// @target else
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
// @target end
        initialized = true;
    }

// @target if minecraft_version=1.20.1
//|    public static void sendToPlayer(ServerPlayer player, CreativeTabPayload payload) {
//|        if (!(payload instanceof SnapshotChunkPayload) && !(payload instanceof EditResultPayload)) {
//|            throw new IllegalArgumentException("Unsupported clientbound creative-tab payload " + payload.id());
//|        }
//|        if (CHANNEL.isRemotePresent(player.connection.connection)) {
//|            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
// @target else
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (CHANNEL.isRemotePresent(player.connection.getConnection())) {
            CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
// @target end
        }
    }

// @target if minecraft_version=1.20.1
//|    public static void sendToServer(CreativeTabPayload payload) {
//|        if (!(payload instanceof EditChunkPayload)) {
//|            throw new IllegalArgumentException("Unsupported serverbound creative-tab payload " + payload.id());
//|        }
//|        CHANNEL.sendToServer(payload);
// @target else
    public static void sendToServer(CustomPacketPayload payload) {
        CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
// @target end
    }

    private static void handleSnapshotChunk(
            SnapshotChunkPayload payload,
// @target if minecraft_version=1.20.1
//|            Supplier<NetworkEvent.Context> contextSupplier
// @target else
            CustomPayloadEvent.Context context
// @target end
    ) {
// @target if minecraft_version=1.20.1
//|        NetworkEvent.Context context = requireDirection(contextSupplier, NetworkDirection.PLAY_TO_CLIENT, payload);
//|        context.enqueueWork(() -> VisualCreativeTabEditorForgeClient.handleSnapshotChunk(payload));
//|        context.setPacketHandled(true);
// @target else
        requireClientbound(context, payload);
        VisualCreativeTabEditorForgeClient.handleSnapshotChunk(payload);
// @target end
    }

    private static void handleEditResult(
            EditResultPayload payload,
// @target if minecraft_version=1.20.1
//|            Supplier<NetworkEvent.Context> contextSupplier
// @target else
            CustomPayloadEvent.Context context
// @target end
    ) {
// @target if minecraft_version=1.20.1
//|        NetworkEvent.Context context = requireDirection(contextSupplier, NetworkDirection.PLAY_TO_CLIENT, payload);
//|        context.enqueueWork(() -> VisualCreativeTabEditorForgeClient.handleEditResult(payload));
//|        context.setPacketHandled(true);
// @target else
        requireClientbound(context, payload);
        VisualCreativeTabEditorForgeClient.handleEditResult(payload);
// @target end
    }

    private static void handleEditChunk(
            EditChunkPayload payload,
// @target if minecraft_version=1.20.1
//|            Supplier<NetworkEvent.Context> contextSupplier
// @target else
            CustomPayloadEvent.Context context
// @target end
    ) {
// @target if minecraft_version=1.20.1
//|        NetworkEvent.Context context = requireDirection(contextSupplier, NetworkDirection.PLAY_TO_SERVER, payload);
// @target else
// @target end
        ServerPlayer player = context.getSender();
        if (player == null) {
            throw new IllegalStateException("Serverbound creative-tab edit payload has no sender");
        }
// @target if minecraft_version=1.20.1
//|        context.enqueueWork(() -> CreativeTabServerManager.handleEditChunk(player, payload));
//|        context.setPacketHandled(true);
// @target else
        CreativeTabServerManager.handleEditChunk(player, payload);
// @target end
    }

// @target if minecraft_version=1.20.1
//|    private static NetworkEvent.Context requireDirection(
//|            Supplier<NetworkEvent.Context> contextSupplier,
//|            NetworkDirection expected,
//|            CreativeTabPayload payload
// @target else
    private static void requireClientbound(
            CustomPayloadEvent.Context context,
            CustomPacketPayload payload
// @target end
    ) {
// @target if minecraft_version=1.20.1
//|        NetworkEvent.Context context = contextSupplier.get();
//|        if (context.getDirection() != expected) {
// @target else
        if (!context.isClientSide()) {
// @target end
            throw new IllegalStateException(
// @target if minecraft_version=1.20.1
//|                    "Creative-tab payload " + payload.id() + " arrived through " + context.getDirection()
// @target else
                    "Clientbound payload " + payload.type().id() + " was delivered on the logical server"
// @target end
            );
        }
// @target if minecraft_version=1.20.1
//|        return context;
// @target else
// @target end
    }
}
