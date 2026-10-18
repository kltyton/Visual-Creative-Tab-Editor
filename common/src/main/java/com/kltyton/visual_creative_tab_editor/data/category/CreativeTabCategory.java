package com.kltyton.visual_creative_tab_editor.data.category;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A named filter inside one creative tab; membership retains native stack data. */
public record CreativeTabCategory(String id, Component title, ItemStack icon, List<ItemStack> items) {
    public CreativeTabCategory {
        if (id == null || id.length() > 256 || !id.matches("[a-z0-9_./:-]+")) {
            throw new IllegalArgumentException("Invalid creative category id: " + id);
        }
        title = Objects.requireNonNull(title, "title").copy();
        if (title.getString().length() > CreativeTabValidation.MAX_COMPONENT_TEXT_LENGTH) {
            throw new IllegalArgumentException("Creative category title exceeds the text limit");
        }
        icon = Objects.requireNonNull(icon, "icon").copyWithCount(1);
        if (icon.isEmpty()) { throw new IllegalArgumentException("Creative category icon cannot be empty"); }
        if (items.size() > CreativeTabValidation.MAX_ITEMS_PER_TAB) {
            throw new IllegalArgumentException("Too many creative category members");
        }
        items = copyItems(items);
        if (CreativeItemGrouping.keys(items).size() != items.size()) {
            throw new IllegalArgumentException("Duplicate creative category member");
        }
    }

    @Override public Component title() { return this.title.copy(); }
    @Override public ItemStack icon() { return this.icon.copy(); }
    @Override public List<ItemStack> items() { return copyItems(this.items); }
    public int itemCount() { return this.items.size(); }

    public boolean sameContents(CreativeTabCategory other) {
        if (!this.id.equals(other.id) || !this.title.equals(other.title)
                || !ItemStack.matches(this.icon, other.icon) || this.items.size() != other.items.size()) { return false; }
        for (int index = 0; index < this.items.size(); index++) {
            if (!new CreativeItemGrouping.ItemKey(this.items.get(index))
                    .equals(new CreativeItemGrouping.ItemKey(other.items.get(index)))) { return false; }
        }
        return true;
    }

    private static List<ItemStack> copyItems(List<ItemStack> items) {
        return items.stream().map(item -> {
            if (Objects.requireNonNull(item, "item").isEmpty()) {
                throw new IllegalArgumentException("Creative category members cannot be empty");
            }
            return item.copyWithCount(1);
        }).toList();
    }
}
