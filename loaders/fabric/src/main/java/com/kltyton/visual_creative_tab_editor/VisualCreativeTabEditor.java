package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
// @target if minecraft_version=1.20.1
// @target elif minecraft_version=26.1.2,26.2
//|import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
//|import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
//|import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
// @target else
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
// @target end
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
// @target if minecraft_version=1.20.1
//|import io.netty.handler.codec.DecoderException;
// @target else
// @target end
import java.util.Comparator;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
// @target if minecraft_version=1.20.1
//|import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
// @target else
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
// @target end
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
// @target if minecraft_version=1.20.1
//|import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
//|import net.minecraft.core.registries.BuiltInRegistries;
//|import net.minecraft.network.FriendlyByteBuf;
// @target elif minecraft_version=26.1.2,26.2
//|import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
// @target else
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
// @target end
import net.minecraft.resources.ResourceLocation;
// @target if minecraft_version=1.20.1
//|import net.minecraft.server.packs.PackType;
// @target elif minecraft_version=26.1.2,26.2
//|import net.minecraft.core.registries.BuiltInRegistries;
// @target else
import net.minecraft.server.packs.PackType;
import net.minecraft.core.registries.BuiltInRegistries;
// @target end

/** Fabric loader entrypoint. */
public final class VisualCreativeTabEditor implements ModInitializer {
// @target if minecraft_version=1.20.1
//|    private static final ResourceLocation RELOAD_LISTENER_ID = new ResourceLocation(
// @target else
    private static final ResourceLocation RELOAD_LISTENER_ID = ResourceLocation.fromNamespaceAndPath(
// @target end
            VisualCreativeTabEditorConstants.MOD_ID,
            "creative_tabs"
    );

    /** Creates the Fabric mod entrypoint. */
    public VisualCreativeTabEditor() {
    }

    @Override
    public void onInitialize() {
        com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.configure(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
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

// @target if minecraft_version=1.20.1
//|        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
//|                new FabricCreativeTabReloadListener(RELOAD_LISTENER_ID)
//|        );
// @target elif minecraft_version=26.1.2,26.2
//|        DataResourceLoader.get().registerReloadListener(RELOAD_LISTENER_ID, CreativeTabReloadListener::new);
// @target else
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                RELOAD_LISTENER_ID,
                registries -> new FabricCreativeTabReloadListener(RELOAD_LISTENER_ID, registries)
        );
// @target end

// @target if minecraft_version=1.20.1
// @target elif minecraft_version=26.1.2,26.2
//|        PayloadTypeRegistry.clientboundPlay().register(SnapshotChunkPayload.TYPE, SnapshotChunkPayload.STREAM_CODEC);
//|        PayloadTypeRegistry.serverboundPlay().register(EditChunkPayload.TYPE, EditChunkPayload.STREAM_CODEC);
//|        PayloadTypeRegistry.clientboundPlay().register(EditResultPayload.TYPE, EditResultPayload.STREAM_CODEC);
//|
// @target else
        PayloadTypeRegistry.playS2C().register(SnapshotChunkPayload.TYPE, SnapshotChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(EditChunkPayload.TYPE, EditChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EditResultPayload.TYPE, EditResultPayload.STREAM_CODEC);

// @target end
        ServerPlayNetworking.registerGlobalReceiver(
// @target if minecraft_version=1.20.1
//|                EditChunkPayload.ID,
//|                (server, player, handler, buffer, responseSender) -> {
//|                    EditChunkPayload payload = EditChunkPayload.decode(buffer);
//|                    requireFullyRead(buffer, EditChunkPayload.ID.toString());
//|                    server.execute(() -> {
//|                        if (player.connection == handler) {
//|                            CreativeTabServerManager.handleEditChunk(player, payload);
//|                        }
//|                    });
//|                }
// @target else
                EditChunkPayload.TYPE,
                (payload, context) -> CreativeTabServerManager.handleEditChunk(context.player(), payload)
// @target end
        );
        CreativeTabNetworkBridge.installServerSender((player, payload) -> {
// @target if minecraft_version=1.20.1
//|            FriendlyByteBuf buffer = PacketByteBufs.create();
//|            payload.write(buffer);
//|            ServerPlayNetworking.send(player, payload.id(), buffer);
// @target else
            if (ServerPlayNetworking.canSend(player, payload.type())) {
                ServerPlayNetworking.send(player, payload);
            }
// @target end
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
// @target if minecraft_version=1.20.1
//|
//|    private static void requireFullyRead(FriendlyByteBuf buffer, String channel) {
//|        if (buffer.isReadable()) {
//|            throw new DecoderException("Trailing bytes in creative-tab payload " + channel);
//|        }
//|    }
// @target else
// @target end
}
