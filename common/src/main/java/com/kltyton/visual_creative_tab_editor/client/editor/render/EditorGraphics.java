package com.kltyton.visual_creative_tab_editor.client.editor.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Drawing operations used by the editor, with native layers retained by the renderer. */
public interface EditorGraphics {
    void fill(int left, int top, int right, int bottom, int color);
    void item(ItemStack stack, int x, int y);
    void outline(int x, int y, int width, int height, int color);
    void text(Font font, Component text, int x, int y, int color, boolean shadow);
    void text(Font font, String text, int x, int y, int color, boolean shadow);
    void groupSprite(String name, int x, int y, int size);
    void tabSprite(CreativeModeTab.Row row, int column, int x, int y, int width, int height);
    void categoryTab(int edge, boolean selected, int x, int y);
    void layer(float depth, Runnable draw);
    void overlay(Runnable draw);
    void title(EditBox editor, int mouseX, int mouseY, float partialTick);
    void tooltip(Font font, Component text, int mouseX, int mouseY);
}
