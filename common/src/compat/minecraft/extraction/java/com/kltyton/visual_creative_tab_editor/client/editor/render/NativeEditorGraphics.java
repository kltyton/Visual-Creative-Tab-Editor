package com.kltyton.visual_creative_tab_editor.client.editor.render;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Retains extraction strata and the native textured GUI pipeline. */
public final class NativeEditorGraphics implements EditorGraphics {
    private static final java.util.Map<String, ResourceId> GROUP_TEXTURES = java.util.stream.Stream
            .of("icon_slot", "item_slot", "plus", "minus").collect(java.util.stream.Collectors.toUnmodifiableMap(
                    name -> name, name -> ResourceId.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID,
                            "container/item_groups/" + name)));

    private final GuiGraphicsExtractor graphics;
    public NativeEditorGraphics(GuiGraphicsExtractor graphics) { this.graphics = graphics; }
    public void fill(int left, int top, int right, int bottom, int color) { graphics.fill(left, top, right, bottom, color); }
    public void item(ItemStack stack, int x, int y) { graphics.item(stack, x, y); }
    public void outline(int x, int y, int width, int height, int color) { graphics.outline(x, y, width, height, color); }
    public void text(Font font, Component text, int x, int y, int color, boolean shadow) { graphics.text(font, text, x, y, color, shadow); }
    public void text(Font font, String text, int x, int y, int color, boolean shadow) { graphics.text(font, text, x, y, color, shadow); }
    public void groupSprite(String name, int x, int y, int size) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NativeIds.unwrap(GROUP_TEXTURES.get(name)), x, y, size, size);
    }
    public void tabSprite(CreativeModeTab.Row row, int column, int x, int y, int width, int height) {
        var sprite = NativeIds.unwrap(ResourceId.withDefaultNamespace("container/creative_inventory/tab_"
                + (row == CreativeModeTab.Row.TOP ? "top" : "bottom") + "_unselected_" + (column + 1)));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }
    public void layer(float depth, Runnable draw) { graphics.nextStratum(); draw.run(); }
    public void categoryTab(int edge, boolean selected, int x, int y) {
        String position = edge == 0 ? "top" : edge == 2 ? "bottom" : "middle";
        var sprite = NativeIds.unwrap(ResourceId.withDefaultNamespace("advancements/tab_left_" + position
                + (selected ? "_selected" : "")));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 32, 28);
    }
    public void overlay(Runnable draw) { graphics.nextStratum(); draw.run(); }
    public void title(EditBox editor, int mouseX, int mouseY, float partialTick) { editor.extractRenderState(graphics, mouseX, mouseY, partialTick); }
    public void tooltip(Font font, Component text, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, font.split(text, graphics.guiWidth() / 2), mouseX, mouseY);
    }
}
