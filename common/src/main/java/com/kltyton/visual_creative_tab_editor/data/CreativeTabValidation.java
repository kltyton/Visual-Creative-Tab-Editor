package com.kltyton.visual_creative_tab_editor.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Shared limits and invariants for creative-tab data. */
public final class CreativeTabValidation {
    public static final int MAX_TABS = 512;
    public static final int MAX_ITEMS_PER_TAB = 262_144;
    public static final int MAX_TOTAL_ITEMS = 2_097_152;
    public static final int MAX_ORDER_ABS = 1_000_000;
    public static final int MAX_COMPONENT_TEXT_LENGTH = 32_768;
    public static final int MAX_CODEC_JSON_LENGTH = 131_072;
    public static final int MAX_DOCUMENT_JSON_LENGTH = 32 * 1024 * 1024;
    public static final int MAX_CATALOG_JSON_LENGTH = 64 * 1024 * 1024;
    private static final Map<ResourceLocation, CreativeTabType> FIXED_VANILLA_TYPES = Map.of(
            ResourceLocation.withDefaultNamespace("hotbar"), CreativeTabType.HOTBAR,
            ResourceLocation.withDefaultNamespace("search"), CreativeTabType.SEARCH,
            ResourceLocation.withDefaultNamespace("inventory"), CreativeTabType.INVENTORY
    );

    private CreativeTabValidation() {
    }

    /** Validates a complete set and its at-least-one-visible-category invariant. */
    public static void validateAll(Collection<CreativeTabDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("Creative tab data must contain at least one tab");
        }
        if (definitions.size() > MAX_TABS) {
            throw new IllegalArgumentException("Too many creative tabs: " + definitions.size() + " > " + MAX_TABS);
        }

        Set<ResourceLocation> ids = new HashSet<>();
        long totalItems = 0;
        boolean hasVisibleCategory = false;
        for (CreativeTabDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            if (!ids.add(definition.id())) {
                throw new IllegalArgumentException("Duplicate creative tab id: " + definition.id());
            }
            CreativeTabType requiredType = FIXED_VANILLA_TYPES.get(definition.id());
            if (requiredType != null && definition.type() != requiredType) {
                throw new IllegalArgumentException(
                        "Vanilla creative tab " + definition.id() + " must keep type "
                                + requiredType.serializedName() + ", got " + definition.type().serializedName()
                );
            }
            // Definitions validate their private defensive copies during construction.
            totalItems += (long) definition.itemCount() + definition.searchItemCount();
            if (totalItems > MAX_TOTAL_ITEMS) {
                throw new IllegalArgumentException("Too many creative tab items: " + totalItems + " > " + MAX_TOTAL_ITEMS);
            }
            if (!definition.hidden() && definition.type() == CreativeTabType.CATEGORY) {
                hasVisibleCategory = true;
            }
        }

        if (!hasVisibleCategory) {
            throw new IllegalArgumentException("At least one non-hidden category creative tab is required");
        }
    }

    static void validatePatchFields(
            int format,
            Optional<Component> title,
            Optional<ItemStack> icon,
            Optional<List<ItemStack>> items,
            Optional<List<ItemStack>> searchItems,
            Optional<Integer> order
    ) {
        validateFormat(format);
        title.ifPresent(CreativeTabValidation::validateTitle);
        icon.ifPresent(stack -> validateStack(stack, "icon"));
        items.ifPresent(value -> validateItems(value, "items"));
        searchItems.ifPresent(value -> validateItems(value, "search_items"));
        order.ifPresent(CreativeTabValidation::validateOrder);
    }

    static void validateDefinitionFields(
            int format,
            Component title,
            ItemStack icon,
            List<ItemStack> items,
            List<ItemStack> searchItems,
            int order,
            CreativeTabType type
    ) {
        validateFormat(format);
        validateTitle(title);
        validateStack(icon, "icon");
        validateItems(items, "items");
        validateItems(searchItems, "search_items");
        validateOrder(order);
        if ((type == CreativeTabType.HOTBAR || type == CreativeTabType.INVENTORY)
                && (!items.isEmpty() || !searchItems.isEmpty())) {
            throw new IllegalArgumentException(type.serializedName() + " creative tabs cannot contain configured items");
        }
    }

    private static void validateFormat(int format) {
        if (format != CreativeTabPatch.CURRENT_FORMAT) {
            throw new IllegalArgumentException("Unsupported creative tab format: " + format);
        }
    }

    private static void validateTitle(Component title) {
        Objects.requireNonNull(title, "title");
        if (title.getString().length() > MAX_COMPONENT_TEXT_LENGTH) {
            throw new IllegalArgumentException("Creative tab title is too long");
        }
    }

    private static void validateItems(List<ItemStack> items, String field) {
        Objects.requireNonNull(items, field);
        if (items.size() > MAX_ITEMS_PER_TAB) {
            throw new IllegalArgumentException("Too many " + field + " in creative tab: "
                    + items.size() + " > " + MAX_ITEMS_PER_TAB);
        }
        for (int index = 0; index < items.size(); index++) {
            validateStack(Objects.requireNonNull(items.get(index), "item"), field + "[" + index + "]");
        }
    }

    private static void validateStack(ItemStack stack, String field) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be an empty item stack");
        }
        if (stack.getCount() != 1) {
            throw new IllegalArgumentException(field + " must have a stack count of exactly 1");
        }
    }

    private static void validateOrder(int order) {
        if (order < -MAX_ORDER_ABS || order > MAX_ORDER_ABS) {
            throw new IllegalArgumentException("Creative tab order is outside the supported range: " + order);
        }
    }
}
