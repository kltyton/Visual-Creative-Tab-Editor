package com.kltyton.visual_creative_tab_editor.platform.forge;

import com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorForgeClient;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/** Binds permission ticks and physical-client bootstrap to the Forge lifecycle API. */
public final class ForgeLifecycle {
    private ForgeLifecycle() {}
    public static void registerPermissionTicks() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) { CreativeTabServerManager.refreshEditPermissions(event.getServer()); }
        });
    }
    public static void initializeClient() {
        com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeBackground.install(net.minecraft.world.item.CreativeModeTab::getBackgroundLocation);
        net.minecraftforge.fml.DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> VisualCreativeTabEditorForgeClient::register);
    }
}
