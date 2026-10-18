package com.kltyton.visual_creative_tab_editor.data.category;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping.ItemKey;
import net.minecraft.world.item.ItemStack;

/** Ordered categories are independent from expandable item groups and may overlap. */
public record CreativeTabCategories(List<CreativeTabCategory> entries) {
    public static final CreativeTabCategories EMPTY = new CreativeTabCategories(List.of());

    public CreativeTabCategories {
        entries = List.copyOf(entries);
        if (entries.size() > CreativeTabValidation.MAX_TABS) {
            throw new IllegalArgumentException("Too many categories in one creative tab");
        }
        Set<String> ids = new HashSet<>();
        long references = 0;
        for (CreativeTabCategory category : entries) {
            if (!ids.add(category.id())) { throw new IllegalArgumentException("Duplicate category id: " + category.id()); }
            references += category.itemCount();
        }
        if (references > CreativeTabValidation.MAX_ITEMS_PER_TAB) {
            throw new IllegalArgumentException("Creative category references exceed the per-tab limit");
        }
    }

    public boolean isEmpty() { return this.entries.isEmpty(); }
    public int referencedItemCount() { return this.entries.stream().mapToInt(CreativeTabCategory::itemCount).sum(); }
    public CreativeTabCategory require(String id) {
        return this.entries.stream().filter(category -> category.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown creative category: " + id));
    }

    public CreativeTabCategories put(CreativeTabCategory category) {
        List<CreativeTabCategory> result = new ArrayList<>(this.entries);
        for (int index = 0; index < result.size(); index++) {
            if (result.get(index).id().equals(category.id())) {
                result.set(index, category);
                return new CreativeTabCategories(result);
            }
        }
        result.add(category);
        return new CreativeTabCategories(result);
    }

    public CreativeTabCategories remove(String id) {
        return new CreativeTabCategories(this.entries.stream().filter(category -> !category.id().equals(id)).toList());
    }

    public CreativeTabCategories move(String id, int direction) {
        List<CreativeTabCategory> result = new ArrayList<>(this.entries);
        int from = result.indexOf(require(id));
        int to = Math.max(0, Math.min(result.size() - 1, from + direction));
        result.add(to, result.remove(from));
        return new CreativeTabCategories(result);
    }

    /** Replacement inherits every former category; an empty replacement only removes membership. */
    public CreativeTabCategories replaceItems(Collection<ItemStack> previous, ItemStack replacement) {
        if (this.entries.isEmpty() || previous.isEmpty()) { return this; }
        Set<ItemKey> removed = CreativeItemGrouping.keys(previous);
        List<CreativeTabCategory> result = new ArrayList<>();
        for (CreativeTabCategory category : this.entries) {
            List<ItemStack> items = category.items();
            List<ItemStack> retained = new ArrayList<>(items.stream()
                    .filter(item -> !removed.contains(new ItemKey(item))).toList());
            if (retained.size() == items.size()) {
                result.add(category);
                continue;
            }
            if (!replacement.isEmpty() && !CreativeItemGrouping.keys(retained).contains(new ItemKey(replacement))) {
                retained.add(replacement);
            }
            result.add(new CreativeTabCategory(category.id(), category.title(), category.icon(), retained));
        }
        return new CreativeTabCategories(result);
    }

    public boolean sameContents(CreativeTabCategories other) {
        if (this.entries.size() != other.entries.size()) { return false; }
        for (int index = 0; index < this.entries.size(); index++) {
            if (!this.entries.get(index).sameContents(other.entries.get(index))) { return false; }
        }
        return true;
    }
}
