package com.kltyton.visual_creative_tab_editor.client.editor.category;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.client.editor.layout.CreativeTabEditorToolbar;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorGraphics;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorTheme;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorTheme.Tone;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import com.kltyton.visual_creative_tab_editor.data.category.CreativeTabCategory;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Collapsible category navigation and editing controls inside a creative tab. */
public final class CreativeTabCategoryBrowser {
    public enum Action { NEW, EDIT, DELETE, ADD, REMOVE, UP, DOWN, BACK }
    private final CreativeTabEditorHost host;
    private final Supplier<CreativeCategoryFilter> filter;
    private final BooleanSupplier editable;
    private final BooleanSupplier hasSelection;
    private final Runnable refresh;
    private final BiConsumer<Action, String> editor;
    private final Supplier<CreativeTabEditorToolbar.Bounds> launcherBounds;
    private boolean open;
    private boolean editing;
    private int scroll;
    private String focused;
    private Object sourceTab;

    public CreativeTabCategoryBrowser(CreativeTabEditorHost host, Supplier<CreativeCategoryFilter> filter,
            BooleanSupplier editable, BooleanSupplier hasSelection, Runnable refresh, BiConsumer<Action, String> editor,
            Supplier<CreativeTabEditorToolbar.Bounds> launcherBounds) {
        this.host = host;
        this.filter = filter;
        this.editable = editable;
        this.hasSelection = hasSelection;
        this.refresh = refresh;
        this.editor = editor;
        this.launcherBounds = launcherBounds;
    }

    public boolean isOpen() { return this.open; }
    public void close() { this.open = false; }
    public void focus(String id) { this.sourceTab = this.host.visualCreativeTabEditor$selectedTab(); this.focused = id; }
    public String focusedCategory() { sync(); return this.focused; }
    private boolean available() { return this.editable.getAsBoolean() || !this.filter.get().categories().isEmpty(); }

    public boolean click(double x, double y, int button, boolean shift) {
        if (!this.open) {
            if (button == 0 && available() && launcher().contains(x, y)) {
                this.open = true;
                this.editing = this.editable.getAsBoolean();
                sync();
                return true;
            }
            return false;
        }
        if (button != 0) { return true; }
        Box panel = panel();
        if (!panel.contains(x, y) || closeButton(panel).contains(x, y)) { close(); return true; }
        if (allButton(panel).contains(x, y)) {
            this.focused = null;
            this.filter.get().showAll();
            this.refresh.run();
            return true;
        }
        if (uncategorizedButton(panel).contains(x, y)) {
            this.focused = null;
            this.filter.get().select(CreativeCategoryFilter.UNCATEGORIZED, shift);
            this.refresh.run();
            return true;
        }
        if (upButton(panel).contains(x, y)) { scroll(1); return true; }
        if (downButton(panel).contains(x, y)) { scroll(-1); return true; }
        List<CreativeTabCategory> entries = this.filter.get().categories().entries();
        for (int row = 0; row < visibleRows(panel) && row + this.scroll < entries.size(); row++) {
            Box bounds = row(panel, row);
            if (!bounds.contains(x, y)) { continue; }
            String id = entries.get(row + this.scroll).id();
            this.focused = id;
            if (!this.editing || x < bounds.x + 20) {
                this.filter.get().select(id, shift);
                this.refresh.run();
            }
            return true;
        }
        if (this.editing) {
            for (Action action : Action.values()) {
                if (actionButton(panel, action).contains(x, y)) {
                    if (action == Action.BACK) { close(); }
                    else if (enabled(action)) { this.editor.accept(action, this.focused); }
                    return true;
                }
            }
        }
        return true;
    }

    public boolean scroll(double amount) {
        if (!this.open) { return false; }
        this.scroll = Mth.clamp(this.scroll + (amount > 0 ? -1 : amount < 0 ? 1 : 0), 0,
                Math.max(0, this.filter.get().categories().entries().size() - visibleRows(panel())));
        return true;
    }

