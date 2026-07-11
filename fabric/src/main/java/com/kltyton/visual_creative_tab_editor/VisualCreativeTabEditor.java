package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import io.netty.handler.codec.DecoderException;
import java.util.Comparator;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/** Fabric loader entrypoint. */
public final class VisualCreativeTabEditor implements ModInitializer {
    private static final ResourceLocation RELOAD_LISTENER_ID = new ResourceLocation(
            VisualCreativeTabEditorConstants.MOD_ID,
            "creative_tabs"
    );

    /** Creates the Fabric mod entrypoint. */
    public VisualCreativeTabEditor() {
    }

    @Override
    public void onInitialize() {
        VisualCreativeTabEditorCommon.initialize();

        CreativeTabNativeOrder.install(registryOrder -> Stream.concat(
                registryOrder.stream().filter(tab -> {
                    ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    return id != null && id.getNamespace().equals("minecraft");
                }),
                registryOrder.stream()
                        .filter(tab -> {
                            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                            return id == null || !id.getNamespace().equals("minecraft");
                        })
                        .sorted(Comparator.comparing(tab -> {
                            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                            return id == null ? "~" : id.getNamespace() + ":" + id.getPath();
                        }))
        ).toList());

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                new FabricCreativeTabReloadListener(RELOAD_LISTENER_ID)
        );

        ServerPlayNetworking.registerGlobalReceiver(
                EditChunkPayload.ID,
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

        ServerLifecycleEvents.SERVER_STARTED.register(CreativeTabServerManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(CreativeTabServerManager::onServerStopped);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
                (player, joined) -> CreativeTabServerManager.syncTo(player)
        );
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> CreativeTabServerManager.onPlayerDisconnected(handler.getPlayer())
        );
        ServerTickEvents.END_SERVER_TICK.register(CreativeTabServerManager::refreshEditPermissions);
    }

    private static void requireFullyRead(FriendlyByteBuf buffer, String channel) {
        if (buffer.isReadable()) {
            throw new DecoderException("Trailing bytes in creative-tab payload " + channel);
        }
    }
}
