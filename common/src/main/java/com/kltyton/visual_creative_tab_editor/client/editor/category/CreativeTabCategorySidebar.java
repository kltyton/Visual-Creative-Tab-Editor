package com.kltyton.visual_creative_tab_editor.client.editor.category;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.client.editor.layout.CreativeTabDockLayout;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorGraphics;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorTheme;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorTheme.Tone;
import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategories;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Icon sub-tabs attached to the current creative tab, independent of its native item list. */
public final class CreativeTabCategorySidebar {
    private static final int WIDTH = 32;
    private static final int TAB_HEIGHT = 28;
    private final CreativeTabEditorHost host;
    private final Supplier<CreativeCategoryFilter> filter;
    private final BooleanSupplier editable;
    private final Runnable refresh;
    private final Runnable create;
    private final Consumer<String> focus;
    private final ItemStack allIcon = new ItemStack(Items.COMPASS);
    private final ItemStack uncategorizedIcon = new ItemStack(Items.PAPER);
    private CreativeTabCategories snapshot;
    private List<Entry> entries = List.of();
    private Object sourceTab;
    private int offset;
    private boolean collapsed;

    public CreativeTabCategorySidebar(CreativeTabEditorHost host, Supplier<CreativeCategoryFilter> filter,
            BooleanSupplier editable, Runnable refresh, Runnable create, Consumer<String> focus) {
        this.host = host;
        this.filter = filter;
        this.editable = editable;
        this.refresh = refresh;
        this.create = create;
        this.focus = focus;
    }

    public boolean isVisible() { return this.editable.getAsBoolean() || !this.filter.get().categories().isEmpty(); }
    public int reservedLeftEdge() { return isVisible() ? left() : this.host.visualCreativeTabEditor$left(); }

    public void reveal(String id) {
        sync();
        this.collapsed = false;
        for (int index = 0; index < this.entries.size(); index++) {
            if (id.equals(this.entries.get(index).id)) {
                this.offset = Math.min(index, Math.max(0, this.entries.size() - visibleRows()));
                return;
            }
        }
    }

    public boolean click(double mouseX, double mouseY, int button, boolean multiple) {
        if (!isVisible()) { return false; }
        sync();
        if (collapseButton().contains(mouseX, mouseY)) {
            if (button == 0) { this.collapsed = !this.collapsed; }
            return true;
        }
        if (this.collapsed || !rail().contains(mouseX, mouseY)) { return false; }
        if (button != 0) { return true; }
        if (previousButton().contains(mouseX, mouseY)) { page(-1); return true; }
        if (this.editable.getAsBoolean() && createButton().contains(mouseX, mouseY)) {
            if (canCreate()) { this.create.run(); }
            return true;
        }
        if (nextButton().contains(mouseX, mouseY)) { page(1); return true; }
        for (int row = 0; row < visibleRows() && row + this.offset < this.entries.size(); row++) {
            if (!tab(row).contains(mouseX, mouseY)) { continue; }
            Entry entry = this.entries.get(row + this.offset);
            if (entry.all) { this.filter.get().showAll(); this.focus.accept(null); }
            else {
                this.filter.get().select(entry.id, multiple);
                this.focus.accept(entry.id.equals(CreativeCategoryFilter.UNCATEGORIZED) ? null : entry.id);
            }
            this.refresh.run();
            return true;
        }
        return true;
    }

    public boolean scroll(double mouseX, double mouseY, double amount) {
        if (!isVisible() || this.collapsed || !rail().contains(mouseX, mouseY)) { return false; }
        sync();
        page(amount > 0 ? -1 : amount < 0 ? 1 : 0);
        return true;
    }

    private void sync() {
        Object tab = this.host.visualCreativeTabEditor$selectedTab();
        if (tab != this.sourceTab) { this.sourceTab = tab; this.offset = 0; }
        CreativeTabCategories categories = this.filter.get().categories();
        if (categories != this.snapshot) {
            this.snapshot = categories;
            List<Entry> next = new ArrayList<>();
            next.add(new Entry(null, this.allIcon, Component.translatable("visual_creative_tab_editor.category.all"), true));
            categories.entries().forEach(category -> next.add(new Entry(category.id(), category.icon(), category.title(), false)));
            next.add(new Entry(CreativeCategoryFilter.UNCATEGORIZED, this.uncategorizedIcon,
                    Component.translatable("visual_creative_tab_editor.category.uncategorized"), false));
            this.entries = List.copyOf(next);
        }
        this.offset = Mth.clamp(this.offset, 0, Math.max(0, this.entries.size() - visibleRows()));
    }

    private void page(int direction) {
        this.offset = Mth.clamp(this.offset + direction * visibleRows(), 0,
                Math.max(0, this.entries.size() - visibleRows()));
    }

    private boolean canCreate() { return this.filter.get().categories().entries().size() < CreativeTabValidation.MAX_TABS; }

