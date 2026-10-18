package com.kltyton.visual_creative_tab_editor.platform.neoforge;

import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;


/** Registers the native optional protocol and preserves its physical-side handlers. */
public final class NeoForgePayloadRegistration {
    private static final String NETWORK_VERSION = "4";
    private NeoForgePayloadRegistration() {}


    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).optional()
                .playToClient(com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK, com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK_CODEC)
                .playToClient(com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT, com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT_CODEC)
                .playToServer(
                        com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK,
                        com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK_CODEC,
                        (payload, context) -> CreativeTabServerManager.handleEditChunk((ServerPlayer) context.player(), payload)
                );
    }

}
