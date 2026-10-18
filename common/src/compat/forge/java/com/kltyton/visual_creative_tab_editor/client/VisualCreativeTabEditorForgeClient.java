package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorForgeNetwork;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Client-only Forge paging, refresh and connection bootstrap. */
public final class VisualCreativeTabEditorForgeClient {
    private VisualCreativeTabEditorForgeClient() {
    }

    /** Installs client-only hooks after the physical-side guard in the mod entrypoint. */
    public static void register() {
        com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig.load(
                net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorForgeClient::loggingOut);
        CreativeTabNetworkBridge.installClientSender(VisualCreativeTabEditorForgeNetwork::sendToServer);
        CreativeTabClientPlatform.installServerCapability(() -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            return connection != null && com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorForgeNetwork
                    .isRemotePresent(connection.getConnection());
        });
        com.kltyton.visual_creative_tab_editor.client.paging.CreativeTabPaging.install(com.kltyton.visual_creative_tab_editor.client.paging.NativePageAccess.INSTANCE);
    }

    public static void handleSnapshotChunk(SnapshotChunkPayload payload) {
        CreativeTabClientState.handleSnapshotChunk(payload);
    }

    public static void handleEditResult(EditResultPayload payload) {
        CreativeTabClientState.handleEditResult(payload);
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeTabClientState.clear();
    }
}
