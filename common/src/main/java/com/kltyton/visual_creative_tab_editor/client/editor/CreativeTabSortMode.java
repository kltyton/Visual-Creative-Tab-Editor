package com.kltyton.visual_creative_tab_editor.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Stable item ordering modes offered by the creative-tab editor. */
public enum CreativeTabSortMode {
    /** Sorts by caller-provided semantic creative groups, then by registry id. */
    TYPE,
    /** Sorts by the registry path, ignoring its namespace until tie-breaking. */
    ID_PATH,
    /** Sorts by the displayed name using the locale selected by Minecraft. */
    LOCALIZED_NAME,
    /** Sorts by the complete {@code namespace:path} registry id. */
    FULL_ID,
    /** Sorts by registry namespace, then by the complete registry id. */
    MOD_ID;

    /**
     * Builds a comparator for this mode. A new localized comparator is created
     * for every call because {@link Collator} is mutable and not thread-safe.
     */
    public Comparator<ItemStack> comparator() {
        Comparator<ItemStack> fullId = Comparator.comparing(CreativeTabSortMode::fullId);
        return switch (this) {
            case TYPE -> Comparator.comparing(CreativeTabSortMode::typeKey).thenComparing(fullId);
            case ID_PATH -> Comparator.comparing(CreativeTabSortMode::idPath).thenComparing(fullId);
            case LOCALIZED_NAME -> localizedComparator();
            case FULL_ID -> fullId;
            case MOD_ID -> Comparator.comparing(CreativeTabSortMode::namespace).thenComparing(fullId);
        };
    }

    public Comparator<ItemStack> comparator(Map<Item, SemanticRank> semanticRanks) {
        Objects.requireNonNull(semanticRanks, "semanticRanks");
        if (this != TYPE) {
            return comparator();
        }
        SemanticRank fallback = new SemanticRank(Integer.MAX_VALUE, Integer.MAX_VALUE);
        return Comparator
                .comparing((ItemStack stack) -> semanticRanks.getOrDefault(requireStack(stack).getItem(), fallback))
                .thenComparing(FULL_ID.comparator());
    }

    public record SemanticRank(int groupIndex, int itemIndex) implements Comparable<SemanticRank> {
        @Override
        public int compareTo(SemanticRank other) {
            int group = Integer.compare(this.groupIndex, other.groupIndex);
            return group != 0 ? group : Integer.compare(this.itemIndex, other.itemIndex);
        }
    }

    private static Comparator<ItemStack> localizedComparator() {
        Collator collator = Collator.getInstance(currentMinecraftLocale());
        collator.setStrength(Collator.TERTIARY);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        return (left, right) -> {
            int byName = collator.compare(displayName(left), displayName(right));
            return byName != 0 ? byName : fullId(left).compareTo(fullId(right));
        };
    }

    private static Locale currentMinecraftLocale() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null) {
            return Locale.ROOT;
        }
        String languageCode = minecraft.getLanguageManager().getSelected();
        if (languageCode == null || languageCode.isBlank()) {
            return Locale.ROOT;
        }
        Locale locale = Locale.forLanguageTag(languageCode.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? Locale.ROOT : locale;
    }

    private static String displayName(ItemStack stack) {
        return requireStack(stack).getHoverName().getString();
    }

    private static String typeKey(ItemStack stack) {
        return requireStack(stack).getItem().getClass().getName();
    }

    private static String idPath(ItemStack stack) {
        Identifier id = itemId(stack);
        return id == null ? fullId(stack) : id.getPath();
    }

    private static String namespace(ItemStack stack) {
        Identifier id = itemId(stack);
        return id == null ? "" : id.getNamespace();
    }

    private static String fullId(ItemStack stack) {
        ItemStack checked = requireStack(stack);
        Identifier id = itemId(checked);
        if (id != null) {
            return id.toString();
        }
        return "~unregistered:" + checked.getItem().getClass().getName()
                + '/' + checked.getItem().getDescriptionId();
    }

    private static Identifier itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(requireStack(stack).getItem());
    }

    private static ItemStack requireStack(ItemStack stack) {
        return Objects.requireNonNull(stack, "stack");
    }
}
