package com.kltyton.one_enough_creative_tab;

import com.kltyton.one_enough_creative_tab.network.CreativeTabNetworkBridge;
import com.kltyton.one_enough_creative_tab.network.EditChunkPayload;
import com.kltyton.one_enough_creative_tab.network.EditResultPayload;
import com.kltyton.one_enough_creative_tab.network.SnapshotChunkPayload;
import com.kltyton.one_enough_creative_tab.server.CreativeTabReloadListener;
import com.kltyton.one_enough_creative_tab.server.CreativeTabServerManager;
import com.kltyton.one_enough_creative_tab.platform.CreativeTabNativeOrder;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** NeoForge loader entrypoint. */
@Mod(OneEnoughCreativeTabConstants.MOD_ID)
public final class OneEnoughCreativeTab {
    private static final String NETWORK_VERSION = "2";
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(
            OneEnoughCreativeTabConstants.MOD_ID,
            "creative_tabs"
    );

    /** Creates and initializes the NeoForge mod entrypoint. */
    public OneEnoughCreativeTab(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(OneEnoughCreativeTab::registerPayloads);

        NeoForge.EVENT_BUS.addListener(OneEnoughCreativeTab::addServerReloadListeners);
        NeoForge.EVENT_BUS.addListener(OneEnoughCreativeTab::serverStarted);
        NeoForge.EVENT_BUS.addListener(OneEnoughCreativeTab::serverStopped);
        NeoForge.EVENT_BUS.addListener(OneEnoughCreativeTab::datapackSync);
        NeoForge.EVENT_BUS.addListener(OneEnoughCreativeTab::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(OneEnoughCreativeTab::serverTick);

        CreativeTabNetworkBridge.installServerSender(PacketDistributor::sendToPlayer);
        CreativeTabNativeOrder.install(registryOrder -> {
            ArrayList<net.minecraft.world.item.CreativeModeTab> ordered = new ArrayList<>(
                    CreativeModeTabRegistry.getSortedCreativeModeTabs()
            );
            for (net.minecraft.world.item.CreativeModeTab tab : registryOrder) {
                if (!ordered.contains(tab)) {
                    ordered.add(tab);
                }
            }
            return ordered;
        });
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            registerClient(modEventBus);
        }

        OneEnoughCreativeTabCommon.initialize();
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToClient(SnapshotChunkPayload.TYPE, SnapshotChunkPayload.STREAM_CODEC)
                .playToClient(EditResultPayload.TYPE, EditResultPayload.STREAM_CODEC)
                .playToServer(
                        EditChunkPayload.TYPE,
                        EditChunkPayload.STREAM_CODEC,
                        (payload, context) -> CreativeTabServerManager.handleEditChunk((ServerPlayer) context.player(), payload)
                );
    }

    @SuppressWarnings("removal")
    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(RELOAD_LISTENER_ID, new CreativeTabReloadListener(event.getRegistryAccess()));
    }

    private static void serverStarted(ServerStartedEvent event) {
        CreativeTabServerManager.onServerStarted(event.getServer());
    }

    private static void serverStopped(ServerStoppedEvent event) {
        CreativeTabServerManager.onServerStopped(event.getServer());
    }

    private static void datapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(CreativeTabServerManager::syncTo);
    }

    private static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CreativeTabServerManager.onPlayerDisconnected(player);
        }
    }

    private static void serverTick(ServerTickEvent.Post event) {
        CreativeTabServerManager.refreshEditPermissions(event.getServer());
    }

    private static void registerClient(IEventBus modEventBus) {
        try {
            Class.forName("com.kltyton.one_enough_creative_tab.client.OneEnoughCreativeTabNeoForgeClient")
                    .getMethod("register", IEventBus.class)
                    .invoke(null, modEventBus);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register One Enough Creative Tab NeoForge client hooks", exception);
        }
    }
}
