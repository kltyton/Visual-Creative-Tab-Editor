package com.kltyton.visual_creative_tab_editor.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
// @target if minecraft_version=26.1.2,26.2
//|import net.minecraft.client.input.CharacterEvent;
//|import net.minecraft.client.input.KeyEvent;
//|import net.minecraft.client.input.MouseButtonEvent;
//|import net.minecraft.client.input.PreeditEvent;
// @target else
// @target end
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
// @target if minecraft_version=26.1.2,26.2
//|import org.jetbrains.annotations.Nullable;
// @target else
// @target end

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

// @target if minecraft_version=26.1.2,26.2
//|    void visualCreativeTabEditor$extractTitleEditor(
// @target else
    void visualCreativeTabEditor$renderTitleEditor(
// @target end
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

// @target if minecraft_version=26.1.2,26.2
//|    boolean visualCreativeTabEditor$titleEditorKeyPressed(KeyEvent event);
// @target else
    boolean visualCreativeTabEditor$titleEditorKeyPressed(int keyCode, int scanCode, int modifiers);
// @target end

// @target if minecraft_version=26.1.2,26.2
//|    boolean visualCreativeTabEditor$titleEditorCharTyped(CharacterEvent event);
//|
//|    boolean visualCreativeTabEditor$titleEditorPreeditUpdated(@Nullable PreeditEvent event);
// @target else
    boolean visualCreativeTabEditor$titleEditorCharTyped(char codePoint, int modifiers);
// @target end

    boolean visualCreativeTabEditor$titleEditorMouseDragged(
// @target if minecraft_version=26.1.2,26.2
//|            MouseButtonEvent event,
// @target else
            double mouseX,
            double mouseY,
            int button,
// @target end
            double deltaX,
            double deltaY
    );

// @target if minecraft_version=26.1.2,26.2
//|    boolean visualCreativeTabEditor$titleEditorMouseReleased(MouseButtonEvent event);
// @target else
    boolean visualCreativeTabEditor$titleEditorMouseReleased(double mouseX, double mouseY, int button);
// @target end

    void visualCreativeTabEditor$hideTitleEditor();

    void visualCreativeTabEditor$replaySlotClick(Slot slot, int slotId, int button, ClickType input);

    void visualCreativeTabEditor$refreshScreen();
}
