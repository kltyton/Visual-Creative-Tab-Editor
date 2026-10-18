package com.kltyton.visual_creative_tab_editor.client.network;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;


/** Native client transport retains the complete packet and current connection checks. */
public final class NativeFabricClientNetworking {
    private NativeFabricClientNetworking() {}
    public static void register() {
        CreativeTabClientPlatform.installServerCapability(() -> ClientPlayNetworking.canSend(com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK));
        ClientPlayNetworking.registerGlobalReceiver(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK,
                (payload, context) -> CreativeTabClientState.handleSnapshotChunk(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT,
                (payload, context) -> CreativeTabClientState.handleEditResult(payload)
        );

        CreativeTabNetworkBridge.installClientSender(ClientPlayNetworking::send);

    }

}
