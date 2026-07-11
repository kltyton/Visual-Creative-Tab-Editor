package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** NeoForge loader entrypoint. */
@Mod(VisualCreativeTabEditorConstants.MOD_ID)
public final class VisualCreativeTabEditor {
    private static final String NETWORK_VERSION = "2";
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(
            VisualCreativeTabEditorConstants.MOD_ID,
            "creative_tabs"
    );

    /** Creates and initializes the NeoForge mod entrypoint. */
    public VisualCreativeTabEditor(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(VisualCreativeTabEditor::registerPayloads);

        NeoForge.EVENT_BUS.addListener(VisualCreativeTabEditor::addServerReloadListeners);
        NeoForge.EVENT_BUS.addListener(VisualCreativeTabEditor::serverStarted);
        NeoForge.EVENT_BUS.addListener(VisualCreativeTabEditor::serverStopped);
        NeoForge.EVENT_BUS.addListener(VisualCreativeTabEditor::datapackSync);
        NeoForge.EVENT_BUS.addListener(VisualCreativeTabEditor::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(VisualCreativeTabEditor::serverTick);

        CreativeTabNetworkBridge.installServerSender(PacketDistributor::sendToPlayer);
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
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            registerClient(modEventBus);
        }

        VisualCreativeTabEditorCommon.initialize();
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToClient(SnapshotChunkPayload.TYPE, SnapshotChunkPayload.STREAM_CODEC)
                .playToClient(EditResultPayload.TYPE, EditResultPayload.STREAM_CODEC)
                .playToServer(
                        EditChunkPayload.TYPE,
                        EditChunkPayload.STREAM_CODEC,
                        (payload, context) -> CreativeTabServerManager.handleEditChunk((ServerPlayer) context.player(), payload)
                );
    }

    @SuppressWarnings("removal")
    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(RELOAD_LISTENER_ID, new CreativeTabReloadListener(event.getRegistryAccess()));
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

    private static void registerClient(IEventBus modEventBus) {
        try {
            Class.forName("com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorNeoForgeClient")
                    .getMethod("register", IEventBus.class)
                    .invoke(null, modEventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Visual Creative Tab Editor NeoForge client hooks", exception);
        }
    }
}
