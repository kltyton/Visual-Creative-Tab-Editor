package com.kltyton.visual_creative_tab_editor.client.editor.category;

import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategories;
import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategory;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping.ItemKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Per-tab browsing choices intersect the current native list without replacing it. */
public final class CreativeCategoryFilter {
    public static final String UNCATEGORIZED = "";
    private CreativeTabCategories categories = CreativeTabCategories.EMPTY;
    private final Map<ItemKey, Set<String>> membership = new HashMap<>();
    private final Set<String> selected = new HashSet<>();
    private boolean all = true;

    public boolean setCategories(CreativeTabCategories categories) {
        if (this.categories == categories) { return false; }
        this.categories = categories;
        this.membership.clear();
        Set<String> ids = new HashSet<>();
        ids.add(UNCATEGORIZED);
        for (CreativeTabCategory category : categories.entries()) {
            ids.add(category.id());
            for (ItemStack item : category.items()) {
                this.membership.computeIfAbsent(new ItemKey(item), ignored -> new HashSet<>()).add(category.id());
            }
        }
        boolean hadSelection = !this.selected.isEmpty();
        this.selected.retainAll(ids);
        if (hadSelection && this.selected.isEmpty()) { showAll(); }
        if (categories.isEmpty()) { showAll(); }
        return true;
    }

    public CreativeTabCategories categories() { return this.categories; }
    public boolean active() { return !this.all && !this.categories.isEmpty(); }
    public boolean allSelected() { return this.all; }
    public boolean selected(String id) { return this.all || this.selected.contains(id); }
    public void showAll() { this.all = true; this.selected.clear(); }

    public void select(String id, boolean multiple) {
        if (!id.equals(UNCATEGORIZED)) { this.categories.require(id); }
        if (!multiple) {
            this.selected.clear();
            this.selected.add(id);
        } else {
            if (this.all) {
                this.categories.entries().forEach(category -> this.selected.add(category.id()));
                this.selected.add(UNCATEGORIZED);
            }
            if (!this.selected.remove(id)) { this.selected.add(id); }
        }
        this.all = false;
    }

    public boolean includes(ItemStack item) {
        if (!active()) { return true; }
        if (item.isEmpty()) { return false; }
        Set<String> ids = this.membership.get(new ItemKey(item));
        return ids == null ? this.selected.contains(UNCATEGORIZED)
                : ids.stream().anyMatch(this.selected::contains);
    }
}
