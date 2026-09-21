package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorNeoForgeClient;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import java.util.ArrayList;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge loader entrypoint. */
@Mod(VisualCreativeTabEditorConstants.MOD_ID)
public final class VisualCreativeTabEditor {
    private static final String NETWORK_VERSION = "2";
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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClient();
        }

        com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.configure(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        VisualCreativeTabEditorCommon.initialize();
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
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
    }

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

    private static void registerClient() {
        VisualCreativeTabEditorNeoForgeClient.register();
    }
}
