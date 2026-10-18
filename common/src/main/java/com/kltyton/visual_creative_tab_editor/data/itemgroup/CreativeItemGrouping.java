package com.kltyton.visual_creative_tab_editor.data.itemgroup;

import com.kltyton.visual_creative_tab_editor.platform.item.StackData;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Explicit membership overrides and items excluded from automatic grouping. */
public record CreativeItemGrouping(List<CreativeItemGroup> groups, List<ItemStack> ungrouped) {
    public static final CreativeItemGrouping EMPTY = new CreativeItemGrouping(List.of(), List.of());

    public CreativeItemGrouping {
        groups = List.copyOf(groups);
        ungrouped = copyStacks(ungrouped);
        Set<String> ids = new HashSet<>();
        Set<ItemKey> assigned = new HashSet<>();
        long references = ungrouped.size();
        for (CreativeItemGroup group : groups) {
            if (!ids.add(group.id())) {
                throw new IllegalArgumentException("Duplicate item group id: " + group.id());
            }
            references += group.itemCount();
            for (ItemStack item : group.items()) {
                if (!assigned.add(new ItemKey(item))) {
                    throw new IllegalArgumentException("An item cannot belong to multiple groups");
                }
            }
        }
        for (ItemStack item : ungrouped) {
            if (!assigned.add(new ItemKey(item))) {
                throw new IllegalArgumentException("Duplicate or grouped item in ungrouped items");
            }
        }
        if (groups.size() > CreativeTabValidation.MAX_ITEMS_PER_TAB
                || references > CreativeTabValidation.MAX_ITEMS_PER_TAB) {
            throw new IllegalArgumentException("Item grouping exceeds the per-tab item limit");
        }
    }

    @Override
    public List<ItemStack> ungrouped() { return copyStacks(this.ungrouped); }

    public boolean isEmpty() { return this.groups.isEmpty() && this.ungrouped.isEmpty(); }

    public int referencedItemCount() {
        return this.ungrouped.size() + this.groups.stream().mapToInt(CreativeItemGroup::itemCount).sum();
    }

    public CreativeItemGrouping assign(CreativeItemGroup assigned) {
        Set<ItemKey> moved = keys(assigned.items());
        List<CreativeItemGroup> result = new ArrayList<>();
        boolean replaced = false;
        for (CreativeItemGroup group : this.groups) {
            if (group.id().equals(assigned.id())) {
                result.add(assigned);
                replaced = true;
            } else {
                List<ItemStack> retained = without(group.items(), moved);
                if (!retained.isEmpty()) {
                    result.add(new CreativeItemGroup(group.id(), group.title(), group.icon(), retained));
                }
            }
        }
        if (!replaced) {
            result.add(assigned);
        }
        return new CreativeItemGrouping(result, without(this.ungrouped, moved));
    }

    public CreativeItemGrouping detach(Collection<ItemStack> items) {
        CreativeItemGrouping retained = remove(items);
        List<ItemStack> ungroupedItems = new ArrayList<>(retained.ungrouped);
        Set<ItemKey> existing = keys(ungroupedItems);
        for (ItemStack item : items) {
            if (existing.add(new ItemKey(item))) {
                ungroupedItems.add(item);
            }
        }
        return new CreativeItemGrouping(retained.groups, ungroupedItems);
    }

    public CreativeItemGrouping dissolve(String id, Collection<ItemStack> effectiveItems) {
        List<ItemStack> detached = new ArrayList<>(effectiveItems);
        for (CreativeItemGroup group : this.groups) {
            if (group.id().equals(id)) {
                Set<ItemKey> present = keys(detached);
                for (ItemStack item : group.items()) {
                    if (present.add(new ItemKey(item))) {
                        detached.add(item);
                    }
                }
            }
        }
        return detach(detached);
    }

    public CreativeItemGrouping remove(Collection<ItemStack> items) {
        Set<ItemKey> removed = keys(items);
        List<CreativeItemGroup> result = new ArrayList<>();
        for (CreativeItemGroup group : this.groups) {
            List<ItemStack> retained = without(group.items(), removed);
            if (!retained.isEmpty()) {
                result.add(new CreativeItemGroup(group.id(), group.title(), group.icon(), retained));
            }
        }
        return new CreativeItemGrouping(result, without(this.ungrouped, removed));
    }

    public CreativeItemGrouping replaceItems(List<ItemStack> previous, ItemStack replacement) {
        if (previous.isEmpty()) { return this; }
        ItemKey anchor = new ItemKey(previous.get(0));
        CreativeItemGroup owner = this.groups.stream()
                .filter(group -> keys(group.items()).contains(anchor)).findFirst().orElse(null);
        CreativeItemGrouping retained = remove(previous);
        if (owner != null) {
            List<ItemStack> members = new ArrayList<>(retained.groups.stream()
                    .filter(group -> group.id().equals(owner.id())).findFirst().map(CreativeItemGroup::items).orElse(List.of()));
            if (!keys(members).contains(new ItemKey(replacement))) { members.add(replacement); }
            return retained.assign(new CreativeItemGroup(owner.id(), owner.title(), owner.icon(), members));
        }
        return keys(this.ungrouped).contains(anchor) ? retained.detach(List.of(replacement)) : retained;
    }

    public boolean sameContents(CreativeItemGrouping other) {
        if (this.groups.size() != other.groups.size() || !sameStacks(this.ungrouped, other.ungrouped)) {
            return false;
        }
        for (int index = 0; index < this.groups.size(); index++) {
            if (!this.groups.get(index).sameContents(other.groups.get(index))) {
                return false;
            }
        }
        return true;
    }

    public static Set<ItemKey> keys(Collection<ItemStack> stacks) {
        Set<ItemKey> keys = new HashSet<>();
        for (ItemStack stack : stacks) { keys.add(new ItemKey(stack)); }
        return keys;
    }

    private static List<ItemStack> without(List<ItemStack> items, Set<ItemKey> excluded) {
        return items.stream().filter(item -> !excluded.contains(new ItemKey(item))).toList();
    }

    static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        if (stacks.size() > CreativeTabValidation.MAX_ITEMS_PER_TAB) {
            throw new IllegalArgumentException("Too many item group members");
        }
        return stacks.stream().map(stack -> {
            if (Objects.requireNonNull(stack, "stack").isEmpty()) {
                throw new IllegalArgumentException("Item group members cannot be empty");
            }
            return stack.copyWithCount(1);
        }).toList();
    }

    static boolean sameStacks(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) { return false; }
        for (int index = 0; index < first.size(); index++) {
            if (!new ItemKey(first.get(index)).equals(new ItemKey(second.get(index)))) { return false; }
        }
        return true;
    }

    /** Immutable keys preserve native NBT/component equality in membership maps. */
    public static final class ItemKey {
        private final ItemStack stack;

        public ItemKey(ItemStack stack) { this.stack = Objects.requireNonNull(stack).copyWithCount(1); }

        @Override
        public boolean equals(Object other) {
            return other instanceof ItemKey key && StackData.same(this.stack, key.stack);
        }

        @Override
        public int hashCode() {
            return StackData.hash(this.stack);
        }
    }
}
