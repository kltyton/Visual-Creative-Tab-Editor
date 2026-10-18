package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only NeoForge network bootstrap, loaded reflectively by the common NeoForge entrypoint. */
public final class VisualCreativeTabEditorNeoForgeClient {
    private VisualCreativeTabEditorNeoForgeClient() {
    }

    /** Registers client payload handlers and the client-to-server transport bridge. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorNeoForgeClient::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorNeoForgeClient::loggingOut);
        com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig.load(
                net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        CreativeTabNetworkBridge.installClientSender(ClientPacketDistributor::sendToServer);
        CreativeTabClientPlatform.installServerCapability(() -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            return connection != null && connection.hasChannel(com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK);
        });
        com.kltyton.visual_creative_tab_editor.client.paging.CreativeTabPaging.install(com.kltyton.visual_creative_tab_editor.client.paging.NativePageAccess.INSTANCE);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK,
                (payload, context) -> CreativeTabClientState.handleSnapshotChunk(payload)
        );
        event.register(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT,
                (payload, context) -> CreativeTabClientState.handleEditResult(payload)
        );
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeTabClientState.clear();
    }
}
