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
    
    public static Pack open(Path root, String id, String directory, Pack.Position position) {
        return Pack.readMetaAndCreate(id, Component.literal(directory), true,
                openedId -> new PathPackResources(openedId, root, false), PackType.SERVER_DATA,
                position, PackSource.WORLD);
    }
}
