package com.kltyton.visual_creative_tab_editor.client.editor;

import com.google.gson.*;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Collapsible navigation over native contents; the editor's item indices remain untouched. */
public final class CreativeTabFolderBrowser {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final CreativeTabEditorHost host;
    private final ItemPicker picker;
    private final View tabs = new View();
    private final View items = new View();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, List<CreativeModeTab>> tabGroups = new TreeMap<>(CreativeTabFolderBrowser::compareSources);
    private final Map<String, List<ItemEntry>> itemGroups = new TreeMap<>(CreativeTabFolderBrowser::compareSources);
    private Kind kind = Kind.CLOSED;
    private CreativeModeTab sourceTab;
    private int scroll;
    private int columns;
    private boolean loaded;

    @FunctionalInterface
    public interface ItemPicker { void pick(int originalIndex, int button, boolean quickMove); }
    private enum Kind { CLOSED, TABS, ITEMS }
    private static final class View {
        final Set<String> expanded = new HashSet<>();
        final Set<String> hidden = new HashSet<>();
        int filter;
    }
    private record ItemEntry(int index, ItemStack stack) { }
    private record Row(String source, CreativeModeTab tab, List<ItemEntry> items) { }
    private record Bounds(int x, int y, int width, int height) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }

    public CreativeTabFolderBrowser(CreativeTabEditorHost host, ItemPicker picker) {
        this.host = host;
        this.picker = picker;
    }

    public boolean isOpen() { return this.kind != Kind.CLOSED; }
    public void close() { this.kind = Kind.CLOSED; }

    public boolean click(double x, double y, int button, boolean shift) {
        if (!isOpen()) {
            if (button == 0 && launcher(0).contains(x, y)) { open(Kind.TABS); return true; }
            if (button == 0 && canBrowseItems() && launcher(1).contains(x, y)) { open(Kind.ITEMS); return true; }
            return false;
        }
        Bounds panel = panel();
        if (!panel.contains(x, y)) { close(); return true; }
        if (button != 0 && button != 1) { return true; }
        if (button == 0 && closeButton(panel).contains(x, y)) { close(); return true; }
        for (int filter = 0; filter < 3; filter++) {
            if (button == 0 && filterButton(panel, filter).contains(x, y)) {
                view().filter = filter;
                if (filter == 0) { view().hidden.clear(); }
                rebuildRows();
                save();
                return true;
            }
        }
        int visibleIndex = (int) ((y - panel.y - 47) / 20);
        int index = this.scroll + visibleIndex;
        if (y < panel.y + 47 || visibleIndex < 0 || visibleIndex >= visibleRows(panel) || index >= this.rows.size()) {
            return true;
        }
        Row row = this.rows.get(index);
        if (row.tab == null && row.items == null) {
            if (button != 0) { return true; }
            if (x >= panel.x + panel.width - 35) {
                if (!view().hidden.remove(row.source)) { view().hidden.add(row.source); }
            } else if (!view().expanded.remove(row.source)) {
                view().expanded.add(row.source);
            }
            rebuildRows();
            save();
        } else if (row.tab != null) {
            if (button == 0 && row.tab.shouldDisplay()) {
                close();
                CreativeTabClientPlatform.revealTab(this.host.visualCreativeTabEditor$screen(), row.tab);
                this.host.visualCreativeTabEditor$selectTab(row.tab);
            }
        } else {
            int column = (int) ((x - panel.x - 18) / 20);
            if (x >= panel.x + 18 && column >= 0 && column < row.items.size()) {
                ItemEntry item = row.items.get(column);
                var menu = this.host.visualCreativeTabEditor$menu();
                if (this.host.visualCreativeTabEditor$selectedTab() != this.sourceTab || item.index >= menu.items.size()
                        || !ItemStack.isSameItemSameComponents(item.stack, menu.items.get(item.index))) {
                    open(Kind.ITEMS);
                    return true;
                }
                close();
                this.picker.pick(item.index, button, shift);
            }
        }
        return true;
    }

    public boolean scroll(double amount) {
        if (!isOpen()) { return false; }
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(amount) * 3, 0,
                Math.max(0, this.rows.size() - visibleRows(panel())));
        return true;
    }

    private boolean canBrowseItems() {
        CreativeModeTab.Type type = this.host.visualCreativeTabEditor$selectedTab().getType();
        return type == CreativeModeTab.Type.CATEGORY || type == CreativeModeTab.Type.SEARCH;
    }

    private void open(Kind kind) {
        load();
        this.kind = kind;
        this.sourceTab = this.host.visualCreativeTabEditor$selectedTab();
        this.columns = Math.max(1, (panel().width - 40) / 20);
        this.tabGroups.clear();
        this.itemGroups.clear();
        if (kind == Kind.TABS) {
            CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream()).filter(CreativeModeTab::shouldDisplay)
                    .forEach(tab -> this.tabGroups.computeIfAbsent(CreativeTabRuntime.id(tab).map(id -> id.getNamespace()).orElse("minecraft"),
                            key -> new ArrayList<>()).add(tab));
        } else {
            List<ItemStack> contents = this.host.visualCreativeTabEditor$menu().items;
            for (int index = 0; index < contents.size(); index++) {
                ItemStack stack = contents.get(index);
                if (!stack.isEmpty()) {
                    String source = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
                    this.itemGroups.computeIfAbsent(source, key -> new ArrayList<>()).add(new ItemEntry(index, stack));
                }
            }
        }
        this.scroll = 0;
        rebuildRows();
    }

    private void rebuildRows() {
        this.rows.clear();
        Collection<String> sources = this.kind == Kind.TABS ? this.tabGroups.keySet() : this.itemGroups.keySet();
        for (String source : sources) {
            if (view().filter == 1 && !source.equals("minecraft") || view().filter == 2 && source.equals("minecraft")) { continue; }
            this.rows.add(new Row(source, null, null));
            if (!view().expanded.contains(source) || view().hidden.contains(source)) { continue; }
            if (this.kind == Kind.TABS) {
                for (CreativeModeTab tab : this.tabGroups.get(source)) { this.rows.add(new Row(source, tab, null)); }
            } else {
                List<ItemEntry> entries = this.itemGroups.get(source);
                for (int index = 0; index < entries.size(); index += this.columns) {
                    this.rows.add(new Row(source, null, entries.subList(index, Math.min(entries.size(), index + this.columns))));
                }
            }
        }
        this.scroll = Math.min(this.scroll, Math.max(0, this.rows.size() - visibleRows(panel())));
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isOpen()) {
            button(graphics, launcher(0), Component.translatable("visual_creative_tab_editor.folders.tabs"), mouseX, mouseY, false);
            if (canBrowseItems()) {
                button(graphics, launcher(1), Component.translatable("visual_creative_tab_editor.folders.items"), mouseX, mouseY, false);
            }
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        Bounds panel = panel();
        int currentColumns = Math.max(1, (panel.width - 40) / 20);
        if (currentColumns != this.columns) {
            this.columns = currentColumns;
            rebuildRows();
        }
        graphics.fill(0, 0, this.host.visualCreativeTabEditor$screenWidth(), this.host.visualCreativeTabEditor$screenHeight(), 0xB0000000);
        graphics.fill(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height, 0xFF252525);
        graphics.renderOutline(panel.x, panel.y, panel.width, panel.height, 0xFFC6C6C6);
        text(graphics, Component.translatable(this.kind == Kind.TABS ? "visual_creative_tab_editor.folders.tabs" : "visual_creative_tab_editor.folders.items"), panel.x + 8, panel.y + 8, 0xFFFFFFFF, panel.width - 70);
        button(graphics, closeButton(panel), Component.literal("X"), mouseX, mouseY, false);
        String[] filters = {"all", "vanilla", "mods"};
        for (int index = 0; index < filters.length; index++) {
            button(graphics, filterButton(panel, index), Component.translatable("visual_creative_tab_editor.folders." + filters[index]), mouseX, mouseY, view().filter == index);
        }
        Component hint = Component.translatable("visual_creative_tab_editor.folders.hint");
        int count = visibleRows(panel);
        for (int index = 0; index < count && index + this.scroll < this.rows.size(); index++) {
            Row row = this.rows.get(index + this.scroll);
            int y = panel.y + 47 + index * 20;
            if (row.tab == null && row.items == null) {
                boolean hidden = view().hidden.contains(row.source);
                boolean expanded = view().expanded.contains(row.source);
                graphics.fill(panel.x + 6, y, panel.x + panel.width - 12, y + 18, hidden ? 0xFF333333 : 0xFF4B4B4B);
                graphics.fill(panel.x + 22, y + 5, panel.x + 36, y + 15, hidden ? 0xFF77705A : 0xFFD6AD43);
                graphics.fill(panel.x + 22, y + 3, panel.x + 29, y + 5, hidden ? 0xFF77705A : 0xFFE9C36B);
                text(graphics, Component.literal(expanded ? "-" : "+"), panel.x + 10, y + 5, 0xFFFFFFFF, 10);
                int entries = this.kind == Kind.TABS ? this.tabGroups.get(row.source).size() : this.itemGroups.get(row.source).size();
                String label = (row.source.equals("minecraft") ? Component.translatable("visual_creative_tab_editor.folders.vanilla").getString() : row.source) + " (" + entries + ")";
                text(graphics, Component.literal(label), panel.x + 42, y + 5, hidden ? 0xFFAAAAAA : 0xFFFFFFFF, panel.width - 82);
                text(graphics, Component.literal(hidden ? "[ ]" : "[x]"), panel.x + panel.width - 33, y + 5, 0xFFFFFFFF, 22);
            } else if (row.tab != null) {
                graphics.renderItem(row.tab.getIconItem(), panel.x + 24, y + 1);
                text(graphics, row.tab.getDisplayName(), panel.x + 44, y + 5, 0xFFFFFFFF, panel.width - 60);
                if (mouseY >= y && mouseY < y + 20 && mouseX >= panel.x + 18 && mouseX < panel.x + panel.width - 14) {
                    graphics.renderOutline(panel.x + 18, y, panel.width - 32, 19, 0xFFFFFFFF);
                    hint = row.tab.getDisplayName();
                }
            } else {
                for (int column = 0; column < row.items.size(); column++) {
                    int x = panel.x + 18 + column * 20;
                    graphics.fill(x, y, x + 18, y + 18, 0xFF666666);
                    graphics.renderItem(row.items.get(column).stack, x + 1, y + 1);
                    if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20) {
                        graphics.renderOutline(x, y, 18, 18, 0xFFFFFFFF);
                        hint = row.items.get(column).stack.getHoverName();
                    }
                }
            }
        }
        if (this.rows.isEmpty()) {
            text(graphics, Component.translatable("visual_creative_tab_editor.folders.empty"), panel.x + 12, panel.y + 55, 0xFFCCCCCC, panel.width - 24);
        }
        if (this.rows.size() > count) {
            int track = count * 20;
            int thumb = Math.max(12, track * count / this.rows.size());
            int top = panel.y + 47 + (track - thumb) * this.scroll / (this.rows.size() - count);
            graphics.fill(panel.x + panel.width - 8, top, panel.x + panel.width - 4, top + thumb, 0xFFC6C6C6);
        }
        text(graphics, hint, panel.x + 8, panel.y + panel.height - 15, 0xFFEEEEEE, panel.width - 16);
        graphics.pose().popPose();
    }

    private void button(GuiGraphics graphics, Bounds box, Component label, int mouseX, int mouseY, boolean selected) {
        int face = selected ? 0xFF3C8527 : box.contains(mouseX, mouseY) ? 0xFFE0E0E0 : 0xFFC6C6C6;
        graphics.fill(box.x, box.y, box.x + box.width, box.y + box.height, 0xFF222222);
        graphics.fill(box.x + 1, box.y + 1, box.x + box.width - 1, box.y + box.height - 3, face);
        graphics.fill(box.x + 1, box.y + 1, box.x + box.width - 1, box.y + 2, 0xFFEEEEEE);
        String clipped = this.host.visualCreativeTabEditor$font().plainSubstrByWidth(label.getString(), box.width - 6);
        graphics.drawString(this.host.visualCreativeTabEditor$font(), clipped,
                box.x + (box.width - this.host.visualCreativeTabEditor$font().width(clipped)) / 2, box.y + 5,
                selected ? 0xFFFFFFFF : 0xFF202020, false);
    }

    private void text(GuiGraphics graphics, Component label, int x, int y, int color, int width) {
        graphics.drawString(this.host.visualCreativeTabEditor$font(), this.host.visualCreativeTabEditor$font().plainSubstrByWidth(label.getString(), Math.max(0, width)), x, y, color, false);
    }

    private Bounds launcher(int row) {
        int left = this.host.visualCreativeTabEditor$left();
        int x = left >= 50 ? left - 48 : this.host.visualCreativeTabEditor$left() + this.host.visualCreativeTabEditor$imageWidth() + 4;
        x = Math.min(x, this.host.visualCreativeTabEditor$screenWidth() - 46);
        return new Bounds(Math.max(2, x), this.host.visualCreativeTabEditor$top() + 4 + row * 22, 44, 19);
    }
    private Bounds panel() {
        int width = Math.min(320, this.host.visualCreativeTabEditor$screenWidth() - 12);
        int height = Math.min(260, this.host.visualCreativeTabEditor$screenHeight() - 12);
        return new Bounds((this.host.visualCreativeTabEditor$screenWidth() - width) / 2,
                (this.host.visualCreativeTabEditor$screenHeight() - height) / 2, width, height);
    }
    private Bounds closeButton(Bounds panel) { return new Bounds(panel.x + panel.width - 27, panel.y + 4, 22, 18); }
    private Bounds filterButton(Bounds panel, int index) {
        int width = (panel.width - 20) / 3;
        return new Bounds(panel.x + 6 + index * (width + 2), panel.y + 24, width, 18);
    }
    private int visibleRows(Bounds panel) { return Math.max(1, (panel.height - 67) / 20); }
    private View view() { return this.kind == Kind.TABS ? this.tabs : this.items; }
    private static int compareSources(String a, String b) {
        if (a.equals(b)) { return 0; }
        if (a.equals("minecraft")) { return -1; }
        if (b.equals("minecraft")) { return 1; }
        return a.compareTo(b);
    }

    private void load() {
        if (this.loaded) { return; }
        this.loaded = true;
        Path file = CreativeTabClientState.contextFile("folders");
        if (!Files.isRegularFile(file)) { return; }
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(65_537);
            if (bytes.length > 65_536) { throw new IOException("Folder preferences too large"); }
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            readView(root.getAsJsonObject("tabs"), this.tabs);
            readView(root.getAsJsonObject("items"), this.items);
        } catch (IOException | RuntimeException exception) {
            VisualCreativeTabEditorConstants.LOGGER.warn("Could not load creative tab folder preferences", exception);
        }
    }
    private static void readView(JsonObject json, View view) {
        view.filter = Mth.clamp(json.get("filter").getAsInt(), 0, 2);
        for (var value : json.getAsJsonArray("expanded")) { view.expanded.add(value.getAsString()); }
        for (var value : json.getAsJsonArray("hidden")) { view.hidden.add(value.getAsString()); }
    }
    private static JsonObject writeView(View view) {
        JsonObject json = new JsonObject();
        json.addProperty("filter", view.filter);
        json.add("expanded", GSON.toJsonTree(new TreeSet<>(view.expanded)));
        json.add("hidden", GSON.toJsonTree(new TreeSet<>(view.hidden)));
        return json;
    }
    private void save() {
        Path file = CreativeTabClientState.contextFile("folders");
        try {
            JsonObject root = new JsonObject();
            root.add("tabs", writeView(this.tabs));
            root.add("items", writeView(this.items));
            Files.createDirectories(file.getParent());
            Path pending = Files.createTempFile(file.getParent(), "folders-", ".pending");
            try {
                Files.writeString(pending, GSON.toJson(root), StandardCharsets.UTF_8);
                try { Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException exception) { Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(pending); }
        } catch (IOException exception) {
            VisualCreativeTabEditorConstants.LOGGER.warn("Could not save creative tab folder preferences", exception);
        }
    }
}
