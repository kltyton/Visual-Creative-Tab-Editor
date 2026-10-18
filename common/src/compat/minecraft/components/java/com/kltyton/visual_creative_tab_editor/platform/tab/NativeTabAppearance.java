package com.kltyton.visual_creative_tab_editor.platform.tab;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.world.item.CreativeModeTab;

/** Captures the native tab background without losing loader-provided textures. */
public final class NativeTabAppearance {
    private NativeTabAppearance() {}
    public static ResourceId defaultBackground() {
        return NativeIds.wrap(CreativeModeTab.createTextureLocation("items"));
    }
    public static ResourceId background(CreativeModeTab tab, ResourceId id) {
        return NativeIds.wrap(tab.getBackgroundTexture());
    }
}