    private void sync() {
        Object tab = this.host.visualCreativeTabEditor$selectedTab();
        if (this.sourceTab != tab) { this.sourceTab = tab; this.focused = null; this.scroll = 0; }
        if (this.focused != null && this.filter.get().categories().entries().stream()
                .noneMatch(category -> category.id().equals(this.focused))) { this.focused = null; }
        this.scroll = Math.min(this.scroll,
                Math.max(0, this.filter.get().categories().entries().size() - visibleRows(panel())));
    }

    public void render(EditorGraphics graphics, int mouseX, int mouseY) {
        if (!available()) { close(); return; }
        drawButton(graphics, launcher(), "open", this.open, true, mouseX, mouseY);
        if (!this.open) { return; }
        sync();
        Box panel = panel();
        graphics.fill(0, 0, this.host.visualCreativeTabEditor$screenWidth(), this.host.visualCreativeTabEditor$screenHeight(), EditorTheme.SCRIM);
        EditorTheme.panel(graphics, panel.x, panel.y, panel.width, panel.height, 22);
        text(graphics, Component.translatable("visual_creative_tab_editor.category.open"), panel.x + 7, panel.y + 7, panel.width - 64, EditorTheme.TEXT_DARK);
        drawButton(graphics, upButton(panel), "previous", false, this.scroll > 0, mouseX, mouseY);
        drawButton(graphics, downButton(panel), "next", false,
                this.scroll + visibleRows(panel) < this.filter.get().categories().entries().size(), mouseX, mouseY);
        drawButton(graphics, allButton(panel), "all", this.filter.get().allSelected(), true, mouseX, mouseY);
        drawButton(graphics, uncategorizedButton(panel), "uncategorized",
                this.filter.get().selected(CreativeCategoryFilter.UNCATEGORIZED), true, mouseX, mouseY);
        List<CreativeTabCategory> entries = this.filter.get().categories().entries();
        for (int row = 0; row < visibleRows(panel) && row + this.scroll < entries.size(); row++) {
            CreativeTabCategory category = entries.get(row + this.scroll);
            Box bounds = row(panel, row);
            boolean selected = this.filter.get().selected(category.id());
            EditorTheme.row(graphics, bounds.x, bounds.y, bounds.width, bounds.height,
                    category.id().equals(this.focused), bounds.contains(mouseX, mouseY));
            EditorTheme.checkbox(graphics, bounds.x + 5, bounds.y + 4, 12, selected, bounds.contains(mouseX, mouseY));
            graphics.item(category.icon(), bounds.x + 23, bounds.y + 3);
            text(graphics, category.title(), bounds.x + 43, bounds.y + 6, bounds.width - 93, EditorTheme.TEXT);
            EditorTheme.inset(graphics, bounds.right() - 48, bounds.y + 4, 44, 13, false);
            text(graphics, Component.literal(Integer.toString(category.itemCount())), bounds.right() - 45, bounds.y + 6, 38, EditorTheme.TEXT_MUTED);
            if (bounds.contains(mouseX, mouseY)) { graphics.tooltip(this.host.visualCreativeTabEditor$font(), category.title(), mouseX, mouseY); }
        }
        if (entries.isEmpty()) {
            text(graphics, Component.translatable("visual_creative_tab_editor.category.empty"), panel.x + 8, panel.y + 54, panel.width - 16, 0xFFCCCCCC);
        }
        if (this.editing) {
            for (Action action : Action.values()) {
                drawButton(graphics, actionButton(panel, action), action.name().toLowerCase(java.util.Locale.ROOT),
                        false, action == Action.BACK || enabled(action), mouseX, mouseY);
            }
        }
        text(graphics, Component.translatable("visual_creative_tab_editor.category.hint"), panel.x + 7,
                panel.bottom() - 17, panel.width - 39, 0xFFCCCCCC);
        drawButton(graphics, closeButton(panel), "collapse", false, true, mouseX, mouseY);
    }

