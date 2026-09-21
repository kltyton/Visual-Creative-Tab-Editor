package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorController;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the long-press, mobile-desktop-style visual editor to the Creative inventory. */
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
    private CreativeTabEditorController visualCreativeTabEditor$editor() {
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

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().mouseClicked(event, doubleClick)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$slotClicked(
            @Nullable Slot slot,
            int slotId,
            int button,
            ContainerInput input,
            CallbackInfo callback
    ) {
        if (this.visualCreativeTabEditor$editor().slotClicked(slot, slotId, button, input)) {
            callback.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().mouseDragged(event, deltaX, deltaY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontal,
            double vertical,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseReleasedHead(
            MouseButtonEvent event,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().mouseReleased(event)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void visualCreativeTabEditor$mouseReleasedReturn(
            MouseButtonEvent event,
            CallbackInfoReturnable<Boolean> callback
    ) {
        this.visualCreativeTabEditor$editor().afterMouseReleased(event);
    }

    @Override
    public void visualCreativeTabEditor$renderEditorOverlay(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.visualCreativeTabEditor$editor().extractRenderState(graphics, mouseX, mouseY, partialTick);
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

    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void visualCreativeTabEditor$arrangeDataDrivenTabs(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
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
        List<CreativeModeTab> visible = new ArrayList<>(
                CreativeTabClientPlatform.visibleTabs((CreativeModeInventoryScreen) (Object) this)
        );
        if (CreativeTabRuntime.catalog().isEmpty()) {
            return visible;
        }
        Map<CreativeModeTab, Integer> nativeOrder = new HashMap<>();
        for (int index = 0; index < visible.size(); index++) {
            nativeOrder.put(visible.get(index), index);
        }
        Map<net.minecraft.resources.Identifier, Integer> configuredOrder = new HashMap<>();
        var definitions = CreativeTabRuntime.catalog().orderedDefinitions();
        for (int index = 0; index < definitions.size(); index++) {
            configuredOrder.put(definitions.get(index).id(), index);
        }
        visible.sort(java.util.Comparator
                .comparingInt((CreativeModeTab tab) -> CreativeTabRuntime.id(tab)
                        .map(id -> configuredOrder.getOrDefault(id, Integer.MAX_VALUE))
                        .orElse(Integer.MAX_VALUE))
                .thenComparingInt(tab -> nativeOrder.getOrDefault(tab, Integer.MAX_VALUE)));
        return visible;
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$suppressTabTooltip(
            GuiGraphicsExtractor graphics,
            CreativeModeTab tab,
            int mouseX,
            int mouseY,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().suppressesTooltips()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (this.visualCreativeTabEditor$editor().keyPressed(event)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$charTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (this.visualCreativeTabEditor$editor().charTyped(event)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$preeditUpdated(
            @Nullable PreeditEvent event,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().preeditUpdated(event)) {
            callback.setReturnValue(true);
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
        editor.setTextShadow(false);
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
    public void visualCreativeTabEditor$extractTitleEditor(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (this.visualCreativeTabEditor$titleEditor != null && this.visualCreativeTabEditor$titleEditor.visible) {
            this.visualCreativeTabEditor$titleEditor.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorKeyPressed(KeyEvent event) {
        return this.visualCreativeTabEditor$titleEditor != null
                && this.visualCreativeTabEditor$titleEditor.keyPressed(event);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorCharTyped(CharacterEvent event) {
        return this.visualCreativeTabEditor$titleEditor != null
                && this.visualCreativeTabEditor$titleEditor.charTyped(event);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorPreeditUpdated(@Nullable PreeditEvent event) {
        return this.visualCreativeTabEditor$titleEditor != null
                && this.visualCreativeTabEditor$titleEditor.preeditUpdated(event);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorMouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY
    ) {
        return this.visualCreativeTabEditor$titleEditor != null
                && this.isDragging()
                && this.getFocused() == this.visualCreativeTabEditor$titleEditor
                && this.visualCreativeTabEditor$titleEditor.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorMouseReleased(MouseButtonEvent event) {
        boolean titleWasDragging = this.isDragging()
                && this.getFocused() == this.visualCreativeTabEditor$titleEditor;
        this.setDragging(false);
        return titleWasDragging
                && this.visualCreativeTabEditor$titleEditor != null
                && this.visualCreativeTabEditor$titleEditor.mouseReleased(event);
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
    public void visualCreativeTabEditor$replaySlotClick(Slot slot, int slotId, int button, ContainerInput input) {
        this.slotClicked(slot, slotId, button, input);
    }

    @Override
    public void visualCreativeTabEditor$refreshScreen() {
        CreativeTabClientPlatform.refreshScreen((CreativeModeInventoryScreen) (Object) this);
    }
}
