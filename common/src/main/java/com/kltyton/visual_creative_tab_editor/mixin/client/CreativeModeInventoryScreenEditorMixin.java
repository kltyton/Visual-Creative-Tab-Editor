package com.kltyton.visual_creative_tab_editor.mixin.client;

// @target if minecraft_version=1.20.1
//|import com.mojang.blaze3d.vertex.PoseStack;
// @target else
// @target end
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
// @target if minecraft_version=1.20.1
//|import javax.annotation.Nullable;
// @target elif minecraft_version=26.1.2,26.2
// @target else
import org.jetbrains.annotations.Nullable;
// @target end
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
// @target if minecraft_version=1.20.1
//|import net.minecraft.resources.ResourceLocation;
// @target elif minecraft_version=26.1.2,26.2
//|import net.minecraft.client.input.CharacterEvent;
//|import net.minecraft.client.input.KeyEvent;
//|import net.minecraft.client.input.MouseButtonEvent;
//|import net.minecraft.client.input.PreeditEvent;
// @target else
// @target end
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
// @target if minecraft_version=26.1.2,26.2
//|import org.jetbrains.annotations.Nullable;
// @target else
// @target end
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
// @target if minecraft_version=1.20.1,26.1.2,26.2
// @target else
    private static final float visualCreativeTabEditor$OVERLAY_Z = 500.0F;
    @Unique
// @target end
    private CreativeTabEditorController visualCreativeTabEditor$editor;
    @Unique
    private @Nullable EditBox visualCreativeTabEditor$titleEditor;
// @target if minecraft_version=1.20.1
//|    @Unique
//|    private boolean visualCreativeTabEditor$loggedOverlayDepth;
// @target elif minecraft_version=26.1.2,26.2
// @target else
    @Unique
    private boolean visualCreativeTabEditor$renderLayerTraceLogged;
// @target end

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
// @target if minecraft_version=26.1.2,26.2
//|            MouseButtonEvent event,
//|            boolean doubleClick,
// @target else
            double mouseX,
            double mouseY,
            int button,
