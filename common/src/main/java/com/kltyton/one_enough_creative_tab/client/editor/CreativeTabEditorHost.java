package com.kltyton.one_enough_creative_tab.client.editor;

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
    CreativeModeInventoryScreen oneEnoughCreativeTab$screen();

    Minecraft oneEnoughCreativeTab$minecraft();

    Font oneEnoughCreativeTab$font();

    CreativeModeInventoryScreen.ItemPickerMenu oneEnoughCreativeTab$menu();

    int oneEnoughCreativeTab$left();

    int oneEnoughCreativeTab$top();

    int oneEnoughCreativeTab$imageWidth();

    int oneEnoughCreativeTab$imageHeight();

    int oneEnoughCreativeTab$screenWidth();

    int oneEnoughCreativeTab$screenHeight();

    CreativeModeTab oneEnoughCreativeTab$selectedTab();

    void oneEnoughCreativeTab$selectTab(CreativeModeTab tab);

    int oneEnoughCreativeTab$tabX(CreativeModeTab tab);

    int oneEnoughCreativeTab$tabY(CreativeModeTab tab);

    boolean oneEnoughCreativeTab$isCreativeSlot(Slot slot);

    float oneEnoughCreativeTab$scrollOffset();

    void oneEnoughCreativeTab$setScrollOffset(float scrollOffset);

    Slot oneEnoughCreativeTab$destroyItemSlot();

    boolean oneEnoughCreativeTab$isPageButtonAt(double mouseX, double mouseY);

    boolean oneEnoughCreativeTab$isInteractiveWidgetAt(double mouseX, double mouseY);

    void oneEnoughCreativeTab$showTitleEditor(String initialValue);

    String oneEnoughCreativeTab$titleEditorValue();

    boolean oneEnoughCreativeTab$isTitleEditorAt(double mouseX, double mouseY);

    void oneEnoughCreativeTab$extractTitleEditor(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

    boolean oneEnoughCreativeTab$titleEditorKeyPressed(KeyEvent event);

    boolean oneEnoughCreativeTab$titleEditorCharTyped(CharacterEvent event);

    boolean oneEnoughCreativeTab$titleEditorPreeditUpdated(@Nullable PreeditEvent event);

    boolean oneEnoughCreativeTab$titleEditorMouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY
    );

    boolean oneEnoughCreativeTab$titleEditorMouseReleased(MouseButtonEvent event);

    void oneEnoughCreativeTab$hideTitleEditor();

    void oneEnoughCreativeTab$replaySlotClick(Slot slot, int slotId, int button, ContainerInput input);

    void oneEnoughCreativeTab$refreshScreen();
}
