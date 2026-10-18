package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

/** Forge loader entrypoint. */
@Mod(VisualCreativeTabEditorConstants.MOD_ID)
public final class VisualCreativeTabEditor {
    /** Creates and initializes the Forge mod entrypoint. */
    public VisualCreativeTabEditor() {
        VisualCreativeTabEditorForgeNetwork.initialize();

        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::addReloadListeners);
        // Capture only after other mods have finished their server-start creative-tab changes.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, VisualCreativeTabEditor::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::datapackSync);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditor::playerLoggedOut);
        // Observe the final permission state for this tick.
        com.kltyton.visual_creative_tab_editor.platform.forge.ForgeLifecycle.registerPermissionTicks();

        CreativeTabNetworkBridge.installServerSender(VisualCreativeTabEditorForgeNetwork::sendToPlayer);
        CreativeTabNativeOrder.install(registryOrder -> CreativeTabNativeOrder.complete(
                registryOrder, CreativeModeTabRegistry.getSortedCreativeModeTabs()));

        com.kltyton.visual_creative_tab_editor.platform.forge.ForgeLifecycle.initializeClient();

        com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.configure(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        VisualCreativeTabEditorCommon.initialize();
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CreativeTabReloadListener(event.getRegistryAccess()));
    }

    private static void serverStarted(ServerStartedEvent event) {
        CreativeTabServerManager.onServerStarted(event.getServer());
    }

    private static void serverStopped(ServerStoppedEvent event) {
        CreativeTabServerManager.onServerStopped(event.getServer());
    }

    private static void datapackSync(OnDatapackSyncEvent event) {
        event.getPlayers().forEach(CreativeTabServerManager::syncTo);
    }

    private static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CreativeTabServerManager.onPlayerDisconnected(player);
        }
    }
}
