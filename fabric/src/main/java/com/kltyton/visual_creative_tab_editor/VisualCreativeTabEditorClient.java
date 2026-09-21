package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import io.netty.handler.codec.DecoderException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/** Fabric physical-client entrypoint. */
public final class VisualCreativeTabEditorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreativeTabClientPlatform.installServerCapability(() -> ClientPlayNetworking.canSend(com.kltyton.visual_creative_tab_editor.network.EditChunkPayload.ID));
        ClientPlayNetworking.registerGlobalReceiver(
                SnapshotChunkPayload.ID,
                (client, handler, buffer, responseSender) -> {
                    SnapshotChunkPayload payload = SnapshotChunkPayload.decode(buffer);
                    requireFullyRead(buffer, SnapshotChunkPayload.ID.toString());
                    client.execute(() -> {
                        if (client.getConnection() == handler) {
                            CreativeTabClientState.handleSnapshotChunk(payload);
                        }
                    });
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                EditResultPayload.ID,
                (client, handler, buffer, responseSender) -> {
                    EditResultPayload payload = EditResultPayload.decode(buffer);
                    requireFullyRead(buffer, EditResultPayload.ID.toString());
                    client.execute(() -> {
                        if (client.getConnection() == handler) {
                            CreativeTabClientState.handleEditResult(payload);
                        }
                    });
                }
        );

        CreativeTabNetworkBridge.installClientSender(payload -> {
            FriendlyByteBuf buffer = PacketByteBufs.create();
            payload.write(buffer);
            ClientPlayNetworking.send(payload.id(), buffer);
        });
        FabricCreativeTabPages.includeOperatorTabInCommonGroups();
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installTabLayoutRefresher(FabricCreativeTabPages::repack);
        CreativeTabClientPlatform.installVisibleTabsProvider(screen -> {
            CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
            return FabricCreativeTabPages.tabsOnPage(fabricScreen.fabric_currentPage());
        });
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> {
            CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
            int targetPage = FabricCreativeTabPages.pageOf(tab, fabricScreen.fabric_currentPage());
            return fabricScreen.fabric_currentPage() == targetPage
                    || FabricCreativeTabPages.switchToPage(fabricScreen, targetPage);
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
            CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
            int pageBeforeResize = fabricScreen.fabric_currentPage();
            screen.resize(Minecraft.getInstance(), screen.width, screen.height);
            int pageCount = Math.max(1, FabricCreativeTabPages.pageCount());
            int targetPage = Mth.clamp(pageBeforeResize, 0, pageCount - 1);
            int pageAfterResize = fabricScreen.fabric_currentPage();
            boolean restored = pageAfterResize == targetPage
                    || FabricCreativeTabPages.switchToPage(fabricScreen, targetPage);
            int pageAfterRestore = fabricScreen.fabric_currentPage();
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
                CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
                int count = Math.max(1, FabricCreativeTabPages.pageCount());
                int current = fabricScreen.fabric_currentPage();
                if (current < 0 || current >= count) {
                    int repaired = Mth.clamp(current, 0, count - 1);
                    FabricCreativeTabPages.switchToPage(fabricScreen, repaired);
                    current = fabricScreen.fabric_currentPage();
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
                CreativeGuiExtensions fabricScreen = (CreativeGuiExtensions) screen;
                return fabricScreen.fabric_currentPage() == targetIndex
                        || FabricCreativeTabPages.switchToPage(fabricScreen, targetIndex);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (listener, client) -> CreativeTabClientState.clear()
        );
    }

    private static void requireFullyRead(FriendlyByteBuf buffer, String channel) {
        if (buffer.isReadable()) {
            throw new DecoderException("Trailing bytes in creative-tab payload " + channel);
        }
    }
}
