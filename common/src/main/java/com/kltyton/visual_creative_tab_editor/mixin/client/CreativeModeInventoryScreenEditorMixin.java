package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorController;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeModeItemPickerMenuVirtualTail;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeItemGroups;
import net.minecraft.ChatFormatting;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorGraphics;
import com.kltyton.visual_creative_tab_editor.client.editor.render.EditorTheme;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Character;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Key;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Pointer;
import com.kltyton.visual_creative_tab_editor.client.editor.input.EditorInput.Preedit;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds editor input and expandable item groups to the Creative inventory. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenEditorMixin
        extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu>
        implements CreativeTabEditorHost {
    @Unique
    private CreativeTabEditorController visualCreativeTabEditor$editor;
    @Unique
    private @Nullable EditBox visualCreativeTabEditor$titleEditor;

    protected CreativeModeInventoryScreenEditorMixin(
            CreativeModeInventoryScreen.ItemPickerMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    @Unique
    public CreativeTabEditorController visualCreativeTabEditor$editor() {
        if (this.visualCreativeTabEditor$editor == null) {
            this.visualCreativeTabEditor$editor = new CreativeTabEditorController(this);
        }
        return this.visualCreativeTabEditor$editor;
    }

    @Inject(method = "hasPermissions", at = @At("RETURN"))
    private void visualCreativeTabEditor$rememberClientPermissions(
            Player player,
            CallbackInfoReturnable<Boolean> callback
    ) {
        CreativeTabRuntime.rememberClientPermissions(callback.getReturnValue());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void visualCreativeTabEditor$initEditor(CallbackInfo callback) {
        this.visualCreativeTabEditor$editor().onInit();
    }

    @Override
    public void visualCreativeTabEditor$renderItemGroupBackground(EditorGraphics graphics, Slot slot) {
        this.visualCreativeTabEditor$editor().renderItemGroupBackground(graphics, slot);
    }

    @Override
    public void visualCreativeTabEditor$renderItemGroupBadge(EditorGraphics graphics, Slot slot) {
        this.visualCreativeTabEditor$editor().renderItemGroupBadge(graphics, slot);
    }

    @Override
    public void visualCreativeTabEditor$renderCategoryFrame(EditorGraphics graphics) {
        this.visualCreativeTabEditor$editor().renderCategoryFrame(graphics);
    }

    @Override
    public void visualCreativeTabEditor$renderEditorOverlay(
            EditorGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.visualCreativeTabEditor$editor().render(graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "selectTab", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$rejectHiddenTab(CreativeModeTab tab, CallbackInfo callback) {
        boolean reject = !CreativeTabRuntime.catalog().isEmpty() && !tab.shouldDisplay();
        VisualCreativeTabEditorConstants.LOGGER.debug(
                "[EditorTrace] native-select-tab-head requested={} shouldDisplay={} selectedBefore={} catalogEmpty={} rejected={}",
                visualCreativeTabEditor$traceTabId(tab),
                tab.shouldDisplay(),
                visualCreativeTabEditor$traceTabId(this.visualCreativeTabEditor$selectedTab()),
                CreativeTabRuntime.catalog().isEmpty(),
                reject
        );
        if (reject) {
            callback.cancel();
        } else {
            ((CreativeModeItemPickerMenuVirtualTail) this.menu).visualCreativeTabEditor$itemGroups().selectTab(tab);
        }
    }

    @Inject(method = "refreshSearchResults", at = @At("HEAD"))
    private void visualCreativeTabEditor$invalidateSearchGroups(CallbackInfo callback) {
        ((CreativeModeItemPickerMenuVirtualTail) this.menu).visualCreativeTabEditor$itemGroups().invalidate();
    }

    @Inject(method = "refreshCurrentTabContents", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/NonNullList;clear()V",
            shift = At.Shift.AFTER
    ))
    private void visualCreativeTabEditor$invalidateRefreshedGroups(CallbackInfo callback) {
        ((CreativeModeItemPickerMenuVirtualTail) this.menu).visualCreativeTabEditor$itemGroups().invalidate();
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void visualCreativeTabEditor$describeItemGroup(
            ItemStack stack, CallbackInfoReturnable<List<Component>> callback
    ) {
        if (this.hoveredSlot == null || !this.visualCreativeTabEditor$isCreativeSlot(this.hoveredSlot)) {
            return;
        }
        CreativeItemGroups groups = ((CreativeModeItemPickerMenuVirtualTail) this.menu).visualCreativeTabEditor$itemGroups();
        int displayIndex = this.menu.getRowIndexForScroll(this.visualCreativeTabEditor$scrollOffset()) * 9
                + this.hoveredSlot.index;
        if (groups.isHeader(displayIndex)) {
            List<Component> lines = new ArrayList<>(callback.getReturnValue());
            if (!lines.isEmpty()) { lines.set(0, groups.title(displayIndex, this.menu.items)); }
            lines.add(Component.translatable(groups.isExpanded(displayIndex)
                    ? "visual_creative_tab_editor.group.collapse" : "visual_creative_tab_editor.group.expand",
                    groups.memberCount(displayIndex)).withStyle(ChatFormatting.GRAY));
            callback.setReturnValue(lines);
        }
    }

    @Inject(method = "selectTab", at = @At("RETURN"))
    private void visualCreativeTabEditor$traceSelectedTab(CreativeModeTab tab, CallbackInfo callback) {
        VisualCreativeTabEditorConstants.LOGGER.debug(
                "[EditorTrace] native-select-tab-return requested={} selectedAfter={} success={}",
                visualCreativeTabEditor$traceTabId(tab),
                visualCreativeTabEditor$traceTabId(this.visualCreativeTabEditor$selectedTab()),
                this.visualCreativeTabEditor$selectedTab() == tab
        );
    }

    @Redirect(
            method = "tryRefreshInvalidatedTabs",
            at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z")
    )
    private boolean visualCreativeTabEditor$keepEmptyDataDrivenCategorySelected(Collection<?> displayItems) {
        boolean empty = displayItems.isEmpty();
        CreativeModeTab selected = this.visualCreativeTabEditor$selectedTab();
        boolean keepSelected = empty
                && selected.getType() == CreativeModeTab.Type.CATEGORY
                && CreativeTabRuntime.definition(selected).isPresent();
        if (keepSelected) {
            VisualCreativeTabEditorConstants.LOGGER.debug(
                    "[EditorTrace] empty-data-tab-preserved selected={} displayItems=0",
                    visualCreativeTabEditor$traceTabId(selected)
            );
        }
        return empty && !keepSelected;
    }

    @Unique
    private static String visualCreativeTabEditor$traceTabId(@Nullable CreativeModeTab tab) {
        return tab == null
                ? "-"
                : CreativeTabRuntime.id(tab).map(Object::toString).orElse("<unregistered>");
    }

    @Override
    @Unique
    public void visualCreativeTabEditor$arrangeDataDrivenTabs() {
        if (CreativeTabRuntime.catalog().isEmpty() || CreativeTabClientPlatform.preservesNativeTabPositions()) {
            return;
        }
        List<CreativeModeTab> tabs = this.visualCreativeTabEditor$orderedVisibleTabs();
        for (int index = 0; index < tabs.size(); index++) {
            CreativeModeTab tab = tabs.get(index);
            tab.row = index < 7 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            tab.column = index % 7;
        }
    }

    @Inject(method = "getTabX", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$dataDrivenTabX(
            CreativeModeTab tab,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (CreativeTabRuntime.catalog().isEmpty() || CreativeTabClientPlatform.preservesNativeTabPositions()) {
            return;
        }
        int index = this.visualCreativeTabEditor$orderedVisibleTabs().indexOf(tab);
        if (index >= 0) {
            callback.setReturnValue((index % 7) * 27);
        }
    }

    @Inject(method = "getTabY", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$dataDrivenTabY(
            CreativeModeTab tab,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (CreativeTabRuntime.catalog().isEmpty() || CreativeTabClientPlatform.preservesNativeTabPositions()) {
            return;
        }
        int index = this.visualCreativeTabEditor$orderedVisibleTabs().indexOf(tab);
        if (index >= 0) {
            callback.setReturnValue(index < 7 ? -32 : this.imageHeight);
        }
    }

    @Unique
    private List<CreativeModeTab> visualCreativeTabEditor$orderedVisibleTabs() {
        return com.kltyton.visual_creative_tab_editor.runtime.CreativeTabOrdering.ordered(
                CreativeTabClientPlatform.visibleTabs((CreativeModeInventoryScreen) (Object) this));
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$suppressTabTooltip(
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().suppressesTooltips()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void visualCreativeTabEditor$removed(CallbackInfo callback) {
        this.visualCreativeTabEditor$editor().removed();
    }

    @Override
    public CreativeModeInventoryScreen visualCreativeTabEditor$screen() {
        return (CreativeModeInventoryScreen) (Object) this;
    }

    @Override
    public boolean visualCreativeTabEditor$suppressesTooltips() {
        return this.visualCreativeTabEditor$editor().suppressesItemTooltip(this.hoveredSlot);
    }

    @Override
    public Minecraft visualCreativeTabEditor$minecraft() {
        return this.minecraft;
    }

    @Override
    public Font visualCreativeTabEditor$font() {
        return this.font;
    }

    @Override
    public CreativeModeInventoryScreen.ItemPickerMenu visualCreativeTabEditor$menu() {
        return this.menu;
    }

    @Override
    public int visualCreativeTabEditor$left() {
        return this.leftPos;
    }

    @Override
    public int visualCreativeTabEditor$top() {
        return this.topPos;
    }

    @Override
    public int visualCreativeTabEditor$imageWidth() {
        return this.imageWidth;
    }

    @Override
    public int visualCreativeTabEditor$imageHeight() {
        return this.imageHeight;
    }

    @Override
    public int visualCreativeTabEditor$screenWidth() {
        return this.width;
    }

    @Override
    public int visualCreativeTabEditor$screenHeight() {
        return this.height;
    }

    @Override
    public CreativeModeTab visualCreativeTabEditor$selectedTab() {
        return CreativeModeInventoryScreen.selectedTab;
    }

    @Override
    public void visualCreativeTabEditor$selectTab(CreativeModeTab tab) {
        ((CreativeModeInventoryScreen) (Object) this).selectTab(tab);
    }

    @Override
    public int visualCreativeTabEditor$tabX(CreativeModeTab tab) {
        return ((CreativeModeInventoryScreen) (Object) this).getTabX(tab);
    }

    @Override
    public int visualCreativeTabEditor$tabY(CreativeModeTab tab) {
        return ((CreativeModeInventoryScreen) (Object) this).getTabY(tab);
    }

    @Override
    public boolean visualCreativeTabEditor$isCreativeSlot(Slot slot) {
        return ((CreativeModeInventoryScreen) (Object) this).isCreativeSlot(slot);
    }

    @Override
    public float visualCreativeTabEditor$scrollOffset() {
        return ((CreativeModeInventoryScreen) (Object) this).scrollOffs;
    }

    @Override
    public void visualCreativeTabEditor$setScrollOffset(float scrollOffset) {
        ((CreativeModeInventoryScreen) (Object) this).scrollOffs = scrollOffset;
    }

    @Override
    public Slot visualCreativeTabEditor$destroyItemSlot() {
        return ((CreativeModeInventoryScreen) (Object) this).destroyItemSlot;
    }

    @Override
    public boolean visualCreativeTabEditor$isPageButtonAt(double mouseX, double mouseY) {
        List<AbstractWidget> widgets = this.children().stream()
                .filter(child -> child instanceof AbstractWidget)
                .map(child -> (AbstractWidget) child)
                .toList();
        List<String> widgetStates = widgets.stream().map(widget ->
                "%s[message='%s',bounds=%d,%d,%dx%d,visible=%s,active=%s,hover=%s]".formatted(
                        widget.getClass().getName(),
                        widget.getMessage().getString(),
                        widget.getX(),
                        widget.getY(),
                        widget.getWidth(),
                        widget.getHeight(),
                        widget.visible,
                        widget.active,
                        widget.isMouseOver(mouseX, mouseY)
                )).toList();
        List<AbstractWidget> pageButtons = widgets.stream()
                .filter(widget -> {
                    String message = widget.getMessage().getString();
                    return message.equals("<") || message.equals(">");
                })
                .filter(widget -> mouseX >= widget.getX()
                        && mouseX < widget.getX() + widget.getWidth()
                        && mouseY >= widget.getY()
                        && mouseY < widget.getY() + widget.getHeight())
                .toList();
        boolean hit = !pageButtons.isEmpty();
        VisualCreativeTabEditorConstants.LOGGER.debug(
                "[EditorTrace] page-button-hit-test x={} y={} hit={} widgets={}",
                mouseX,
                mouseY,
                hit,
                widgetStates
        );
        return hit;
    }

    @Override
    public boolean visualCreativeTabEditor$isInteractiveWidgetAt(double mouseX, double mouseY) {
        return this.children().stream()
                .filter(child -> child instanceof AbstractWidget)
                .map(child -> (AbstractWidget) child)
                .anyMatch(widget -> widget.visible && widget.active && widget.isMouseOver(mouseX, mouseY));
    }

    @Override
    public void visualCreativeTabEditor$showTitleEditor(String initialValue) {
        String value = this.visualCreativeTabEditor$titleEditor == null
                ? initialValue
                : this.visualCreativeTabEditor$titleEditor.getValue();
        this.visualCreativeTabEditor$hideTitleEditor();
        int panelX = (this.width - 190) / 2;
        int panelY = (this.height - 80) / 2;
        EditBox editor = new EditBox(
                this.font,
                panelX + 8,
                panelY + 22,
                174,
                20,
                Component.translatable("visual_creative_tab_editor.editor.title")
        );
        com.kltyton.visual_creative_tab_editor.client.CreativeTabTextInput.disableShadow(editor);
        editor.setMaxLength(128);
        editor.setValue(value);
        this.visualCreativeTabEditor$titleEditor = this.addWidget(editor);
        this.setFocused(editor);
        editor.setFocused(true);
    }

    @Override
    public String visualCreativeTabEditor$titleEditorValue() {
        return this.visualCreativeTabEditor$titleEditor == null ? "" : this.visualCreativeTabEditor$titleEditor.getValue();
    }

    @Override
    public boolean visualCreativeTabEditor$isTitleEditorAt(double mouseX, double mouseY) {
        return this.visualCreativeTabEditor$titleEditor != null
                && this.visualCreativeTabEditor$titleEditor.visible
                && this.visualCreativeTabEditor$titleEditor.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void visualCreativeTabEditor$renderTitleEditor(
            EditorGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (this.visualCreativeTabEditor$titleEditor != null && this.visualCreativeTabEditor$titleEditor.visible) {
            EditBox editor = this.visualCreativeTabEditor$titleEditor;
            EditorTheme.inset(graphics, editor.getX() - 1, editor.getY() - 1,
                    editor.getWidth() + 2, editor.getHeight() + 2, editor.isFocused());
            graphics.title(this.visualCreativeTabEditor$titleEditor, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorKeyPressed(Key event) {
        return this.visualCreativeTabEditor$titleEditor != null
                && event.press(this.visualCreativeTabEditor$titleEditor);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorCharTyped(Character event) {
        return this.visualCreativeTabEditor$titleEditor != null
                && event.type(this.visualCreativeTabEditor$titleEditor);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorPreeditUpdated(@Nullable Preedit event) {
        return this.visualCreativeTabEditor$titleEditor != null
                && event.update(this.visualCreativeTabEditor$titleEditor);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorMouseDragged(
            Pointer event,
            double deltaX,
            double deltaY
    ) {
        return this.visualCreativeTabEditor$titleEditor != null
                && this.isDragging()
                && this.getFocused() == this.visualCreativeTabEditor$titleEditor
                && event.drag(this.visualCreativeTabEditor$titleEditor, deltaX, deltaY);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorMouseReleased(Pointer event) {
        boolean titleWasDragging = this.isDragging()
                && this.getFocused() == this.visualCreativeTabEditor$titleEditor;
        this.setDragging(false);
        return titleWasDragging
                && this.visualCreativeTabEditor$titleEditor != null
                && event.release(this.visualCreativeTabEditor$titleEditor);
    }

    @Override
    public void visualCreativeTabEditor$hideTitleEditor() {
        if (this.visualCreativeTabEditor$titleEditor == null) {
            return;
        }
        EditBox editor = this.visualCreativeTabEditor$titleEditor;
        editor.visible = false;
        editor.setFocused(false);
        if (this.getFocused() == editor) {
            this.setFocused(null);
        }
        this.removeWidget(editor);
        this.visualCreativeTabEditor$titleEditor = null;
    }

    @Override
    public void visualCreativeTabEditor$refreshScreen() {
        CreativeTabClientPlatform.refreshScreen((CreativeModeInventoryScreen) (Object) this);
    }
}
