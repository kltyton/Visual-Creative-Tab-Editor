package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
// @target if minecraft_version=1.20.1
//|import io.netty.handler.codec.DecoderException;
// @target else
// @target end
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
// @target if minecraft_version=1.20.1
//|import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
//|import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
//|import net.minecraft.client.Minecraft;
//|import net.minecraft.network.FriendlyByteBuf;
//|import net.minecraft.util.Mth;
// @target elif minecraft_version=26.1.2,26.2
//|import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
// @target else
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.Minecraft;
// @target end

/** Fabric physical-client entrypoint. */
public final class VisualCreativeTabEditorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
// @target if minecraft_version=1.20.1
//|        CreativeTabClientPlatform.installServerCapability(() -> ClientPlayNetworking.canSend(com.kltyton.visual_creative_tab_editor.network.EditChunkPayload.ID));
// @target else
        CreativeTabClientPlatform.installServerCapability(() -> ClientPlayNetworking.canSend(com.kltyton.visual_creative_tab_editor.network.EditChunkPayload.TYPE));
// @target end
        ClientPlayNetworking.registerGlobalReceiver(
// @target if minecraft_version=1.20.1
//|                SnapshotChunkPayload.ID,
//|                (client, handler, buffer, responseSender) -> {
//|                    SnapshotChunkPayload payload = SnapshotChunkPayload.decode(buffer);
//|                    requireFullyRead(buffer, SnapshotChunkPayload.ID.toString());
//|                    client.execute(() -> {
//|                        if (client.getConnection() == handler) {
//|                            CreativeTabClientState.handleSnapshotChunk(payload);
//|                        }
//|                    });
//|                }
// @target else
                SnapshotChunkPayload.TYPE,
                (payload, context) -> CreativeTabClientState.handleSnapshotChunk(payload)
// @target end
        );
        ClientPlayNetworking.registerGlobalReceiver(
// @target if minecraft_version=1.20.1
//|                EditResultPayload.ID,
//|                (client, handler, buffer, responseSender) -> {
//|                    EditResultPayload payload = EditResultPayload.decode(buffer);
//|                    requireFullyRead(buffer, EditResultPayload.ID.toString());
//|                    client.execute(() -> {
//|                        if (client.getConnection() == handler) {
//|                            CreativeTabClientState.handleEditResult(payload);
//|                        }
//|                    });
//|                }
// @target else
                EditResultPayload.TYPE,
                (payload, context) -> CreativeTabClientState.handleEditResult(payload)
// @target end
        );

// @target if minecraft_version=1.20.1
//|        CreativeTabNetworkBridge.installClientSender(payload -> {
//|            FriendlyByteBuf buffer = PacketByteBufs.create();
//|            payload.write(buffer);
//|            ClientPlayNetworking.send(payload.id(), buffer);
//|        });
//|        FabricCreativeTabPages.includeOperatorTabInCommonGroups();
// @target else
        CreativeTabNetworkBridge.installClientSender(ClientPlayNetworking::send);
