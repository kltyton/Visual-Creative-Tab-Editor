package com.kltyton.visual_creative_tab_editor.client.editor.render;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.CreativeModeTab;


/** Native texture and foreground handling for Minecraft 1.21.1. */
public final class NativeEditorGraphics extends LegacyEditorGraphics {
    private static final java.util.Map<String, ResourceId> GROUP_TEXTURES = java.util.stream.Stream
            .of("icon_slot", "item_slot", "plus", "minus").collect(java.util.stream.Collectors.toUnmodifiableMap(
                    name -> name, name -> ResourceId.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID,
                            "container/item_groups/" + name)));

    public NativeEditorGraphics(GuiGraphics graphics) { super(graphics); }
    public void groupSprite(String name, int x, int y, int size) {
        var sprite = NativeIds.unwrap(GROUP_TEXTURES.get(name));
        graphics.blitSprite(sprite, x, y, size, size);
    }
    public void tabSprite(CreativeModeTab.Row row, int column, int x, int y, int width, int height) {
        var sprite = NativeIds.unwrap(ResourceId.withDefaultNamespace("container/creative_inventory/tab_"
                + (row == CreativeModeTab.Row.TOP ? "top" : "bottom") + "_unselected_" + (column + 1)));
        graphics.blitSprite(sprite, x, y, width, height);
    }
    public void categoryTab(int edge, boolean selected, int x, int y) {
        String position = edge == 0 ? "top" : edge == 2 ? "bottom" : "middle";
        var sprite = NativeIds.unwrap(ResourceId.withDefaultNamespace("advancements/tab_left_" + position
                + (selected ? "_selected" : "")));
        graphics.blitSprite(sprite, x, y, 32, 28);
    }
    public void overlay(Runnable draw) {
        graphics.flush();
        layer(500.0F, () -> { draw.run(); graphics.flush(); });
    }
}
