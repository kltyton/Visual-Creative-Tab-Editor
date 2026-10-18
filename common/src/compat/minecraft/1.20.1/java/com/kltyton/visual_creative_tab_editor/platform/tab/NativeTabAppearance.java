package com.kltyton.visual_creative_tab_editor.platform.tab;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.world.item.CreativeModeTab;

/** Captures the native tab background without losing loader-provided textures. */
public final class NativeTabAppearance {
    private NativeTabAppearance() {}
    public static ResourceId defaultBackground() {
        return ResourceId.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tab_items.png");
    }
    public static ResourceId background(CreativeModeTab tab, ResourceId id) {
        ResourceId background = NativeIds.wrap(com.kltyton.visual_creative_tab_editor.platform.CreativeTabNativeBackground.resolve(tab));
        if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
            com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants.LOGGER.info(
                    "Captured native system creative tab id={} type={} background={}", id, tab.getType(), background);
        }
        return background;
    }
}
