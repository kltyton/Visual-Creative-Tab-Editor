package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only NeoForge network bootstrap, loaded reflectively by the common NeoForge entrypoint. */
public final class VisualCreativeTabEditorNeoForgeClient {
    private VisualCreativeTabEditorNeoForgeClient() {
    }

    /** Registers client payload handlers and the client-to-server transport bridge. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(VisualCreativeTabEditorNeoForgeClient::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(VisualCreativeTabEditorNeoForgeClient::loggingOut);
        CreativeTabNetworkBridge.installClientSender(ClientPacketDistributor::sendToServer);
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installVisibleTabsProvider(screen -> screen.getCurrentPage().getVisibleTabs());
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> {
            return screen.pages.stream()
                    .filter(page -> page.getVisibleTabs().contains(tab))
                    .findFirst()
                    .map(page -> {
                        screen.setCurrentPage(page);
                        return true;
                    })
                .orElse(false);
        });
        CreativeTabClientPlatform.installPageNavigator(new CreativeTabClientPlatform.PageNavigator() {
            @Override
            public CreativeTabClientPlatform.PageState state(
                    net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen
            ) {
                List<CreativeTabsScreenPage> pages = screen.pages;
                if (pages.isEmpty()) {
                    return new CreativeTabClientPlatform.PageState(0, 1);
                }
                int current = pages.indexOf(screen.getCurrentPage());
                if (current < 0) {
                    net.minecraft.world.item.CreativeModeTab selected =
                            net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.selectedTab;
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
                    net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen,
                    int targetIndex
            ) {
                List<CreativeTabsScreenPage> pages = screen.pages;
                if (targetIndex < 0 || targetIndex >= pages.size()) {
                    return false;
                }
                CreativeTabsScreenPage page = pages.get(targetIndex);
                screen.setCurrentPage(page);
                net.minecraft.world.item.CreativeModeTab selected =
                        net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.selectedTab;
                List<net.minecraft.world.item.CreativeModeTab> visible = page.getVisibleTabs();
                if (!visible.isEmpty() && !visible.contains(selected)) {
                    screen.selectTab(page.getDefaultTab());
                }
                return screen.getCurrentPage() == page;
            }
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
            int pageIndex = Math.max(0, screen.pages.indexOf(screen.getCurrentPage()));
            screen.resize(screen.width, screen.height);
            List<CreativeTabsScreenPage> pages = screen.pages;
            if (!pages.isEmpty()) {
                screen.setCurrentPage(pages.get(Math.min(pageIndex, pages.size() - 1)));
            }
        });
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(
                SnapshotChunkPayload.TYPE,
                (payload, context) -> CreativeTabClientState.handleSnapshotChunk(payload)
        );
        event.register(
                EditResultPayload.TYPE,
                (payload, context) -> CreativeTabClientState.handleEditResult(payload)
        );
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeTabClientState.clear();
    }
}
