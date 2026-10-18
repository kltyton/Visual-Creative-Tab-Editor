package com.kltyton.visual_creative_tab_editor.network.fabric;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.handler.codec.DecoderException;

/** Native server transport keeps direction, connection identity and thread scheduling intact. */
public final class NativeFabricServerNetworking {
    private NativeFabricServerNetworking() {}
    public static void register() {
        
        ServerPlayNetworking.registerGlobalReceiver(
                com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds.unwrap(EditChunkPayload.ID),
                (server, player, handler, buffer, responseSender) -> {
                    EditChunkPayload payload = EditChunkPayload.decode(buffer);
                    requireFullyRead(buffer, EditChunkPayload.ID.toString());
                    server.execute(() -> {
                        if (player.connection == handler) {
                            CreativeTabServerManager.handleEditChunk(player, payload);
                        }
                    });
                }
        );
        CreativeTabNetworkBridge.installServerSender((player, payload) -> {
            FriendlyByteBuf buffer = PacketByteBufs.create();
            payload.write(buffer);
            ServerPlayNetworking.send(player, payload.id(), buffer);
        });


    }


    private static void requireFullyRead(FriendlyByteBuf buffer, String channel) {
        if (buffer.isReadable()) {
            throw new DecoderException("Trailing bytes in creative-tab payload " + channel);
        }
    }
}
