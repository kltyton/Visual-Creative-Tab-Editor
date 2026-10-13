package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
// @target if minecraft_version=26.1.2,26.2
// @target else
import com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorNeoForgeClient;
// @target end
import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import java.util.ArrayList;
// @target if minecraft_version=26.1.2,26.2
//|import net.minecraft.resources.ResourceLocation;
// @target else
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
// @target end
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
// @target if minecraft_version=26.1.2,26.2
//|import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
// @target else
import net.neoforged.neoforge.event.AddReloadListenerEvent;
// @target end
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
// @target if minecraft_version=26.1.2,26.2
// @target else
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
// @target end

/** NeoForge loader entrypoint. */
@Mod(VisualCreativeTabEditorConstants.MOD_ID)
public final class VisualCreativeTabEditor {
    private static final String NETWORK_VERSION = "2";
// @target if minecraft_version=26.1.2,26.2
//|    private static final ResourceLocation RELOAD_LISTENER_ID = ResourceLocation.fromNamespaceAndPath(
//|            VisualCreativeTabEditorConstants.MOD_ID,
//|            "creative_tabs"
//|    );
//|
// @target else
// @target end
    /** Creates and initializes the NeoForge mod entrypoint. */
    public VisualCreativeTabEditor(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::registerPayloads);

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::addServerReloadListeners);
        // Capture only after other mods have finished their server-start creative-tab changes.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VisualCreativeTabEditor::serverStarted);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::serverStopped);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::datapackSync);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::playerLoggedOut);
        // Observe the final permission state for this tick.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VisualCreativeTabEditor::serverTick);

        CreativeTabNetworkBridge.installServerSender((player, payload) -> {
            if (player.connection.hasChannel(payload.type())) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        });
        CreativeTabNativeOrder.install(registryOrder -> {
            ArrayList<net.minecraft.world.item.CreativeModeTab> ordered = new ArrayList<>(
                    CreativeModeTabRegistry.getSortedCreativeModeTabs()
            );
            for (net.minecraft.world.item.CreativeModeTab tab : registryOrder) {
                if (!ordered.contains(tab)) {
                    ordered.add(tab);
                }
            }
            return ordered;
        });
// @target if minecraft_version=26.1.2,26.2
//|        if (FMLEnvironment.getDist() == Dist.CLIENT) {
//|            registerClient(modEventBus);
// @target else
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClient();
// @target end
        }

        com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.configure(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        VisualCreativeTabEditorCommon.initialize();
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
// @target if minecraft_version=26.1.2,26.2
//|        event.registrar(NETWORK_VERSION).optional()
//|                .playToClient(SnapshotChunkPayload.TYPE, SnapshotChunkPayload.STREAM_CODEC)
//|                .playToClient(EditResultPayload.TYPE, EditResultPayload.STREAM_CODEC)
//|                .playToServer(
//|                        EditChunkPayload.TYPE,
//|                        EditChunkPayload.STREAM_CODEC,
//|                        (payload, context) -> CreativeTabServerManager.handleEditChunk((ServerPlayer) context.player(), payload)
//|                );
// @target else
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION).optional();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            VisualCreativeTabEditorNeoForgeClient.registerPayloads(registrar);
        } else {
            registrar.playToClient(
                    SnapshotChunkPayload.TYPE,
                    SnapshotChunkPayload.STREAM_CODEC,
                    VisualCreativeTabEditor::rejectMisroutedClientboundPayload
            );
            registrar.playToClient(
                    EditResultPayload.TYPE,
                    EditResultPayload.STREAM_CODEC,
                    VisualCreativeTabEditor::rejectMisroutedClientboundPayload
            );
        }
        registrar.playToServer(
                EditChunkPayload.TYPE,
                EditChunkPayload.STREAM_CODEC,
                (payload, context) -> CreativeTabServerManager.handleEditChunk((ServerPlayer) context.player(), payload)
        );
// @target end
    }

// @target if minecraft_version=26.1.2,26.2
//|    @SuppressWarnings("removal")
//|    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
//|        event.addListener(RELOAD_LISTENER_ID, new CreativeTabReloadListener(event.getRegistryAccess()));
// @target else
    private static void rejectMisroutedClientboundPayload(
            CustomPacketPayload payload,
            IPayloadContext context
    ) {
        throw new IllegalStateException(
                "Clientbound payload " + payload.type().id() + " was delivered on a dedicated server"
        );
    }

    private static void addServerReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CreativeTabReloadListener(event.getRegistryAccess()));
// @target end
    }

    private static void serverStarted(ServerStartedEvent event) {
        CreativeTabServerManager.onServerStarted(event.getServer());
    }

    private static void serverStopped(ServerStoppedEvent event) {
        CreativeTabServerManager.onServerStopped(event.getServer());
    }

    private static void datapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(CreativeTabServerManager::syncTo);
    }

    private static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CreativeTabServerManager.onPlayerDisconnected(player);
        }
    }

    private static void serverTick(ServerTickEvent.Post event) {
        CreativeTabServerManager.refreshEditPermissions(event.getServer());
    }

// @target if minecraft_version=26.1.2,26.2
//|    private static void registerClient(IEventBus modEventBus) {
//|        try {
//|            Class.forName("com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorNeoForgeClient")
//|                    .getMethod("register", IEventBus.class)
//|                    .invoke(null, modEventBus);
//|        } catch (ReflectiveOperationException exception) {
//|            throw new IllegalStateException("Failed to register Visual Creative Tab Editor NeoForge client hooks", exception);
//|        }
// @target else
    private static void registerClient() {
        VisualCreativeTabEditorNeoForgeClient.register();
// @target end
    }
}
