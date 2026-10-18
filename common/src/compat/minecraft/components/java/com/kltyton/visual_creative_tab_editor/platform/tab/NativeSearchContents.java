package com.kltyton.visual_creative_tab_editor.platform.tab;

import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabContents;
import java.util.List;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Replaces native search contents and refreshes the search tree where the tab owns it. */
public final class NativeSearchContents {
    private NativeSearchContents() {}
    public static void replace(CreativeModeTab search, List<ItemStack> ordered) {
        ((CreativeTabContents) search).visualCreativeTabEditor$replaceContents(ordered, ordered);
        
    }
}