// @target end
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installTabLayoutRefresher(FabricCreativeTabPages::repack);
        CreativeTabClientPlatform.installVisibleTabsProvider(screen -> {
// @target if minecraft_version=1.20.1
//|            CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
//|            return FabricCreativeTabPages.tabsOnPage(fabricScreen.fabric_currentPage());
// @target elif minecraft_version=26.1.2,26.2
//|            FabricCreativeModeInventoryScreen fabricScreen = (FabricCreativeModeInventoryScreen) screen;
//|            return fabricScreen.getTabsOnPage(fabricScreen.getCurrentPage());
// @target else
            FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
            return fabricScreen.getItemGroupsOnPage(fabricScreen.getCurrentPage());
// @target end
        });
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> {
// @target if minecraft_version=1.20.1
//|            CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
//|            int targetPage = FabricCreativeTabPages.pageOf(tab, fabricScreen.fabric_currentPage());
//|            return fabricScreen.fabric_currentPage() == targetPage
//|                    || FabricCreativeTabPages.switchToPage(fabricScreen, targetPage);
// @target elif minecraft_version=26.1.2,26.2
//|            FabricCreativeModeInventoryScreen fabricScreen = (FabricCreativeModeInventoryScreen) screen;
//|            int targetPage = fabricScreen.getPage(tab);
//|            return fabricScreen.getCurrentPage() == targetPage || fabricScreen.switchToPage(targetPage);
// @target else
            FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
            int targetPage = fabricScreen.getPage(tab);
            return fabricScreen.getCurrentPage() == targetPage || fabricScreen.switchToPage(targetPage);
// @target end
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
// @target if minecraft_version=1.20.1
//|            CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
//|            int pageBeforeResize = fabricScreen.fabric_currentPage();
//|            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
//|            int pageCount = Math.max(1, FabricCreativeTabPages.pageCount());
//|            int targetPage = Mth.clamp(pageBeforeResize, 0, pageCount - 1);
//|            int pageAfterResize = fabricScreen.fabric_currentPage();
//|            boolean restored = pageAfterResize == targetPage
//|                    || FabricCreativeTabPages.switchToPage(fabricScreen, targetPage);
//|            int pageAfterRestore = fabricScreen.fabric_currentPage();
// @target elif minecraft_version=26.1.2,26.2
//|            FabricCreativeModeInventoryScreen fabricScreen = (FabricCreativeModeInventoryScreen) screen;
//|            int pageBeforeResize = fabricScreen.getCurrentPage();
//|            screen.resize(screen.width, screen.height);
//|            int pageCount = Math.max(1, fabricScreen.getPageCount());
//|            int targetPage = Math.clamp(pageBeforeResize, 0, pageCount - 1);
//|            int pageAfterResize = fabricScreen.getCurrentPage();
//|            boolean restored = pageAfterResize == targetPage || fabricScreen.switchToPage(targetPage);
//|            int pageAfterRestore = fabricScreen.getCurrentPage();
// @target else
            FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
            int pageBeforeResize = fabricScreen.getCurrentPage();
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
            int pageCount = Math.max(1, fabricScreen.getPageCount());
            int targetPage = Math.clamp(pageBeforeResize, 0, pageCount - 1);
            int pageAfterResize = fabricScreen.getCurrentPage();
            boolean restored = pageAfterResize == targetPage || fabricScreen.switchToPage(targetPage);
            int pageAfterRestore = fabricScreen.getCurrentPage();
// @target end
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
// @target if minecraft_version=1.20.1
//|                CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
//|                int count = Math.max(1, FabricCreativeTabPages.pageCount());
//|                int current = fabricScreen.fabric_currentPage();
// @target elif minecraft_version=26.1.2,26.2
//|                FabricCreativeModeInventoryScreen fabricScreen = (FabricCreativeModeInventoryScreen) screen;
//|                int count = Math.max(1, fabricScreen.getPageCount());
//|                int current = fabricScreen.getCurrentPage();
// @target else
                FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
                int count = Math.max(1, fabricScreen.getPageCount());
                int current = fabricScreen.getCurrentPage();
// @target end
                if (current < 0 || current >= count) {
// @target if minecraft_version=1.20.1
//|                    int repaired = Mth.clamp(current, 0, count - 1);
//|                    FabricCreativeTabPages.switchToPage(fabricScreen, repaired);
//|                    current = fabricScreen.fabric_currentPage();
// @target else
                    int repaired = Math.clamp(current, 0, count - 1);
                    fabricScreen.switchToPage(repaired);
                    current = fabricScreen.getCurrentPage();
// @target end
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
// @target if minecraft_version=1.20.1
//|                CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
//|                return fabricScreen.fabric_currentPage() == targetIndex
//|                        || FabricCreativeTabPages.switchToPage(fabricScreen, targetIndex);
// @target elif minecraft_version=26.1.2,26.2
//|                FabricCreativeModeInventoryScreen fabricScreen = (FabricCreativeModeInventoryScreen) screen;
//|                return fabricScreen.getCurrentPage() == targetIndex || fabricScreen.switchToPage(targetIndex);
// @target else
                FabricCreativeInventoryScreen fabricScreen = (FabricCreativeInventoryScreen) screen;
                return fabricScreen.getCurrentPage() == targetIndex || fabricScreen.switchToPage(targetIndex);
// @target end
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (listener, client) -> CreativeTabClientState.clear()
        );
    }
// @target if minecraft_version=1.20.1
//|
//|    private static void requireFullyRead(FriendlyByteBuf buffer, String channel) {
//|        if (buffer.isReadable()) {
//|            throw new DecoderException("Trailing bytes in creative-tab payload " + channel);
//|        }
//|    }
// @target else
// @target end
}
