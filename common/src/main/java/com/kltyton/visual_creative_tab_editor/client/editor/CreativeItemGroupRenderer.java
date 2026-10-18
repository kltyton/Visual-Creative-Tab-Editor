package com.kltyton.visual_creative_tab_editor.client.editor;

import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorGraphics;
import net.minecraft.world.inventory.Slot;

/** Renders group headers and members using the selected native GUI API. */
public final class CreativeItemGroupRenderer {
    private static final int MARKER_OFFSET = 9;
    private CreativeItemGroupRenderer() {}
    public static void background(EditorGraphics graphics, Slot slot, boolean header) {
        graphics.groupSprite(header ? "icon_slot" : "item_slot", slot.x - 1, slot.y - 1, 18);
    }
    public static void badge(EditorGraphics graphics, Slot slot, boolean expanded, boolean editing) {
        // The 16px sprite contains a 7px bottom-right marker; editing reserves the top-right for selection.
        int y = editing ? slot.y : slot.y - MARKER_OFFSET;
        graphics.layer(350.0F, () -> graphics.groupSprite(expanded ? "minus" : "plus", slot.x, y, 16));
    }
}
