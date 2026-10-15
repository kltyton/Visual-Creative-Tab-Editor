package com.kltyton.visual_creative_tab_editor.data.itemgroup;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A named group with item identities used for tab membership. */
public record CreativeItemGroup(String id, Component title, ItemStack icon, List<ItemStack> items) {
    public CreativeItemGroup {
        if (id == null || id.length() > 256 || !id.matches("[a-z0-9_./:-]+")) {
            throw new IllegalArgumentException("Invalid item group id: " + id);
        }
        title = Objects.requireNonNull(title, "title").copy();
        if (title.getString().length() > CreativeTabValidation.MAX_COMPONENT_TEXT_LENGTH) {
            throw new IllegalArgumentException("Item group title exceeds the text limit");
        }
        icon = Objects.requireNonNull(icon, "icon").copyWithCount(1);
        if (icon.isEmpty()) {
            throw new IllegalArgumentException("Item group icon cannot be empty");
        }
        items = CreativeItemGrouping.copyStacks(items);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("An item group must contain at least one item");
        }
    }

    @Override
    public Component title() { return this.title.copy(); }

    @Override
    public ItemStack icon() { return this.icon.copy(); }

    @Override
    public List<ItemStack> items() { return CreativeItemGrouping.copyStacks(this.items); }

    public int itemCount() { return this.items.size(); }

    public boolean sameContents(CreativeItemGroup other) {
        return this.id.equals(other.id) && this.title.equals(other.title)
                && ItemStack.matches(this.icon, other.icon)
                && CreativeItemGrouping.sameStacks(this.items, other.items);
    }
}
