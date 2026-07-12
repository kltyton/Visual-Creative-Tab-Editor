package com.kltyton.visual_creative_tab_editor.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A partial creative-tab document; present fields replace lower-priority values. */
public record CreativeTabPatch(
        int format,
        Optional<Component> title,
        Optional<ItemStack> icon,
        Optional<List<ItemStack>> items,
        Optional<List<ItemStack>> searchItems,
        Optional<Boolean> hidden,
        Optional<Integer> order,
        Optional<CreativeTabType> type,
        Optional<Boolean> canScroll,
        Optional<Boolean> showTitle,
        Optional<Boolean> alignedRight,
        Optional<Identifier> background
) {
    /** Current JSON schema version. */
    public static final int CURRENT_FORMAT = 1;

    public CreativeTabPatch {
        title = copyComponentOptional(title, "title");
        icon = copyStackOptional(icon, "icon");
        items = copyStackListOptional(items, "items");
        searchItems = copyStackListOptional(searchItems, "searchItems");
        hidden = requireOptional(hidden, "hidden");
        order = requireOptional(order, "order");
        type = requireOptional(type, "type");
        canScroll = requireOptional(canScroll, "canScroll");
        showTitle = requireOptional(showTitle, "showTitle");
        alignedRight = requireOptional(alignedRight, "alignedRight");
        background = requireOptional(background, "background");
        CreativeTabValidation.validatePatchFields(format, title, icon, items, searchItems, order);
    }

    /** Compatibility constructor for callers written before search visibility was preserved. */
    public CreativeTabPatch(
            int format,
            Optional<Component> title,
            Optional<ItemStack> icon,
            Optional<List<ItemStack>> items,
            Optional<Boolean> hidden,
            Optional<Integer> order,
            Optional<CreativeTabType> type,
            Optional<Boolean> canScroll,
            Optional<Boolean> showTitle,
            Optional<Boolean> alignedRight,
            Optional<Identifier> background
    ) {
        this(format, title, icon, items, Optional.empty(), hidden, order, type,
                canScroll, showTitle, alignedRight, background);
    }

    /** Returns an empty format-1 patch. */
    public static CreativeTabPatch empty() {
        return new CreativeTabPatch(
                CURRENT_FORMAT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    /** Returns a patch containing every field of a resolved definition. */
    public static CreativeTabPatch full(CreativeTabDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        CreativeTabLayout layout = definition.layout();
        return new CreativeTabPatch(
                definition.format(),
                Optional.of(definition.title()),
                Optional.of(definition.icon()),
                Optional.of(definition.items()),
                Optional.of(definition.searchItems()),
                Optional.of(definition.hidden()),
                Optional.of(definition.order()),
                Optional.of(definition.type()),
                Optional.of(layout.canScroll()),
                Optional.of(layout.showTitle()),
                Optional.of(layout.alignedRight()),
                Optional.of(layout.background())
        );
    }

    /** Produces the smallest field-level patch that changes {@code base} into {@code target}. */
    public static CreativeTabPatch diff(CreativeTabDefinition base, CreativeTabDefinition target) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(target, "target");
        if (!base.id().equals(target.id())) {
            throw new IllegalArgumentException("Cannot diff different creative tab ids");
        }

        CreativeTabLayout baseLayout = base.layout();
        CreativeTabLayout targetLayout = target.layout();
        return new CreativeTabPatch(
                target.format(),
                changed(base.title(), target.title()) ? Optional.of(target.title()) : Optional.empty(),
                ItemStack.matches(base.icon(), target.icon()) ? Optional.empty() : Optional.of(target.icon()),
                base.hasSameItems(target) ? Optional.empty() : Optional.of(target.items()),
                base.hasSameSearchItems(target)
                        ? Optional.empty()
                        : Optional.of(target.searchItems()),
                changed(base.hidden(), target.hidden()),
                changed(base.order(), target.order()),
                changed(base.type(), target.type()),
                changed(baseLayout.canScroll(), targetLayout.canScroll()),
                changed(baseLayout.showTitle(), targetLayout.showTitle()),
                changed(baseLayout.alignedRight(), targetLayout.alignedRight()),
                changed(baseLayout.background(), targetLayout.background())
        );
    }

    /** Merges patches in resource-pack order, from lowest to highest priority. */
    public static CreativeTabPatch merge(Iterable<CreativeTabPatch> lowToHigh) {
        Objects.requireNonNull(lowToHigh, "lowToHigh");
        CreativeTabPatch merged = empty();
        for (CreativeTabPatch patch : lowToHigh) {
            merged = merged.overlay(Objects.requireNonNull(patch, "patch"));
        }
        return merged;
    }

    /** Applies a higher-priority patch to this patch. */
    public CreativeTabPatch overlay(CreativeTabPatch higher) {
        Objects.requireNonNull(higher, "higher");
        if (format != higher.format) {
            throw new IllegalArgumentException("Cannot merge creative tab formats " + format + " and " + higher.format);
        }
        return new CreativeTabPatch(
                higher.format,
                higher.title.isPresent() ? higher.title : title,
                higher.icon.isPresent() ? higher.icon : icon,
                higher.items.isPresent() ? higher.items : items,
                higher.searchItems.isPresent() ? higher.searchItems : searchItems,
                higher.hidden.isPresent() ? higher.hidden : hidden,
                higher.order.isPresent() ? higher.order : order,
                higher.type.isPresent() ? higher.type : type,
                higher.canScroll.isPresent() ? higher.canScroll : canScroll,
                higher.showTitle.isPresent() ? higher.showTitle : showTitle,
                higher.alignedRight.isPresent() ? higher.alignedRight : alignedRight,
                higher.background.isPresent() ? higher.background : background
        );
    }

    /** Returns whether this patch changes no tab field. */
    public boolean isEmpty() {
        return title.isEmpty()
                && icon.isEmpty()
                && items.isEmpty()
                && searchItems.isEmpty()
                && hidden.isEmpty()
                && order.isEmpty()
                && type.isEmpty()
                && canScroll.isEmpty()
                && showTitle.isEmpty()
                && alignedRight.isEmpty()
                && background.isEmpty();
    }

    @Override
    public Optional<Component> title() {
        return title.map(Component::copy);
    }

    @Override
    public Optional<ItemStack> icon() {
        return icon.map(ItemStack::copy);
    }

    @Override
    public Optional<List<ItemStack>> items() {
        return items.map(CreativeTabPatch::copyStacks);
    }

    @Override
    public Optional<List<ItemStack>> searchItems() {
        return searchItems.map(CreativeTabPatch::copyStacks);
    }

    private static boolean changed(Component first, Component second) {
        return !first.equals(second);
    }

    private static Optional<Boolean> changed(boolean first, boolean second) {
        return first == second ? Optional.empty() : Optional.of(second);
    }

    private static Optional<Integer> changed(int first, int second) {
        return first == second ? Optional.empty() : Optional.of(second);
    }

    private static <T> Optional<T> changed(T first, T second) {
        return Objects.equals(first, second) ? Optional.empty() : Optional.of(second);
    }

    private static Optional<Component> copyComponentOptional(Optional<Component> value, String name) {
        return requireOptional(value, name).map(Component::copy);
    }

    private static Optional<ItemStack> copyStackOptional(Optional<ItemStack> value, String name) {
        return requireOptional(value, name).map(ItemStack::copy);
    }

    private static Optional<List<ItemStack>> copyStackListOptional(Optional<List<ItemStack>> value, String name) {
        return requireOptional(value, name).map(CreativeTabPatch::copyStacks);
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        Objects.requireNonNull(stacks, "stacks");
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(Objects.requireNonNull(stack, "stack").copy());
        }
        return List.copyOf(copies);
    }

    private static <T> Optional<T> requireOptional(Optional<T> value, String name) {
        return Objects.requireNonNull(value, name);
    }
}
