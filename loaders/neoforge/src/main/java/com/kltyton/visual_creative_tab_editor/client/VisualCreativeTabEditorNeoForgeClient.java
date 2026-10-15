package com.kltyton.visual_creative_tab_editor.client;

// @target if minecraft_version=26.1.2,26.2
//|import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
// @target else
// @target end
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import java.util.List;
// @target if minecraft_version=26.1.2,26.2
// @target else
import net.minecraft.client.Minecraft;
// @target end
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
// @target if minecraft_version=26.1.2,26.2
//|import net.neoforged.bus.api.IEventBus;
// @target else
// @target end
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
// @target if minecraft_version=26.1.2,26.2
//|import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//|import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
// @target else
// @target end
import net.neoforged.neoforge.common.NeoForge;
// @target if minecraft_version=26.1.2,26.2
// @target else
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
// @target end

// @target if minecraft_version=26.1.2,26.2
//|/** Client-only NeoForge network bootstrap, loaded reflectively by the common NeoForge entrypoint. */
// @target else
/** Client-only NeoForge network, paging and connection bootstrap. */
// @target end
public final class VisualCreativeTabEditorNeoForgeClient {
    private VisualCreativeTabEditorNeoForgeClient() {
    }

    /** Registers client payload handlers and the client-to-server transport bridge. */
// @target if minecraft_version=26.1.2,26.2
//|    public static void register(IEventBus modEventBus) {
//|        modEventBus.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorNeoForgeClient::registerPayloadHandlers);
// @target else
    public static void register() {
// @target end
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VisualCreativeTabEditorNeoForgeClient::loggingOut);
        com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig.load(
                net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
// @target if minecraft_version=26.1.2,26.2
//|        CreativeTabNetworkBridge.installClientSender(ClientPacketDistributor::sendToServer);
// @target else
        CreativeTabNetworkBridge.installClientSender(PacketDistributor::sendToServer);
// @target end
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
// @target if minecraft_version=26.1.2,26.2
//|            screen.resize(screen.width, screen.height);
// @target else
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
// @target end
            List<CreativeTabsScreenPage> pages = screen.pages;
            if (!pages.isEmpty()) {
                screen.setCurrentPage(pages.get(Math.min(pageIndex, pages.size() - 1)));
            }
        });
    }

// @target if minecraft_version=26.1.2,26.2
//|    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
//|        event.register(
// @target else
    /** Registers the clientbound codecs and handlers on the physical client. */
    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(
// @target end
                SnapshotChunkPayload.TYPE,
// @target if minecraft_version=26.1.2,26.2
//|                (payload, context) -> CreativeTabClientState.handleSnapshotChunk(payload)
// @target else
                SnapshotChunkPayload.STREAM_CODEC,
                VisualCreativeTabEditorNeoForgeClient::handleSnapshotChunk
// @target end
        );
// @target if minecraft_version=26.1.2,26.2
//|        event.register(
// @target else
        registrar.playToClient(
// @target end
                EditResultPayload.TYPE,
// @target if minecraft_version=26.1.2,26.2
//|                (payload, context) -> CreativeTabClientState.handleEditResult(payload)
// @target else
                EditResultPayload.STREAM_CODEC,
                VisualCreativeTabEditorNeoForgeClient::handleEditResult
// @target end
        );
// @target if minecraft_version=26.1.2,26.2
// @target else
    }

    private static void handleSnapshotChunk(SnapshotChunkPayload payload, IPayloadContext context) {
        CreativeTabClientState.handleSnapshotChunk(payload);
    }

    private static void handleEditResult(EditResultPayload payload, IPayloadContext context) {
        CreativeTabClientState.handleEditResult(payload);
// @target end
    }

    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CreativeTabClientState.clear();
    }
}
