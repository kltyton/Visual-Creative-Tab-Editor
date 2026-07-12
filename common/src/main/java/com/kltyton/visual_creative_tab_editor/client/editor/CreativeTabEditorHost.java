package com.kltyton.visual_creative_tab_editor.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

/** Narrow bridge from the screen Mixin to the loader-neutral editor controller. */
public interface CreativeTabEditorHost {
    CreativeModeInventoryScreen visualCreativeTabEditor$screen();

    boolean visualCreativeTabEditor$suppressesTooltips();

    void visualCreativeTabEditor$renderEditorOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

    Minecraft visualCreativeTabEditor$minecraft();

    Font visualCreativeTabEditor$font();

    CreativeModeInventoryScreen.ItemPickerMenu visualCreativeTabEditor$menu();

    int visualCreativeTabEditor$left();

    int visualCreativeTabEditor$top();

    int visualCreativeTabEditor$imageWidth();

    int visualCreativeTabEditor$imageHeight();

    int visualCreativeTabEditor$screenWidth();

    int visualCreativeTabEditor$screenHeight();

    CreativeModeTab visualCreativeTabEditor$selectedTab();

    void visualCreativeTabEditor$selectTab(CreativeModeTab tab);

    int visualCreativeTabEditor$tabX(CreativeModeTab tab);

    int visualCreativeTabEditor$tabY(CreativeModeTab tab);

    boolean visualCreativeTabEditor$isCreativeSlot(Slot slot);

    float visualCreativeTabEditor$scrollOffset();

    void visualCreativeTabEditor$setScrollOffset(float scrollOffset);

    Slot visualCreativeTabEditor$destroyItemSlot();

    boolean visualCreativeTabEditor$isPageButtonAt(double mouseX, double mouseY);

    boolean visualCreativeTabEditor$isInteractiveWidgetAt(double mouseX, double mouseY);

    void visualCreativeTabEditor$showTitleEditor(String initialValue);

    String visualCreativeTabEditor$titleEditorValue();

    boolean visualCreativeTabEditor$isTitleEditorAt(double mouseX, double mouseY);

    void visualCreativeTabEditor$renderTitleEditor(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

    boolean visualCreativeTabEditor$titleEditorKeyPressed(int keyCode, int scanCode, int modifiers);

    boolean visualCreativeTabEditor$titleEditorCharTyped(char codePoint, int modifiers);

    boolean visualCreativeTabEditor$titleEditorMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY
    );

    boolean visualCreativeTabEditor$titleEditorMouseReleased(double mouseX, double mouseY, int button);

    void visualCreativeTabEditor$hideTitleEditor();

    void visualCreativeTabEditor$replaySlotClick(Slot slot, int slotId, int button, ClickType input);

    void visualCreativeTabEditor$refreshScreen();
}
