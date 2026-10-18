package com.kltyton.visual_creative_tab_editor.data;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import net.minecraft.world.item.CreativeModeTab;

import java.util.Objects;

/** Fully resolved visual layout settings for a creative tab. */
public record CreativeTabLayout(
        boolean canScroll,
        boolean showTitle,
        boolean alignedRight,
        ResourceId background
) {
    /** Vanilla's default category-tab layout. */
    public static final CreativeTabLayout DEFAULT = new CreativeTabLayout(
            true,
            true,
            false,
            com.kltyton.visual_creative_tab_editor.platform.tab.NativeTabAppearance.defaultBackground()
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
