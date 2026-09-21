package com.kltyton.visual_creative_tab_editor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalog;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalogJson;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.PayloadChunks;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;

/** Client-side snapshot assembly and edit submission. */
public final class CreativeTabClientState {
    private static long revision;
    private static boolean canEdit;
    private static boolean localMode;
    private static java.nio.file.Path localFile;
    private static boolean localInitializationAttempted;
    private static CreativeTabCatalog base = CreativeTabCatalog.EMPTY;
    private static CreativeTabCatalog resolved = CreativeTabCatalog.EMPTY;
    private static PendingSnapshot pending;
    private static PendingEditResult pendingEditResult;

    private CreativeTabClientState() {
    }

    public static void handleSnapshotChunk(SnapshotChunkPayload payload) {
        try {
            if (pending == null || !pending.matches(payload)) {
                pending = new PendingSnapshot(payload);
            }
            pending.add(payload);
            if (!pending.complete()) {
                return;
            }
            byte[] bytes = PayloadChunks.joinAndDecompress(List.of(pending.chunks), pending.uncompressedSize);
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            HolderLookup.Provider lookup = lookup();
            CreativeTabCatalog decodedBase = CreativeTabCatalogJson.decode(root.get("base").toString(), lookup);
            CreativeTabCatalog decodedResolved = CreativeTabCatalogJson.decode(root.get("resolved").toString(), lookup);
            localMode = false;
            localFile = null;
            revision = pending.revision;
            canEdit = pending.canEdit;
            base = decodedBase;
            resolved = decodedResolved;
            VisualCreativeTabEditorConstants.LOGGER.debug(
                    "[EditorTrace] snapshot-applied revision={} canEdit={} baseTabs={} resolvedTabs={} chunks={}",
                    revision,
                    canEdit,
                    base.orderedDefinitions().size(),
                    resolved.orderedDefinitions().size(),
                    pending.chunks.length
            );
            pending = null;
            CreativeTabRuntime.install(decodedResolved);
        } catch (RuntimeException exception) {
            pending = null;
            VisualCreativeTabEditorConstants.LOGGER.error("Rejected invalid creative-tab snapshot", exception);
        }
    }

