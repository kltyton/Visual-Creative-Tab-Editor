package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Client-only NeoForge network, paging and connection bootstrap. */
public final class VisualCreativeTabEditorNeoForgeClient {
    private VisualCreativeTabEditorNeoForgeClient() {
    }

    /** Registers client payload handlers and the client-to-server transport bridge. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorNeoForgeClient::loggingOut);
        com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig.load(
                net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        CreativeTabNetworkBridge.installClientSender(PacketDistributor::sendToServer);
        CreativeTabClientPlatform.installServerCapability(() -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            return connection != null && connection.hasChannel(com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK);
        });
        com.kltyton.visual_creative_tab_editor.client.paging.CreativeTabPaging.install(com.kltyton.visual_creative_tab_editor.client.paging.NativePageAccess.INSTANCE);
    }

    /** Registers the clientbound codecs and handlers on the physical client. */
    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK,
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK_CODEC,
                VisualCreativeTabEditorNeoForgeClient::handleSnapshotChunk
        );
        registrar.playToClient(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT,
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT_CODEC,
                VisualCreativeTabEditorNeoForgeClient::handleEditResult
        );
    }

    private static void handleSnapshotChunk(SnapshotChunkPayload payload, IPayloadContext context) {
        CreativeTabClientState.handleSnapshotChunk(payload);
    }

    private static void handleEditResult(EditResultPayload payload, IPayloadContext context) {
        CreativeTabClientState.handleEditResult(payload);
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeTabClientState.clear();
    }
}
