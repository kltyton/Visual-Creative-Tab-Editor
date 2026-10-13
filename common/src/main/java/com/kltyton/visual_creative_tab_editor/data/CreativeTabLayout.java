package com.kltyton.visual_creative_tab_editor.data;

import net.minecraft.resources.ResourceLocation;
// @target if minecraft_version=1.20.1
// @target else
import net.minecraft.world.item.CreativeModeTab;
// @target end

import java.util.Objects;

/** Fully resolved visual layout settings for a creative tab. */
public record CreativeTabLayout(
        boolean canScroll,
        boolean showTitle,
        boolean alignedRight,
        ResourceLocation background
) {
    /** Vanilla's default category-tab layout. */
    public static final CreativeTabLayout DEFAULT = new CreativeTabLayout(
            true,
            true,
            false,
// @target if minecraft_version=1.20.1
//|            new ResourceLocation("minecraft", "textures/gui/container/creative_inventory/tab_items.png")
// @target else
            CreativeModeTab.createTextureLocation("items")
// @target end
    );

    public CreativeTabLayout {
        Objects.requireNonNull(background, "background");
    }

    /** Returns a copy with the supplied patch fields applied. */
    public CreativeTabLayout apply(CreativeTabPatch patch) {
        Objects.requireNonNull(patch, "patch");
        return new CreativeTabLayout(
                patch.canScroll().orElse(canScroll),
                patch.showTitle().orElse(showTitle),
                patch.alignedRight().orElse(alignedRight),
                patch.background().orElse(background)
        );
    }
}
