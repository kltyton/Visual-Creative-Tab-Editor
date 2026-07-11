package com.kltyton.visual_creative_tab_editor.network;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-neutral transport bridge. Loader modules install method references for their networking API.
 */
public final class CreativeTabNetworkBridge {
    private static final AtomicReference<ServerSender> SERVER_SENDER = new AtomicReference<>();
    private static final AtomicReference<ClientSender> CLIENT_SENDER = new AtomicReference<>();

    private CreativeTabNetworkBridge() {
    }

    /** Installs or replaces the logical-server sender. */
    public static void installServerSender(ServerSender sender) {
        SERVER_SENDER.set(Objects.requireNonNull(sender, "sender"));
    }

    /** Installs or replaces the logical-client sender. */
    public static void installClientSender(ClientSender sender) {
        CLIENT_SENDER.set(Objects.requireNonNull(sender, "sender"));
    }

    /** Removes the logical-server sender, primarily for lifecycle cleanup and tests. */
    public static void uninstallServerSender() {
        SERVER_SENDER.set(null);
    }

    /** Removes the logical-client sender, primarily for lifecycle cleanup and tests. */
    public static void uninstallClientSender() {
        CLIENT_SENDER.set(null);
    }

    /**
     * Sends a payload to one player, returning {@code false} when no loader bridge is installed or sending fails.
     */
    public static boolean sendToPlayer(ServerPlayer player, CreativeTabPayload payload) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(payload, "payload");
        ServerSender sender = SERVER_SENDER.get();
        if (sender == null) {
            VisualCreativeTabEditorConstants.LOGGER.error("Cannot send {} to player: server network bridge is not installed", payload.id());
            return false;
        }

        try {
            sender.send(player, payload);
            return true;
        } catch (RuntimeException exception) {
            VisualCreativeTabEditorConstants.LOGGER.error("Failed to send {} to player {}", payload.id(), player.getScoreboardName(), exception);
            return false;
        }
    }

    /**
     * Sends a payload to the logical server, returning {@code false} when no loader bridge is installed or sending fails.
     */
    public static boolean sendToServer(CreativeTabPayload payload) {
        Objects.requireNonNull(payload, "payload");
        ClientSender sender = CLIENT_SENDER.get();
        if (sender == null) {
            VisualCreativeTabEditorConstants.LOGGER.error("Cannot send {} to server: client network bridge is not installed", payload.id());
            return false;
        }

        try {
            sender.send(payload);
            return true;
        } catch (RuntimeException exception) {
            VisualCreativeTabEditorConstants.LOGGER.error("Failed to send {} to server", payload.id(), exception);
            return false;
        }
    }

    @FunctionalInterface
    public interface ServerSender {
        void send(ServerPlayer player, CreativeTabPayload payload);
    }

    @FunctionalInterface
    public interface ClientSender {
        void send(CreativeTabPayload payload);
    }
}
