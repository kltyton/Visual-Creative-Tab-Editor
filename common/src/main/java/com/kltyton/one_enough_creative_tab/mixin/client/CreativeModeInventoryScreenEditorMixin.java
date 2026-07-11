package com.kltyton.one_enough_creative_tab.mixin.client;

import com.kltyton.one_enough_creative_tab.OneEnoughCreativeTabConstants;
import com.kltyton.one_enough_creative_tab.client.editor.CreativeTabEditorController;
import com.kltyton.one_enough_creative_tab.client.editor.CreativeTabEditorHost;
import com.kltyton.one_enough_creative_tab.client.CreativeTabClientPlatform;
import com.kltyton.one_enough_creative_tab.runtime.CreativeTabRuntime;
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
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
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
    private CreativeTabEditorController oneEnoughCreativeTab$editor;
    @Unique
    private @Nullable EditBox oneEnoughCreativeTab$titleEditor;

    protected CreativeModeInventoryScreenEditorMixin(
            CreativeModeInventoryScreen.ItemPickerMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    @Unique
    private CreativeTabEditorController oneEnoughCreativeTab$editor() {
        if (this.oneEnoughCreativeTab$editor == null) {
            this.oneEnoughCreativeTab$editor = new CreativeTabEditorController(this);
        }
        return this.oneEnoughCreativeTab$editor;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void oneEnoughCreativeTab$initEditor(CallbackInfo callback) {
        this.oneEnoughCreativeTab$editor().onInit();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.oneEnoughCreativeTab$editor().mouseClicked(event, doubleClick)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$slotClicked(
            @Nullable Slot slot,
            int slotId,
            int button,
            ContainerInput input,
            CallbackInfo callback
    ) {
        if (this.oneEnoughCreativeTab$editor().slotClicked(slot, slotId, button, input)) {
            callback.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$mouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.oneEnoughCreativeTab$editor().mouseDragged(event, deltaX, deltaY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontal,
            double vertical,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.oneEnoughCreativeTab$editor().mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$mouseReleasedHead(
            MouseButtonEvent event,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.oneEnoughCreativeTab$editor().mouseReleased(event)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void oneEnoughCreativeTab$mouseReleasedReturn(
            MouseButtonEvent event,
            CallbackInfoReturnable<Boolean> callback
    ) {
        this.oneEnoughCreativeTab$editor().afterMouseReleased(event);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void oneEnoughCreativeTab$extractEditor(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        this.oneEnoughCreativeTab$editor().extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "selectTab", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$rejectHiddenTab(CreativeModeTab tab, CallbackInfo callback) {
        boolean reject = !CreativeTabRuntime.catalog().isEmpty() && !tab.shouldDisplay();
        OneEnoughCreativeTabConstants.LOGGER.info(
                "[EditorTrace] native-select-tab-head requested={} shouldDisplay={} selectedBefore={} catalogEmpty={} rejected={}",
                oneEnoughCreativeTab$traceTabId(tab),
                tab.shouldDisplay(),
                oneEnoughCreativeTab$traceTabId(this.oneEnoughCreativeTab$selectedTab()),
                CreativeTabRuntime.catalog().isEmpty(),
                reject
        );
        if (reject) {
            callback.cancel();
        }
    }

    @Inject(method = "selectTab", at = @At("RETURN"))
    private void oneEnoughCreativeTab$traceSelectedTab(CreativeModeTab tab, CallbackInfo callback) {
        OneEnoughCreativeTabConstants.LOGGER.info(
                "[EditorTrace] native-select-tab-return requested={} selectedAfter={} success={}",
                oneEnoughCreativeTab$traceTabId(tab),
                oneEnoughCreativeTab$traceTabId(this.oneEnoughCreativeTab$selectedTab()),
                this.oneEnoughCreativeTab$selectedTab() == tab
        );
    }

    @Redirect(
            method = "tryRefreshInvalidatedTabs",
            at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z")
    )
    private boolean oneEnoughCreativeTab$keepEmptyDataDrivenCategorySelected(Collection<?> displayItems) {
        boolean empty = displayItems.isEmpty();
        CreativeModeTab selected = this.oneEnoughCreativeTab$selectedTab();
        boolean keepSelected = empty
                && selected.getType() == CreativeModeTab.Type.CATEGORY
                && CreativeTabRuntime.definition(selected).isPresent();
        if (keepSelected) {
            OneEnoughCreativeTabConstants.LOGGER.info(
                    "[EditorTrace] empty-data-tab-preserved selected={} displayItems=0",
                    oneEnoughCreativeTab$traceTabId(selected)
            );
        }
        return empty && !keepSelected;
    }

    @Unique
    private static String oneEnoughCreativeTab$traceTabId(@Nullable CreativeModeTab tab) {
        return tab == null
                ? "-"
                : CreativeTabRuntime.id(tab).map(Object::toString).orElse("<unregistered>");
    }

    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void oneEnoughCreativeTab$arrangeDataDrivenTabs(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        if (CreativeTabRuntime.catalog().isEmpty() || CreativeTabClientPlatform.preservesNativeTabPositions()) {
            return;
        }
        List<CreativeModeTab> tabs = this.oneEnoughCreativeTab$orderedVisibleTabs();
        for (int index = 0; index < tabs.size(); index++) {
            CreativeModeTab tab = tabs.get(index);
            tab.row = index < 7 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            tab.column = index % 7;
        }
    }

    @Inject(method = "getTabX", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$dataDrivenTabX(
            CreativeModeTab tab,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (CreativeTabRuntime.catalog().isEmpty() || CreativeTabClientPlatform.preservesNativeTabPositions()) {
            return;
        }
        int index = this.oneEnoughCreativeTab$orderedVisibleTabs().indexOf(tab);
        if (index >= 0) {
            callback.setReturnValue((index % 7) * 27);
        }
    }

    @Inject(method = "getTabY", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$dataDrivenTabY(
            CreativeModeTab tab,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (CreativeTabRuntime.catalog().isEmpty() || CreativeTabClientPlatform.preservesNativeTabPositions()) {
            return;
        }
        int index = this.oneEnoughCreativeTab$orderedVisibleTabs().indexOf(tab);
        if (index >= 0) {
            callback.setReturnValue(index < 7 ? -32 : this.imageHeight);
        }
    }

    @Unique
    private List<CreativeModeTab> oneEnoughCreativeTab$orderedVisibleTabs() {
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

    @Inject(method = "getTooltipFromContainerItem", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$suppressItemTooltip(
            ItemStack stack,
            CallbackInfoReturnable<List<Component>> callback
    ) {
        if (this.oneEnoughCreativeTab$editor().suppressesTooltips()) {
            callback.setReturnValue(List.of());
        }
    }

    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$suppressTabTooltip(
            GuiGraphicsExtractor graphics,
            CreativeModeTab tab,
            int mouseX,
            int mouseY,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.oneEnoughCreativeTab$editor().suppressesTooltips()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (this.oneEnoughCreativeTab$editor().keyPressed(event)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$charTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (this.oneEnoughCreativeTab$editor().charTyped(event)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$preeditUpdated(
            @Nullable PreeditEvent event,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.oneEnoughCreativeTab$editor().preeditUpdated(event)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void oneEnoughCreativeTab$removed(CallbackInfo callback) {
        this.oneEnoughCreativeTab$editor().removed();
    }

    @Override
    public CreativeModeInventoryScreen oneEnoughCreativeTab$screen() {
        return (CreativeModeInventoryScreen) (Object) this;
    }

    @Override
    public Minecraft oneEnoughCreativeTab$minecraft() {
        return this.minecraft;
    }

    @Override
    public Font oneEnoughCreativeTab$font() {
        return this.font;
    }

    @Override
    public CreativeModeInventoryScreen.ItemPickerMenu oneEnoughCreativeTab$menu() {
        return this.menu;
    }

    @Override
    public int oneEnoughCreativeTab$left() {
        return this.leftPos;
    }

    @Override
    public int oneEnoughCreativeTab$top() {
        return this.topPos;
    }

    @Override
    public int oneEnoughCreativeTab$imageWidth() {
        return this.imageWidth;
    }

    @Override
    public int oneEnoughCreativeTab$imageHeight() {
        return this.imageHeight;
    }

    @Override
    public int oneEnoughCreativeTab$screenWidth() {
        return this.width;
    }

    @Override
    public int oneEnoughCreativeTab$screenHeight() {
        return this.height;
    }

    @Override
    public CreativeModeTab oneEnoughCreativeTab$selectedTab() {
        return CreativeModeInventoryScreen.selectedTab;
    }

    @Override
    public void oneEnoughCreativeTab$selectTab(CreativeModeTab tab) {
        ((CreativeModeInventoryScreen) (Object) this).selectTab(tab);
    }

    @Override
    public int oneEnoughCreativeTab$tabX(CreativeModeTab tab) {
        return ((CreativeModeInventoryScreen) (Object) this).getTabX(tab);
    }

    @Override
    public int oneEnoughCreativeTab$tabY(CreativeModeTab tab) {
        return ((CreativeModeInventoryScreen) (Object) this).getTabY(tab);
    }

    @Override
    public boolean oneEnoughCreativeTab$isCreativeSlot(Slot slot) {
        return ((CreativeModeInventoryScreen) (Object) this).isCreativeSlot(slot);
    }

    @Override
    public float oneEnoughCreativeTab$scrollOffset() {
        return ((CreativeModeInventoryScreen) (Object) this).scrollOffs;
    }

    @Override
    public void oneEnoughCreativeTab$setScrollOffset(float scrollOffset) {
        ((CreativeModeInventoryScreen) (Object) this).scrollOffs = scrollOffset;
    }

    @Override
    public Slot oneEnoughCreativeTab$destroyItemSlot() {
        return ((CreativeModeInventoryScreen) (Object) this).destroyItemSlot;
    }

    @Override
    public boolean oneEnoughCreativeTab$isPageButtonAt(double mouseX, double mouseY) {
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
        OneEnoughCreativeTabConstants.LOGGER.info(
                "[EditorTrace] page-button-hit-test x={} y={} hit={} widgets={}",
                mouseX,
                mouseY,
                hit,
                widgetStates
        );
        return hit;
    }

    @Override
    public boolean oneEnoughCreativeTab$isInteractiveWidgetAt(double mouseX, double mouseY) {
        return this.children().stream()
                .filter(child -> child instanceof AbstractWidget)
                .map(child -> (AbstractWidget) child)
                .anyMatch(widget -> widget.visible && widget.active && widget.isMouseOver(mouseX, mouseY));
    }

    @Override
    public void oneEnoughCreativeTab$showTitleEditor(String initialValue) {
        String value = this.oneEnoughCreativeTab$titleEditor == null
                ? initialValue
                : this.oneEnoughCreativeTab$titleEditor.getValue();
        this.oneEnoughCreativeTab$hideTitleEditor();
        int panelX = (this.width - 190) / 2;
        int panelY = (this.height - 80) / 2;
        EditBox editor = new EditBox(
                this.font,
                panelX + 8,
                panelY + 22,
                174,
                20,
                Component.translatable("one_enough_creative_tab.editor.title")
        );
        editor.setMaxLength(128);
        editor.setValue(value);
        this.oneEnoughCreativeTab$titleEditor = this.addWidget(editor);
        this.setFocused(editor);
        editor.setFocused(true);
    }

    @Override
    public String oneEnoughCreativeTab$titleEditorValue() {
        return this.oneEnoughCreativeTab$titleEditor == null ? "" : this.oneEnoughCreativeTab$titleEditor.getValue();
    }

    @Override
    public boolean oneEnoughCreativeTab$isTitleEditorAt(double mouseX, double mouseY) {
        return this.oneEnoughCreativeTab$titleEditor != null
                && this.oneEnoughCreativeTab$titleEditor.visible
                && this.oneEnoughCreativeTab$titleEditor.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void oneEnoughCreativeTab$extractTitleEditor(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (this.oneEnoughCreativeTab$titleEditor != null && this.oneEnoughCreativeTab$titleEditor.visible) {
            this.oneEnoughCreativeTab$titleEditor.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean oneEnoughCreativeTab$titleEditorKeyPressed(KeyEvent event) {
        return this.oneEnoughCreativeTab$titleEditor != null
                && this.oneEnoughCreativeTab$titleEditor.keyPressed(event);
    }

    @Override
    public boolean oneEnoughCreativeTab$titleEditorCharTyped(CharacterEvent event) {
        return this.oneEnoughCreativeTab$titleEditor != null
                && this.oneEnoughCreativeTab$titleEditor.charTyped(event);
    }

    @Override
    public boolean oneEnoughCreativeTab$titleEditorPreeditUpdated(@Nullable PreeditEvent event) {
        return this.oneEnoughCreativeTab$titleEditor != null
                && this.oneEnoughCreativeTab$titleEditor.preeditUpdated(event);
    }

    @Override
    public boolean oneEnoughCreativeTab$titleEditorMouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY
    ) {
        return this.oneEnoughCreativeTab$titleEditor != null
                && this.isDragging()
                && this.getFocused() == this.oneEnoughCreativeTab$titleEditor
                && this.oneEnoughCreativeTab$titleEditor.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean oneEnoughCreativeTab$titleEditorMouseReleased(MouseButtonEvent event) {
        boolean titleWasDragging = this.isDragging()
                && this.getFocused() == this.oneEnoughCreativeTab$titleEditor;
        this.setDragging(false);
        return titleWasDragging
                && this.oneEnoughCreativeTab$titleEditor != null
                && this.oneEnoughCreativeTab$titleEditor.mouseReleased(event);
    }

    @Override
    public void oneEnoughCreativeTab$hideTitleEditor() {
        if (this.oneEnoughCreativeTab$titleEditor == null) {
            return;
        }
        EditBox editor = this.oneEnoughCreativeTab$titleEditor;
        editor.visible = false;
        editor.setFocused(false);
        if (this.getFocused() == editor) {
            this.setFocused(null);
        }
        this.removeWidget(editor);
        this.oneEnoughCreativeTab$titleEditor = null;
    }

    @Override
    public void oneEnoughCreativeTab$replaySlotClick(Slot slot, int slotId, int button, ContainerInput input) {
        this.slotClicked(slot, slotId, button, input);
    }

    @Override
    public void oneEnoughCreativeTab$refreshScreen() {
        CreativeTabClientPlatform.refreshScreen((CreativeModeInventoryScreen) (Object) this);
    }
}
