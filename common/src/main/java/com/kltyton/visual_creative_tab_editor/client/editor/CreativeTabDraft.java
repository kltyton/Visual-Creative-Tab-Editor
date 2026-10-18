package com.kltyton.visual_creative_tab_editor.client.editor;

import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalog;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabLayout;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabPatch;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabType;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGroup;
import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategories;
import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategory;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping.ItemKey;
import com.kltyton.visual_creative_tab_editor.platform.item.StackData;
import net.minecraft.network.chat.Component;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;
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
    private final LinkedHashSet<ResourceId> selectedTabIds = new LinkedHashSet<>();
    private final NavigableSet<Integer> selectedItemIndices = new TreeSet<>();
    private final Map<String, String> lastMovedGroupIds = new HashMap<>();
    private final List<ItemStack> currentSearchItems = new ArrayList<>();
    private final Map<Item, Map<StackData.Key, Integer>> currentSearchItemIndices = new HashMap<>();
    private ResourceId currentTabId;
    private CreativeTabType currentTabType = CreativeTabType.CATEGORY;
    private int currentSearchPreferenceCapacity;
    private boolean currentSearchItemsPrepared;
    private long preparedSearchRevision;
    private long itemSelectionRevision;
    private CreativeTabDefinition groupedDefinition;
    private long groupedSearchRevision = -1L;
    private CreativeItemGroups groupedItems;
    private CreativeItemGroups selectedGroupProjection;
    private long groupedSelectionRevision = -1L;
    private final Map<String, Integer> selectedGroupCounts = new HashMap<>();

    /** Creates a deep editing copy of {@code catalog}. */
    public CreativeTabDraft(CreativeTabCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        int index = 0;
        for (CreativeTabDefinition definition : catalog.orderedDefinitions()) {
            this.tabs.add(definition.order() == index ? definition : copyDefinition(definition, index));
            index++;
        }
        this.currentTabId = this.tabs.stream()
                .filter(definition -> definition.type() == CreativeTabType.CATEGORY && !definition.hidden())
                .map(CreativeTabDefinition::id)
                .findFirst()
                .orElseGet(() -> this.tabs.isEmpty() ? null : this.tabs.get(0).id());
        if (this.currentTabId != null) {
            this.currentTabType = this.tabs.get(requireTabIndex(this.currentTabId)).type();
        }
    }

    public static CreativeTabDraft fromCatalog(CreativeTabCatalog catalog) {
        return new CreativeTabDraft(catalog);
    }

    /** Returns an immutable list of immutable definitions in their current draft order. */
    public List<CreativeTabDefinition> tabs() {
        return List.copyOf(this.tabs);
    }

    /** Returns the tab count without materializing a list snapshot. */
    public int tabCount() {
        return this.tabs.size();
    }

    public Optional<CreativeTabDefinition> definition(ResourceId id) {
        int index = indexOf(requireId(id));
        return index < 0 ? Optional.empty() : Optional.of(this.tabs.get(index));
    }

    public Optional<ResourceId> currentTabId() {
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
    public void setCurrentTab(ResourceId id) {
        switchCurrentTab(requireExistingId(id));
    }

    public Set<ResourceId> selectedTabIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.selectedTabIds));
    }

    public boolean isTabSelected(ResourceId id) {
        return this.selectedTabIds.contains(requireId(id));
    }

    public void setTabSelected(ResourceId id, boolean selected) {
        ResourceId checked = requireExistingId(id);
        if (selected) {
            this.selectedTabIds.add(checked);
        } else {
            this.selectedTabIds.remove(checked);
        }
    }

    /** Toggles an id-based tab selection and returns its new state. */
    public boolean toggleTabSelection(ResourceId id) {
        ResourceId checked = requireExistingId(id);
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
    public void moveTabs(Collection<ResourceId> ids, int targetIndex) {
        Objects.requireNonNull(ids, "ids");
        checkInsertionIndex(targetIndex, this.tabs.size(), "tab target index");
        Set<ResourceId> movingIds = checkedIds(ids);
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
        normalizeTabOrders();
        if (this.currentTabType == CreativeTabType.SEARCH) {
            this.itemSelectionRevision++;
            this.selectedItemIndices.clear();
            invalidatePreparedSearchItems();
        }
    }

    public void setSelectedTabsHidden(boolean hidden) {
        setTabsHidden(this.selectedTabIds, hidden);
    }

    public void setTabsHidden(Collection<ResourceId> ids, boolean hidden) {
        Set<ResourceId> checked = checkedIds(ids);
        boolean changed = false;
        for (int index = 0; index < this.tabs.size(); index++) {
            CreativeTabDefinition definition = this.tabs.get(index);
            if (checked.contains(definition.id()) && definition.hidden() != hidden) {
                this.tabs.set(index, withHidden(definition, hidden));
                changed = true;
            }
        }
        if (changed && this.currentTabType == CreativeTabType.SEARCH) {
            this.itemSelectionRevision++;
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
        return definition.items();
    }

    /** Returns the current visible working-list size without copying its stacks. */
    public int currentItemCount() {
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() == CreativeTabType.SEARCH && this.currentSearchItemsPrepared) {
            return this.currentSearchItems.size();
        }
        return definition.itemCount();
    }

    public int currentSearchItemCount() {
        return requireCurrentTab().searchItemCount();
    }

    /** Returns one defensive stack copy without copying the complete current list. */
    public ItemStack currentItemAt(int index) {
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() == CreativeTabType.SEARCH && this.currentSearchItemsPrepared) {
            return index >= 0 && index < this.currentSearchItems.size()
                    ? normalizeStack(this.currentSearchItems.get(index))
                    : ItemStack.EMPTY;
        }
        return index >= 0 && index < definition.itemCount()
                ? definition.itemAt(index)
                : ItemStack.EMPTY;
    }

    /** Finds an equal current stack without materializing a defensive list copy. */
    public int indexOfMatchingCurrentItem(ItemStack target, int excludedIndex) {
        if (target == null || target.isEmpty()) {
            return -1;
        }
        CreativeTabDefinition definition = requireCurrentTab();
        if (definition.type() == CreativeTabType.SEARCH && this.currentSearchItemsPrepared) {
            for (int index = 0; index < this.currentSearchItems.size(); index++) {
                if (index != excludedIndex
                        && StackData.same(this.currentSearchItems.get(index), target)) {
                    return index;
                }
            }
            return -1;
        }
        for (int index = 0; index < definition.itemCount(); index++) {
            if (index != excludedIndex && definition.itemMatches(index, target)) {
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
        checkElementIndex(index, reorderableItemCount(definition), "item index");
        if (selected) {
            this.itemSelectionRevision++;
            this.selectedItemIndices.add(index);
        } else {
            this.itemSelectionRevision++;
            this.selectedItemIndices.remove(index);
        }
    }

    /** Toggles an index-based item selection and returns its new state. */
    public boolean toggleItemSelection(int index) {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        checkElementIndex(index, reorderableItemCount(definition), "item index");
        this.itemSelectionRevision++;
        if (this.selectedItemIndices.remove(index)) {
            return false;
        }
        this.itemSelectionRevision++;
        this.selectedItemIndices.add(index);
        return true;
    }

    public void clearItemSelection() {
        this.itemSelectionRevision++;
        this.selectedItemIndices.clear();
    }

    /**
     * Moves selected items as one stable block. {@code targetIndex} is a slot in
     * the pre-move item list: {@code 0} is the start and {@code size()} is the end.
     */
    public boolean moveSelectedItems(int targetIndex) {
        return moveItems(this.selectedItemIndices, targetIndex);
    }

    public CreativeTabCategory category(String id) {
        return requireEditableCategory().categories().require(id);
    }

    public String createCategory() {
        CreativeTabDefinition definition = requireEditableCategory();
        Set<ItemKey> seen = new LinkedHashSet<>();
        List<ItemStack> items = selectedItems().stream()
                .filter(item -> !item.isEmpty() && seen.add(new ItemKey(item))).toList();
        String id = "custom/" + UUID.randomUUID();
        putCategories(definition.categories().put(new CreativeTabCategory(id,
                Component.translatable("visual_creative_tab_editor.category.unnamed"),
                items.isEmpty() ? definition.icon() : items.get(0), items)));
        return id;
    }

    public void setCategoryTitle(String id, Component title) {
        CreativeTabCategory category = category(id);
        putCategories(requireEditableCategory().categories().put(
                new CreativeTabCategory(id, title, category.icon(), category.items())));
    }

    public void setCategoryIcon(String id, ItemStack icon) {
        CreativeTabCategory category = category(id);
        putCategories(requireEditableCategory().categories().put(
                new CreativeTabCategory(id, category.title(), icon, category.items())));
    }

    public void deleteCategory(String id) {
        category(id);
        putCategories(requireEditableCategory().categories().remove(id));
    }

    public void moveCategory(String id, int direction) {
        putCategories(requireEditableCategory().categories().move(id, direction));
    }

    public void changeSelectedCategoryMembers(String id, boolean add) {
        CreativeTabCategory category = category(id);
        Set<ItemKey> selected = CreativeItemGrouping.keys(selectedItems());
        List<ItemStack> items = new ArrayList<>(category.items());
        if (add) {
            Set<ItemKey> seen = CreativeItemGrouping.keys(items);
            for (ItemStack item : selectedItems()) {
                if (!item.isEmpty() && seen.add(new ItemKey(item))) { items.add(item); }
            }
        } else {
            items.removeIf(item -> selected.contains(new ItemKey(item)));
        }
        putCategories(requireEditableCategory().categories().put(
                new CreativeTabCategory(id, category.title(), category.icon(), items)));
    }

    private void putCategories(CreativeTabCategories categories) {
        CreativeTabDefinition definition = requireEditableCategory();
        int index = requireTabIndex(definition.id());
        CreativeTabDefinition updated = definition.withCategories(categories);
        List<CreativeTabDefinition> candidate = new ArrayList<>(this.tabs);
        candidate.set(index, updated);
        CreativeTabValidation.validateAll(candidate);
        this.tabs.set(index, updated);
    }

    public CreativeItemGroup itemGroup(String id) {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        List<ItemStack> items = currentItems();
        return currentItemGroups().group(id, items);
    }

    public String itemGroupId(int itemIndex) {
        return currentItemGroups().groupIdForSource(itemIndex);
    }

    public void selectItemGroup(String id, boolean selected) {
        CreativeItemGroups groups = currentItemGroups();
        for (int index : groups.memberIndices(id)) { setItemSelected(index, selected); }
    }

    public int selectedItemCount() { return this.selectedItemIndices.size(); }

    public boolean isItemGroupSelected(String id) {
        updateSelectedGroups();
        int members = this.groupedItems.memberCount(id);
        return members > 0 && this.selectedGroupCounts.getOrDefault(id, 0) == members;
    }

    public void toggleItemGroupSelection(String id) { selectItemGroup(id, !isItemGroupSelected(id)); }

    public boolean hasSelectedGroupedItems() {
        updateSelectedGroups();
        return !this.selectedGroupCounts.isEmpty();
    }

    public boolean selectedItemsBelongToGroup(String id) {
        updateSelectedGroups();
        return !this.selectedItemIndices.isEmpty()
                && this.selectedGroupCounts.getOrDefault(id, 0) == this.selectedItemIndices.size();
    }

    private CreativeItemGroups currentItemGroups() {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        if (this.groupedItems == null || this.groupedDefinition != definition
                || this.groupedSearchRevision != this.preparedSearchRevision) {
            this.groupedItems = CreativeItemGroups.forItems(definition.grouping(), currentItems());
            this.groupedDefinition = definition;
            this.groupedSearchRevision = this.preparedSearchRevision;
        }
        return this.groupedItems;
    }

    private void updateSelectedGroups() {
        CreativeItemGroups groups = currentItemGroups();
        if (this.selectedGroupProjection == groups && this.groupedSelectionRevision == this.itemSelectionRevision) {
            return;
        }
        this.selectedGroupCounts.clear();
        for (int index : this.selectedItemIndices) {
            String group = groups.groupIdForSource(index);
            if (group != null) {
                this.selectedGroupCounts.merge(group, 1, Integer::sum);
            }
        }
        this.selectedGroupProjection = groups;
        this.groupedSelectionRevision = this.itemSelectionRevision;
    }

    public String createItemGroup() {
        requireReorderableItemsTab();
        List<ItemStack> selected = selectedItems();
        if (selected.size() < 2) { throw new IllegalStateException("Select at least two items to create a group"); }
        String id = "custom/" + UUID.randomUUID();
        putItemGroup(new CreativeItemGroup(id, Component.translatable("visual_creative_tab_editor.group.unnamed"),
                selected.get(0), selected));
        return id;
    }

    public void setItemGroupTitle(String id, Component title) {
        CreativeItemGroup group = itemGroup(id);
        putItemGroup(new CreativeItemGroup(id, title, group.icon(), group.items()));
    }

    public void setItemGroupIcon(String id, ItemStack icon) {
        CreativeItemGroup group = itemGroup(id);
        putItemGroup(new CreativeItemGroup(id, group.title(), normalizeStack(icon), group.items()));
    }

    public void dissolveItemGroup(String id) {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        putGrouping(definition.grouping().dissolve(id, itemGroup(id).items()));
    }

    public void assignSelectedItemsToGroup(String id) {
        CreativeItemGroup group = itemGroup(id);
        List<ItemStack> members = new ArrayList<>(group.items());
        Set<ItemKey> present = CreativeItemGrouping.keys(members);
        for (ItemStack item : selectedItems()) {
            if (present.add(new ItemKey(item))) { members.add(item); }
        }
        putItemGroup(new CreativeItemGroup(id, group.title(), group.icon(), members));
    }

    public void detachSelectedItemsFromGroups() {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        putGrouping(materializeGrouping(definition, currentItems()).detach(selectedItems()));
    }

    public record GroupMove(String groupId, int movedItems) { }

    public GroupMove moveItemGroupToTab(String id, ResourceId targetId) {
        int moved = moveSelectedItemsToTab(targetId);
        return new GroupMove(this.lastMovedGroupIds.getOrDefault(id, id), moved);
    }

    private List<ItemStack> selectedItems() {
        List<ItemStack> items = currentItems();
        return checkedItemIndices(this.selectedItemIndices, items.size()).stream().map(items::get).toList();
    }

    private void putItemGroup(CreativeItemGroup group) {
        putGrouping(materializeGrouping(requireReorderableItemsTab(), currentItems()).assign(group));
    }

    private void putGrouping(CreativeItemGrouping grouping) {
        CreativeTabDefinition definition = requireReorderableItemsTab();
        this.tabs.set(requireTabIndex(definition.id()), definition.withGrouping(grouping));
    }

    /**
     * Moves the selected items into another category tab, appending them as one
     * stable block and making that tab the current item-editing target.
     *
     * @return the number of items moved, or {@code -1} when the target tab
     *         cannot accept the block without exceeding a per-list limit
     */
    public int moveSelectedItemsToTab(ResourceId targetTabId) {
        this.lastMovedGroupIds.clear();
        CreativeTabDefinition source = requireEditableCategory();
        ResourceId checkedTargetId = requireExistingId(targetTabId);
        if (source.id().equals(checkedTargetId)) {
            return 0;
        }
        CreativeTabDefinition target = this.tabs.get(requireTabIndex(checkedTargetId));
        if (target.type() != CreativeTabType.CATEGORY) {
            throw new IllegalStateException("Only category tabs can receive dragged items");
        }

        List<ItemStack> sourceSnapshot = source.items();
        List<ItemStack> sourceItems = new ArrayList<>(sourceSnapshot);
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

        CreativeItemGrouping sourceGrouping = materializeGrouping(source, sourceSnapshot);
        CreativeItemGroups sourceGroups = CreativeItemGroups.forItems(sourceGrouping, sourceSnapshot);
        List<CreativeItemGroup> transferredGroups = new ArrayList<>();
        List<ItemStack> transferredMembers = new ArrayList<>();
        for (String groupId : sourceGroups.groupIds()) {
            List<Integer> indices = sourceGroups.memberIndices(groupId);
            if (!indices.isEmpty() && movingIndices.containsAll(indices)) {
                CreativeItemGroup group = sourceGroups.group(groupId, sourceSnapshot);
                String movedId = "custom/" + UUID.randomUUID();
                List<ItemStack> members = indices.stream().map(sourceSnapshot::get).toList();
                transferredGroups.add(new CreativeItemGroup(movedId, group.title(), group.icon(), members));
                transferredMembers.addAll(members);
                this.lastMovedGroupIds.put(groupId, movedId);
            }
        }
        CreativeItemGrouping targetGrouping = materializeGrouping(target, target.items()).remove(transferredMembers);
        List<CreativeItemGroup> targetGroups = new ArrayList<>(targetGrouping.groups());
        targetGroups.addAll(transferredGroups);
        CreativeTabDefinition updatedSource = withItemsAndSearchItems(source, sourceItems, sourceSearchItems)
                .withGrouping(sourceGrouping.remove(moving))
                .withCategories(source.categories().replaceItems(moving, ItemStack.EMPTY));
        CreativeTabDefinition updatedTarget = withItemsAndSearchItems(target, targetItems, targetSearchItems)
                .withGrouping(new CreativeItemGrouping(targetGroups, targetGrouping.ungrouped()));
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
        putGrouping(materializeGrouping(definition, definition.items()).remove(removed));
        putCategories(definition.categories().replaceItems(removed, ItemStack.EMPTY));
        this.itemSelectionRevision++;
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
        putGrouping(materializeGrouping(definition, before).replaceItems(List.of(previous), replacement));
        putCategories(definition.categories().replaceItems(List.of(previous), replacement));
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
        putGrouping(materializeGrouping(definition, definition.items()).replaceItems(replaced, normalized));
        putCategories(definition.categories().replaceItems(replaced, normalized));
        selectContiguousItems(resultIndex, 1);
        return checked.size();
    }

    /** Adds an item at the end and returns its stable index. */
    public int addItem(ItemStack stack) {
        return addItem(requireEditableCategory().itemCount(), stack);
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
        Objects.requireNonNull(comparator, "comparator");
        for (int start = 0; start < items.size();) {
            if (items.get(start).isEmpty()) { start++; continue; }
            int end = start + 1;
            while (end < items.size() && !items.get(end).isEmpty()) { end++; }
            items.subList(start, end).sort(comparator);
            start = end;
        }
        replaceCurrentItems(definition, items);
        this.itemSelectionRevision++;
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
        this.itemSelectionRevision++;
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
        this.itemSelectionRevision++;
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
        Map<StackData.Key, Integer> byComponents = this.currentSearchItemIndices.get(stack.getItem());
        Integer index = byComponents == null ? null : byComponents.get(StackData.key(stack));
        int limit = Math.min(maxExclusive, this.currentSearchPreferenceCapacity);
        return index != null && index < limit ? index : -1;
    }

    public void setCurrentTabIcon(ItemStack icon) {
        setTabIcon(requireCurrentTab().id(), icon);
    }

    public void setTabIcon(ResourceId id, ItemStack icon) {
        int index = requireTabIndex(id);
        CreativeTabDefinition definition = this.tabs.get(index);
        this.tabs.set(index, withIcon(definition, normalizeStack(icon)));
    }

    public void setCurrentTabTitle(String title) {
        setTabTitle(requireCurrentTab().id(), title);
    }

    /** Stores user-entered titles as literal components. */
    public void setTabTitle(ResourceId id, String title) {
        int index = requireTabIndex(id);
        CreativeTabDefinition definition = this.tabs.get(index);
        this.tabs.set(index, withTitle(definition, Component.literal(Objects.requireNonNull(title, "title"))));
    }

    /**
     * Adds an empty category whose icon and default literal title come from the
     * selected inventory stack. The generated id is always unique in this draft.
     */
    public ResourceId addCustomCategory(ItemStack selectedStack) {
        ItemStack icon = normalizeStack(selectedStack);
        return addCustomCategory(icon, icon.getHoverName().getString());
    }

    public ResourceId addCustomCategory(ItemStack selectedStack, String title) {
        ItemStack icon = normalizeStack(selectedStack);
        ResourceId id;
        do {
            id = ResourceId.fromNamespaceAndPath(CUSTOM_NAMESPACE, "custom/" + UUID.randomUUID());
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

    /** Builds an immutable catalog while sharing unchanged immutable definitions. */
    public void restoreDefaultOrder(CreativeTabCatalog baseline) {
        CreativeTabDefinition current = requireCurrentTab();
        if (current.type() == CreativeTabType.CATEGORY) {
            Map<DefaultOrderKey, Integer> ranks = new HashMap<>();
            List<ItemStack> original = baseline.definition(current.id()).map(CreativeTabDefinition::items).orElse(List.of());
            for (int index = 0; index < original.size(); index++) {
                ranks.putIfAbsent(DefaultOrderKey.of(original.get(index)), index);
            }
            sortCurrentItems(Comparator.comparingInt(stack -> ranks.getOrDefault(DefaultOrderKey.of(stack), Integer.MAX_VALUE)));
        } else if (current.type() == CreativeTabType.SEARCH) {
            replaceCurrentItems(current, List.of());
        }
        this.tabs.sort(Comparator.comparingInt(tab -> baseline.definition(tab.id())
                .map(CreativeTabDefinition::order).orElse(Integer.MAX_VALUE)));
        normalizeTabOrders();
        this.itemSelectionRevision++;
        this.selectedItemIndices.clear();
        invalidatePreparedSearchItems();
    }

    private record DefaultOrderKey(Item item, StackData.Key data) {
        private static DefaultOrderKey of(ItemStack stack) {
            return new DefaultOrderKey(stack.getItem(), StackData.key(stack));
        }
    }

    public boolean matchesCatalog(CreativeTabCatalog catalog) {
        List<CreativeTabDefinition> definitions = catalog.orderedDefinitions();
        if (this.tabs.size() != definitions.size()) {
            return false;
        }
        for (int index = 0; index < this.tabs.size(); index++) {
            if (this.tabs.get(index) != definitions.get(index)) {
                return false;
            }
        }
        return true;
    }

    public CreativeTabCatalog toCatalog() {
        return new CreativeTabCatalog(this.tabs);
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

    private int reorderableItemCount(CreativeTabDefinition definition) {
        if (definition.type() != CreativeTabType.SEARCH) {
            return definition.itemCount();
        }
        if (!this.currentSearchItemsPrepared) {
            throw new IllegalStateException("The dynamic search order has not been prepared");
        }
        return this.currentSearchItems.size();
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
        Set<ItemStack> visible = StackData.set();
        visible.addAll(visibleItems);
        List<IndexedSearchPreference> invisible = new ArrayList<>();
        int preferenceCount = definition.itemCount();
        for (int index = 0; index < preferenceCount; index++) {
            ItemStack preferred = definition.itemAt(index);
            if (!visible.contains(preferred)) {
                invisible.add(new IndexedSearchPreference(index, normalizeStack(preferred)));
            }
        }
        return invisible;
    }

    private int maxSearchPreferenceSize(CreativeTabDefinition currentSearch) {
        long otherItems = 0L;
        for (CreativeTabDefinition definition : this.tabs) {
            otherItems += definition.searchItemCount();
            if (!definition.id().equals(currentSearch.id())) {
                otherItems += definition.itemCount();
            }
        }
        long globalCapacity = CreativeTabValidation.MAX_TOTAL_ITEMS - otherItems;
        return (int) Math.max(0L, Math.min(globalCapacity, CreativeTabValidation.MAX_ITEMS_PER_TAB));
    }

    private void replacePreparedSearchItems(Collection<ItemStack> stacks) {
        this.preparedSearchRevision++;
        this.currentSearchItems.clear();
        this.currentSearchItemIndices.clear();
        for (ItemStack stack : stacks) {
            ItemStack normalized = normalizeStack(stack);
            Map<StackData.Key, Integer> byComponents = this.currentSearchItemIndices.computeIfAbsent(
                    normalized.getItem(),
                    ignored -> new HashMap<>()
            );
            if (byComponents.putIfAbsent(StackData.key(normalized), this.currentSearchItems.size()) == null) {
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
        this.preparedSearchRevision++;
        this.currentSearchItems.clear();
        this.currentSearchItemIndices.clear();
        this.currentSearchPreferenceCapacity = 0;
        this.currentSearchItemsPrepared = false;
    }

    private void switchCurrentTab(ResourceId id) {
        if (id.equals(this.currentTabId)) {
            return;
        }
        this.currentTabId = id;
        this.currentTabType = this.tabs.get(requireTabIndex(id)).type();
        this.itemSelectionRevision++;
        this.selectedItemIndices.clear();
        invalidatePreparedSearchItems();
    }

    private static boolean sameStackOrder(List<ItemStack> left, List<ItemStack> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            if (!StackData.same(left.get(index), right.get(index))) {
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
                    || StackData.same(searchItem, normalized)) {
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
        this.itemSelectionRevision++;
        this.selectedItemIndices.clear();
        this.itemSelectionRevision++;
        this.selectedItemIndices.addAll(remapped);
    }

    private static boolean matchesAny(ItemStack stack, Collection<ItemStack> candidates) {
        for (ItemStack candidate : candidates) {
            if (StackData.same(stack, candidate)) {
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
            if (index != excludedIndex && StackData.same(stacks.get(index), target)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean removeFirstMatching(List<ItemStack> candidates, ItemStack stack) {
        for (int index = 0; index < candidates.size(); index++) {
            if (StackData.same(candidates.get(index), stack)) {
                candidates.remove(index);
                return true;
            }
        }
        return false;
    }

    private void selectContiguousItems(int firstIndex, int count) {
        this.itemSelectionRevision++;
        this.selectedItemIndices.clear();
        for (int offset = 0; offset < count; offset++) {
            this.itemSelectionRevision++;
            this.selectedItemIndices.add(firstIndex + offset);
        }
    }

    private Set<ResourceId> checkedIds(Collection<ResourceId> ids) {
        LinkedHashSet<ResourceId> checked = new LinkedHashSet<>();
        for (ResourceId id : ids) {
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

    private ResourceId requireExistingId(ResourceId id) {
        ResourceId checked = requireId(id);
        if (indexOf(checked) < 0) {
            throw new IllegalArgumentException("Unknown creative tab id: " + checked);
        }
        return checked;
    }

    private int requireTabIndex(ResourceId id) {
        ResourceId checked = requireId(id);
        int index = indexOf(checked);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown creative tab id: " + checked);
        }
        return index;
    }

    private int indexOf(ResourceId id) {
        for (int index = 0; index < this.tabs.size(); index++) {
            if (this.tabs.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    /** Keeps the draft's dense-order invariant after a tab move. */
    private void normalizeTabOrders() {
        for (int index = 0; index < this.tabs.size(); index++) {
            CreativeTabDefinition definition = this.tabs.get(index);
            if (definition.order() != index) {
                this.tabs.set(index, copyDefinition(definition, index));
            }
        }
    }

    private static ResourceId requireId(ResourceId id) {
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
        return checked.isEmpty() ? ItemStack.EMPTY : checked.copyWithCount(1);
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            copies.add(normalizeStack(stack));
        }
        return copies;
    }

    private record IndexedSearchPreference(int originalIndex, ItemStack stack) {
    }

    private static CreativeItemGrouping materializeGrouping(CreativeTabDefinition definition, List<ItemStack> items) {
        if (definition.grouping().groups().isEmpty()) { return definition.grouping(); }
        CreativeItemGroups view = CreativeItemGroups.forItems(definition.grouping(), items);
        List<CreativeItemGroup> groups = new ArrayList<>();
        for (CreativeItemGroup group : definition.grouping().groups()) {
            groups.add(view.memberIndices(group.id()).isEmpty() ? group : view.group(group.id(), items));
        }
        return new CreativeItemGrouping(groups, definition.grouping().ungrouped());
    }

    private static CreativeTabDefinition copyDefinition(CreativeTabDefinition definition, int order) {
        return new CreativeTabDefinition(
                definition.id(),
                definition.format(),
                definition.title(),
                definition.icon(),
                definition.items(),
                definition.searchItems(),
                definition.hidden(),
                order,
                definition.type(),
                definition.layout(), definition.grouping(), definition.categories()
        );
    }

    private static CreativeTabDefinition withItems(CreativeTabDefinition definition, List<ItemStack> items) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), definition.title(), definition.icon(), copyStacks(items),
                definition.searchItems(),
                definition.hidden(), definition.order(), definition.type(), definition.layout(), definition.grouping(), definition.categories()
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
                definition.type(), definition.layout(), definition.grouping(), definition.categories()
        );
    }

    private static CreativeTabDefinition withHidden(CreativeTabDefinition definition, boolean hidden) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), definition.title(), definition.icon(), definition.items(),
                definition.searchItems(),
                hidden, definition.order(), definition.type(), definition.layout(), definition.grouping(), definition.categories()
        );
    }

    private static CreativeTabDefinition withIcon(CreativeTabDefinition definition, ItemStack icon) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), definition.title(), normalizeStack(icon), definition.items(),
                definition.searchItems(),
                definition.hidden(), definition.order(), definition.type(), definition.layout(), definition.grouping(), definition.categories()
        );
    }

    private static CreativeTabDefinition withTitle(CreativeTabDefinition definition, Component title) {
        return new CreativeTabDefinition(
                definition.id(), definition.format(), title, definition.icon(), definition.items(),
                definition.searchItems(),
                definition.hidden(), definition.order(), definition.type(), definition.layout(), definition.grouping(), definition.categories()
        );
    }
}
