package com.kltyton.visual_creative_tab_editor.platform.item;

import java.util.Objects;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.nbt.CompoundTag;

/** Preserves native item identity while ignoring stack count. */
public final class StackData {
    private StackData() {}
    public static boolean same(ItemStack first, ItemStack second) { return ItemStack.isSameItemSameTags(first, second); }
    public static Set<ItemStack> set() { return ItemStackLinkedSet.createTypeAndTagSet(); }
    public static int hash(ItemStack stack) { return 31 * stack.getItem().hashCode() + Objects.hashCode(stack.getTag()); }
    public static Key key(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return new Key(stack.getItem(), tag == null ? null : tag.copy());
    }
    public record Key(Item item, CompoundTag data) {}
}
