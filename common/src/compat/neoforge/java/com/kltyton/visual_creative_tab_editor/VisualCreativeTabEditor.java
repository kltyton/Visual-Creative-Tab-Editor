package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge loader entrypoint. */
@Mod(VisualCreativeTabEditorConstants.MOD_ID)
public final class VisualCreativeTabEditor {
    /** Creates and initializes the NeoForge mod entrypoint. */
    public VisualCreativeTabEditor(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(EventPriority.HIGHEST, com.kltyton.visual_creative_tab_editor.platform.neoforge.NeoForgePayloadRegistration::register);

        com.kltyton.visual_creative_tab_editor.platform.neoforge.NeoForgeLifecycle.registerReloads();
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
        CreativeTabNativeOrder.install(registryOrder -> CreativeTabNativeOrder.complete(
                registryOrder, CreativeModeTabRegistry.getSortedCreativeModeTabs()));
        com.kltyton.visual_creative_tab_editor.platform.neoforge.NeoForgeLifecycle.registerClient(modEventBus);

        com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.configure(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        VisualCreativeTabEditorCommon.initialize();
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
}
