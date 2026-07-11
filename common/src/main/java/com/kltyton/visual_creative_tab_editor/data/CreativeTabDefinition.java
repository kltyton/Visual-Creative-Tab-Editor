package com.kltyton.visual_creative_tab_editor.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
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

    @Override
    public List<ItemStack> searchItems() {
        return copyStacks(searchItems);
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
