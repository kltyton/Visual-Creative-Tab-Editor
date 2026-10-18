package com.kltyton.visual_creative_tab_editor.client.network;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.handler.codec.DecoderException;

/** Native client transport retains the complete packet and current connection checks. */
public final class NativeFabricClientNetworking {
    private NativeFabricClientNetworking() {}
    public static void register() {
        CreativeTabClientPlatform.installServerCapability(() -> ClientPlayNetworking.canSend(com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds.unwrap(com.kltyton.visual_creative_tab_editor.network.EditChunkPayload.ID)));
        ClientPlayNetworking.registerGlobalReceiver(
                com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds.unwrap(SnapshotChunkPayload.ID),
                (client, handler, buffer, responseSender) -> {
                    SnapshotChunkPayload payload = SnapshotChunkPayload.decode(buffer);
                    requireFullyRead(buffer, SnapshotChunkPayload.ID.toString());
                    client.execute(() -> {
                        if (client.getConnection() == handler) {
                            CreativeTabClientState.handleSnapshotChunk(payload);
                        }
                    });
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds.unwrap(EditResultPayload.ID),
                (client, handler, buffer, responseSender) -> {
                    EditResultPayload payload = EditResultPayload.decode(buffer);
                    requireFullyRead(buffer, EditResultPayload.ID.toString());
                    client.execute(() -> {
                        if (client.getConnection() == handler) {
                            CreativeTabClientState.handleEditResult(payload);
                        }
                    });
                }
        );

        CreativeTabNetworkBridge.installClientSender(payload -> {
            FriendlyByteBuf buffer = PacketByteBufs.create();
            payload.write(buffer);
            ClientPlayNetworking.send(payload.id(), buffer);
        });
        FabricCreativeTabPages.includeOperatorTabInCommonGroups();

    }


    private static void requireFullyRead(FriendlyByteBuf buffer, String channel) {
        if (buffer.isReadable()) {
            throw new DecoderException("Trailing bytes in creative-tab payload " + channel);
        }
    }
}
