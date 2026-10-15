package com.kltyton.visual_creative_tab_editor.client.editor;

import com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGroup;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping.ItemKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;

/** Projects creative items into expandable groups without changing the tab's item list. */
public final class CreativeItemGroups {
    private final Map<CreativeModeTab, Set<Object>> expandedTabs = new IdentityHashMap<>();
    private final List<Entry> entries = new ArrayList<>();
    private final Map<Object, List<Integer>> members = new HashMap<>();
    private Set<Object> expanded = new HashSet<>();
    private CreativeModeTab tab;
    private CreativeItemGrouping grouping = CreativeItemGrouping.EMPTY;
    private final Map<ItemKey, String> assigned = new HashMap<>();
    private final Set<ItemKey> ungrouped = new HashSet<>();
    private final Map<String, CreativeItemGroup> definitions = new HashMap<>();
    private boolean enabled;
    private boolean dirty = true;
    private int sourceSize;
    private int[] displayIndices = new int[0];
    private int[] sourceSections = new int[0];
    private boolean projected;
    private final Map<Integer, Integer> reservedRuns = new HashMap<>();
    private final List<Integer> reservedRowSources = new ArrayList<>();
    private CreativeTabDefinition nativeLayout;
    private final Map<Integer, Integer> nativeRowOrdinals = new HashMap<>();

    public void selectTab(CreativeModeTab tab) {
        this.tab = tab;
        this.enabled = CreativeTabClientConfig.groupSimilarItems()
                && (tab.getType() == CreativeModeTab.Type.CATEGORY || tab.getType() == CreativeModeTab.Type.SEARCH);
        this.expanded = this.expandedTabs.computeIfAbsent(tab, ignored -> new HashSet<>());
        invalidate();
    }

    public CreativeModeTab tab() { return this.tab; }

    public void setGrouping(CreativeItemGrouping grouping) {
        if (this.grouping != grouping) {
            this.grouping = grouping;
            invalidate();
        }
    }

    public static CreativeItemGroups forItems(CreativeItemGrouping grouping, List<ItemStack> items) {
        CreativeItemGroups result = new CreativeItemGroups();
        result.enabled = true;
        result.setGrouping(grouping);
        result.refresh(items);
        return result;
    }

    public void invalidate() {
        this.dirty = true;
    }

    public void refresh(List<ItemStack> items) {
        if (!this.dirty) {
            return;
        }
        this.dirty = false;
        this.sourceSize = items.size();
        this.entries.clear();
        this.members.clear();
        this.assigned.clear();
        this.ungrouped.clear();
        this.definitions.clear();
        this.displayIndices = new int[items.size()];
        prepareReservedRows(items);
        this.projected = this.enabled || !this.reservedRowSources.isEmpty();
        if (!this.projected) {
            return;
        }
        for (CreativeItemGroup group : this.grouping.groups()) {
            this.definitions.put(group.id(), group);
            for (ItemStack member : group.items()) {
                this.assigned.put(new ItemKey(member), group.id());
            }
        }
        this.ungrouped.addAll(CreativeItemGrouping.keys(this.grouping.ungrouped()));
        Object[] keys = new Object[items.size()];
        for (int index = 0; index < items.size(); index++) {
            Object key = family(items.get(index), this.sourceSections[index]);
            keys[index] = key;
            this.members.computeIfAbsent(key, ignored -> new ArrayList<>()).add(index);
        }
        Set<Object> emitted = new HashSet<>();
        for (int index = 0; index < items.size(); index++) {
            Integer reservedEnd = this.reservedRuns.get(index);
            if (reservedEnd != null) {
                int headerStart = reservedEnd - ((reservedEnd - index) / 9) * 9;
                for (int padding = index; padding < headerStart; padding++) {
                    this.displayIndices[padding] = this.entries.size();
                }
                while (this.entries.size() % 9 != 0) {
                    this.entries.add(new Entry(index, keys[index], false));
                }
                for (int cell = headerStart; cell < reservedEnd; cell++) {
                    this.displayIndices[cell] = this.entries.size();
                    this.entries.add(new Entry(cell, keys[cell], false));
                }
                index = reservedEnd - 1;
                continue;
            }
            Object key = keys[index];
            List<Integer> group = this.members.get(key);
            if (!isGroup(key)) {
                this.displayIndices[index] = this.entries.size();
                this.entries.add(new Entry(index, key, false));
            } else if (emitted.add(key)) {
                int header = this.entries.size();
                this.entries.add(new Entry(index, key, true));
                for (int member : group) {
                    this.displayIndices[member] = this.expanded.contains(key) ? this.entries.size() : header;
                    if (this.expanded.contains(key)) {
                        this.entries.add(new Entry(member, key, false));
                    }
                }
            }
        }
    }

