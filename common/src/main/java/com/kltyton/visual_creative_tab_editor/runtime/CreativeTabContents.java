package com.kltyton.visual_creative_tab_editor.runtime;

import java.util.Collection;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Replaces vanilla backing collections without mutating possibly immutable mod-provided views. */
public interface CreativeTabContents {
    Collection<ItemStack> visualCreativeTabEditor$backingDisplayItems();

    /** Allows presentation-only tab subclasses to expose their unformatted catalog contents. */
    default Collection<ItemStack> visualCreativeTabEditor$catalogDisplayItems() {
        return ((CreativeModeTab) this).getDisplayItems();
    }

    void visualCreativeTabEditor$applyRegisteredContents(net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters parameters);

    void visualCreativeTabEditor$replaceContents(Collection<ItemStack> display, Collection<ItemStack> searchable);
}
