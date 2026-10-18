package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import com.kltyton.visual_creative_tab_editor.client.platform.NativeFabricScreen;
import com.kltyton.visual_creative_tab_editor.client.platform.NativeScreenLifecycle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Fabric client lifecycle and page-preserving editor refresh. */
public final class VisualCreativeTabEditorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig.load(
                net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        com.kltyton.visual_creative_tab_editor.client.network.NativeFabricClientNetworking.register();
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installTabLayoutRefresher(FabricCreativeTabPages::repack);
        CreativeTabClientPlatform.installVisibleTabsProvider(NativeFabricScreen::visible);
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> {
            int targetPage = NativeFabricScreen.page(screen, tab);
            return NativeFabricScreen.current(screen) == targetPage || NativeFabricScreen.switchTo(screen, targetPage);
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
            int pageBeforeResize = NativeFabricScreen.current(screen);
            NativeScreenLifecycle.refresh(screen);
            int pageCount = Math.max(1, NativeFabricScreen.count(screen));
            int targetPage = net.minecraft.util.Mth.clamp(pageBeforeResize, 0, pageCount - 1);
            int pageAfterResize = NativeFabricScreen.current(screen);
            boolean restored = pageAfterResize == targetPage || NativeFabricScreen.switchTo(screen, targetPage);
            int pageAfterRestore = NativeFabricScreen.current(screen);
            if (pageAfterResize != targetPage || pageAfterRestore != targetPage) {
                VisualCreativeTabEditorConstants.LOGGER.debug(
                        "[EditorTrace] fabric-screen-refresh-page-preserve before={} afterResize={} target={} restored={} afterRestore={} pageCount={}",
                        pageBeforeResize,
                        pageAfterResize,
                        targetPage,
                        restored,
                        pageAfterRestore,
                        pageCount
                );
            }
        });
        CreativeTabClientPlatform.installPageNavigator(new CreativeTabClientPlatform.PageNavigator() {
            @Override
            public CreativeTabClientPlatform.PageState state(net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen) {
                int count = Math.max(1, NativeFabricScreen.count(screen));
                int current = NativeFabricScreen.current(screen);
                if (current < 0 || current >= count) {
                    int repaired = net.minecraft.util.Mth.clamp(current, 0, count - 1);
                    NativeFabricScreen.switchTo(screen, repaired);
                    current = NativeFabricScreen.current(screen);
                }
                return new CreativeTabClientPlatform.PageState(
                        current,
                        count
                );
            }

            @Override
            public boolean switchTo(
                    net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen,
                    int targetIndex
            ) {
                return NativeFabricScreen.current(screen) == targetIndex || NativeFabricScreen.switchTo(screen, targetIndex);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (listener, client) -> CreativeTabClientState.clear()
        );
    }
}