    public boolean enabled() {
        return this.enabled;
    }

    public boolean hasProjection() { return this.projected; }

    public int size() {
        return this.projected ? this.entries.size() : this.sourceSize;
    }

    public int sourceIndex(int displayIndex) {
        if (displayIndex < 0) {
            return -1;
        }
        if (!this.projected) {
            return Math.min(displayIndex, this.sourceSize);
        }
        return displayIndex < this.entries.size() ? this.entries.get(displayIndex).sourceIndex : this.sourceSize;
    }

    public int displayIndex(int sourceIndex) {
        return this.projected ? this.displayIndices[sourceIndex] : sourceIndex;
    }

    public boolean isHeader(int displayIndex) {
        return this.enabled && displayIndex >= 0 && displayIndex < this.entries.size()
                && this.entries.get(displayIndex).header;
    }

    public boolean isExpanded(int displayIndex) {
        return this.expanded.contains(this.entries.get(displayIndex).key);
    }

    public int memberCount(int displayIndex) {
        return this.members.get(this.entries.get(displayIndex).key).size();
    }

    public int memberCount(String id) {
        List<Integer> indices = this.members.get(id);
        return indices == null ? 0 : indices.size();
    }

    public String groupId(int displayIndex) {
        if (!this.enabled || displayIndex < 0 || displayIndex >= this.entries.size()) { return null; }
        Object key = this.entries.get(displayIndex).key;
        return isGroup(key) ? (String) key : null;
    }

    public String groupIdForSource(int sourceIndex) { return groupId(displayIndex(sourceIndex)); }

    public boolean containsGroup(String id) {
        return this.enabled && this.members.containsKey(id) && isGroup(id);
    }

    public List<Integer> memberIndices(String id) {
        return List.copyOf(this.members.getOrDefault(id, List.of()));
    }

    public List<String> groupIds() {
        return this.entries.stream().filter(Entry::header).map(entry -> (String) entry.key).toList();
    }

    public CreativeItemGroup group(String id, List<ItemStack> items) {
        List<Integer> indices = this.members.get(id);
        if (indices == null || indices.isEmpty()) { throw new IllegalArgumentException("Unknown item group: " + id); }
        CreativeItemGroup definition = this.definitions.get(id);
        ItemStack first = items.get(indices.get(0));
        List<ItemStack> resolved = new ArrayList<>(definition == null ? List.of() : definition.items());
        Set<ItemKey> present = CreativeItemGrouping.keys(resolved);
        for (int index : indices) {
            ItemStack member = items.get(index);
            if (present.add(new ItemKey(member))) { resolved.add(member); }
        }
        return new CreativeItemGroup(id, groupTitle(id, first), definition == null ? first : definition.icon(), resolved);
    }

    public ItemStack displayStack(int index, List<ItemStack> items) {
        Entry entry = this.entries.get(index);
        CreativeItemGroup definition = entry.header ? this.definitions.get(entry.key) : null;
        return definition == null ? items.get(entry.sourceIndex) : definition.icon();
    }

    public Component title(int displayIndex, List<ItemStack> items) {
        return groupTitle(groupId(displayIndex), items.get(this.entries.get(displayIndex).sourceIndex));
    }

    private Component groupTitle(String id, ItemStack first) {
        CreativeItemGroup definition = this.definitions.get(id);
        if (definition != null) { return definition.title(); }
        String family = id.startsWith("section/") ? id.substring(id.indexOf('/', 8) + 1) : id;
        return family.startsWith("block/")
                ? Component.translatable("visual_creative_tab_editor.group." + family.substring(6)) : first.getHoverName();
    }

    private boolean isGroup(Object key) {
        return key instanceof String && (this.members.get(key).size() > 1 || this.definitions.containsKey(key));
    }

    public void toggle(int displayIndex, List<ItemStack> items) {
        Object key = this.entries.get(displayIndex).key;
        if (!this.expanded.remove(key)) {
            this.expanded.add(key);
        }
        invalidate();
        refresh(items);
    }

