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
                (net.neoforged.neoforge.event.AddServerReloadListenersEvent event) ->
                        event.addListener(com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds.unwrap(
                                com.kltyton.visual_creative_tab_editor.data.ResourceId.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "creative_tabs")), 
                                new CreativeTabReloadListener(event.getRegistryAccess())));
    }
    public static void registerClient(IEventBus modEventBus) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) { return; }
        try {
            Class.forName("com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorNeoForgeClient")
                    .getMethod("register", IEventBus.class).invoke(null, modEventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Visual Creative Tab Editor NeoForge client hooks", exception);
        }
    }
}
