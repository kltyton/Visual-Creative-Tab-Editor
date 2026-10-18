package com.kltyton.visual_creative_tab_editor.platform.item;

import java.util.Objects;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.core.component.DataComponentMap;

/** Preserves native item identity while ignoring stack count. */
public final class StackData {
    private StackData() {}
    public static boolean same(ItemStack first, ItemStack second) { return ItemStack.isSameItemSameComponents(first, second); }
    public static Set<ItemStack> set() { return ItemStackLinkedSet.createTypeAndComponentsSet(); }
    public static int hash(ItemStack stack) { return 31 * stack.getItem().hashCode() + stack.getComponents().hashCode(); }
    public static Key key(ItemStack stack) {
        return new Key(stack.getItem(), stack.getComponents());
    }
    public record Key(Item item, DataComponentMap data) {}
}