    public static void handleEditResult(EditResultPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        PendingEditResult pendingResult = pendingEditResult;
        if (pendingResult == null || !pendingResult.sessionId().equals(payload.sessionId())) {
            return;
        }
        canEdit = payload.canEdit();
        pendingEditResult = null;
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(payload.message());
            }
            pendingResult.listener().accept(payload);
        });
    }

    public static boolean submit(
            CreativeTabCatalog target,
            long baseRevision,
            Consumer<EditResultPayload> resultListener
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(resultListener, "resultListener");
        if (!canEdit || baseRevision < 0 || pendingEditResult != null) {
            return false;
        }
        try {
            if (localMode) {
                com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.save(localFile, base, target, lookup());
                resolved = target;
                revision++;
                CreativeTabRuntime.install(target);
                resultListener.accept(new EditResultPayload(UUID.randomUUID(), true, revision, true,
                        net.minecraft.network.chat.Component.translatable("visual_creative_tab_editor.editor.local_saved")));
                return true;
            }
            byte[] bytes = CreativeTabCatalogJson.encode(target, lookup()).getBytes(StandardCharsets.UTF_8);
            List<byte[]> chunks = PayloadChunks.compressAndSplit(bytes);
            UUID sessionId = UUID.randomUUID();
            PendingEditResult registeredResult = new PendingEditResult(sessionId, resultListener);
            pendingEditResult = registeredResult;
            for (int index = 0; index < chunks.size(); index++) {
                if (!CreativeTabNetworkBridge.sendToServer(new EditChunkPayload(
                        sessionId,
                        baseRevision,
                        index,
                        chunks.size(),
                        chunks.get(index)
                ))) {
                    if (pendingEditResult == registeredResult) {
                        pendingEditResult = null;
                    }
                    return false;
                }
            }
            return true;
        } catch (RuntimeException | java.io.IOException exception) {
            pendingEditResult = null;
            VisualCreativeTabEditorConstants.LOGGER.error("Failed to submit creative-tab edit", exception);
            return false;
        }
    }

    public static void clear() {
        revision = 0;
        canEdit = false;
        localMode = false;
        localFile = null;
        localInitializationAttempted = false;
        base = CreativeTabCatalog.EMPTY;
        resolved = CreativeTabCatalog.EMPTY;
        pending = null;
        pendingEditResult = null;
        CreativeTabRuntime.clear();
    }

    public static void initializeLocalIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (localInitializationAttempted || minecraft.getConnection() == null || minecraft.player == null
                || minecraft.level == null || minecraft.hasSingleplayerServer()
                || CreativeTabClientPlatform.serverSupportsEditing()) {
            return;
        }
        localInitializationAttempted = true;
        try {
            var parameters = new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
                    minecraft.level.enabledFeatures(), CreativeTabRuntime.hasClientPermissions(), lookup());
            CreativeTabCatalog captured = com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager.captureNativeCatalog(parameters);
            String address = minecraft.getCurrentServer() == null
                    ? minecraft.getConnection().getConnection().getRemoteAddress().toString()
                    : minecraft.getCurrentServer().ip;
            var file = com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.serverFile(address);
            var source = java.nio.file.Files.isRegularFile(file) ? file
                    : com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.globalFile();
            CreativeTabCatalog preferences;
            try {
                preferences = com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.load(source, captured, lookup());
            } catch (java.io.IOException exception) {
                VisualCreativeTabEditorConstants.LOGGER.warn("Could not load local creative tab preferences; keeping the file intact", exception);
                return;
            }
            base = captured;
            resolved = preferences;
            localFile = file;
            localMode = true;
            canEdit = true;
            CreativeTabRuntime.install(resolved);
            CreativeTabClientPlatform.refreshTabLayout();
        } catch (RuntimeException exception) {
            VisualCreativeTabEditorConstants.LOGGER.error("Could not initialize local creative tab editing", exception);
        }
    }

    public static boolean saveGlobal(CreativeTabCatalog target) {
        if (!canEdit) {
            return false;
        }
        try {
            com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.save(
                    com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.globalFile(), base, target, lookup());
            return true;
        } catch (java.io.IOException | RuntimeException exception) {
            VisualCreativeTabEditorConstants.LOGGER.error("Could not save global creative tab preferences", exception);
            return false;
        }
    }

    public static java.nio.file.Path contextFile(String kind) {
        Minecraft minecraft = Minecraft.getInstance();
        var singleplayer = minecraft.getSingleplayerServer();
        String context = singleplayer != null
                ? "world:" + singleplayer.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize()
                : "server:" + (minecraft.getCurrentServer() != null ? minecraft.getCurrentServer().ip
                : Objects.requireNonNull(minecraft.getConnection()).getConnection().getRemoteAddress());
        String key = UUID.nameUUIDFromBytes(context.getBytes(StandardCharsets.UTF_8)).toString();
        return com.kltyton.visual_creative_tab_editor.data.CreativeTabPreferences.globalFile().getParent()
                .resolve(kind).resolve(key + ".json");
    }

    public static boolean isLocalMode() {
        return localMode;
    }

    public static long revision() {
        return revision;
    }

    public static boolean canEdit() {
        return canEdit;
    }

    public static CreativeTabCatalog baseCatalog() {
        return base;
    }

    public static CreativeTabCatalog resolvedCatalog() {
        return resolved;
    }

    public static HolderLookup.Provider lookup() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            throw new IllegalStateException("No client registry connection");
        }
        return connection.registryAccess();
    }

    private static final class PendingSnapshot {
        private final long revision;
        private final boolean canEdit;
        private final int uncompressedSize;
        private final byte[][] chunks;
        private int received;

        private PendingSnapshot(SnapshotChunkPayload first) {
            this.revision = first.revision();
            this.canEdit = first.canEdit();
            this.uncompressedSize = first.uncompressedSize();
            this.chunks = new byte[first.total()][];
        }

        private boolean matches(SnapshotChunkPayload payload) {
            return this.revision == payload.revision()
                    && this.canEdit == payload.canEdit()
                    && this.uncompressedSize == payload.uncompressedSize()
                    && this.chunks.length == payload.total();
        }

        private void add(SnapshotChunkPayload payload) {
            byte[] incoming = payload.chunk();
            byte[] previous = this.chunks[payload.index()];
            if (previous != null) {
                if (!Arrays.equals(previous, incoming)) {
                    throw new IllegalArgumentException("Conflicting duplicate snapshot chunk");
                }
                return;
            }
            this.chunks[payload.index()] = incoming;
            this.received++;
        }

        private boolean complete() {
            return this.received == this.chunks.length;
        }
    }

    private record PendingEditResult(UUID sessionId, Consumer<EditResultPayload> listener) {
    }
}
