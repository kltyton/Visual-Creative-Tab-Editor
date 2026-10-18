package com.kltyton.visual_creative_tab_editor.client.editor.layout;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.network.chat.Component;

/** Keeps editor actions outside category docks and combines the right column when space is limited. */
public record CreativeTabEditorToolbar(int actionX, int actionY, int actionWidth,
        int secondaryX, int secondaryY, int secondaryWidth, int step) {
    public static final int HEIGHT = 18;
    private static final int STEP = 21;

    public static CreativeTabEditorToolbar layout(CreativeTabEditorHost host, int reservedLeft, boolean editing) {
        int top = host.visualCreativeTabEditor$top() + 4;
        int actionWidth = captionWidth(host, 44, "editor.save", "editor.cancel", "editor.undo", "editor.redo",
                "editor.restore", "editor.default_order", "editor.global_save");
        int left = CreativeTabDockLayout.outsideWidgets(host.visualCreativeTabEditor$screen(),
                reservedLeft - 4, top, actionWidth, 7 * STEP - 3);
        int right = host.visualCreativeTabEditor$left() + host.visualCreativeTabEditor$imageWidth() + 4;
        int preferredRightWidth = captionWidth(host, 72, "group.controls", "category.open", "group.disable_all",
                "group.enable_all", "category.edit_selected", "category.delete_selected");
        int rightWidth = Math.min(preferredRightWidth, host.visualCreativeTabEditor$screenWidth() - right - 2);
        if (editing && left < 2) {
            int step = Math.min(STEP, (host.visualCreativeTabEditor$screenHeight() - HEIGHT - 4) / 11);
            int y = Math.max(2, Math.min(top, host.visualCreativeTabEditor$screenHeight() - 11 * step - HEIGHT - 2));
            return new CreativeTabEditorToolbar(right, y, rightWidth, right, y + 7 * step, rightWidth, step);
        }
        return new CreativeTabEditorToolbar(left, top, actionWidth, right, top, rightWidth, STEP);
    }

    private static int captionWidth(CreativeTabEditorHost host, int minimum, String... keys) {
        int width = minimum;
        for (String key : keys) {
            width = Math.max(width, host.visualCreativeTabEditor$font().width(
                    Component.translatable("visual_creative_tab_editor." + key)) + 8);
        }
        return width;
    }

    public Bounds action(int row) { return new Bounds(this.actionX, this.actionY + row * this.step, this.actionWidth, HEIGHT); }
    public Bounds secondary(int row) { return new Bounds(this.secondaryX, this.secondaryY + row * this.step, this.secondaryWidth, HEIGHT); }

    public record Bounds(int x, int y, int width, int height) {}
}
