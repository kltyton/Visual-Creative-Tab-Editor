package com.kltyton.visual_creative_tab_editor.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.jspecify.annotations.Nullable;

/** Narrow bridge from the screen Mixin to the loader-neutral editor controller. */
public interface CreativeTabEditorHost {
    CreativeModeInventoryScreen visualCreativeTabEditor$screen();

    boolean visualCreativeTabEditor$suppressesTooltips();

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

    void visualCreativeTabEditor$extractTitleEditor(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

    boolean visualCreativeTabEditor$titleEditorKeyPressed(KeyEvent event);

    boolean visualCreativeTabEditor$titleEditorCharTyped(CharacterEvent event);

    boolean visualCreativeTabEditor$titleEditorPreeditUpdated(@Nullable PreeditEvent event);

    boolean visualCreativeTabEditor$titleEditorMouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY
    );

    boolean visualCreativeTabEditor$titleEditorMouseReleased(MouseButtonEvent event);

    void visualCreativeTabEditor$hideTitleEditor();

    void visualCreativeTabEditor$replaySlotClick(Slot slot, int slotId, int button, ContainerInput input);

    void visualCreativeTabEditor$refreshScreen();
}
