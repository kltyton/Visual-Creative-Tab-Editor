package com.kltyton.visual_creative_tab_editor.client.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Character;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Key;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Pointer;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Preedit;
import com.kltyton.visual_creative_tab_editor.client.editor.input.SlotInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import javax.annotation.Nullable;

/** Narrow bridge from the screen Mixin to the loader-neutral editor controller. */
public interface CreativeTabEditorHost {
    CreativeTabEditorController visualCreativeTabEditor$editor();

    CreativeModeInventoryScreen visualCreativeTabEditor$screen();

    boolean visualCreativeTabEditor$suppressesTooltips();

    void visualCreativeTabEditor$renderItemGroupBackground(EditorGraphics graphics, Slot slot);

    void visualCreativeTabEditor$renderItemGroupBadge(EditorGraphics graphics, Slot slot);

    void visualCreativeTabEditor$renderCategoryFrame(EditorGraphics graphics);

    void visualCreativeTabEditor$renderEditorOverlay(
            EditorGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

    void visualCreativeTabEditor$arrangeDataDrivenTabs();

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
            EditorGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    );

    boolean visualCreativeTabEditor$titleEditorKeyPressed(Key event);

    boolean visualCreativeTabEditor$titleEditorCharTyped(Character event);

    boolean visualCreativeTabEditor$titleEditorPreeditUpdated(@Nullable Preedit event);

    boolean visualCreativeTabEditor$titleEditorMouseDragged(
            Pointer event,
            double deltaX,
            double deltaY
    );

    boolean visualCreativeTabEditor$titleEditorMouseReleased(Pointer event);

    void visualCreativeTabEditor$hideTitleEditor();

    void visualCreativeTabEditor$replaySlotClick(Slot slot, int slotId, int button, SlotInput input);

    void visualCreativeTabEditor$refreshScreen();
}
