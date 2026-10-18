package com.kltyton.visual_creative_tab_editor.platform.neoforge;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Binds reload registration and client bootstrap to the selected NeoForge lifecycle. */
public final class NeoForgeLifecycle {
    private NeoForgeLifecycle() {}
    @SuppressWarnings("removal")
    public static void registerReloads() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                (net.neoforged.neoforge.event.AddReloadListenerEvent event) ->
                        event.addListener(
                                new CreativeTabReloadListener(event.getRegistryAccess())));
    }
    public static void registerClient(IEventBus modEventBus) {
        if (FMLEnvironment.dist != Dist.CLIENT) { return; }
        com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorNeoForgeClient.register();
    }
}
