package com.kltyton.visual_creative_tab_editor.client.editor;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalog;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabLayout;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabPatch;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabType;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * A controlled, client-side editing copy of a creative-tab catalog.
 * Mutable operations never expose the draft's internal stacks or components.
 */
public final class CreativeTabDraft {
    public static final String CUSTOM_NAMESPACE = "visual_creative_tab_editor";

    private final List<CreativeTabDefinition> tabs = new ArrayList<>();
    private final LinkedHashSet<ResourceLocation> selectedTabIds = new LinkedHashSet<>();
    private final NavigableSet<Integer> selectedItemIndices = new TreeSet<>();
    private final List<ItemStack> currentSearchItems = new ArrayList<>();
    private final Map<Item, Map<DataComponentMap, Integer>> currentSearchItemIndices = new HashMap<>();
    private ResourceLocation currentTabId;
    private CreativeTabType currentTabType = CreativeTabType.CATEGORY;
    private int currentSearchPreferenceCapacity;
    private boolean currentSearchItemsPrepared;

    /** Creates a deep editing copy of {@code catalog}. */
    public CreativeTabDraft(CreativeTabCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        int index = 0;
        for (CreativeTabDefinition definition : catalog.orderedDefinitions()) {
            this.tabs.add(copyDefinition(definition, index++));
        }
        this.currentTabId = this.tabs.stream()
                .filter(definition -> definition.type() == CreativeTabType.CATEGORY && !definition.hidden())
                .map(CreativeTabDefinition::id)
                .findFirst()
                .orElseGet(() -> this.tabs.isEmpty() ? null : this.tabs.getFirst().id());
        if (this.currentTabId != null) {
            this.currentTabType = this.tabs.get(requireTabIndex(this.currentTabId)).type();
        }
    }

    public static CreativeTabDraft fromCatalog(CreativeTabCatalog catalog) {
        return new CreativeTabDraft(catalog);
    }

    /** Returns detached definitions in their current draft order. */
    public List<CreativeTabDefinition> tabs() {
        List<CreativeTabDefinition> copies = new ArrayList<>(this.tabs.size());
        for (int index = 0; index < this.tabs.size(); index++) {
            copies.add(copyDefinition(this.tabs.get(index), index));
        }
        return List.copyOf(copies);
    }

    public Optional<CreativeTabDefinition> definition(ResourceLocation id) {
        int index = indexOf(requireId(id));
        return index < 0 ? Optional.empty() : Optional.of(copyDefinition(this.tabs.get(index), index));
    }

    public Optional<ResourceLocation> currentTabId() {
        return Optional.ofNullable(this.currentTabId);
    }

    public Optional<CreativeTabDefinition> currentTab() {
        return this.currentTabId == null ? Optional.empty() : definition(this.currentTabId);
    }

    /** Returns the current type without copying the definition or its stacks. */
    public CreativeTabType currentTabType() {
        return this.currentTabType;
    }

    /** Changes the item-editing target and clears index-based item selection. */
    public void setCurrentTab(ResourceLocation id) {
        switchCurrentTab(requireExistingId(id));
    }

