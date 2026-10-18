package com.kltyton.visual_creative_tab_editor.platform.pack;

import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

/** Opens world packs with the required top or bottom native selection policy. */
public final class NativePinnedPack {
    private NativePinnedPack() {}
    private static final net.minecraft.server.packs.PackSelectionConfig DEFAULT =
            new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.BOTTOM, true);
    private static final net.minecraft.server.packs.PackSelectionConfig PLAYER =
            new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, true);
    public static Pack open(Path root, String id, String directory, Pack.Position position) {
        return Pack.readMetaAndCreate(new net.minecraft.server.packs.PackLocationInfo(
                id, Component.literal(directory), PackSource.WORLD, java.util.Optional.empty()),
                new PathPackResources.PathResourcesSupplier(root), PackType.SERVER_DATA,
                position == Pack.Position.TOP ? PLAYER : DEFAULT);
    }
}
