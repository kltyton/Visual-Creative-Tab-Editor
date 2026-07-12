package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorForgeNetwork;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Client-only Forge paging, refresh and connection bootstrap. */
public final class VisualCreativeTabEditorForgeClient {
    private VisualCreativeTabEditorForgeClient() {
    }

    /** Installs client-only Forge hooks after the physical-side guard in the mod entrypoint. */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorForgeClient::loggingOut);
        CreativeTabNetworkBridge.installClientSender(VisualCreativeTabEditorForgeNetwork::sendToServer);
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installVisibleTabsProvider(screen -> screen.getCurrentPage().getVisibleTabs());
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> pages(screen).stream()
                .filter(page -> page.getVisibleTabs().contains(tab))
                .findFirst()
                .map(page -> {
                    screen.setCurrentPage(page);
                    return true;
                })
                .orElse(false));
        CreativeTabClientPlatform.installPageNavigator(new CreativeTabClientPlatform.PageNavigator() {
            @Override
            public CreativeTabClientPlatform.PageState state(
                    CreativeModeInventoryScreen screen
            ) {
                List<CreativeTabsScreenPage> pages = pages(screen);
                if (pages.isEmpty()) {
                    return new CreativeTabClientPlatform.PageState(0, 1);
                }
                int current = pages.indexOf(screen.getCurrentPage());
                if (current < 0) {
                    net.minecraft.world.item.CreativeModeTab selected =
                            CreativeModeInventoryScreen.selectedTab;
                    CreativeTabsScreenPage repaired = pages.stream()
                            .filter(page -> page.getVisibleTabs().contains(selected))
                            .findFirst()
                            .orElse(pages.getFirst());
                    screen.setCurrentPage(repaired);
                    List<net.minecraft.world.item.CreativeModeTab> visible = repaired.getVisibleTabs();
                    if (!visible.isEmpty() && !visible.contains(selected)) {
                        screen.selectTab(visible.getFirst());
                    }
                    current = pages.indexOf(repaired);
                }
                return new CreativeTabClientPlatform.PageState(current, pages.size());
            }

            @Override
            public boolean switchTo(
                    CreativeModeInventoryScreen screen,
                    int targetIndex
            ) {
                List<CreativeTabsScreenPage> pages = pages(screen);
                if (targetIndex < 0 || targetIndex >= pages.size()) {
                    return false;
                }
                CreativeTabsScreenPage page = pages.get(targetIndex);
                screen.setCurrentPage(page);
                net.minecraft.world.item.CreativeModeTab selected =
                        CreativeModeInventoryScreen.selectedTab;
                List<net.minecraft.world.item.CreativeModeTab> visible = page.getVisibleTabs();
                if (!visible.isEmpty() && !visible.contains(selected)) {
                    screen.selectTab(page.getDefaultTab());
                }
                return screen.getCurrentPage() == page;
            }
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
            int pageIndex = Math.max(0, pages(screen).indexOf(screen.getCurrentPage()));
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
            List<CreativeTabsScreenPage> pages = pages(screen);
            if (!pages.isEmpty()) {
                screen.setCurrentPage(pages.get(Math.min(pageIndex, pages.size() - 1)));
            }
        });
    }

    public static void handleSnapshotChunk(SnapshotChunkPayload payload) {
        CreativeTabClientState.handleSnapshotChunk(payload);
    }

    public static void handleEditResult(EditResultPayload payload) {
        CreativeTabClientState.handleEditResult(payload);
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeTabClientState.clear();
    }

    private static List<CreativeTabsScreenPage> pages(CreativeModeInventoryScreen screen) {
        return ((ForgeCreativeTabPagesView) screen).visualCreativeTabEditor$pages();
    }
}