    public Set<ResourceLocation> selectedTabIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.selectedTabIds));
    }

    public boolean isTabSelected(ResourceLocation id) {
        return this.selectedTabIds.contains(requireId(id));
    }

    public void setTabSelected(ResourceLocation id, boolean selected) {
        ResourceLocation checked = requireExistingId(id);
        if (selected) {
            this.selectedTabIds.add(checked);
        } else {
            this.selectedTabIds.remove(checked);
        }
    }

    /** Toggles an id-based tab selection and returns its new state. */
    public boolean toggleTabSelection(ResourceLocation id) {
        ResourceLocation checked = requireExistingId(id);
        if (this.selectedTabIds.remove(checked)) {
            return false;
        }
        this.selectedTabIds.add(checked);
        return true;
    }

    public void clearTabSelection() {
        this.selectedTabIds.clear();
    }

    /**
     * Moves selected tabs as one stable block. {@code targetIndex} is a slot in
     * the pre-move list: {@code 0} is the start and {@code size()} is the end.
     */
    public void moveSelectedTabs(int targetIndex) {
        moveTabs(this.selectedTabIds, targetIndex);
    }

    /** Moves the supplied tab ids as one block while preserving their old order. */
    public void moveTabs(Collection<ResourceLocation> ids, int targetIndex) {
        Objects.requireNonNull(ids, "ids");
        checkInsertionIndex(targetIndex, this.tabs.size(), "tab target index");
        Set<ResourceLocation> movingIds = checkedIds(ids);
        if (movingIds.isEmpty()) {
            return;
        }

        List<CreativeTabDefinition> moving = new ArrayList<>(movingIds.size());
        List<CreativeTabDefinition> remaining = new ArrayList<>(this.tabs.size() - movingIds.size());
        int removedBeforeTarget = 0;
        for (int index = 0; index < this.tabs.size(); index++) {
            CreativeTabDefinition definition = this.tabs.get(index);
            if (movingIds.contains(definition.id())) {
                moving.add(definition);
                if (index < targetIndex) {
                    removedBeforeTarget++;
                }
            } else {
                remaining.add(definition);
            }
        }
        int insertionIndex = targetIndex - removedBeforeTarget;
        remaining.addAll(insertionIndex, moving);
        boolean changed = false;
        for (int index = 0; index < this.tabs.size(); index++) {
            if (!this.tabs.get(index).id().equals(remaining.get(index).id())) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return;
        }
        this.tabs.clear();
        this.tabs.addAll(remaining);
        if (this.currentTabType == CreativeTabType.SEARCH) {
            this.selectedItemIndices.clear();
            invalidatePreparedSearchItems();
        }
    }

    public void setSelectedTabsHidden(boolean hidden) {
        setTabsHidden(this.selectedTabIds, hidden);
    }

    public void setTabsHidden(Collection<ResourceLocation> ids, boolean hidden) {
        Set<ResourceLocation> checked = checkedIds(ids);
        boolean changed = false;
        for (int index = 0; index < this.tabs.size(); index++) {
            CreativeTabDefinition definition = this.tabs.get(index);
            if (checked.contains(definition.id()) && definition.hidden() != hidden) {
                this.tabs.set(index, withHidden(definition, hidden));
                changed = true;
            }
        }
        if (changed && this.currentTabType == CreativeTabType.SEARCH) {
            this.selectedItemIndices.clear();
            invalidatePreparedSearchItems();
        }
    }

    /** Returns defensive count-one copies of the current reorderable tab's items. */
    public List<ItemStack> currentItems() {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        if (definition.type() == CreativeTabType.SEARCH && this.currentSearchItemsPrepared) {
            return copyStacks(this.currentSearchItems);
        }
        return copyStacks(definition.items());
    }

    /** Returns the current visible working-list size without copying its stacks. */
    public int currentItemCount() {
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() == CreativeTabType.SEARCH && this.currentSearchItemsPrepared) {
            return this.currentSearchItems.size();
        }
        return definition.items().size();
    }

    public int currentSearchItemCount() {
        return requireCurrentTab().searchItems().size();
    }

    /** Returns one defensive stack copy without copying the complete current list. */
    public ItemStack currentItemAt(int index) {
        CreativeTabDefinition definition = requireCurrentTab();
        List<ItemStack> items = definition.type() == CreativeTabType.SEARCH && this.currentSearchItemsPrepared
                ? this.currentSearchItems
                : definition.items();
        return index >= 0 && index < items.size() ? normalizeStack(items.get(index)) : ItemStack.EMPTY;
    }

    /** Finds an equal current stack without materializing a defensive list copy. */
    public int indexOfMatchingCurrentItem(ItemStack target, int excludedIndex) {
        if (target == null || target.isEmpty()) {
            return -1;
        }
        CreativeTabDefinition definition = requireCurrentTab();
        List<ItemStack> items = definition.type() == CreativeTabType.SEARCH && this.currentSearchItemsPrepared
                ? this.currentSearchItems
                : definition.items();
        for (int index = 0; index < items.size(); index++) {
            if (index != excludedIndex && ItemStack.isSameItemSameComponents(items.get(index), target)) {
                return index;
            }
        }
        return -1;
    }

    public Set<Integer> selectedItemIndices() {
        return Collections.unmodifiableSet(new TreeSet<>(this.selectedItemIndices));
    }

    public boolean isItemSelected(int index) {
        return this.selectedItemIndices.contains(index);
    }

    public void setItemSelected(int index, boolean selected) {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        checkElementIndex(index, reorderableItems(definition).size(), "item index");
        if (selected) {
            this.selectedItemIndices.add(index);
        } else {
            this.selectedItemIndices.remove(index);
        }
    }

    /** Toggles an index-based item selection and returns its new state. */
    public boolean toggleItemSelection(int index) {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        checkElementIndex(index, reorderableItems(definition).size(), "item index");
        if (this.selectedItemIndices.remove(index)) {
            return false;
        }
        this.selectedItemIndices.add(index);
        return true;
    }

    public void clearItemSelection() {
        this.selectedItemIndices.clear();
    }

    /**
     * Moves selected items as one stable block. {@code targetIndex} is a slot in
     * the pre-move item list: {@code 0} is the start and {@code size()} is the end.
     */
    public boolean moveSelectedItems(int targetIndex) {
        return moveItems(this.selectedItemIndices, targetIndex);
    }

    /**
     * Moves the selected items into another category tab, appending them as one
     * stable block and making that tab the current item-editing target.
     *
     * @return the number of items moved, or {@code -1} when the target tab
     *         cannot accept the block without exceeding a per-list limit
     */
    public int moveSelectedItemsToTab(ResourceLocation targetTabId) {
        CreativeTabDefinition source = requireEditableCategory();
        ResourceLocation checkedTargetId = requireExistingId(targetTabId);
        if (source.id().equals(checkedTargetId)) {
            return 0;
        }
        CreativeTabDefinition target = this.tabs.get(requireTabIndex(checkedTargetId));
        if (target.type() != CreativeTabType.CATEGORY) {
            throw new IllegalStateException("Only category tabs can receive dragged items");
        }

        List<ItemStack> sourceItems = new ArrayList<>(source.items());
        NavigableSet<Integer> movingIndices = checkedItemIndices(this.selectedItemIndices, sourceItems.size());
        if (movingIndices.isEmpty()) {
            return 0;
        }
        List<ItemStack> moving = movingIndices.stream()
                .map(sourceItems::get)
                .map(CreativeTabDraft::normalizeStack)
                .toList();
        for (int index : movingIndices.descendingSet()) {
            sourceItems.remove(index);
        }

        List<ItemStack> sourceSearchItems = new ArrayList<>();
        List<ItemStack> movingSearchItems = new ArrayList<>();
        List<ItemStack> remainingDisplayPool = copyStacks(sourceItems);
        List<ItemStack> movingDisplayPool = copyStacks(moving);
        for (ItemStack searchItem : source.searchItems()) {
            ItemStack normalized = normalizeStack(searchItem);
            if (removeFirstMatching(remainingDisplayPool, normalized)) {
                sourceSearchItems.add(normalized);
            } else if (removeFirstMatching(movingDisplayPool, normalized)) {
                movingSearchItems.add(normalized);
            } else {
                sourceSearchItems.add(normalized);
            }
        }

        List<ItemStack> targetItems = removeMatchingStacks(target.items(), moving);
        int insertionIndex = targetItems.size();
        targetItems.addAll(copyStacks(moving));

        List<ItemStack> originalTargetSearchItems = copyStacks(target.searchItems());
        List<ItemStack> targetSearchItems = removeMatchingStacks(originalTargetSearchItems, moving);
        for (ItemStack movingItem : moving) {
            if (matchesAny(movingItem, movingSearchItems)
                    || matchesAny(movingItem, originalTargetSearchItems)) {
                targetSearchItems.add(normalizeStack(movingItem));
            }
        }
        if (targetItems.size() > CreativeTabValidation.MAX_ITEMS_PER_TAB
                || targetSearchItems.size() > CreativeTabValidation.MAX_ITEMS_PER_TAB) {
            return -1;
        }

        CreativeTabDefinition updatedSource = withItemsAndSearchItems(source, sourceItems, sourceSearchItems);
        CreativeTabDefinition updatedTarget = withItemsAndSearchItems(target, targetItems, targetSearchItems);
        int sourceTabIndex = requireTabIndex(source.id());
        int targetTabIndex = requireTabIndex(target.id());
        this.tabs.set(sourceTabIndex, updatedSource);
        this.tabs.set(targetTabIndex, updatedTarget);
        switchCurrentTab(checkedTargetId);
        selectContiguousItems(insertionIndex, moving.size());
        return moving.size();
    }

    /** Moves the supplied stable indices as one block in their original order. */
    public boolean moveItems(Collection<Integer> indices, int targetIndex) {
        Objects.requireNonNull(indices, "indices");
        CreativeTabDefinition definition = requireReorderableItemsTab();
        List<ItemStack> items = new ArrayList<>(reorderableItems(definition));
        checkInsertionIndex(targetIndex, items.size(), "item target index");
        NavigableSet<Integer> movingIndices = checkedItemIndices(indices, items.size());
        if (movingIndices.isEmpty()) {
            return true;
        }
        if (definition.type() == CreativeTabType.SEARCH) {
            int preferenceCapacity = this.currentSearchPreferenceCapacity;
            if (targetIndex > preferenceCapacity
                    || movingIndices.stream().anyMatch(index -> index >= preferenceCapacity)) {
                return false;
            }
        }

        List<ItemStack> moving = new ArrayList<>(movingIndices.size());
        List<ItemStack> remaining = new ArrayList<>(items.size() - movingIndices.size());
        int removedBeforeTarget = 0;
        for (int index = 0; index < items.size(); index++) {
            if (movingIndices.contains(index)) {
                moving.add(normalizeStack(items.get(index)));
                if (index < targetIndex) {
                    removedBeforeTarget++;
                }
            } else {
                remaining.add(normalizeStack(items.get(index)));
            }
        }
        int insertionIndex = targetIndex - removedBeforeTarget;
        remaining.addAll(insertionIndex, moving);
        if (sameStackOrder(items, remaining)) {
            selectContiguousItems(insertionIndex, moving.size());
            return true;
        }
        if (definition.type() == CreativeTabType.SEARCH) {
            replacePreparedSearchItems(remaining);
            replaceCurrentItems(definition, searchPreference(definition, this.currentSearchItems));
        } else {
            replaceCurrentItems(definition, remaining);
        }
        selectContiguousItems(insertionIndex, moving.size());
        return true;
    }

    /** Deletes selected items and returns the number removed. */
    public int deleteSelectedItems() {
        return deleteItems(this.selectedItemIndices);
    }

    public int deleteItems(Collection<Integer> indices) {
        Objects.requireNonNull(indices, "indices");
        CreativeTabDefinition definition = requireEditableCategory();
        List<ItemStack> items = new ArrayList<>(definition.items());
        NavigableSet<Integer> checked = checkedItemIndices(indices, items.size());
        List<ItemStack> removed = checked.stream().map(items::get).toList();
        for (int index : checked.descendingSet()) {
            items.remove(index);
        }
        List<ItemStack> searchItems = removeMatchingStacks(definition.searchItems(), removed);
        replaceCurrentItemsAndSearchItems(definition, items, searchItems);
        this.selectedItemIndices.clear();
        return checked.size();
    }

    /**
     * Replaces one item and returns the replacement's final index. If an equal
     * type-and-components stack already exists, that icon is moved to the
     * replacement position instead of creating an invisible duplicate.
     */
    public int replaceItem(int index, ItemStack stack) {
        CreativeTabDefinition definition = requireEditableCategory();
        List<ItemStack> before = copyStacks(definition.items());
        checkElementIndex(index, before.size(), "item index");
        ItemStack previous = before.get(index);
        ItemStack replacement = normalizeStack(stack);
        int existingIndex = indexOfMatching(before, replacement, index);
        int resultIndex = index - (existingIndex >= 0 && existingIndex < index ? 1 : 0);

        List<ItemStack> items = new ArrayList<>(before);
        NavigableSet<Integer> removedIndices = new TreeSet<>();
        removedIndices.add(index);
        if (existingIndex >= 0) {
            removedIndices.add(existingIndex);
        }
        for (int removedIndex : removedIndices.descendingSet()) {
            items.remove(removedIndex);
        }
        items.add(resultIndex, replacement);

        List<ItemStack> searchItems = replaceMatchingStacks(
                definition.searchItems(), List.of(previous), replacement);
        replaceCurrentItemsAndSearchItems(definition, items, searchItems);
        remapSelectedItems(before, items, removedIndices, resultIndex);
        return resultIndex;
    }

    /**
     * Collapses the selected slots to one unique replacement icon. The return
     * value remains the number of selected source slots consumed.
     */
    public int replaceSelectedItems(ItemStack stack) {
        CreativeTabDefinition definition = requireEditableCategory();
        List<ItemStack> items = copyStacks(definition.items());
        ItemStack normalized = normalizeStack(stack);
        NavigableSet<Integer> checked = checkedItemIndices(this.selectedItemIndices, items.size());
        if (checked.isEmpty()) {
            return 0;
        }
        List<ItemStack> replaced = checked.stream().map(items::get).toList();

        int firstSelectedIndex = checked.first();
        int existingIndex = indexOfMatching(items, normalized);
        NavigableSet<Integer> removedIndices = new TreeSet<>(checked);
        if (existingIndex >= 0) {
            removedIndices.add(existingIndex);
        }
        int resultIndex = firstSelectedIndex;
        for (int removedIndex : removedIndices) {
            if (removedIndex < firstSelectedIndex) {
                resultIndex--;
            }
        }
        for (int removedIndex : removedIndices.descendingSet()) {
            items.remove(removedIndex);
        }
        items.add(resultIndex, normalized);

        List<ItemStack> searchItems = replaceMatchingStacks(definition.searchItems(), replaced, normalized);
        replaceCurrentItemsAndSearchItems(definition, items, searchItems);
        selectContiguousItems(resultIndex, 1);
        return checked.size();
    }

    /** Adds an item at the end and returns its stable index. */
    public int addItem(ItemStack stack) {
        return addItem(requireEditableCategory().items().size(), stack);
    }

    /**
     * Adds an item at an insertion slot and selects it. An already-present
     * type-and-components stack is moved to that slot; duplicates are never
     * created. The returned index is the final, post-move index.
     */
    public int addItem(int insertionIndex, ItemStack stack) {
        CreativeTabDefinition definition = requireEditableCategory();
        List<ItemStack> items = copyStacks(definition.items());
        checkInsertionIndex(insertionIndex, items.size(), "item insertion index");
        ItemStack added = normalizeStack(stack);
        int existingIndex = indexOfMatching(items, added);
        int resultIndex = insertionIndex;
        if (existingIndex >= 0) {
            items.remove(existingIndex);
            if (existingIndex < insertionIndex) {
                resultIndex--;
            }
        }
        items.add(resultIndex, added);

        boolean wasSearchable = matchesAny(added, definition.searchItems());
        List<ItemStack> searchItems = removeMatchingStacks(definition.searchItems(), List.of(added));
        if (existingIndex < 0 || wasSearchable) {
            searchItems.add(added.copyWithCount(1));
        }
        replaceCurrentItemsAndSearchItems(definition, items, searchItems);
        selectContiguousItems(resultIndex, 1);
        return resultIndex;
    }

    /** Sorts current category items; Java's list sort preserves equal-key order. */
    public void sortCurrentItems(CreativeTabSortMode mode) {
        sortCurrentItems(Objects.requireNonNull(mode, "mode").comparator());
    }

    public void sortCurrentItems(Comparator<ItemStack> comparator) {
        CreativeTabDefinition definition = requireEditableCategory();
        List<ItemStack> items = new ArrayList<>(copyStacks(definition.items()));
        items.sort(Objects.requireNonNull(comparator, "comparator"));
        replaceCurrentItems(definition, items);
        this.selectedItemIndices.clear();
    }

    /** Stores a sorted preference list for the dynamic search tab. */
    public void sortCurrentSearchItems(Collection<ItemStack> currentSearchItems, CreativeTabSortMode mode) {
        sortCurrentSearchItems(currentSearchItems, Objects.requireNonNull(mode, "mode").comparator());
    }

    public void sortCurrentSearchItems(
            Collection<ItemStack> currentSearchItems,
            Comparator<ItemStack> comparator
    ) {
        Objects.requireNonNull(currentSearchItems, "currentSearchItems");
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() != CreativeTabType.SEARCH) {
            throw new IllegalStateException("The current tab is not the search tab");
        }
        List<ItemStack> items = new ArrayList<>(currentSearchItems.size());
        for (ItemStack stack : currentSearchItems) {
            items.add(normalizeStack(stack));
        }
        items.sort(Objects.requireNonNull(comparator, "comparator"));
        replacePreparedSearchItems(items);
        replaceCurrentItems(definition, searchPreference(definition, this.currentSearchItems));
        this.selectedItemIndices.clear();
    }

    /**
     * Prepares the dynamic search display for index-based selection without
     * changing the persisted search preference.
     */
    public void prepareCurrentSearchItems(Collection<ItemStack> currentSearchItems) {
        Objects.requireNonNull(currentSearchItems, "currentSearchItems");
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() != CreativeTabType.SEARCH) {
            throw new IllegalStateException("The current tab is not the search tab");
        }
        replacePreparedSearchItems(currentSearchItems);
        this.selectedItemIndices.clear();
    }

    public boolean hasPreparedCurrentSearchItems() {
        return this.currentTabType == CreativeTabType.SEARCH && this.currentSearchItemsPrepared;
    }

    public int currentSearchPreferenceCapacity() {
        return this.currentSearchItemsPrepared ? this.currentSearchPreferenceCapacity : 0;
    }

    /** Returns the prepared search index without copying or scanning the search list. */
    public int indexOfPreparedSearchItem(ItemStack stack, int maxExclusive) {
        if (!this.currentSearchItemsPrepared || stack == null || stack.isEmpty() || maxExclusive <= 0) {
            return -1;
        }
        Map<DataComponentMap, Integer> byComponents = this.currentSearchItemIndices.get(stack.getItem());
        Integer index = byComponents == null ? null : byComponents.get(stack.getComponents());
        int limit = Math.min(maxExclusive, this.currentSearchPreferenceCapacity);
        return index != null && index < limit ? index : -1;
    }

    public void setCurrentTabIcon(ItemStack icon) {
        setTabIcon(requireCurrentTab().id(), icon);
    }

    public void setTabIcon(ResourceLocation id, ItemStack icon) {
        int index = requireTabIndex(id);
        CreativeTabDefinition definition = this.tabs.get(index);
        this.tabs.set(index, withIcon(definition, normalizeStack(icon)));
    }

    public void setCurrentTabTitle(String title) {
        setTabTitle(requireCurrentTab().id(), title);
    }

    /** Stores user-entered titles as literal components. */
    public void setTabTitle(ResourceLocation id, String title) {
        int index = requireTabIndex(id);
        CreativeTabDefinition definition = this.tabs.get(index);
        this.tabs.set(index, withTitle(definition, Component.literal(Objects.requireNonNull(title, "title"))));
    }

    /**
     * Adds an empty category whose icon and default literal title come from the
     * selected inventory stack. The generated id is always unique in this draft.
     */
    public ResourceLocation addCustomCategory(ItemStack selectedStack) {
        ItemStack icon = normalizeStack(selectedStack);
        return addCustomCategory(icon, icon.getHoverName().getString());
    }

    public ResourceLocation addCustomCategory(ItemStack selectedStack, String title) {
        ItemStack icon = normalizeStack(selectedStack);
        ResourceLocation id;
        do {
            id = ResourceLocation.fromNamespaceAndPath(CUSTOM_NAMESPACE, "custom/" + UUID.randomUUID());
        } while (indexOf(id) >= 0);

        CreativeTabDefinition definition = new CreativeTabDefinition(
                id,
                CreativeTabPatch.CURRENT_FORMAT,
                Component.literal(Objects.requireNonNull(title, "title")),
                icon,
                List.of(),
                List.of(),
                false,
                this.tabs.size(),
                CreativeTabType.CATEGORY,
                CreativeTabLayout.DEFAULT
        );
        this.tabs.add(definition);
        switchCurrentTab(id);
        return id;
    }

    /** Builds an immutable catalog and rewrites every order to a dense sequence. */
    public CreativeTabCatalog toCatalog() {
        return new CreativeTabCatalog(tabs());
    }

    private CreativeTabDefinition requireCurrentTab() {
        if (this.currentTabId == null) {
            throw new IllegalStateException("No current creative tab");
        }
        return this.tabs.get(requireTabIndex(this.currentTabId));
    }

    private CreativeTabDefinition requireEditableCategory() {
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() == CreativeTabType.HOTBAR || definition.type() == CreativeTabType.INVENTORY) {
            throw new IllegalStateException("Items of " + definition.type().serializedName() + " tabs are protected");
        }
        if (definition.type() != CreativeTabType.CATEGORY) {
            throw new IllegalStateException("Only category-tab items can be edited");
        }
        return definition;
    }

    private CreativeTabDefinition requireReorderableItemsTab() {
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() != CreativeTabType.CATEGORY && definition.type() != CreativeTabType.SEARCH) {
            throw new IllegalStateException(
                    "Items of " + definition.type().serializedName() + " tabs cannot be reordered"
            );
        }
        return definition;
    }

    private List<ItemStack> reorderableItems(CreativeTabDefinition definition) {
        if (definition.type() != CreativeTabType.SEARCH) {
            return definition.items();
        }
        if (!this.currentSearchItemsPrepared) {
            throw new IllegalStateException("The dynamic search order has not been prepared");
        }
        return this.currentSearchItems;
    }

    private int searchVisiblePreferenceCapacity(
            CreativeTabDefinition definition,
            List<ItemStack> visibleItems
    ) {
        int invisibleCount = invisibleSearchPreferences(definition, visibleItems).size();
        int capacity = maxSearchPreferenceSize(definition) - invisibleCount;
        return Math.min(visibleItems.size(), Math.max(0, capacity));
    }

    private List<ItemStack> searchPreference(
            CreativeTabDefinition definition,
            List<ItemStack> orderedVisibleItems
    ) {
        List<IndexedSearchPreference> invisible = invisibleSearchPreferences(definition, orderedVisibleItems);
        int maxPreferenceSize = maxSearchPreferenceSize(definition);
        int visibleLimit = Math.max(0, maxPreferenceSize - invisible.size());
        List<ItemStack> preference = new ArrayList<>(Math.min(
                maxPreferenceSize,
                orderedVisibleItems.size() + invisible.size()
        ));
        for (int index = 0; index < orderedVisibleItems.size() && index < visibleLimit; index++) {
            preference.add(normalizeStack(orderedVisibleItems.get(index)));
        }
        for (IndexedSearchPreference retained : invisible) {
            int insertionIndex = Math.min(retained.originalIndex(), preference.size());
            preference.add(insertionIndex, normalizeStack(retained.stack()));
        }
        return preference;
    }

    private static List<IndexedSearchPreference> invisibleSearchPreferences(
            CreativeTabDefinition definition,
            List<ItemStack> visibleItems
    ) {
        Set<ItemStack> visible = ItemStackLinkedSet.createTypeAndComponentsSet();
        visible.addAll(visibleItems);
        List<IndexedSearchPreference> invisible = new ArrayList<>();
        for (int index = 0; index < definition.items().size(); index++) {
            ItemStack preferred = definition.items().get(index);
            if (!visible.contains(preferred)) {
                invisible.add(new IndexedSearchPreference(index, normalizeStack(preferred)));
            }
        }
        return invisible;
    }

    private int maxSearchPreferenceSize(CreativeTabDefinition currentSearch) {
        long otherItems = 0L;
        for (CreativeTabDefinition definition : this.tabs) {
            otherItems += definition.searchItems().size();
            if (!definition.id().equals(currentSearch.id())) {
                otherItems += definition.items().size();
            }
        }
        long globalCapacity = CreativeTabValidation.MAX_TOTAL_ITEMS - otherItems;
        return (int) Math.clamp(globalCapacity, 0L, CreativeTabValidation.MAX_ITEMS_PER_TAB);
    }

    private void replacePreparedSearchItems(Collection<ItemStack> stacks) {
        this.currentSearchItems.clear();
        this.currentSearchItemIndices.clear();
        for (ItemStack stack : stacks) {
            ItemStack normalized = normalizeStack(stack);
            Map<DataComponentMap, Integer> byComponents = this.currentSearchItemIndices.computeIfAbsent(
                    normalized.getItem(),
                    ignored -> new HashMap<>()
            );
            if (byComponents.putIfAbsent(normalized.getComponents(), this.currentSearchItems.size()) == null) {
                this.currentSearchItems.add(normalized);
            }
        }
        this.currentSearchPreferenceCapacity = searchVisiblePreferenceCapacity(
                requireCurrentTab(),
                this.currentSearchItems
        );
        this.currentSearchItemsPrepared = true;
    }

    private void invalidatePreparedSearchItems() {
        this.currentSearchItems.clear();
        this.currentSearchItemIndices.clear();
        this.currentSearchPreferenceCapacity = 0;
        this.currentSearchItemsPrepared = false;
    }

    private void switchCurrentTab(ResourceLocation id) {
        if (id.equals(this.currentTabId)) {
            return;
        }
        this.currentTabId = id;
        this.currentTabType = this.tabs.get(requireTabIndex(id)).type();
        this.selectedItemIndices.clear();
        invalidatePreparedSearchItems();
    }

    private static boolean sameStackOrder(List<ItemStack> left, List<ItemStack> right) {
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

    private void replaceCurrentItems(CreativeTabDefinition expected, List<ItemStack> items) {
        int index = requireTabIndex(expected.id());
        this.tabs.set(index, withItems(this.tabs.get(index), items));
    }

    private void replaceCurrentItemsAndSearchItems(
            CreativeTabDefinition expected,
            List<ItemStack> items,
            List<ItemStack> searchItems
    ) {
        int index = requireTabIndex(expected.id());
        this.tabs.set(index, withItemsAndSearchItems(this.tabs.get(index), items, searchItems));
    }

    private static List<ItemStack> removeMatchingStacks(
            List<ItemStack> searchItems,
            Collection<ItemStack> removed
    ) {
        List<ItemStack> result = new ArrayList<>(searchItems.size());
        for (ItemStack searchItem : searchItems) {
            if (!matchesAny(searchItem, removed)) {
                result.add(normalizeStack(searchItem));
            }
        }
        return result;
    }

    private static List<ItemStack> replaceMatchingStacks(
            List<ItemStack> searchItems,
            Collection<ItemStack> replaced,
            ItemStack replacement
    ) {
        ItemStack normalized = normalizeStack(replacement);
        List<ItemStack> uniqueSearchItems = copyStacks(searchItems);
        int replacedAnchor = firstMatchingIndex(uniqueSearchItems, replaced);
        int replacementAnchor = indexOfMatching(uniqueSearchItems, normalized);
        int anchor = replacedAnchor >= 0 ? replacedAnchor : replacementAnchor;
        if (anchor < 0) {
            return uniqueSearchItems;
        }

        int removedBeforeAnchor = 0;
        List<ItemStack> result = new ArrayList<>(uniqueSearchItems.size());
        for (int index = 0; index < uniqueSearchItems.size(); index++) {
            ItemStack searchItem = uniqueSearchItems.get(index);
            if (matchesAny(searchItem, replaced)
                    || ItemStack.isSameItemSameComponents(searchItem, normalized)) {
                if (index < anchor) {
                    removedBeforeAnchor++;
                }
            } else {
                result.add(normalizeStack(searchItem));
            }
        }
        result.add(anchor - removedBeforeAnchor, normalized);
        return result;
    }

    private void remapSelectedItems(
            List<ItemStack> before,
            List<ItemStack> after,
            Collection<Integer> resultAliases,
            int resultIndex
    ) {
        NavigableSet<Integer> remapped = new TreeSet<>();
        for (int selectedIndex : this.selectedItemIndices) {
            if (selectedIndex < 0 || selectedIndex >= before.size()) {
                continue;
            }
            if (resultAliases.contains(selectedIndex)) {
                remapped.add(resultIndex);
                continue;
            }
            int mappedIndex = indexOfMatching(after, before.get(selectedIndex));
            if (mappedIndex >= 0) {
                remapped.add(mappedIndex);
            }
        }
        this.selectedItemIndices.clear();
        this.selectedItemIndices.addAll(remapped);
    }

    private static boolean matchesAny(ItemStack stack, Collection<ItemStack> candidates) {
        for (ItemStack candidate : candidates) {
            if (ItemStack.isSameItemSameComponents(stack, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static int firstMatchingIndex(List<ItemStack> stacks, Collection<ItemStack> candidates) {
        for (int index = 0; index < stacks.size(); index++) {
            if (matchesAny(stacks.get(index), candidates)) {
                return index;
            }
        }
        return -1;
    }

    private static int indexOfMatching(List<ItemStack> stacks, ItemStack target) {
        return indexOfMatching(stacks, target, -1);
    }

    private static int indexOfMatching(List<ItemStack> stacks, ItemStack target, int excludedIndex) {
        for (int index = 0; index < stacks.size(); index++) {
            if (index != excludedIndex && ItemStack.isSameItemSameComponents(stacks.get(index), target)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean removeFirstMatching(List<ItemStack> candidates, ItemStack stack) {
        for (int index = 0; index < candidates.size(); index++) {
            if (ItemStack.isSameItemSameComponents(candidates.get(index), stack)) {
                candidates.remove(index);
                return true;
            }
        }
        return false;
    }

    private void selectContiguousItems(int firstIndex, int count) {
        this.selectedItemIndices.clear();
        for (int offset = 0; offset < count; offset++) {
            this.selectedItemIndices.add(firstIndex + offset);
        }
    }

    private Set<ResourceLocation> checkedIds(Collection<ResourceLocation> ids) {
        LinkedHashSet<ResourceLocation> checked = new LinkedHashSet<>();
        for (ResourceLocation id : ids) {
            checked.add(requireExistingId(id));
        }
        return checked;
    }

    private static NavigableSet<Integer> checkedItemIndices(Collection<Integer> indices, int size) {
        NavigableSet<Integer> checked = new TreeSet<>();
        for (Integer index : indices) {
            int value = Objects.requireNonNull(index, "item index");
            checkElementIndex(value, size, "item index");
            checked.add(value);
        }
        return checked;
    }

    private ResourceLocation requireExistingId(ResourceLocation id) {
        ResourceLocation checked = requireId(id);
        if (indexOf(checked) < 0) {
            throw new IllegalArgumentException("Unknown creative tab id: " + checked);
        }
        return checked;
    }

    private int requireTabIndex(ResourceLocation id) {
        ResourceLocation checked = requireId(id);
        int index = indexOf(checked);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown creative tab id: " + checked);
        }
        return index;
    }

    private int indexOf(ResourceLocation id) {
        for (int index = 0; index < this.tabs.size(); index++) {
            if (this.tabs.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static ResourceLocation requireId(ResourceLocation id) {
        return Objects.requireNonNull(id, "id");
    }

    private static void checkElementIndex(int index, int size, String name) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(name + " " + index + " out of bounds for size " + size);
        }
    }

    private static void checkInsertionIndex(int index, int size, String name) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(name + " " + index + " out of bounds for size " + size);
        }
    }

    private static ItemStack normalizeStack(ItemStack stack) {
        ItemStack checked = Objects.requireNonNull(stack, "stack");
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("Creative-tab stacks cannot be empty");
        }
        return checked.copyWithCount(1);
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        Set<ItemStack> unique = ItemStackLinkedSet.createTypeAndComponentsSet();
        for (ItemStack stack : stacks) {
            ItemStack copy = normalizeStack(stack);
            if (unique.add(copy)) {
                copies.add(copy);
            }
        }
        return copies;
    }

    private record IndexedSearchPreference(int originalIndex, ItemStack stack) {
    }

    private static CreativeTabDefinition copyDefinition(CreativeTabDefinition definition, int order) {
        return new CreativeTabDefinition(
                definition.id(),
                definition.format(),
                definition.title(),
                normalizeStack(definition.icon()),
                copyStacks(definition.items()),
                copyStacks(definition.searchItems()),
                definition.hidden(),
                order,
                definition.type(),
                definition.layout()
        );
    }

    private static CreativeTabDefinition withItems(CreativeTabDefinition definition, List<ItemStack> items) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), definition.title(), definition.icon(), copyStacks(items),
                definition.searchItems(),
                definition.hidden(), definition.order(), definition.type(), definition.layout()
        );
    }

    private static CreativeTabDefinition withItemsAndSearchItems(
            CreativeTabDefinition definition,
            List<ItemStack> items,
            List<ItemStack> searchItems
    ) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), definition.title(), definition.icon(),
                copyStacks(items), copyStacks(searchItems), definition.hidden(), definition.order(),
                definition.type(), definition.layout()
        );
    }

    private static CreativeTabDefinition withHidden(CreativeTabDefinition definition, boolean hidden) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), definition.title(), definition.icon(), definition.items(),
                definition.searchItems(),
                hidden, definition.order(), definition.type(), definition.layout()
        );
    }

    private static CreativeTabDefinition withIcon(CreativeTabDefinition definition, ItemStack icon) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), definition.title(), normalizeStack(icon), definition.items(),
                definition.searchItems(),
                definition.hidden(), definition.order(), definition.type(), definition.layout()
        );
    }

    private static CreativeTabDefinition withTitle(CreativeTabDefinition definition, Component title) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), title, definition.icon(), definition.items(),
                definition.searchItems(),
                definition.hidden(), definition.order(), definition.type(), definition.layout()
        );
    }
}
