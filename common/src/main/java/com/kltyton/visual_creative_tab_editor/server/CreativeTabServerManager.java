package com.kltyton.visual_creative_tab_editor.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalog;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabJsonCodec;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabLayout;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabPatch;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabType;
import com.kltyton.visual_creative_tab_editor.network.CreativeTabNetworkBridge;
import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.PayloadChunks;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.pack.PinnedWorldPackSource;
import com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeOrder;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.level.storage.LevelResource;

/** Owns world-pack generation and the server-authoritative resolved catalog. */
public final class CreativeTabServerManager {
    /** Legacy command level matching 26.2's Permissions.COMMANDS_GAMEMASTER. */
    private static final int GAMEMASTER_PERMISSION_LEVEL = 2;
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final AtomicLong REVISION = new AtomicLong();
    private static final Map<UUID, PendingEdit> PENDING_EDITS = new HashMap<>();
    private static final Map<UUID, Boolean> EDIT_PERMISSION_STATE = new HashMap<>();
    private static volatile MinecraftServer server;
    private static volatile HolderLookup.Provider registries;
    private static volatile CreativeTabCatalog base = CreativeTabCatalog.EMPTY;
    private static volatile CreativeTabCatalog resolved = CreativeTabCatalog.EMPTY;
    private static volatile boolean defaultPresent;
    private static boolean commitInProgress;
    private static int permissionCheckTicks;

    private CreativeTabServerManager() {
    }

    public static synchronized void acceptReload(CreativeTabReloadListener.Prepared prepared) {
        registries = prepared.registries();
        base = prepared.base();
        resolved = prepared.resolved();
        defaultPresent = prepared.defaultPresent();
        if (defaultPresent) {
            REVISION.incrementAndGet();
        }
    }

    public static void onServerStarted(MinecraftServer startedServer) {
        server = startedServer;
        registries = startedServer.registryAccess();
        try {
            boolean migrated = migrateLegacyPacks(datapackRoot(startedServer));
            CreativeTabCatalog nativeCatalog = captureNativeCatalog(startedServer);
            Path root = datapackRoot(startedServer).resolve(PinnedWorldPackSource.DEFAULT_DIRECTORY);
            boolean changed = writePack(root, fullDocuments(nativeCatalog, startedServer.registryAccess()), "Generated native creative tabs");
            if (migrated || changed || !defaultPresent) {
                reloadWorldPacks(startedServer);
            }
        } catch (Exception exception) {
            VisualCreativeTabEditorConstants.LOGGER.error("Failed to generate the default creative-tab data pack", exception);
        }
    }

    public static void onServerStopped(MinecraftServer stoppedServer) {
        if (server == stoppedServer) {
            server = null;
            registries = null;
            base = CreativeTabCatalog.EMPTY;
            resolved = CreativeTabCatalog.EMPTY;
            defaultPresent = false;
            PENDING_EDITS.clear();
            EDIT_PERMISSION_STATE.clear();
            commitInProgress = false;
            permissionCheckTicks = 0;
        }
    }

    public static long revision() {
        return REVISION.get();
    }

    public static CreativeTabCatalog baseCatalog() {
        return base;
    }

    public static CreativeTabCatalog resolvedCatalog() {
        return resolved;
    }

    public static HolderLookup.Provider registries() {
        HolderLookup.Provider current = registries;
        if (current == null) {
            throw new IllegalStateException("Creative tab registries are not available");
        }
        return current;
    }

    public static void syncTo(ServerPlayer player) {
        if (!defaultPresent || resolved.isEmpty()) {
            return;
        }
        try {
            byte[] bytes = encodeSnapshotBundle(base, resolved, registries());
            List<byte[]> chunks = PayloadChunks.compressAndSplit(bytes);
            boolean canEdit = player.hasPermissions(GAMEMASTER_PERMISSION_LEVEL);
            long revision = REVISION.get();
            for (int index = 0; index < chunks.size(); index++) {
                CreativeTabNetworkBridge.sendToPlayer(player, new SnapshotChunkPayload(
                        revision,
                        canEdit,
                        index,
                        chunks.size(),
                        bytes.length,
                        chunks.get(index)
                ));
            }
            EDIT_PERMISSION_STATE.put(player.getUUID(), canEdit);
        } catch (RuntimeException exception) {
            VisualCreativeTabEditorConstants.LOGGER.error("Failed to encode creative-tab snapshot for {}", player.getScoreboardName(), exception);
        }
    }

