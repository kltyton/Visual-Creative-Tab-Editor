package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorForgeClient;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabPayload;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Forge 47 SimpleChannel transport for the loader-neutral payload records. */
public final class VisualCreativeTabEditorForgeNetwork {
    private static final String NETWORK_VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VisualCreativeTabEditorConstants.MOD_ID, "main"),
            () -> NETWORK_VERSION,
            NETWORK_VERSION::equals,
            NETWORK_VERSION::equals
    );
    private static boolean initialized;

    private VisualCreativeTabEditorForgeNetwork() {
    }

    /** Registers the three bounded payloads exactly once. */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        CHANNEL.registerMessage(
                0,
                SnapshotChunkPayload.class,
                SnapshotChunkPayload::write,
                SnapshotChunkPayload::decode,
                VisualCreativeTabEditorForgeNetwork::handleSnapshotChunk,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                1,
                EditChunkPayload.class,
                EditChunkPayload::write,
                EditChunkPayload::decode,
                VisualCreativeTabEditorForgeNetwork::handleEditChunk,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                2,
                EditResultPayload.class,
                EditResultPayload::write,
                EditResultPayload::decode,
                VisualCreativeTabEditorForgeNetwork::handleEditResult,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        initialized = true;
    }

    public static void sendToPlayer(ServerPlayer player, CreativeTabPayload payload) {
        if (!(payload instanceof SnapshotChunkPayload) && !(payload instanceof EditResultPayload)) {
            throw new IllegalArgumentException("Unsupported clientbound creative-tab payload " + payload.id());
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToServer(CreativeTabPayload payload) {
        if (!(payload instanceof EditChunkPayload)) {
            throw new IllegalArgumentException("Unsupported serverbound creative-tab payload " + payload.id());
        }
        CHANNEL.sendToServer(payload);
    }

    private static void handleSnapshotChunk(
            SnapshotChunkPayload payload,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = requireDirection(contextSupplier, NetworkDirection.PLAY_TO_CLIENT, payload);
        context.enqueueWork(() -> VisualCreativeTabEditorForgeClient.handleSnapshotChunk(payload));
        context.setPacketHandled(true);
    }

    private static void handleEditResult(
            EditResultPayload payload,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = requireDirection(contextSupplier, NetworkDirection.PLAY_TO_CLIENT, payload);
        context.enqueueWork(() -> VisualCreativeTabEditorForgeClient.handleEditResult(payload));
        context.setPacketHandled(true);
    }

    private static void handleEditChunk(
            EditChunkPayload payload,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = requireDirection(contextSupplier, NetworkDirection.PLAY_TO_SERVER, payload);
        ServerPlayer player = context.getSender();
        if (player == null) {
            throw new IllegalStateException("Serverbound creative-tab edit payload has no sender");
        }
        context.enqueueWork(() -> CreativeTabServerManager.handleEditChunk(player, payload));
        context.setPacketHandled(true);
    }

    private static NetworkEvent.Context requireDirection(
            Supplier<NetworkEvent.Context> contextSupplier,
            NetworkDirection expected,
            CreativeTabPayload payload
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() != expected) {
            throw new IllegalStateException(
                    "Creative-tab payload " + payload.id() + " arrived through " + context.getDirection()
            );
        }
        return context;
    }
}