// @target end
            CallbackInfoReturnable<Boolean> callback
    ) {
// @target if minecraft_version=26.1.2,26.2
//|        if (this.visualCreativeTabEditor$editor().mouseClicked(event, doubleClick)) {
// @target else
        if (this.visualCreativeTabEditor$editor().mouseClicked(mouseX, mouseY, button)) {
// @target end
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$slotClicked(
            @Nullable Slot slot,
            int slotId,
            int button,
            ClickType input,
            CallbackInfo callback
    ) {
        if (this.visualCreativeTabEditor$editor().slotClicked(slot, slotId, button, input)) {
            callback.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseDragged(
// @target if minecraft_version=26.1.2,26.2
//|            MouseButtonEvent event,
// @target else
            double mouseX,
            double mouseY,
            int button,
// @target end
            double deltaX,
            double deltaY,
            CallbackInfoReturnable<Boolean> callback
    ) {
// @target if minecraft_version=26.1.2,26.2
//|        if (this.visualCreativeTabEditor$editor().mouseDragged(event, deltaX, deltaY)) {
// @target else
        if (this.visualCreativeTabEditor$editor().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
// @target end
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseScrolled(
            double mouseX,
            double mouseY,
// @target if minecraft_version=1.20.1
//|            double delta,
// @target else
            double horizontal,
            double vertical,
// @target end
            CallbackInfoReturnable<Boolean> callback
    ) {
// @target if minecraft_version=1.20.1
//|        if (this.visualCreativeTabEditor$editor().mouseScrolled(mouseX, mouseY, delta)) {
// @target else
        if (this.visualCreativeTabEditor$editor().mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
// @target end
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseReleasedHead(
// @target if minecraft_version=26.1.2,26.2
//|            MouseButtonEvent event,
// @target else
            double mouseX,
            double mouseY,
            int button,
// @target end
            CallbackInfoReturnable<Boolean> callback
    ) {
// @target if minecraft_version=26.1.2,26.2
//|        if (this.visualCreativeTabEditor$editor().mouseReleased(event)) {
// @target else
        if (this.visualCreativeTabEditor$editor().mouseReleased(mouseX, mouseY, button)) {
// @target end
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void visualCreativeTabEditor$mouseReleasedReturn(
// @target if minecraft_version=26.1.2,26.2
//|            MouseButtonEvent event,
// @target else
            double mouseX,
            double mouseY,
            int button,
// @target end
            CallbackInfoReturnable<Boolean> callback
    ) {
// @target if minecraft_version=26.1.2,26.2
//|        this.visualCreativeTabEditor$editor().afterMouseReleased(event);
// @target else
        this.visualCreativeTabEditor$editor().afterMouseReleased(mouseX, mouseY, button);
// @target end
    }

    @Override
    public void visualCreativeTabEditor$renderEditorOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
// @target if minecraft_version=1.20.1
//|        PoseStack pose = graphics.pose();
//|        float baseZ = pose.last().pose().m32();
//|        graphics.flush();
//|        pose.pushPose();
//|        pose.translate(0.0F, 0.0F, 500.0F);
//|        if (!this.visualCreativeTabEditor$loggedOverlayDepth) {
//|            this.visualCreativeTabEditor$loggedOverlayDepth = true;
//|            VisualCreativeTabEditorConstants.LOGGER.debug(
//|                    "[EditorTrace] overlay-depth baseZ={} overlayZ={} flush=true",
//|                    baseZ,
//|                    pose.last().pose().m32()
//|            );
//|        }
//|        try {
//|            this.visualCreativeTabEditor$editor().render(graphics, mouseX, mouseY, partialTick);
//|        } finally {
//|            graphics.flush();
//|            pose.popPose();
//|        }
// @target elif minecraft_version=26.1.2,26.2
//|        this.visualCreativeTabEditor$editor().extractRenderState(graphics, mouseX, mouseY, partialTick);
// @target else
        CreativeTabEditorController editor = this.visualCreativeTabEditor$editor();
        if (!editor.prepareRender(mouseX, mouseY)) {
            this.visualCreativeTabEditor$renderLayerTraceLogged = false;
            return;
        }

        float basePoseZ = graphics.pose().last().pose().m32();
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, 0.0F, visualCreativeTabEditor$OVERLAY_Z);
            float overlayPoseZ = graphics.pose().last().pose().m32();
            if (!this.visualCreativeTabEditor$renderLayerTraceLogged) {
                VisualCreativeTabEditorConstants.LOGGER.debug(
                        "[EditorTrace] render-layer-pass basePoseZ={} overlayPoseZ={} translation={} "
                                + "vanillaTooltipZ=400 flushBefore=true flushAfter=true",
                        basePoseZ,
                        overlayPoseZ,
                        visualCreativeTabEditor$OVERLAY_Z
                );
                this.visualCreativeTabEditor$renderLayerTraceLogged = true;
            }
            editor.renderOverlay(graphics, mouseX, mouseY, partialTick);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
// @target end
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

// @target if minecraft_version=26.1.2,26.2
//|    @Inject(method = "extractBackground", at = @At("HEAD"))
// @target else
    @Inject(method = "renderBg", at = @At("HEAD"))
// @target end
    private void visualCreativeTabEditor$arrangeDataDrivenTabs(
            GuiGraphics graphics,
// @target if minecraft_version=26.1.2,26.2
// @target else
            float partialTick,
// @target end
            int mouseX,
            int mouseY,
// @target if minecraft_version=26.1.2,26.2
//|            float partialTick,
// @target else
// @target end
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
// @target if minecraft_version=1.20.1
//|    }
//|
//|    @Redirect(
//|            method = "renderBg",
//|            at = @At(
//|                    value = "INVOKE",
//|                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
//|                    ordinal = 0
//|            )
//|    )
//|    private void visualCreativeTabEditor$renderDataDrivenBackground(
//|            GuiGraphics graphics,
//|            ResourceLocation nativeBackground,
//|            int x,
//|            int y,
//|            int u,
//|            int v,
//|            int width,
//|            int height
//|    ) {
//|        ResourceLocation background = CreativeTabRuntime.definition(this.visualCreativeTabEditor$selectedTab())
//|                .map(definition -> definition.layout().background())
//|                .orElse(nativeBackground);
//|        graphics.blit(background, x, y, u, v, width, height);
// @target else
// @target end
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
        Map<net.minecraft.resources.ResourceLocation, Integer> configuredOrder = new HashMap<>();
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
            GuiGraphics graphics,
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
// @target if minecraft_version=26.1.2,26.2
//|    private void visualCreativeTabEditor$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
//|        if (this.visualCreativeTabEditor$editor().keyPressed(event)) {
// @target else
    private void visualCreativeTabEditor$keyPressed(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (this.visualCreativeTabEditor$editor().keyPressed(keyCode, scanCode, modifiers)) {
// @target end
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
// @target if minecraft_version=26.1.2,26.2
//|    private void visualCreativeTabEditor$charTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> callback) {
//|        if (this.visualCreativeTabEditor$editor().charTyped(event)) {
//|            callback.setReturnValue(true);
//|        }
//|    }
//|
//|    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
//|    private void visualCreativeTabEditor$preeditUpdated(
//|            @Nullable PreeditEvent event,
// @target else
    private void visualCreativeTabEditor$charTyped(
            char codePoint,
            int modifiers,
// @target end
            CallbackInfoReturnable<Boolean> callback
    ) {
// @target if minecraft_version=26.1.2,26.2
//|        if (this.visualCreativeTabEditor$editor().preeditUpdated(event)) {
// @target else
        if (this.visualCreativeTabEditor$editor().charTyped(codePoint, modifiers)) {
// @target end
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
// @target if minecraft_version=26.1.2,26.2
//|        editor.setTextShadow(false);
// @target else
        com.kltyton.visual_creative_tab_editor.client.CreativeTabTextInput.disableShadow(editor);
// @target end
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
// @target if minecraft_version=26.1.2,26.2
//|    public void visualCreativeTabEditor$extractTitleEditor(
// @target else
    public void visualCreativeTabEditor$renderTitleEditor(
// @target end
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (this.visualCreativeTabEditor$titleEditor != null && this.visualCreativeTabEditor$titleEditor.visible) {
// @target if minecraft_version=26.1.2,26.2
//|            this.visualCreativeTabEditor$titleEditor.extractRenderState(graphics, mouseX, mouseY, partialTick);
// @target else
            this.visualCreativeTabEditor$titleEditor.render(graphics, mouseX, mouseY, partialTick);
// @target end
        }
    }

    @Override
// @target if minecraft_version=26.1.2,26.2
//|    public boolean visualCreativeTabEditor$titleEditorKeyPressed(KeyEvent event) {
// @target else
    public boolean visualCreativeTabEditor$titleEditorKeyPressed(int keyCode, int scanCode, int modifiers) {
// @target end
        return this.visualCreativeTabEditor$titleEditor != null
// @target if minecraft_version=26.1.2,26.2
//|                && this.visualCreativeTabEditor$titleEditor.keyPressed(event);
// @target else
                && this.visualCreativeTabEditor$titleEditor.keyPressed(keyCode, scanCode, modifiers);
// @target end
    }

    @Override
// @target if minecraft_version=26.1.2,26.2
//|    public boolean visualCreativeTabEditor$titleEditorCharTyped(CharacterEvent event) {
// @target else
    public boolean visualCreativeTabEditor$titleEditorCharTyped(char codePoint, int modifiers) {
// @target end
        return this.visualCreativeTabEditor$titleEditor != null
// @target if minecraft_version=26.1.2,26.2
//|                && this.visualCreativeTabEditor$titleEditor.charTyped(event);
//|    }
//|
//|    @Override
//|    public boolean visualCreativeTabEditor$titleEditorPreeditUpdated(@Nullable PreeditEvent event) {
//|        return this.visualCreativeTabEditor$titleEditor != null
//|                && this.visualCreativeTabEditor$titleEditor.preeditUpdated(event);
// @target else
                && this.visualCreativeTabEditor$titleEditor.charTyped(codePoint, modifiers);
// @target end
    }

    @Override
    public boolean visualCreativeTabEditor$titleEditorMouseDragged(
// @target if minecraft_version=26.1.2,26.2
//|            MouseButtonEvent event,
// @target else
            double mouseX,
            double mouseY,
            int button,
// @target end
            double deltaX,
            double deltaY
    ) {
        return this.visualCreativeTabEditor$titleEditor != null
                && this.isDragging()
                && this.getFocused() == this.visualCreativeTabEditor$titleEditor
// @target if minecraft_version=26.1.2,26.2
//|                && this.visualCreativeTabEditor$titleEditor.mouseDragged(event, deltaX, deltaY);
// @target else
                && this.visualCreativeTabEditor$titleEditor.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
// @target end
    }

    @Override
// @target if minecraft_version=26.1.2,26.2
//|    public boolean visualCreativeTabEditor$titleEditorMouseReleased(MouseButtonEvent event) {
// @target else
    public boolean visualCreativeTabEditor$titleEditorMouseReleased(double mouseX, double mouseY, int button) {
// @target end
        boolean titleWasDragging = this.isDragging()
                && this.getFocused() == this.visualCreativeTabEditor$titleEditor;
        this.setDragging(false);
        return titleWasDragging
                && this.visualCreativeTabEditor$titleEditor != null
// @target if minecraft_version=26.1.2,26.2
//|                && this.visualCreativeTabEditor$titleEditor.mouseReleased(event);
// @target else
                && this.visualCreativeTabEditor$titleEditor.mouseReleased(mouseX, mouseY, button);
// @target end
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
    public void visualCreativeTabEditor$replaySlotClick(Slot slot, int slotId, int button, ClickType input) {
        this.slotClicked(slot, slotId, button, input);
    }

    @Override
    public void visualCreativeTabEditor$refreshScreen() {
        CreativeTabClientPlatform.refreshScreen((CreativeModeInventoryScreen) (Object) this);
    }
}