    public int reveal(int sourceIndex, List<ItemStack> items) {
        Object key = family(items.get(sourceIndex), this.sourceSections[sourceIndex]);
        if (this.enabled && isGroup(key) && this.expanded.add(key)) {
            invalidate();
            refresh(items);
        }
        return displayIndex(sourceIndex);
    }

    private Object family(ItemStack stack, int section) {
        if (!this.enabled || stack.isEmpty()) { return new Object(); }
        ItemKey key = new ItemKey(stack);
        if (this.ungrouped.contains(key)) { return new UngroupedKey(key); }
        String assignedGroup = this.assigned.get(key);
        if (assignedGroup != null) { return assignedGroup; }
        String automatic = automaticGroupId(stack);
        return section == 0 ? automatic : "section/" + section + "/" + automatic;
    }

    /** Maps native banner rows through the same projection used by their item cells. */
    public Integer projectReservedRow(int nativeRow, CreativeTabDefinition baseline) {
        if (this.nativeLayout != baseline) {
            this.nativeRowOrdinals.clear();
            if (baseline != null) {
                List<Integer> rows = reservedRows(baseline.items());
                for (int index = 0; index < rows.size(); index++) {
                    this.nativeRowOrdinals.put(rows.get(index) / 9, index);
                }
            }
            this.nativeLayout = baseline;
        }
        Integer ordinal = baseline == null ? this.reservedRowSources.indexOf(nativeRow * 9) : this.nativeRowOrdinals.get(nativeRow);
        if (ordinal == null || ordinal < 0 || ordinal >= this.reservedRowSources.size()) {
            return null;
        }
        return displayIndex(this.reservedRowSources.get(ordinal)) / 9;
    }

    private void prepareReservedRows(List<ItemStack> items) {
        this.reservedRuns.clear();
        this.reservedRowSources.clear();
        this.sourceSections = new int[items.size()];
        int section = 0;
        for (int index = 0; index < items.size();) {
            if (!items.get(index).isEmpty()) {
                this.sourceSections[index++] = section;
                continue;
            }
            int end = emptyRunEnd(items, index);
            if (end - index >= 9) {
                this.reservedRuns.put(index, end);
                for (int row = end - ((end - index) / 9) * 9; row < end; row += 9) {
                    this.reservedRowSources.add(row);
                }
            }
            section++;
            index = end;
        }
    }

    private static List<Integer> reservedRows(List<ItemStack> items) {
        List<Integer> rows = new ArrayList<>();
        for (int index = 0; index < items.size();) {
            if (!items.get(index).isEmpty()) { index++; continue; }
            int end = emptyRunEnd(items, index);
            for (int row = end - ((end - index) / 9) * 9; row < end; row += 9) {
                rows.add(row);
            }
            index = end;
        }
        return rows;
    }

    private static int emptyRunEnd(List<ItemStack> items, int start) {
        int end = start;
        while (end < items.size() && items.get(end).isEmpty()) { end++; }
        return end;
    }

    public static String automaticGroupId(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof StairBlock) {
                return "block/stairs";
            }
            for (Family family : Family.values()) {
                if (block.defaultBlockState().is(family.tag)) {
                    return "block/" + family.name().toLowerCase(Locale.ROOT);
                }
            }
        }
        return "item/" + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private record UngroupedKey(ItemKey item) { }

    private record Entry(int sourceIndex, Object key, boolean header) {
    }

    private enum Family {
        STAIRS(BlockTags.STAIRS),
        SLABS(BlockTags.SLABS),
        WALLS(BlockTags.WALLS),
        FENCES(BlockTags.FENCES),
        FENCE_GATES(BlockTags.FENCE_GATES),
        DOORS(BlockTags.DOORS),
        TRAPDOORS(BlockTags.TRAPDOORS),
        BUTTONS(BlockTags.BUTTONS),
        PRESSURE_PLATES(BlockTags.PRESSURE_PLATES),
        BEDS(BlockTags.BEDS),
        WOOL(BlockTags.WOOL),
        CARPETS(BlockTags.WOOL_CARPETS),
        PLANKS(BlockTags.PLANKS),
        LOGS(BlockTags.LOGS),
        LEAVES(BlockTags.LEAVES),
        CANDLES(BlockTags.CANDLES);

        private final TagKey<Block> tag;

        Family(TagKey<Block> tag) {
            this.tag = tag;
        }
    }
}
