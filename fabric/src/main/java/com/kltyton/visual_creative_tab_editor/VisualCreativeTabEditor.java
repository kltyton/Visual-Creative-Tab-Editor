package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import java.util.Comparator;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Fabric loader entrypoint. */
public final class VisualCreativeTabEditor implements ModInitializer {
    private static final ResourceLocation RELOAD_LISTENER_ID = ResourceLocation.fromNamespaceAndPath(
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
                RELOAD_LISTENER_ID,
                registries -> new FabricCreativeTabReloadListener(RELOAD_LISTENER_ID, registries)
        );

        PayloadTypeRegistry.playS2C().register(SnapshotChunkPayload.TYPE, SnapshotChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(EditChunkPayload.TYPE, EditChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EditResultPayload.TYPE, EditResultPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                EditChunkPayload.TYPE,
                (payload, context) -> CreativeTabServerManager.handleEditChunk(context.player(), payload)
        );
        CreativeTabNetworkBridge.installServerSender(ServerPlayNetworking::send);

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
}