    public static void handleEditChunk(ServerPlayer player, EditChunkPayload payload) {
        if (!player.hasPermissions(GAMEMASTER_PERMISSION_LEVEL)) {
            if (payload.index() == 0) {
                reject(player, payload.sessionId(), "visual_creative_tab_editor.edit.permission_denied");
            }
            return;
        }
        if (payload.baseRevision() != REVISION.get()) {
            if (payload.index() == 0) {
                reject(player, payload.sessionId(), "visual_creative_tab_editor.edit.stale");
                syncTo(player);
            }
            return;
        }

        long now = net.minecraft.Util.getMillis();
        PENDING_EDITS.entrySet().removeIf(entry -> now - entry.getValue().createdAt > 30_000L);
        UUID playerId = player.getUUID();
        PendingEdit pending = PENDING_EDITS.get(playerId);
        if (pending == null || !pending.sessionId.equals(payload.sessionId())) {
            if (payload.index() != 0) {
                return;
            }
            pending = new PendingEdit(payload.sessionId(), payload.baseRevision(), payload.total(), now);
            PENDING_EDITS.put(playerId, pending);
        }
        boolean ownsCommit = false;
        try {
            pending.add(payload);
            if (!pending.complete()) {
                return;
            }
            PENDING_EDITS.remove(playerId);
            if (commitInProgress) {
                reject(player, payload.sessionId(), "visual_creative_tab_editor.edit.busy");
                return;
            }
            commitInProgress = true;
            ownsCommit = true;
            byte[] jsonBytes = PayloadChunks.joinAndDecompress(List.of(pending.chunks));
            CreativeTabCatalog target = com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalogJson.decode(
                    new String(jsonBytes, StandardCharsets.UTF_8),
                    registries()
            );
            validateEditableTarget(target);
            Path playerRoot = playerPackRoot();
            Map<Path, byte[]> backup = snapshotOwnedPack(playerRoot);
            CreativeTabCatalog previousBase = base;
            CreativeTabCatalog previousResolved = resolved;
            HolderLookup.Provider previousRegistries = registries;
            boolean previousDefaultPresent = defaultPresent;
            long previousRevision = REVISION.get();
            try {
                boolean changed = writePlayerOverrides(target);
                if (changed) {
                    MinecraftServer current = server;
                    if (current == null) {
                        throw new IllegalStateException("Server stopped during creative-tab edit");
                    }
                    reloadWorldPacks(current).join();
                }
            } catch (Exception saveFailure) {
                base = previousBase;
                resolved = previousResolved;
                registries = previousRegistries;
                defaultPresent = previousDefaultPresent;
                REVISION.set(previousRevision);
                try {
                    restoreOwnedPack(playerRoot, backup);
                } catch (Exception restoreFailure) {
                    saveFailure.addSuppressed(restoreFailure);
                }
                MinecraftServer current = server;
                if (current != null) {
                    try {
                        current.getPackRepository().reload();
                    } catch (RuntimeException repositoryFailure) {
                        saveFailure.addSuppressed(repositoryFailure);
                    }
                }
                throw saveFailure;
            }
            CreativeTabNetworkBridge.sendToPlayer(player, new EditResultPayload(
                    pending.sessionId,
                    true,
                    REVISION.get(),
                    player.hasPermissions(GAMEMASTER_PERMISSION_LEVEL),
                    net.minecraft.network.chat.Component.translatable("visual_creative_tab_editor.edit.saved")
            ));
        } catch (Exception exception) {
            PENDING_EDITS.remove(playerId);
            VisualCreativeTabEditorConstants.LOGGER.warn("Rejected creative-tab edit from {}", player.getScoreboardName(), exception);
            reject(player, payload.sessionId(), "visual_creative_tab_editor.edit.invalid");
            syncTo(player);
        } finally {
            if (ownsCommit) {
                commitInProgress = false;
            }
        }
    }

