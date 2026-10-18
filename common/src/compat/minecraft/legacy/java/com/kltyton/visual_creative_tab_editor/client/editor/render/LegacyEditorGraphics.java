package com.kltyton.visual_creative_tab_editor.client.editor.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Shared immediate GUI operations for the pre-extractor render API. */
public abstract class LegacyEditorGraphics implements EditorGraphics {
    protected final GuiGraphics graphics;
    protected LegacyEditorGraphics(GuiGraphics graphics) { this.graphics = graphics; }
    public void fill(int left, int top, int right, int bottom, int color) { graphics.fill(left, top, right, bottom, color); }
    public void item(ItemStack stack, int x, int y) { graphics.renderItem(stack, x, y); }
    public void outline(int x, int y, int width, int height, int color) { graphics.renderOutline(x, y, width, height, color); }
    public void text(Font font, Component text, int x, int y, int color, boolean shadow) { graphics.drawString(font, text, x, y, color, shadow); }
    public void text(Font font, String text, int x, int y, int color, boolean shadow) { graphics.drawString(font, text, x, y, color, shadow); }
    public void layer(float depth, Runnable draw) {
        graphics.pose().pushPose();
        try { graphics.pose().translate(0.0F, 0.0F, depth); draw.run(); }
        finally { graphics.pose().popPose(); }
    }
    public void title(EditBox editor, int mouseX, int mouseY, float partialTick) { editor.render(graphics, mouseX, mouseY, partialTick); }
    public void tooltip(Font font, Component text, int mouseX, int mouseY) {
        graphics.renderTooltip(font, font.split(text, graphics.guiWidth() / 2), mouseX, mouseY);
    }
}
