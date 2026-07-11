package com.kltyton.one_enough_creative_tab.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

/** Supplies the two world-local packs with non-negotiable boundary priorities. */
public final class PinnedWorldPackSource implements RepositorySource {
    public static final String DEFAULT_DIRECTORY = "one_enough_creative_tab_generated_default";
    public static final String PLAYER_DIRECTORY = "one_enough_creative_tab_player_overrides";
    public static final String DEFAULT_PACK_ID = "file/" + DEFAULT_DIRECTORY;
    public static final String PLAYER_PACK_ID = "file/" + PLAYER_DIRECTORY;

    private static final PackSelectionConfig DEFAULT_SELECTION =
            new PackSelectionConfig(true, Pack.Position.BOTTOM, true);
    private static final PackSelectionConfig PLAYER_SELECTION =
            new PackSelectionConfig(true, Pack.Position.TOP, true);

    private final Path datapackDirectory;

    public PinnedWorldPackSource(Path datapackDirectory) {
        this.datapackDirectory = datapackDirectory.toAbsolutePath().normalize();
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        load(consumer, DEFAULT_DIRECTORY, DEFAULT_PACK_ID, DEFAULT_SELECTION);
        load(consumer, PLAYER_DIRECTORY, PLAYER_PACK_ID, PLAYER_SELECTION);
    }

    private void load(Consumer<Pack> consumer, String directoryName, String packId, PackSelectionConfig selection) {
        Path root = this.datapackDirectory.resolve(directoryName).normalize();
        if (!root.startsWith(this.datapackDirectory) || !Files.isRegularFile(root.resolve("pack.mcmeta"))) {
            return;
        }

        PackLocationInfo location = new PackLocationInfo(
                packId,
                Component.literal(directoryName),
                PackSource.WORLD,
                Optional.empty()
        );
        Pack pack = Pack.readMetaAndCreate(
                location,
                new PathPackResources.PathResourcesSupplier(root),
                PackType.SERVER_DATA,
                selection
        );
        if (pack != null) {
            consumer.accept(pack);
        }
    }
}
