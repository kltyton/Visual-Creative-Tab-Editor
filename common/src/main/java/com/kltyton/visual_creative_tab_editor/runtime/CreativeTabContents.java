package com.kltyton.visual_creative_tab_editor.runtime;

import java.util.Collection;
import net.minecraft.world.item.ItemStack;

/** Replaces vanilla backing collections without mutating possibly immutable mod-provided views. */
public interface CreativeTabContents {
    void visualCreativeTabEditor$replaceContents(Collection<ItemStack> display, Collection<ItemStack> searchable);
}
