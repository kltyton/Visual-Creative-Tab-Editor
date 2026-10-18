package com.kltyton.visual_creative_tab_editor.client.editor.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Invalidates the display projection when vanilla or another mod changes the menu's items. */
public final class CreativeMenuItemList extends NonNullList<ItemStack> {
    private final NonNullList<ItemStack> source;
    private final Runnable changed;

    public CreativeMenuItemList(NonNullList<ItemStack> source, Runnable changed) {
        super(source, null);
        this.source = source;
        this.changed = changed;
    }

    @Override
    public ItemStack set(int index, ItemStack stack) {
        ItemStack previous = super.set(index, stack);
        this.changed.run();
        return previous;
    }

    @Override
    public void add(int index, ItemStack stack) {
        super.add(index, stack);
        this.changed.run();
    }

    @Override
    public ItemStack remove(int index) {
        ItemStack previous = super.remove(index);
        this.changed.run();
        return previous;
    }

    @Override
    public void clear() {
        this.source.clear();
        this.changed.run();
    }
}