    public void render(EditorGraphics graphics, int mouseX, int mouseY) {
        if (!isVisible()) { return; }
        sync();
        Component hoveredTooltip = null;
        drawButton(graphics, collapseButton(), this.collapsed ? Glyph.RIGHT : Glyph.LEFT, true, mouseX, mouseY);
        if (collapseButton().contains(mouseX, mouseY)) {
            hoveredTooltip = Component.translatable("visual_creative_tab_editor.category."
                    + (this.collapsed ? "expand_sidebar" : "collapse_sidebar"));
        }
        if (this.collapsed) {
            if (hoveredTooltip != null) { tooltip(graphics, hoveredTooltip, mouseX, mouseY); }
            return;
        }
        drawButton(graphics, previousButton(), Glyph.UP, this.offset > 0, mouseX, mouseY);
        if (previousButton().contains(mouseX, mouseY)) {
            hoveredTooltip = Component.translatable("visual_creative_tab_editor.category.previous_page");
        }
        if (this.editable.getAsBoolean()) {
            drawButton(graphics, createButton(), Glyph.ADD, canCreate(), mouseX, mouseY);
            if (createButton().contains(mouseX, mouseY)) {
                hoveredTooltip = Component.translatable("visual_creative_tab_editor.category.new");
            }
        }
        drawButton(graphics, nextButton(), Glyph.DOWN, this.offset + visibleRows() < this.entries.size(), mouseX, mouseY);
        if (nextButton().contains(mouseX, mouseY)) {
            hoveredTooltip = Component.translatable("visual_creative_tab_editor.category.next_page");
        }
        for (int row = 0; row < visibleRows() && row + this.offset < this.entries.size(); row++) {
            Entry entry = this.entries.get(row + this.offset);
            Box bounds = tab(row);
            boolean selected = entry.all ? this.filter.get().allSelected()
                    : this.filter.get().selected(entry.id);
            int lastRow = Math.min(visibleRows(), this.entries.size() - this.offset) - 1;
            graphics.categoryTab(row == 0 ? 0 : row == lastRow ? 2 : 1, selected, bounds.x, bounds.y);
            graphics.item(entry.icon, bounds.x + 10, bounds.y + 5);
            if (bounds.contains(mouseX, mouseY)) { hoveredTooltip = entry.title; }
        }
        if (hoveredTooltip != null) { tooltip(graphics, hoveredTooltip, mouseX, mouseY); }
    }

    /** Joins the outer tabs to the inventory behind the native filter widgets. */
    public void renderFrame(EditorGraphics graphics) {
        if (!isVisible() || this.collapsed) { return; }
        int x = frameLeft();
        int right = this.host.visualCreativeTabEditor$left();
        if (x >= right) { return; }
        int y = top();
        int bottom = y + this.host.visualCreativeTabEditor$imageHeight();
        graphics.fill(x, y + 3, right + 4, bottom - 3, 0xFF000000);
        graphics.fill(x + 3, y, right + 4, bottom, 0xFF000000);
        graphics.fill(x + 1, y + 3, right + 4, bottom - 3, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 1, right + 4, bottom - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, right + 4, bottom - 3, 0xFFC6C6C6);
        graphics.fill(x + 3, bottom - 3, right + 4, bottom - 1, 0xFF555555);
        graphics.fill(x + 1, bottom - 5, x + 3, bottom - 3, 0xFF555555);
    }

    private void tooltip(EditorGraphics graphics, Component text, int x, int y) {
        graphics.tooltip(this.host.visualCreativeTabEditor$font(), text, x, y);
    }

    private void drawButton(EditorGraphics graphics, Box bounds, Glyph glyph, boolean enabled, int mouseX, int mouseY) {
        int color = EditorTheme.button(graphics, bounds.x, bounds.y, bounds.width, bounds.height,
                Tone.NORMAL, enabled, bounds.contains(mouseX, mouseY));
        int x = bounds.x + bounds.width / 2;
        int y = bounds.y + (bounds.height - 3) / 2;
        switch (glyph) {
            case ADD -> {
                graphics.fill(x - 2, y, x + 3, y + 1, color);
                graphics.fill(x, y - 2, x + 1, y + 3, color);
            }
            case UP, DOWN -> {
                for (int step = 0; step < 3; step++) {
                    int row = glyph == Glyph.UP ? y - 1 + step : y + 1 - step;
                    graphics.fill(x - step, row, x + step + 1, row + 1, color);
                }
            }
            case LEFT, RIGHT -> {
                for (int step = 0; step < 3; step++) {
                    int column = glyph == Glyph.LEFT ? x - 1 + step : x + 1 - step;
                    graphics.fill(column, y - step, column + 1, y + step + 1, color);
                }
            }
        }
    }

    private int left() {
        return frameLeft() - 28;
    }
    private int frameLeft() {
        return CreativeTabDockLayout.categoryFrameLeft(this.host.visualCreativeTabEditor$screen(),
                this.host.visualCreativeTabEditor$left(), top(), this.host.visualCreativeTabEditor$imageHeight());
    }
    private int top() { return this.host.visualCreativeTabEditor$top(); }
    private int controlsTop() { return Math.max(2, top() - 36); }
    private int tabsTop() { return Math.max(top(), controlsTop() + 34); }
    private int visibleRows() {
        return Math.max(1, (top() + this.host.visualCreativeTabEditor$imageHeight() - 14 - tabsTop()) / TAB_HEIGHT);
    }
    private Box rail() {
        return new Box(left(), controlsTop() + 18, WIDTH,
                top() + this.host.visualCreativeTabEditor$imageHeight() - controlsTop() - 18);
    }
    private Box collapseButton() { return new Box(left() + 7, controlsTop(), 14, 14); }
    private Box previousButton() { return new Box(left(), controlsTop() + 18, this.editable.getAsBoolean() ? 13 : 28, 14); }
    private Box createButton() { return new Box(left() + 15, controlsTop() + 18, 13, 14); }
    private Box nextButton() { return new Box(left(), top() + this.host.visualCreativeTabEditor$imageHeight() - 14, 28, 14); }
    private Box tab(int row) { return new Box(left(), tabsTop() + row * TAB_HEIGHT, WIDTH, TAB_HEIGHT); }

    private record Entry(String id, ItemStack icon, Component title, boolean all) {}
    private enum Glyph { UP, DOWN, LEFT, RIGHT, ADD }
    private record Box(int x, int y, int width, int height) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
}
