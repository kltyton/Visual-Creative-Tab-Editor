package com.kltyton.visual_creative_tab_editor.client.editor.layout;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/** Places editor docks outside visible native widgets, including other mods' filter tabs. */
public final class CreativeTabDockLayout {
    private CreativeTabDockLayout() {}

    /** Includes connected native filter tabs without moving their input regions. */
    public static int categoryFrameLeft(Screen screen, int inventoryLeft, int y, int height) {
        int left = inventoryLeft;
        int previous;
        do {
            previous = left;
            for (var child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible
                        && widget.getY() < y + height && widget.getY() + widget.getHeight() > y
                        && widget.getX() < left && widget.getX() + widget.getWidth() >= left - 4) {
                    left = widget.getX() - 6;
                }
            }
        } while (left != previous);
        return left;
    }

    public static int outsideWidgets(Screen screen, int right, int y, int width, int height) {
        int left = right - width;
        int previous;
        do {
            previous = left;
            for (var child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible
                        && widget.getY() < y + height && widget.getY() + widget.getHeight() > y
                        && widget.getX() < left + width + 2 && widget.getX() + widget.getWidth() > left) {
                    left = Math.min(left, widget.getX() - width - 2);
                }
            }
        } while (left != previous);
        return left;
    }
}