    private boolean enabled(Action action) {
        boolean focused = this.focused != null;
        return switch (action) {
            case NEW -> this.filter.get().categories().entries().size() < CreativeTabValidation.MAX_TABS;
            case EDIT, DELETE -> focused;
            case ADD, REMOVE -> focused && this.hasSelection.getAsBoolean();
            case UP -> focused && !this.filter.get().categories().entries().get(0).id().equals(this.focused);
            case DOWN -> focused && !this.filter.get().categories().entries()
                    .get(this.filter.get().categories().entries().size() - 1).id().equals(this.focused);
            case BACK -> true;
        };
    }

    private void text(EditorGraphics graphics, Component label, int x, int y, int width, int color) {
        var font = this.host.visualCreativeTabEditor$font();
        graphics.text(font, font.plainSubstrByWidth(label.getString(), Math.max(0, width)), x, y, color, false);
    }

    private void drawButton(EditorGraphics graphics, Box box, String key, boolean selected, boolean enabled, int mouseX, int mouseY) {
        Tone tone = key.equals("delete") ? Tone.DANGER : selected || key.equals("new") ? Tone.PRIMARY : Tone.NORMAL;
        int textColor = EditorTheme.button(graphics, box.x, box.y, box.width, box.height,
                tone, enabled, box.contains(mouseX, mouseY));
        Component label = Component.translatable("visual_creative_tab_editor.category." + key);
        if (key.equals("collapse")) {
            int centerX = box.x + box.width / 2;
            int centerY = box.y + box.height / 2 - 1;
            for (int offset = -3; offset <= 3; offset++) {
                graphics.fill(centerX + offset, centerY + offset, centerX + offset + 1, centerY + offset + 1, textColor);
                graphics.fill(centerX + offset, centerY - offset, centerX + offset + 1, centerY - offset + 1, textColor);
            }
        } else {
            text(graphics, label, box.x + 3, box.y + 5, box.width - 6, textColor);
        }
        if ((key.equals("collapse") || this.host.visualCreativeTabEditor$font().width(label) > box.width - 6)
                && box.contains(mouseX, mouseY)) {
            graphics.tooltip(this.host.visualCreativeTabEditor$font(), label, mouseX, mouseY);
        }
    }

    private Box launcher() {
        CreativeTabEditorToolbar.Bounds bounds = this.launcherBounds.get();
        return new Box(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }
    private Box panel() {
        int width = Math.min(296, this.host.visualCreativeTabEditor$screenWidth() - 12);
        int height = Math.min(244, this.host.visualCreativeTabEditor$screenHeight() - 12);
        return new Box((this.host.visualCreativeTabEditor$screenWidth() - width) / 2,
                (this.host.visualCreativeTabEditor$screenHeight() - height) / 2, width, height);
    }
    private int visibleRows(Box panel) { return Math.max(1, (panel.height - (this.editing ? 112 : 68)) / 22); }
    private Box row(Box panel, int row) { return new Box(panel.x + 6, panel.y + 46 + row * 22, panel.width - 12, 21); }
    private Box upButton(Box panel) { return new Box(panel.right() - 52, panel.y + 3, 22, 18); }
    private Box downButton(Box panel) { return new Box(panel.right() - 27, panel.y + 3, 22, 18); }
    private Box allButton(Box panel) { return new Box(panel.x + 6, panel.y + 25, (panel.width - 15) / 2, 18); }
    private Box uncategorizedButton(Box panel) { return new Box(panel.x + 9 + (panel.width - 15) / 2, panel.y + 25, (panel.width - 15) / 2, 18); }
    private Box closeButton(Box panel) { return new Box(panel.right() - 31, panel.bottom() - 22, 25, 18); }
    private Box actionButton(Box panel, Action action) {
        int width = (panel.width - 21) / 4;
        return new Box(panel.x + 6 + (action.ordinal() % 4) * (width + 3),
                panel.bottom() - 64 + (action.ordinal() / 4) * 20, width, 18);
    }
    private record Box(int x, int y, int width, int height) {
        int right() { return this.x + this.width; }
        int bottom() { return this.y + this.height; }
        boolean contains(double x, double y) { return x >= this.x && x < right() && y >= this.y && y < bottom(); }
    }
}
