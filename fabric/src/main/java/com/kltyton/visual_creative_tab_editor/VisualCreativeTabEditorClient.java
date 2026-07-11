package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.Minecraft;

/** Fabric physical-client entrypoint. */
public final class VisualCreativeTabEditorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                SnapshotChunkPayload.TYPE,
                (payload, context) -> CreativeTabClientState.handleSnapshotChunk(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                EditResultPayload.TYPE,
                (payload, context) -> CreativeTabClientState.handleEditResult(payload)
        );

        CreativeTabNetworkBridge.installClientSender(ClientPlayNetworking::send);
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installTabLayoutRefresher(FabricCreativeTabPages::repack);
        CreativeTabClientPlatform.installVisibleTabsProvider(screen -> {
            FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
            return fabricScreen.getItemGroupsOnPage(fabricScreen.getCurrentPage());
        });
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> {
            FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
            int targetPage = fabricScreen.getPage(tab);
            return fabricScreen.getCurrentPage() == targetPage || fabricScreen.switchToPage(targetPage);
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
            FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
            int pageBeforeResize = fabricScreen.getCurrentPage();
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
            int pageCount = Math.max(1, fabricScreen.getPageCount());
            int targetPage = Math.clamp(pageBeforeResize, 0, pageCount - 1);
            int pageAfterResize = fabricScreen.getCurrentPage();
            boolean restored = pageAfterResize == targetPage || fabricScreen.switchToPage(targetPage);
            int pageAfterRestore = fabricScreen.getCurrentPage();
            if (pageAfterResize != targetPage || pageAfterRestore != targetPage) {
                VisualCreativeTabEditorConstants.LOGGER.info(
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
                FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
                int count = Math.max(1, fabricScreen.getPageCount());
                int current = fabricScreen.getCurrentPage();
                if (current < 0 || current >= count) {
                    int repaired = Math.clamp(current, 0, count - 1);
                    fabricScreen.switchToPage(repaired);
                    current = fabricScreen.getCurrentPage();
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
                FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
                return fabricScreen.getCurrentPage() == targetIndex || fabricScreen.switchToPage(targetIndex);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (listener, client) -> CreativeTabClientState.clear()
        );
    }
}
