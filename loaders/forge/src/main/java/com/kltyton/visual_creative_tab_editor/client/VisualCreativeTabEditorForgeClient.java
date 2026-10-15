package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorForgeNetwork;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
// @target if minecraft_version=1.20.1
//|import net.minecraft.world.item.CreativeModeTab;
// @target else
// @target end
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Client-only Forge paging, refresh and connection bootstrap. */
public final class VisualCreativeTabEditorForgeClient {
    private VisualCreativeTabEditorForgeClient() {
    }

// @target if minecraft_version=1.20.1
//|    /** Installs client-only hooks after the physical-side guard in the mod entrypoint. */
// @target else
    /** Installs client-only Forge hooks after the physical-side guard in the mod entrypoint. */
// @target end
    public static void register() {
        com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig.load(
                net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorForgeClient::loggingOut);
        CreativeTabNetworkBridge.installClientSender(VisualCreativeTabEditorForgeNetwork::sendToServer);
        CreativeTabClientPlatform.installServerCapability(() -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            return connection != null && com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorForgeNetwork
                    .isRemotePresent(connection.getConnection());
        });
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installVisibleTabsProvider(screen -> screen.getCurrentPage().getVisibleTabs());
// @target if minecraft_version=1.20.1
//|        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> screen.pages.stream()
// @target else
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> pages(screen).stream()
// @target end
                .filter(page -> page.getVisibleTabs().contains(tab))
                .findFirst()
                .map(page -> {
                    screen.setCurrentPage(page);
                    return true;
                })
                .orElse(false));
        CreativeTabClientPlatform.installPageNavigator(new CreativeTabClientPlatform.PageNavigator() {
            @Override
// @target if minecraft_version=1.20.1
//|            public CreativeTabClientPlatform.PageState state(CreativeModeInventoryScreen screen) {
//|                List<CreativeTabsScreenPage> pages = screen.pages;
// @target else
            public CreativeTabClientPlatform.PageState state(
                    CreativeModeInventoryScreen screen
            ) {
                List<CreativeTabsScreenPage> pages = pages(screen);
// @target end
                if (pages.isEmpty()) {
                    return new CreativeTabClientPlatform.PageState(0, 1);
                }
                int current = pages.indexOf(screen.getCurrentPage());
                if (current < 0) {
// @target if minecraft_version=1.20.1
//|                    CreativeModeTab selected = CreativeModeInventoryScreen.selectedTab;
// @target else
                    net.minecraft.world.item.CreativeModeTab selected =
                            CreativeModeInventoryScreen.selectedTab;
// @target end
                    CreativeTabsScreenPage repaired = pages.stream()
                            .filter(page -> page.getVisibleTabs().contains(selected))
                            .findFirst()
// @target if minecraft_version=1.20.1
//|                            .orElse(pages.get(0));
// @target else
                            .orElse(pages.getFirst());
// @target end
                    screen.setCurrentPage(repaired);
// @target if minecraft_version=1.20.1
//|                    List<CreativeModeTab> visible = repaired.getVisibleTabs();
// @target else
                    List<net.minecraft.world.item.CreativeModeTab> visible = repaired.getVisibleTabs();
// @target end
                    if (!visible.isEmpty() && !visible.contains(selected)) {
// @target if minecraft_version=1.20.1
//|                        screen.selectTab(visible.get(0));
// @target else
                        screen.selectTab(visible.getFirst());
// @target end
                    }
                    current = pages.indexOf(repaired);
                }
                return new CreativeTabClientPlatform.PageState(current, pages.size());
            }

            @Override
// @target if minecraft_version=1.20.1
//|            public boolean switchTo(CreativeModeInventoryScreen screen, int targetIndex) {
//|                List<CreativeTabsScreenPage> pages = screen.pages;
// @target else
            public boolean switchTo(
                    CreativeModeInventoryScreen screen,
                    int targetIndex
            ) {
                List<CreativeTabsScreenPage> pages = pages(screen);
// @target end
                if (targetIndex < 0 || targetIndex >= pages.size()) {
                    return false;
                }
                CreativeTabsScreenPage page = pages.get(targetIndex);
                screen.setCurrentPage(page);
// @target if minecraft_version=1.20.1
//|                CreativeModeTab selected = CreativeModeInventoryScreen.selectedTab;
//|                List<CreativeModeTab> visible = page.getVisibleTabs();
// @target else
                net.minecraft.world.item.CreativeModeTab selected =
                        CreativeModeInventoryScreen.selectedTab;
                List<net.minecraft.world.item.CreativeModeTab> visible = page.getVisibleTabs();
// @target end
                if (!visible.isEmpty() && !visible.contains(selected)) {
                    screen.selectTab(page.getDefaultTab());
                }
                return screen.getCurrentPage() == page;
            }
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
// @target if minecraft_version=1.20.1
//|            int pageIndex = Math.max(0, screen.pages.indexOf(screen.getCurrentPage()));
// @target else
            int pageIndex = Math.max(0, pages(screen).indexOf(screen.getCurrentPage()));
// @target end
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
// @target if minecraft_version=1.20.1
//|            List<CreativeTabsScreenPage> pages = screen.pages;
// @target else
            List<CreativeTabsScreenPage> pages = pages(screen);
// @target end
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
// @target if minecraft_version=1.20.1
// @target else

    private static List<CreativeTabsScreenPage> pages(CreativeModeInventoryScreen screen) {
        return ((ForgeCreativeTabPagesView) screen).visualCreativeTabEditor$pages();
    }
// @target end
}
