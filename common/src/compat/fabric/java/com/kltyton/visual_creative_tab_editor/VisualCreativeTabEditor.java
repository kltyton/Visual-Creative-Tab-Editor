package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import java.util.Comparator;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;

/** Fabric server lifecycle and deterministic native tab order. */
public final class VisualCreativeTabEditor implements ModInitializer {
    @Override public void onInitialize() {
        com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.configure(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        VisualCreativeTabEditorCommon.initialize();

        CreativeTabNativeOrder.install(registryOrder -> Stream.concat(
                registryOrder.stream().filter(tab -> {
                    var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    return id != null && id.getNamespace().equals("minecraft");
                }),
                registryOrder.stream()
                        .filter(tab -> {
                            var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                            return id == null || !id.getNamespace().equals("minecraft");
                        })
                        .sorted(Comparator.comparing(tab -> {
                            var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                            return id == null ? "~" : id.getNamespace() + ":" + id.getPath();
                        }))
        ).toList());


        com.kltyton.visual_creative_tab_editor.platform.fabric.NativeFabricServerResources.register();
        com.kltyton.visual_creative_tab_editor.network.fabric.NativeFabricServerNetworking.register();
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
