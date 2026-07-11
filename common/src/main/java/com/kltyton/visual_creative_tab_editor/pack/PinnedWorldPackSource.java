package com.kltyton.visual_creative_tab_editor.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

/** Supplies the two world-local packs with non-negotiable boundary priorities. */
public final class PinnedWorldPackSource implements RepositorySource {
    public static final String DEFAULT_DIRECTORY = "visual_creative_tab_editor_generated_default";
    public static final String PLAYER_DIRECTORY = "visual_creative_tab_editor_player_overrides";
    public static final String LEGACY_DEFAULT_DIRECTORY = "one_enough_creative_tab_generated_default";
    public static final String LEGACY_PLAYER_DIRECTORY = "one_enough_creative_tab_player_overrides";
    public static final String DEFAULT_PACK_ID = "file/" + DEFAULT_DIRECTORY;
    public static final String PLAYER_PACK_ID = "file/" + PLAYER_DIRECTORY;

    private final Path datapackDirectory;

    public PinnedWorldPackSource(Path datapackDirectory) {
        this.datapackDirectory = datapackDirectory.toAbsolutePath().normalize();
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        loadPreferred(
                consumer,
                DEFAULT_DIRECTORY,
                LEGACY_DEFAULT_DIRECTORY,
                DEFAULT_PACK_ID,
                Pack.Position.BOTTOM
        );
        loadPreferred(
                consumer,
                PLAYER_DIRECTORY,
                LEGACY_PLAYER_DIRECTORY,
                PLAYER_PACK_ID,
                Pack.Position.TOP
        );
    }

    private void loadPreferred(
            Consumer<Pack> consumer,
            String currentDirectory,
            String legacyDirectory,
            String packId,
            Pack.Position position
    ) {
        Path currentRoot = this.datapackDirectory.resolve(currentDirectory).normalize();
        String selectedDirectory = Files.isRegularFile(currentRoot.resolve("pack.mcmeta"))
                ? currentDirectory
                : legacyDirectory;
        load(consumer, selectedDirectory, packId, position);
    }

    private void load(Consumer<Pack> consumer, String directoryName, String packId, Pack.Position position) {
        Path root = this.datapackDirectory.resolve(directoryName).normalize();
        if (!root.startsWith(this.datapackDirectory) || !Files.isRegularFile(root.resolve("pack.mcmeta"))) {
            return;
        }

        Pack pack = Pack.readMetaAndCreate(
                packId,
                Component.literal(directoryName),
                true,
                openedId -> new PathPackResources(openedId, root, false),
                PackType.SERVER_DATA,
                position,
                PackSource.WORLD
        );
        if (pack != null) {
            consumer.accept(pack);
        }
    }
}
