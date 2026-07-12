package com.kltyton.visual_creative_tab_editor.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A fully resolved creative-tab definition. {@code items} is the parent-tab
 * sequence; {@code searchItems} is the sequence contributed to global search.
 */
public record CreativeTabDefinition(
        Identifier id,
        int format,
        Component title,
        ItemStack icon,
        List<ItemStack> items,
        List<ItemStack> searchItems,
        boolean hidden,
        int order,
        CreativeTabType type,
        CreativeTabLayout layout
) {
    public CreativeTabDefinition {
        Objects.requireNonNull(id, "id");
        title = Objects.requireNonNull(title, "title").copy();
        icon = Objects.requireNonNull(icon, "icon").copy();
        items = copyStacks(items);
        searchItems = copyStacks(searchItems);
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(layout, "layout");
        CreativeTabValidation.validateDefinitionFields(format, title, icon, items, searchItems, order, type);
    }

    /**
     * Compatibility constructor for definitions that treated every parent item
     * as visible in search. Both lists are still copied independently.
     */
    public CreativeTabDefinition(
            Identifier id,
            int format,
            Component title,
            ItemStack icon,
            List<ItemStack> items,
            boolean hidden,
            int order,
            CreativeTabType type,
            CreativeTabLayout layout
    ) {
        this(id, format, title, icon, items,
                type == CreativeTabType.CATEGORY ? items : List.of(),
                hidden, order, type, layout);
    }

    /** Creates a safe category baseline for a new identifier. */
    public static CreativeTabDefinition defaults(Identifier id) {
        Objects.requireNonNull(id, "id");
        return new CreativeTabDefinition(
                id,
                CreativeTabPatch.CURRENT_FORMAT,
                Component.literal(id.toString()),
                new ItemStack(Items.STONE),
                List.of(),
                List.of(),
                false,
                0,
                CreativeTabType.CATEGORY,
                CreativeTabLayout.DEFAULT
        );
    }

    /** Resolves low-to-high patches against the safe default baseline. */
    public static CreativeTabDefinition resolve(Identifier id, Iterable<CreativeTabPatch> lowToHigh) {
        return defaults(id).apply(CreativeTabPatch.merge(lowToHigh));
    }

    /** Applies one higher-priority patch. */
    public CreativeTabDefinition apply(CreativeTabPatch patch) {
        Objects.requireNonNull(patch, "patch");
        if (format != patch.format()) {
            throw new IllegalArgumentException("Cannot apply creative tab format " + patch.format() + " to format " + format);
        }
        return new CreativeTabDefinition(
                id,
                patch.format(),
                patch.title().orElse(title),
                patch.icon().orElse(icon),
                patch.items().orElse(items),
                patch.searchItems().orElse(searchItems),
                patch.hidden().orElse(hidden),
                patch.order().orElse(order),
                patch.type().orElse(type),
                layout.apply(patch)
        );
    }

    /** Applies patches in resource-pack order, from lowest to highest priority. */
    public CreativeTabDefinition applyAll(Iterable<CreativeTabPatch> lowToHigh) {
        return apply(CreativeTabPatch.merge(lowToHigh));
    }

    @Override
    public Component title() {
        return title.copy();
    }

    @Override
    public ItemStack icon() {
        return icon.copy();
    }

    @Override
    public List<ItemStack> items() {
        return copyStacks(items);
    }

    /** Returns the parent-tab item count without materializing defensive copies. */
    public int itemCount() {
        return items.size();
    }

    /** Returns one defensive parent-tab stack copy. */
    public ItemStack itemAt(int index) {
        return items.get(index).copy();
    }

    /** Compares one parent-tab stack without exposing or copying the stored stack. */
    public boolean itemMatches(int index, ItemStack other) {
        return ItemStack.isSameItemSameComponents(items.get(index), Objects.requireNonNull(other, "other"));
    }

    /** Appends defensive count-one copies without allocating an intermediate list. */
    public void copyItemsTo(Collection<ItemStack> destination) {
        Objects.requireNonNull(destination, "destination");
        for (ItemStack stack : items) {
            destination.add(stack.copyWithCount(1));
        }
    }

    @Override
    public List<ItemStack> searchItems() {
        return copyStacks(searchItems);
    }

    /** Returns the search-contribution count without materializing defensive copies. */
    public int searchItemCount() {
        return searchItems.size();
    }

    /** Returns one defensive search-contribution stack copy. */
    public ItemStack searchItemAt(int index) {
        return searchItems.get(index).copy();
    }

    /** Appends defensive count-one search copies without allocating an intermediate list. */
    public void copySearchItemsTo(Collection<ItemStack> destination) {
        Objects.requireNonNull(destination, "destination");
        for (ItemStack stack : searchItems) {
            destination.add(stack.copyWithCount(1));
        }
    }

    /** Compares parent-tab item order without creating defensive snapshots. */
    public boolean hasSameItems(CreativeTabDefinition other) {
        return sameStacks(items, Objects.requireNonNull(other, "other").items);
    }

    /** Compares search contribution order without creating defensive snapshots. */
    public boolean hasSameSearchItems(CreativeTabDefinition other) {
        return sameStacks(searchItems, Objects.requireNonNull(other, "other").searchItems);
    }

    private static boolean sameStacks(List<ItemStack> left, List<ItemStack> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            if (!ItemStack.isSameItemSameComponents(left.get(index), right.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        Objects.requireNonNull(stacks, "items");
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(Objects.requireNonNull(stack, "stack").copy());
        }
        return List.copyOf(copies);
    }
}
