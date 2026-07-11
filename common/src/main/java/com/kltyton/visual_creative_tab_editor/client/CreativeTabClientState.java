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
            revision = pending.revision;
            canEdit = pending.canEdit;
            base = decodedBase;
            resolved = decodedResolved;
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
        } catch (RuntimeException exception) {
            pendingEditResult = null;
            VisualCreativeTabEditorConstants.LOGGER.error("Failed to submit creative-tab edit", exception);
            return false;
        }
    }

    public static void clear() {
        revision = 0;
        canEdit = false;
        base = CreativeTabCatalog.EMPTY;
        resolved = CreativeTabCatalog.EMPTY;
        pending = null;
        pendingEditResult = null;
        CreativeTabRuntime.clear();
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

    private static HolderLookup.Provider lookup() {
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
