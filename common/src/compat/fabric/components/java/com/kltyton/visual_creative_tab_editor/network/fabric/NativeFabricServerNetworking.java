package com.kltyton.visual_creative_tab_editor.network.fabric;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;


/** Native server transport keeps direction, connection identity and thread scheduling intact. */
public final class NativeFabricServerNetworking {
    private NativeFabricServerNetworking() {}
    public static void register() {
        NativeFabricPayloadRegistry.register();
        ServerPlayNetworking.registerGlobalReceiver(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK,
                (payload, context) -> CreativeTabServerManager.handleEditChunk(context.player(), payload)
        );
        CreativeTabNetworkBridge.installServerSender((player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) {
                ServerPlayNetworking.send(player, payload);
            }
        });


    }

}