    public static void onPlayerDisconnected(ServerPlayer player) {
        PENDING_EDITS.remove(player.getUUID());
        EDIT_PERMISSION_STATE.remove(player.getUUID());
    }

    public static void refreshEditPermissions(MinecraftServer target) {
        if (server != target || !defaultPresent || ++permissionCheckTicks < 20) {
            return;
        }
        permissionCheckTicks = 0;
        for (ServerPlayer player : target.getPlayerList().getPlayers()) {
            boolean current = player.hasPermissions(GAMEMASTER_PERMISSION_LEVEL);
            Boolean previous = EDIT_PERMISSION_STATE.get(player.getUUID());
            if (previous == null || previous != current) {
                syncTo(player);
            }
        }
    }

    static byte[] encodeSnapshotBundle(
            CreativeTabCatalog baseCatalog,
            CreativeTabCatalog resolvedCatalog,
            HolderLookup.Provider lookup
    ) {
        JsonObject bundle = new JsonObject();
        bundle.add("base", JsonParser.parseString(
                com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalogJson.encode(baseCatalog, lookup)
        ));
        bundle.add("resolved", JsonParser.parseString(
                com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalogJson.encode(resolvedCatalog, lookup)
        ));
        return bundle.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void validateEditableTarget(CreativeTabCatalog target) {
        for (CreativeTabDefinition definition : target.orderedDefinitions()) {
            CreativeTabDefinition baseline = base.definition(definition.id()).orElse(null);
            if (baseline == null) {
                if (definition.type() != CreativeTabType.CATEGORY) {
                    throw new IllegalArgumentException("New special creative tabs are not supported: " + definition.id());
                }
            } else if (baseline.type() != definition.type()) {
                throw new IllegalArgumentException("Creative tab type cannot be changed: " + definition.id());
            }
        }
        for (CreativeTabDefinition baseline : base.orderedDefinitions()) {
            if (target.definition(baseline.id()).isEmpty()) {
                throw new IllegalArgumentException("Creative tabs must be hidden instead of removing their identity: " + baseline.id());
            }
        }
    }

    private static void reject(ServerPlayer player, UUID sessionId, String translationKey) {
        CreativeTabNetworkBridge.sendToPlayer(player, new EditResultPayload(
                sessionId,
                false,
                REVISION.get(),
                player.hasPermissions(GAMEMASTER_PERMISSION_LEVEL),
                net.minecraft.network.chat.Component.translatable(translationKey)
        ));
    }

    public static CompletableFuture<Void> reloadWorldPacks(MinecraftServer target) {
        PackRepository repository = target.getPackRepository();
        repository.reload();
        List<String> orderedIds = repository.getSelectedPacks().stream().map(Pack::getId).toList();
        return target.reloadResources(orderedIds).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                VisualCreativeTabEditorConstants.LOGGER.error("Creative-tab data pack reload failed", throwable);
            }
        });
    }

    public static Path playerPackRoot() {
        MinecraftServer current = server;
        if (current == null) {
            throw new IllegalStateException("No active server");
        }
        return datapackRoot(current).resolve(PinnedWorldPackSource.PLAYER_DIRECTORY);
    }

    private static boolean migrateLegacyPacks(Path datapackDirectory) {
        boolean defaultMigrated = migrateLegacyPack(
                datapackDirectory,
                PinnedWorldPackSource.LEGACY_DEFAULT_DIRECTORY,
                PinnedWorldPackSource.DEFAULT_DIRECTORY
        );
        boolean playerMigrated = migrateLegacyPack(
                datapackDirectory,
                PinnedWorldPackSource.LEGACY_PLAYER_DIRECTORY,
                PinnedWorldPackSource.PLAYER_DIRECTORY
        );
        return defaultMigrated || playerMigrated;
    }

    private static boolean migrateLegacyPack(Path datapackDirectory, String legacyName, String currentName) {
        Path normalizedRoot = datapackDirectory.toAbsolutePath().normalize();
        Path legacy = normalizedRoot.resolve(legacyName).normalize();
        Path current = normalizedRoot.resolve(currentName).normalize();
        if (!legacy.startsWith(normalizedRoot) || !current.startsWith(normalizedRoot)
                || !Files.isDirectory(legacy)) {
            return false;
        }
        if (Files.exists(current)) {
            VisualCreativeTabEditorConstants.LOGGER.warn(
                    "Both legacy and current creative-tab data packs exist; keeping {} authoritative and preserving {} without loading it",
                    currentName,
                    legacyName
            );
            return false;
        }
        try {
            try {
                Files.move(legacy, current, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(legacy, current);
            }
            VisualCreativeTabEditorConstants.LOGGER.info(
                    "Migrated legacy creative-tab data pack {} to {}",
                    legacyName,
                    currentName
            );
            return true;
        } catch (IOException exception) {
            VisualCreativeTabEditorConstants.LOGGER.warn(
                    "Could not migrate legacy creative-tab data pack {} to {}; the legacy pack remains readable",
                    legacyName,
                    currentName,
                    exception
            );
            return false;
        }
    }

    public static boolean writePlayerOverrides(CreativeTabCatalog target) throws IOException {
        Map<Path, String> documents = new LinkedHashMap<>();
        for (CreativeTabDefinition targetDefinition : target.orderedDefinitions()) {
            CreativeTabDefinition baseDefinition = base.definition(targetDefinition.id()).orElse(null);
            CreativeTabPatch patch = baseDefinition == null
                    ? CreativeTabPatch.full(targetDefinition)
                    : CreativeTabPatch.diff(baseDefinition, targetDefinition);
            if (!patch.isEmpty()) {
                ResourceLocation file = CreativeTabJsonCodec.resourceFileFromTabId(targetDefinition.id());
                Path relative = Path.of("data", file.getNamespace(), file.getPath());
                documents.put(relative, PRETTY_GSON.toJson(CreativeTabJsonCodec.encodePatch(patch, registries())) + "\n");
            }
        }
        return writePack(playerPackRoot(), documents, "Player creative tab overrides");
    }

    private static CreativeTabCatalog captureNativeCatalog(MinecraftServer target) {
        return CreativeTabRuntime.withNativeBypass(() -> {
            List<CreativeModeTab> registryTabs = BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList();
            Map<CreativeModeTab, NativeContents> originalContents = new IdentityHashMap<>();
            for (CreativeModeTab tab : registryTabs) {
                Set<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
                tab.getDisplayItems().forEach(stack -> displayItems.add(stack.copyWithCount(1)));
                Set<ItemStack> searchItems = ItemStackLinkedSet.createTypeAndComponentsSet();
                tab.getSearchTabDisplayItems().forEach(stack -> searchItems.add(stack.copyWithCount(1)));
                originalContents.put(tab, new NativeContents(displayItems, searchItems));
            }
            try {
                CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                        target.getWorldData().enabledFeatures(),
                        true,
                        target.registryAccess()
                );
                for (CreativeModeTab tab : registryTabs) {
                    if (tab.getType() == CreativeModeTab.Type.CATEGORY) {
                        rebuildNativeContents(tab, parameters, originalContents.get(tab));
                    }
                }
                for (CreativeModeTab tab : registryTabs) {
                    if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                        rebuildNativeContents(tab, parameters, originalContents.get(tab));
                    }
                }
                List<CreativeTabDefinition> definitions = new ArrayList<>();
                int order = 0;
                List<CreativeModeTab> nativeOrder = CreativeTabNativeOrder.apply(registryTabs);
                for (CreativeModeTab tab : nativeOrder) {
                    ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    if (id == null) {
                        continue;
                    }
                    List<ItemStack> items = tab.getType() == CreativeModeTab.Type.CATEGORY
                            ? tab.getDisplayItems().stream().map(stack -> stack.copyWithCount(1)).toList()
                            : List.of();
                    List<ItemStack> searchItems = tab.getType() == CreativeModeTab.Type.CATEGORY
                            ? tab.getSearchTabDisplayItems().stream().map(stack -> stack.copyWithCount(1)).toList()
                            : List.of();
                    definitions.add(new CreativeTabDefinition(
                            id,
                            CreativeTabPatch.CURRENT_FORMAT,
                            tab.getDisplayName(),
                            tab.getIconItem().copyWithCount(1),
                            items,
                            searchItems,
                            !tab.shouldDisplay(),
                            order++,
                            CreativeTabType.fromVanilla(tab.getType()),
                            new CreativeTabLayout(tab.canScroll(), tab.showTitle(), tab.isAlignedRight(), tab.getBackgroundTexture())
                    ));
                }
                return new CreativeTabCatalog(definitions);
            } finally {
                originalContents.forEach(CreativeTabServerManager::restoreNativeContents);
            }
        });
    }

    private static void rebuildNativeContents(
            CreativeModeTab tab,
            CreativeModeTab.ItemDisplayParameters parameters,
            NativeContents fallback
    ) {
        try {
            tab.buildContents(parameters);
        } catch (RuntimeException | LinkageError exception) {
            restoreNativeContents(tab, fallback);
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            VisualCreativeTabEditorConstants.LOGGER.warn(
                    "Could not rebuild native creative tab id={} type={}; preserving {} display and {} search items",
                    id == null ? "<unregistered>" : id,
                    tab.getType(),
                    fallback.displayItems().size(),
                    fallback.searchItems().size(),
                    exception
            );
        }
    }

    private static void restoreNativeContents(CreativeModeTab tab, NativeContents contents) {
        tab.getDisplayItems().clear();
        tab.getDisplayItems().addAll(contents.displayItems());
        tab.getSearchTabDisplayItems().clear();
        tab.getSearchTabDisplayItems().addAll(contents.searchItems());
    }

    private static Map<Path, String> fullDocuments(CreativeTabCatalog catalog, HolderLookup.Provider lookup) {
        Map<Path, String> documents = new LinkedHashMap<>();
        for (CreativeTabDefinition definition : catalog.orderedDefinitions()) {
            ResourceLocation file = CreativeTabJsonCodec.resourceFileFromTabId(definition.id());
            Path relative = Path.of("data", file.getNamespace(), file.getPath());
            documents.put(relative, PRETTY_GSON.toJson(CreativeTabJsonCodec.encodeDefinition(definition, lookup)) + "\n");
        }
        return documents;
    }

    private static boolean writePack(Path root, Map<Path, String> documents, String description) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Files.createDirectories(normalizedRoot);
        Map<Path, byte[]> desired = new LinkedHashMap<>();
        desired.put(Path.of("pack.mcmeta"), packMetadata(description).getBytes(StandardCharsets.UTF_8));
        documents.forEach((path, content) -> desired.put(path, content.getBytes(StandardCharsets.UTF_8)));

        boolean changed = false;
        for (Map.Entry<Path, byte[]> entry : desired.entrySet()) {
            Path destination = normalizedRoot.resolve(entry.getKey()).normalize();
            if (!destination.startsWith(normalizedRoot)) {
                throw new IOException("Refusing to write outside generated pack: " + entry.getKey());
            }
            byte[] existing = Files.isRegularFile(destination) ? Files.readAllBytes(destination) : null;
            if (!java.util.Arrays.equals(existing, entry.getValue())) {
                writeAtomic(destination, entry.getValue());
                changed = true;
            }
        }

        if (Files.exists(normalizedRoot)) {
            try (var paths = Files.walk(normalizedRoot)) {
                List<Path> obsolete = paths.filter(Files::isRegularFile)
                        .filter(path -> {
                            Path relative = normalizedRoot.relativize(path);
                            return (relative.toString().equals("pack.mcmeta") || relative.toString().endsWith(".json"))
                                    && !desired.containsKey(relative);
                        })
                        .toList();
                for (Path path : obsolete) {
                    Files.deleteIfExists(path);
                    changed = true;
                }
            }
            try (var paths = Files.walk(normalizedRoot)) {
                for (Path directory : paths.filter(Files::isDirectory)
                        .sorted(Comparator.reverseOrder()).toList()) {
                    if (!directory.equals(normalizedRoot)) {
                        try (var children = Files.list(directory)) {
                            if (children.findAny().isEmpty()) {
                                Files.deleteIfExists(directory);
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }

    private static void writeAtomic(Path destination, byte[] bytes) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        try {
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Map<Path, byte[]> snapshotOwnedPack(Path root) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Map<Path, byte[]> snapshot = new LinkedHashMap<>();
        if (!Files.exists(normalizedRoot)) {
            return snapshot;
        }
        try (var paths = Files.walk(normalizedRoot)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                Path relative = normalizedRoot.relativize(path);
                if (relative.toString().equals("pack.mcmeta") || relative.toString().endsWith(".json")) {
                    snapshot.put(relative, Files.readAllBytes(path));
                }
            }
        }
        return snapshot;
    }

    private static void restoreOwnedPack(Path root, Map<Path, byte[]> snapshot) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Files.createDirectories(normalizedRoot);
        try (var paths = Files.walk(normalizedRoot)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                Path relative = normalizedRoot.relativize(path);
                if ((relative.toString().equals("pack.mcmeta") || relative.toString().endsWith(".json"))
                        && !snapshot.containsKey(relative)) {
                    Files.deleteIfExists(path);
                }
            }
        }
        for (Map.Entry<Path, byte[]> entry : snapshot.entrySet()) {
            Path destination = normalizedRoot.resolve(entry.getKey()).normalize();
            if (!destination.startsWith(normalizedRoot)) {
                throw new IOException("Refusing to restore outside player pack: " + entry.getKey());
            }
            writeAtomic(destination, entry.getValue());
        }
    }

    private static String packMetadata(String description) {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", description);
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return PRETTY_GSON.toJson(root) + "\n";
    }

    private static Path datapackRoot(MinecraftServer target) {
        return target.getWorldPath(LevelResource.DATAPACK_DIR).toAbsolutePath().normalize();
    }

    private static final class PendingEdit {
        private final UUID sessionId;
        private final long baseRevision;
        private final byte[][] chunks;
        private final long createdAt;
        private int received;
        private int compressedBytes;

        private PendingEdit(UUID sessionId, long baseRevision, int total, long createdAt) {
            this.sessionId = sessionId;
            this.baseRevision = baseRevision;
            this.chunks = new byte[total][];
            this.createdAt = createdAt;
        }

        private void add(EditChunkPayload payload) {
            if (payload.baseRevision() != this.baseRevision || payload.total() != this.chunks.length) {
                throw new IllegalArgumentException("Edit chunk metadata changed mid-stream");
            }
            byte[] incoming = payload.chunk();
            byte[] previous = this.chunks[payload.index()];
            if (previous != null) {
                if (!Arrays.equals(previous, incoming)) {
                    throw new IllegalArgumentException("Conflicting duplicate edit chunk");
                }
                return;
            }
            this.chunks[payload.index()] = incoming;
            if ((long) this.compressedBytes + incoming.length > PayloadChunks.MAX_COMPRESSED_BYTES) {
                throw new IllegalArgumentException("Compressed edit exceeds the logical payload limit");
            }
            this.compressedBytes += incoming.length;
            this.received++;
        }

        private boolean complete() {
            return this.received == this.chunks.length;
        }
    }

    private record NativeContents(Collection<ItemStack> displayItems, Set<ItemStack> searchItems) {
    }
}
