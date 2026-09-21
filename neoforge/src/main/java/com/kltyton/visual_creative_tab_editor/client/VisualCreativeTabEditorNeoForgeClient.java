package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Client-only NeoForge network, paging and connection bootstrap. */
public final class VisualCreativeTabEditorNeoForgeClient {
    private VisualCreativeTabEditorNeoForgeClient() {
    }

    /** Registers client payload handlers and the client-to-server transport bridge. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorNeoForgeClient::loggingOut);
        CreativeTabNetworkBridge.installClientSender(PacketDistributor::sendToServer);
        CreativeTabClientPlatform.installServerCapability(() -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            return connection != null && connection.hasChannel(com.kltyton.visual_creative_tab_editor.network.EditChunkPayload.TYPE);
        });
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
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
            List<CreativeTabsScreenPage> pages = screen.pages;
            if (!pages.isEmpty()) {
                screen.setCurrentPage(pages.get(Math.min(pageIndex, pages.size() - 1)));
            }
        });
    }

    /** Registers the clientbound codecs and handlers on the physical client. */
    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(
                SnapshotChunkPayload.TYPE,
                SnapshotChunkPayload.STREAM_CODEC,
                VisualCreativeTabEditorNeoForgeClient::handleSnapshotChunk
        );
        registrar.playToClient(
                EditResultPayload.TYPE,
                EditResultPayload.STREAM_CODEC,
                VisualCreativeTabEditorNeoForgeClient::handleEditResult
        );
    }

    private static void handleSnapshotChunk(SnapshotChunkPayload payload, IPayloadContext context) {
        CreativeTabClientState.handleSnapshotChunk(payload);
    }

    private static void handleEditResult(EditResultPayload payload, IPayloadContext context) {
        CreativeTabClientState.handleEditResult(payload);
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeTabClientState.clear();
    }
}
