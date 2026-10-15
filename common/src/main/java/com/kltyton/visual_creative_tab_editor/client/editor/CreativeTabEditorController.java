package com.kltyton.visual_creative_tab_editor.client.editor;

import com.kltyton.visual_creative_tab_editor.client.config.CreativeTabClientConfig;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGroup;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabCatalog;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabType;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabValidation;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
// @target if minecraft_version=1.20.1
//|import com.mojang.blaze3d.systems.RenderSystem;
// @target else
// @target end
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
// @target if minecraft_version=1.20.1
//|import javax.annotation.Nullable;
//|import net.minecraft.Util;
// @target elif minecraft_version=26.1.2,26.2
// @target else
import org.jetbrains.annotations.Nullable;
// @target end
import net.minecraft.client.gui.GuiGraphics;
// @target if minecraft_version=26.1.2,26.2
//|import net.minecraft.client.input.CharacterEvent;
//|import net.minecraft.client.input.KeyEvent;
//|import net.minecraft.client.input.MouseButtonEvent;
//|import net.minecraft.client.input.PreeditEvent;
//|import net.minecraft.client.renderer.RenderPipelines;
// @target else
// @target end
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
// @target if minecraft_version=1.20.1
// @target else
import net.minecraft.Util;
// @target end
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
// @target if minecraft_version=26.1.2,26.2
// @target else
import net.minecraft.world.item.Item;
// @target end
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
// @target if minecraft_version=26.1.2,26.2
//|import net.minecraft.world.item.Item;
//|import org.jetbrains.annotations.Nullable;
// @target else
// @target end
import org.lwjgl.glfw.GLFW;

/** State machine for the phone-home-screen style creative-tab editor. */
public final class CreativeTabEditorController {
    private static final long LONG_PRESS_MILLIS = 500L;
    private static final long ITEM_DRAG_TAB_HOVER_MILLIS = 450L;
    private static final long PAGE_TURN_HOVER_MILLIS = 500L;
    private static final double DRAG_DISTANCE_SQUARED = 25.0D;
    private static final double LONG_PRESS_SLOP_SQUARED = 144.0D;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 44;
    private static final int ORDINARY_TABS_PER_PAGE = 10;
    private static final int ORDINARY_TABS_PER_ROW = 5;
// @target if minecraft_version=1.20.1
//|    private static final ResourceLocation CREATIVE_TABS_LOCATION = new ResourceLocation(
//|            "textures/gui/container/creative_inventory/tabs.png"
//|    );
// @target else
// @target end
    private static final Set<ResourceLocation> COMMON_TAB_IDS = Set.of(
// @target if minecraft_version=1.20.1
//|            new ResourceLocation("hotbar"),
//|            new ResourceLocation("search"),
//|            new ResourceLocation("op_blocks"),
//|            new ResourceLocation("inventory")
// @target else
            ResourceLocation.withDefaultNamespace("hotbar"),
            ResourceLocation.withDefaultNamespace("search"),
            ResourceLocation.withDefaultNamespace("op_blocks"),
            ResourceLocation.withDefaultNamespace("inventory")
// @target end
    );
    private static final List<ResourceLocation> SEMANTIC_CATEGORY_ORDER = List.of(
// @target if minecraft_version=1.20.1
//|            new ResourceLocation("building_blocks"),
//|            new ResourceLocation("colored_blocks"),
//|            new ResourceLocation("natural_blocks"),
//|            new ResourceLocation("functional_blocks"),
//|            new ResourceLocation("redstone_blocks"),
//|            new ResourceLocation("tools_and_utilities"),
//|            new ResourceLocation("combat"),
//|            new ResourceLocation("food_and_drinks"),
//|            new ResourceLocation("ingredients"),
//|            new ResourceLocation("spawn_eggs")
// @target else
            ResourceLocation.withDefaultNamespace("building_blocks"),
            ResourceLocation.withDefaultNamespace("colored_blocks"),
            ResourceLocation.withDefaultNamespace("natural_blocks"),
            ResourceLocation.withDefaultNamespace("functional_blocks"),
            ResourceLocation.withDefaultNamespace("redstone_blocks"),
            ResourceLocation.withDefaultNamespace("tools_and_utilities"),
            ResourceLocation.withDefaultNamespace("combat"),
            ResourceLocation.withDefaultNamespace("food_and_drinks"),
            ResourceLocation.withDefaultNamespace("ingredients"),
            ResourceLocation.withDefaultNamespace("spawn_eggs")
// @target end
    );

    private final CreativeTabEditorHost host;
    private final CreativeTabFolderBrowser folders;
    private Mode mode = Mode.NORMAL;
    private @Nullable CreativeTabDraft draft;
    private @Nullable CreativeTabHistory history;
    private boolean dragCheckpointed;
    private @Nullable Press press;
    private @Nullable ResourceLocation contextTabId;
    private int contextItemIndex = -1;
    private int contextX;
    private int contextY;
    private int contextTargetX;
    private int contextTargetY;
    private @Nullable CreativeModeTab pickerReturnTab;
    private int pickerReturnScrollRow;
    private @Nullable ResourceLocation editorEntryTabId;
    private boolean replayingSlotClick;
    private boolean nativeReleaseCleanup;
    private boolean nativeReleaseAllowsAction;
// @target if minecraft_version=26.1.2,26.2
//|    private boolean currentClickDouble;
// @target else
// @target end
    private @Nullable Press pendingSlotReplay;
    private @Nullable Mode saveReturnMode;
    private @Nullable String titleEditorInitialValue;
    private @Nullable DragVisual dragVisual;
    private @Nullable CreativeModeTab pendingNativeTabSelection;
    private @Nullable CreativeModeTab nativeTabSelectionBefore;
    private @Nullable ResourceLocation itemDragHoverTabId;
    private long itemDragHoverStartedAt;
    private boolean itemDragHoverBlockedLogged;
    private long draftRevision = -1L;
    private int lastDragTarget = -1;
    private int pageHoverDirection;
    private int pageHoverOrigin = -1;
    private long pageHoverStartedAt;
    private boolean pageHoverBlockedLogged;
    private boolean pageHoverTurnConsumed;
    private boolean virtualTailEnabled;
    private double pointerX;
    private double pointerY;
    private @Nullable String contextGroupId;
    private @Nullable String contextMemberGroupId;
    private ItemStack contextGroupIcon = ItemStack.EMPTY;
    private @Nullable String focusedGroupId;
    private @Nullable ResourceLocation focusedGroupTabId;
    private Component groupControlTitle = Component.empty();

    public CreativeTabEditorController(CreativeTabEditorHost host) {
        this.host = Objects.requireNonNull(host, "host");
        this.folders = new CreativeTabFolderBrowser(host, this::pickFolderItem);
    }

    private void pickFolderItem(int originalIndex, int button, boolean quickMove) {
        var menu = this.host.visualCreativeTabEditor$menu();
        CreativeItemGroups groups = itemGroups();
        int displayIndex = groups.reveal(originalIndex, menu.items);
        int maxRow = Math.max(0, Mth.positiveCeilDiv(groups.size(), 9) - 5);
        int row = Mth.clamp(displayIndex / 9, 0, maxRow);
        float scroll = maxRow == 0 ? 0.0F : menu.getScrollForRowIndex(row);
        this.host.visualCreativeTabEditor$setScrollOffset(scroll);
        menu.scrollTo(scroll);
        Slot slot = menu.slots.get(displayIndex - row * 9);
        if (!this.host.visualCreativeTabEditor$isCreativeSlot(slot) || !slot.hasItem()) { return; }
        this.replayingSlotClick = true;
        try {
            this.host.visualCreativeTabEditor$replaySlotClick(slot, slot.index, button,
                    quickMove ? ClickType.QUICK_MOVE : ClickType.PICKUP);
        } finally { this.replayingSlotClick = false; }
    }

    public void onInit() {
        CreativeTabClientState.initializeLocalIfNeeded();
        if (isEditing() && this.draft != null) {
            if (!isPicker()) {
                syncDraftCurrentTabToScreen("screen-init");
            }
            preview();
        }
        updateVirtualTail("screen-init");
        if (this.mode == Mode.TAB_PROPERTIES) {
            this.host.visualCreativeTabEditor$showTitleEditor(
                    this.contextGroupId == null ? draft().definition(requireContextTab()).orElseThrow().title().getString()
                            : draft().itemGroup(this.contextGroupId).title().getString()
            );
        }
    }

// @target if minecraft_version=26.1.2,26.2
//|    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
// @target else
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
// @target end
        this.dragCheckpointed = false;
// @target if minecraft_version=26.1.2,26.2
//|        this.pointerX = event.x();
//|        this.pointerY = event.y();
//|        this.currentClickDouble = doubleClick;
//|        if (this.mode == Mode.NORMAL && this.folders.click(event.x(), event.y(), event.button(), event.hasShiftDown())) {
// @target else
        this.pointerX = mouseX;
        this.pointerY = mouseY;
        if (this.mode == Mode.NORMAL && this.folders.click(mouseX, mouseY, button, net.minecraft.client.gui.screens.Screen.hasShiftDown())) {
// @target end
            this.press = null;
            return true;
        }
// @target if minecraft_version=26.1.2,26.2
//|        if (event.button() != 0) {
// @target else
        if (button != 0) {
// @target end
            return isModal();
        }
        if ((this.mode == Mode.NORMAL || this.mode == Mode.EDIT || isPicker())
                && beginItemGroupPress(this.pointerX, this.pointerY)) {
            return true;
        }
        if (this.mode == Mode.NORMAL) {
            if (!CreativeTabClientState.canEdit()) {
// @target if minecraft_version=26.1.2,26.2
//|                trace("mouse-down rejected mode={} x={} y={} reason=no-edit-permission", this.mode, event.x(), event.y());
// @target else
                trace("mouse-down rejected mode={} x={} y={} reason=no-edit-permission", this.mode, mouseX, mouseY);
// @target end
                return false;
            }
// @target if minecraft_version=26.1.2,26.2
//|            CreativeModeTab tab = tabAt(event.x(), event.y());
//|            boolean inTabStrip = isInTabStrip(event.x(), event.y());
//|            boolean blankFrame = tab == null && isBlankEditorEntryArea(event.x(), event.y());
// @target else
            CreativeModeTab tab = tabAt(mouseX, mouseY);
            boolean inTabStrip = isInTabStrip(mouseX, mouseY);
            boolean blankFrame = tab == null && isBlankEditorEntryArea(mouseX, mouseY);
// @target end
            trace(
                    "mouse-down mode={} x={} y={} tab={} tabShouldDisplay={} inTabStrip={} blankFrame={} pageTabs={} left={} top={} image={}x{}",
                    this.mode,
// @target if minecraft_version=26.1.2,26.2
//|                    event.x(),
//|                    event.y(),
// @target else
                    mouseX,
                    mouseY,
// @target end
                    tabId(tab),
                    tab == null ? "-" : tab.shouldDisplay(),
                    inTabStrip,
                    blankFrame,
                    CreativeTabClientPlatform.visibleTabs(this.host.visualCreativeTabEditor$screen()).size(),
                    this.host.visualCreativeTabEditor$left(),
                    this.host.visualCreativeTabEditor$top(),
                    this.host.visualCreativeTabEditor$imageWidth(),
                    this.host.visualCreativeTabEditor$imageHeight()
            );
            if (tab != null) {
// @target if minecraft_version=26.1.2,26.2
//|                this.press = Press.tab(PressKind.NORMAL_TAB, tab, event.x(), event.y());
// @target else
                this.press = Press.tab(PressKind.NORMAL_TAB, tab, mouseX, mouseY);
// @target end
                tracePressStart(this.press);
                trace("tab-click-down delegated-to-native tab={}", tabId(tab));
                return false;
            }
            if (blankFrame) {
// @target if minecraft_version=26.1.2,26.2
//|                this.press = Press.blank(event.x(), event.y());
// @target else
                this.press = Press.blank(mouseX, mouseY);
// @target end
                tracePressStart(this.press);
                return true;
            }
            return false;
        }
        if (this.mode == Mode.SAVING) {
            return true;
        }
        if (this.mode == Mode.CONFLICT) {
// @target if minecraft_version=26.1.2,26.2
//|            if (buttonAt(event.x(), event.y(), cancelRect())) {
// @target else
            if (buttonAt(mouseX, mouseY, cancelRect())) {
// @target end
                cancelAndExit();
            }
            return true;
        }
// @target if minecraft_version=26.1.2,26.2
//|        if (isModal() && buttonAt(event.x(), event.y(), saveRect())) {
// @target else
        if (isModal() && buttonAt(mouseX, mouseY, saveRect())) {
// @target end
            saveAndExit();
            return true;
        }
// @target if minecraft_version=26.1.2,26.2
//|        if (isModal() && buttonAt(event.x(), event.y(), cancelRect())) {
// @target else
        if (isModal() && buttonAt(mouseX, mouseY, cancelRect())) {
// @target end
            cancelAndExit();
            return true;
        }
        if (isPicker()) {
// @target if minecraft_version=26.1.2,26.2
//|            if (slotAt(event.x(), event.y()) == null && !buttonAt(event.x(), event.y(), cancelRect())) {
// @target else
            if (slotAt(mouseX, mouseY) == null && !buttonAt(mouseX, mouseY, cancelRect())) {
// @target end
                saveAndExit();
                return true;
            }
            return false;
        }
        if (this.mode == Mode.GROUP_CONTROLS) {
            return clickGroupControls(this.pointerX, this.pointerY);
        }
        if (this.mode == Mode.TAB_PROPERTIES) {
// @target if minecraft_version=26.1.2,26.2
//|            return clickTabProperties(event.x(), event.y());
// @target else
            return clickTabProperties(mouseX, mouseY);
// @target end
        }
        if (this.mode == Mode.SORT_MENU) {
// @target if minecraft_version=26.1.2,26.2
//|            return clickSortMenu(event.x(), event.y());
// @target else
            return clickSortMenu(mouseX, mouseY);
// @target end
        }
        if (this.mode == Mode.CONTEXT_TAB || this.mode == Mode.CONTEXT_ITEM) {
// @target if minecraft_version=26.1.2,26.2
//|            return clickContextMenu(event.x(), event.y());
// @target else
            return clickContextMenu(mouseX, mouseY);
// @target end
        }
        if ((this.mode == Mode.EDIT || this.mode == Mode.DRAG_TABS || this.mode == Mode.DRAG_ITEMS)
// @target if minecraft_version=26.1.2,26.2
//|                && clickControl(event.x(), event.y())) {
// @target else
                && clickControl(mouseX, mouseY)) {
// @target end
            return true;
        }
        if ((this.mode == Mode.EDIT || this.mode == Mode.DRAG_TABS || this.mode == Mode.DRAG_ITEMS)
// @target if minecraft_version=26.1.2,26.2
//|                && this.host.visualCreativeTabEditor$isPageButtonAt(event.x(), event.y())) {
// @target else
                && this.host.visualCreativeTabEditor$isPageButtonAt(mouseX, mouseY)) {
// @target end
            return false;
        }
        if ((this.mode == Mode.EDIT || this.mode == Mode.DRAG_TABS || this.mode == Mode.DRAG_ITEMS)
// @target if minecraft_version=26.1.2,26.2
//|                && isCreativeScrollbarAt(event.x(), event.y())) {
// @target else
                && isCreativeScrollbarAt(mouseX, mouseY)) {
// @target end
            return false;
        }
        if (this.mode == Mode.EDIT || this.mode == Mode.DRAG_TABS || this.mode == Mode.DRAG_ITEMS) {
            TabPlusTarget tabPlus = tabPlusTarget();
            Rect itemPlus = currentType() == CreativeTabType.CATEGORY ? itemPlusRect() : null;
// @target if minecraft_version=26.1.2,26.2
//|            boolean tabPlusHit = tabPlus != null && tabPlus.hitRect().contains(event.x(), event.y());
//|            boolean itemPlusHit = itemPlus != null && itemPlus.contains(event.x(), event.y());
// @target else
            boolean tabPlusHit = tabPlus != null && tabPlus.hitRect().contains(mouseX, mouseY);
            boolean itemPlusHit = itemPlus != null && itemPlus.contains(mouseX, mouseY);
// @target end
            trace(
                    "edit-click mode={} x={} y={} tabPlusHit={} tabPlusHitRect={} tabPlusSpriteRect={} tabPlusRow={} tabPlusColumn={} tabPlusExternal={} itemPlusHit={} itemPlusRect={} currentTab={} currentItems={}",
                    this.mode,
// @target if minecraft_version=26.1.2,26.2
//|                    event.x(),
//|                    event.y(),
// @target else
                    mouseX,
                    mouseY,
// @target end
                    tabPlusHit,
                    tabPlus == null ? null : tabPlus.hitRect(),
                    tabPlus == null ? null : tabPlus.spriteRect(),
                    tabPlus == null ? null : tabPlus.row(),
                    tabPlus == null ? -1 : tabPlus.column(),
                    tabPlus != null && tabPlus.external(),
                    itemPlusHit,
                    itemPlus,
                    draft().currentTabId().orElse(null),
                    currentItemCount()
            );
            if (tabPlusHit) {
                beginPicker(Mode.PICK_NEW_TAB);
                return true;
            }
            if (itemPlusHit) {
                beginPicker(Mode.PICK_NEW_ITEM);
                return true;
            }
// @target if minecraft_version=26.1.2,26.2
//|            CreativeModeTab tab = tabAt(event.x(), event.y());
// @target else
            CreativeModeTab tab = tabAt(mouseX, mouseY);
// @target end
            if (tab != null) {
                ResourceLocation id = CreativeTabRuntime.id(tab).orElse(null);
                if (id != null) {
// @target if minecraft_version=26.1.2,26.2
//|                    this.press = Press.tab(PressKind.EDIT_TAB, tab, event.x(), event.y());
// @target else
                    this.press = Press.tab(PressKind.EDIT_TAB, tab, mouseX, mouseY);
// @target end
                    this.contextTabId = id;
                    tracePressStart(this.press);
                    return true;
                }
            }
// @target if minecraft_version=26.1.2,26.2
//|            Slot slot = creativeSlotAt(event.x(), event.y());
// @target else
            Slot slot = creativeSlotAt(mouseX, mouseY);
// @target end
            if (slot != null && slot.hasItem() && canReorderCurrentItems()) {
                ensureSearchOrderPrepared(this.host.visualCreativeTabEditor$selectedTab(), "edit-item-press");
                int itemIndex = itemIndexForSlot(slot);
                int draftItems = currentItemCount();
                boolean accepted = itemIndex >= 0 && itemIndex < draftItems;
                trace(
                        "item-press-map type={} slotIndex={} mappedIndex={} menuItems={} visibleItems={} draftItems={} accepted={} reason={}",
                        currentType(),
                        slot.index,
                        itemIndex,
                        this.host.visualCreativeTabEditor$menu().items.size(),
                        this.host.visualCreativeTabEditor$selectedTab().getDisplayItems().size(),
                        draftItems,
                        accepted,
                        accepted ? "ok" : "outside-persistable-search-prefix"
                );
                if (accepted) {
                    this.contextItemIndex = itemIndex;
// @target if minecraft_version=26.1.2,26.2
//|                    this.press = Press.item(PressKind.EDIT_ITEM, slot, itemIndex, event.x(), event.y());
// @target else
                    this.press = Press.item(PressKind.EDIT_ITEM, slot, itemIndex, mouseX, mouseY);
// @target end
                    tracePressStart(this.press);
                    return true;
                }
                return true;
            }
            saveAndExit();
            return true;
        }
        return isEditing();
    }

    public boolean slotClicked(@Nullable Slot slot, int slotId, int button, ClickType input) {
        if (this.folders.isOpen()) { return true; }
        boolean creativeSlot = slot != null && this.host.visualCreativeTabEditor$isCreativeSlot(slot);
        if (creativeSlot && itemGroups().isHeader(displayItemIndex(slot))) {
            return true;
        }
        ItemStack carried = this.host.visualCreativeTabEditor$menu().getCarried();
        trace(
                "slot-click-enter mode={} replaying={} releaseCleanup={} releaseAllows={} slotId={} slotIndex={} button={} input={} creativeSlot={} slotStack={} carried={}",
                this.mode,
                this.replayingSlotClick,
                this.nativeReleaseCleanup,
                this.nativeReleaseAllowsAction,
                slotId,
                slot == null ? -1 : slot.index,
                button,
                input,
                creativeSlot,
                stackId(slot == null ? ItemStack.EMPTY : slot.getItem()),
                stackId(carried)
        );
        if (this.replayingSlotClick) {
            trace("slot-click-delegate reason=replay");
            return false;
        }
        if (this.nativeReleaseCleanup) {
            trace("slot-click-release-phase delegated={} carried={}", this.nativeReleaseAllowsAction, stackId(this.host.visualCreativeTabEditor$menu().getCarried()));
            return !this.nativeReleaseAllowsAction;
        }
        if (this.mode == Mode.NORMAL
                && CreativeTabClientState.canEdit()
                && button == 0
                && input == ClickType.PICKUP
                && creativeSlot
                && slot != null
                && slot.hasItem()
                && !carried.isEmpty()) {
            trace(
                    "slot-click-delegate reason=non-empty-carried-preserve-vanilla slotId={} slotStack={} carried={}",
                    slotId,
                    stackId(slot.getItem()),
                    stackId(carried)
            );
            return false;
        }
        if (isPicker()) {
            if (slot != null
                    && slot != this.host.visualCreativeTabEditor$destroyItemSlot()
                    && slot.isActive()
                    && !this.host.visualCreativeTabEditor$isCreativeSlot(slot)
                    && slot.hasItem()) {
                acceptPickedItem(slot.getItem());
            }
            return true;
        }
        if (this.mode == Mode.NORMAL
                && CreativeTabClientState.canEdit()
                && button == 0
                && input == ClickType.PICKUP
                && slot != null
                && this.host.visualCreativeTabEditor$isCreativeSlot(slot)
                && slot.hasItem()
                && carried.isEmpty()) {
            int absolute = absoluteItemIndex(slot);
            this.press = Press.slot(
                    PressKind.NORMAL_ITEM,
                    slot,
                    slotId,
                    button,
                    input,
                    absolute,
                    this.pointerX,
// @target if minecraft_version=26.1.2,26.2
//|                    this.pointerY,
//|                    this.currentClickDouble
// @target else
                    this.pointerY
// @target end
            );
            tracePressStart(this.press);
            trace(
                    "slot-click-intercept reason=long-press-candidate slotId={} slotStack={} carried={}",
                    slotId,
                    stackId(slot.getItem()),
                    stackId(this.host.visualCreativeTabEditor$menu().getCarried())
            );
            return true;
        }
        return isEditing();
    }

// @target if minecraft_version=26.1.2,26.2
//|    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
// @target else
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY
    ) {
// @target end
        if (this.folders.isOpen()) { return true; }
        Press active = this.press;
        if (active != null
// @target if minecraft_version=26.1.2,26.2
//|                && event.button() == 0
// @target else
                && button == 0
// @target end
                && !active.longTriggered
                && !active.moved
                && (active.kind == PressKind.NORMAL_TAB
                        || active.kind == PressKind.NORMAL_ITEM
                        || active.kind == PressKind.BLANK_TAB)
                && Util.getMillis() - active.startedAt >= LONG_PRESS_MILLIS
                && active.distanceSquared(this.pointerX, this.pointerY) < LONG_PRESS_SLOP_SQUARED) {
            trace(
                    "long-press-before-drag mode={} kind={} elapsedMs={} lastPointer=({}, {}) nextPointer=({}, {})",
                    this.mode,
                    active.kind,
                    Util.getMillis() - active.startedAt,
                    this.pointerX,
                    this.pointerY,
// @target if minecraft_version=26.1.2,26.2
//|                    event.x(),
//|                    event.y()
// @target else
                    mouseX,
                    mouseY
// @target end
            );
            triggerLongPress();
        }
// @target if minecraft_version=26.1.2,26.2
//|        this.pointerX = event.x();
//|        this.pointerY = event.y();
// @target else
        this.pointerX = mouseX;
        this.pointerY = mouseY;
// @target end
        if (this.mode == Mode.TAB_PROPERTIES) {
// @target if minecraft_version=26.1.2,26.2
//|            this.host.visualCreativeTabEditor$titleEditorMouseDragged(event, deltaX, deltaY);
// @target else
            this.host.visualCreativeTabEditor$titleEditorMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
// @target end
            return true;
        }
        active = this.press;
        if (active != null && active.cancelled) { return true; }
// @target if minecraft_version=26.1.2,26.2
//|        if (active == null || event.button() != 0) {
// @target else
        if (active == null || button != 0) {
// @target end
            return isModal();
        }
// @target if minecraft_version=26.1.2,26.2
//|        double distanceSquared = active.distanceSquared(event.x(), event.y());
// @target else
        double distanceSquared = active.distanceSquared(mouseX, mouseY);
// @target end
        if (distanceSquared < DRAG_DISTANCE_SQUARED) {
            return true;
        }
        if (!active.dragThresholdLogged) {
            active.dragThresholdLogged = true;
            trace(
                    "drag-threshold mode={} kind={} longTriggered={} moved={} distanceSq={} press=({}, {}) pointer=({}, {})",
                    this.mode,
                    active.kind,
                    active.longTriggered,
                    active.moved,
                    distanceSquared,
                    active.x,
                    active.y,
// @target if minecraft_version=26.1.2,26.2
//|                    event.x(),
//|                    event.y()
// @target else
                    mouseX,
                    mouseY
// @target end
            );
        }
        boolean normalPress = active.kind == PressKind.NORMAL_TAB
                || active.kind == PressKind.NORMAL_ITEM
                || active.kind == PressKind.BLANK_TAB;
        if (normalPress && !active.longTriggered) {
            if (distanceSquared < LONG_PRESS_SLOP_SQUARED) {
                return true;
            }
            active.moved = true;
            if (active.kind == PressKind.NORMAL_ITEM && active.groupId == null) {
                trace(
                        "slot-long-press-abort-to-native-drag distanceSq={} carriedBefore={}",
                        distanceSquared,
                        stackId(this.host.visualCreativeTabEditor$menu().getCarried())
                );
                this.press = null;
                replay(active);
                return false;
            }
            if (!active.dragBlockedLogged) {
                active.dragBlockedLogged = true;
                trace(
                        "drag-blocked mode={} kind={} longTriggered={} action=mark-moved-and-return",
                        this.mode,
                        active.kind,
                        active.longTriggered
                );
            }
            return true;
        }
        if (active.kind == PressKind.BLANK_TAB) {
            return true;
        }
        if (active.kind == PressKind.EDIT_TAB
                || (active.kind == PressKind.NORMAL_TAB && active.longTriggered)) {
            ResourceLocation id = CreativeTabRuntime.id(active.tab).orElse(null);
            if (id == null) {
                return true;
            }
            if (!draft().selectedTabIds().contains(id)) {
                draft().clearTabSelection();
                draft().setTabSelected(id, true);
            }
            this.mode = Mode.DRAG_TABS;
// @target if minecraft_version=26.1.2,26.2
//|            traceDragStart(active, event, Mode.DRAG_TABS);
//|            int target = tabInsertionIndex(event.x(), event.y());
// @target else
            traceDragStart(active, mouseX, mouseY, Mode.DRAG_TABS);
            int target = tabInsertionIndex(mouseX, mouseY);
// @target end
            if (target >= 0 && target != this.lastDragTarget) {
                checkpoint();
                draft().moveSelectedTabs(target);
                this.lastDragTarget = target;
                preview();
                this.host.visualCreativeTabEditor$refreshScreen();
            }
            return true;
        }
        if ((active.kind == PressKind.EDIT_ITEM
                || (active.kind == PressKind.NORMAL_ITEM && active.longTriggered))
                && canReorderCurrentItems()) {
            if (this.mode != Mode.DRAG_ITEMS && active.groupId != null) {
                if (!draft().isItemGroupSelected(active.groupId)) {
                    draft().clearItemSelection();
                    draft().selectItemGroup(active.groupId, true);
                }
                active.itemIndex = draft().selectedItemIndices().stream().findFirst().orElse(-1);
            }
            if (active.itemIndex < 0) {
                if (!active.dragBlockedLogged) {
                    active.dragBlockedLogged = true;
                    trace(
                            "drag-dispatch type={} kind={} selectedItems={} target=-1 blockedReason=outside-persistable-search-prefix menuItems={} visibleItems={}",
                            currentType(),
                            active.kind,
                            draft().selectedItemIndices().size(),
                            this.host.visualCreativeTabEditor$menu().items.size(),
                            this.host.visualCreativeTabEditor$selectedTab().getDisplayItems().size()
                    );
                }
                return true;
            }
            if (this.mode != Mode.DRAG_ITEMS && !draft().selectedItemIndices().contains(active.itemIndex)) {
                draft().clearItemSelection();
                draft().setItemSelected(active.itemIndex, true);
            }
            if (this.mode != Mode.DRAG_ITEMS) {
                active.hadGroupedItems = CreativeTabClientConfig.groupSimilarItems() && draft().hasSelectedGroupedItems();
                active.originGroupId = CreativeTabClientConfig.groupSimilarItems() ? draft().itemGroupId(active.itemIndex) : null;
            }
            this.mode = Mode.DRAG_ITEMS;
// @target if minecraft_version=26.1.2,26.2
//|            traceDragStart(active, event, Mode.DRAG_ITEMS);
//|            if (handleItemDragTabHover(event.x(), event.y())) {
// @target else
            traceDragStart(active, mouseX, mouseY, Mode.DRAG_ITEMS);
            if (handleItemDragTabHover(mouseX, mouseY)) {
// @target end
                return true;
            }
// @target if minecraft_version=26.1.2,26.2
//|            int target = itemInsertionIndex(event.x(), event.y());
// @target else
            int target = itemInsertionIndex(mouseX, mouseY);
// @target end
            captureItemGroupDrop(active);
            String originGroup = active.groupId == null ? active.originGroupId : active.groupId;
            if (active.dropGroupId != null && !active.dropGroupId.equals(originGroup)) {
                return true;
            }
            if (target >= 0 && target != this.lastDragTarget) {
                int row = this.host.visualCreativeTabEditor$menu()
                        .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
                checkpoint();
                boolean moved = draft().moveSelectedItems(target);
                this.lastDragTarget = target;
                if (!moved) {
                    trace(
                            "drag-dispatch type={} kind={} selectedItems={} target={} blockedReason=search-preference-capacity persistablePrefix={} draftItems={}",
                            currentType(),
                            active.kind,
                            draft().selectedItemIndices().size(),
                            target,
                            draft().currentSearchPreferenceCapacity(),
                            currentItemCount()
                    );
                    return true;
                }
                preview();
// @target if minecraft_version=1.20.1
//|                int targetFocus = Mth.clamp(target, 0, Math.max(0, currentItemCount() - 1));
// @target else
                int targetFocus = Math.clamp(target, 0, Math.max(0, currentItemCount() - 1));
// @target end
                int focusIndex = draft().selectedItemIndices().stream()
                        .min(Comparator.comparingInt(index -> Math.abs(index - targetFocus)))
                        .orElse(-1);
                refreshCurrentDraftItems(row, focusIndex, "item-drag-reorder");
            }
            return true;
        }
        return true;
    }

// @target if minecraft_version=26.1.2,26.2
//|    public boolean mouseReleased(MouseButtonEvent event) {
// @target else
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
// @target end
        if (this.folders.isOpen()) { return true; }
// @target if minecraft_version=26.1.2,26.2
//|        this.pointerX = event.x();
//|        this.pointerY = event.y();
// @target else
        this.pointerX = mouseX;
        this.pointerY = mouseY;
// @target end
        if (this.mode == Mode.TAB_PROPERTIES && this.press == null) {
// @target if minecraft_version=26.1.2,26.2
//|            this.host.visualCreativeTabEditor$titleEditorMouseReleased(event);
// @target else
            this.host.visualCreativeTabEditor$titleEditorMouseReleased(mouseX, mouseY, button);
// @target end
            return true;
        }
// @target if minecraft_version=26.1.2,26.2
//|        if (event.button() != 0 || this.press == null) {
// @target else
        if (button != 0 || this.press == null) {
// @target end
            return isModal();
        }
        Press pendingRelease = this.press;
        long releaseElapsed = Util.getMillis() - pendingRelease.startedAt;
        if (!pendingRelease.longTriggered
                && !pendingRelease.moved
                && releaseElapsed >= LONG_PRESS_MILLIS
// @target if minecraft_version=26.1.2,26.2
//|                && pendingRelease.distanceSquared(event.x(), event.y()) < LONG_PRESS_SLOP_SQUARED) {
// @target else
                && pendingRelease.distanceSquared(mouseX, mouseY) < LONG_PRESS_SLOP_SQUARED) {
// @target end
            trace(
                    "long-press-release-fallback mode={} kind={} elapsedMs={} distanceSq={}",
                    this.mode,
                    pendingRelease.kind,
                    releaseElapsed,
// @target if minecraft_version=26.1.2,26.2
//|                    pendingRelease.distanceSquared(event.x(), event.y())
// @target else
                    pendingRelease.distanceSquared(mouseX, mouseY)
// @target end
            );
            triggerLongPress();
        }
        Press released = this.press;
        if (released.cancelled) { this.press = null; return true; }
        trace(
                "mouse-up mode={} kind={} elapsedMs={} longTriggered={} moved={} press=({}, {}) release=({}, {})",
                this.mode,
                released.kind,
                Util.getMillis() - released.startedAt,
                released.longTriggered,
                released.moved,
                released.x,
                released.y,
// @target if minecraft_version=26.1.2,26.2
//|                event.x(),
//|                event.y()
// @target else
                mouseX,
                mouseY
// @target end
        );
        if (this.mode == Mode.DRAG_ITEMS) {
// @target if minecraft_version=26.1.2,26.2
//|            handleItemDragTabHover(event.x(), event.y());
// @target else
            handleItemDragTabHover(mouseX, mouseY);
// @target end
        }
        this.press = null;
        if (this.mode == Mode.DRAG_TABS) {
// @target if minecraft_version=26.1.2,26.2
//|            boolean deleted = buttonAt(event.x(), event.y(), deleteRect());
// @target else
            boolean deleted = buttonAt(mouseX, mouseY, deleteRect());
// @target end
            if (deleted) {
                deleteTabs();
            } else if (this.lastDragTarget < 0) {
// @target if minecraft_version=26.1.2,26.2
//|                int target = tabInsertionIndex(event.x(), event.y());
// @target else
                int target = tabInsertionIndex(mouseX, mouseY);
// @target end
                if (target >= 0) {
                    checkpoint();
                    draft().moveSelectedTabs(target);
                    preview();
                    this.host.visualCreativeTabEditor$refreshScreen();
// @target if minecraft_version=26.1.2,26.2
//|                    trace("tab-drag-release-insert target={} release=({}, {})", target, event.x(), event.y());
// @target else
                    trace("tab-drag-release-insert target={} release=({}, {})", target, mouseX, mouseY);
// @target end
                }
            }
            this.mode = Mode.EDIT;
            this.lastDragTarget = -1;
            this.dragVisual = null;
            refreshSearchAfterTabMutation("tab-drag-release");
            syncDraftCurrentTabToScreen("tab-drag-release");
            updateVirtualTail("tab-drag-release");
            clearPageHover("tab-drag-release");
            return true;
        }
        if (this.mode == Mode.DRAG_ITEMS) {
// @target if minecraft_version=26.1.2,26.2
//|            if (buttonAt(event.x(), event.y(), deleteRect())) {
// @target else
            if (buttonAt(mouseX, mouseY, deleteRect())) {
// @target end
                deleteItems();
            } else {
                finishItemGroupDrop(released);
            }
            this.mode = Mode.EDIT;
            this.lastDragTarget = -1;
            this.dragVisual = null;
            clearItemDragTabHover();
            syncDraftCurrentTabToScreen("item-drag-release");
            updateVirtualTail("item-drag-release");
            clearPageHover("item-drag-release");
            return true;
        }
        if (released.groupId != null) {
            if (!released.longTriggered && !released.moved) {
                if (released.toggleGroup) {
                    if (Objects.equals(released.groupId, itemGroupAt(this.pointerX, this.pointerY))) {
                        toggleItemGroup(this.pointerX, this.pointerY);
                    }
                } else {
                    draft().toggleItemGroupSelection(released.groupId);
                }
            }
            return true;
        }
        if (released.kind == PressKind.NORMAL_ITEM) {
            this.nativeReleaseCleanup = true;
            this.nativeReleaseAllowsAction = false;
            if (!released.longTriggered && !released.moved) {
                this.pendingSlotReplay = released;
            }
            trace(
// @target if minecraft_version=26.1.2,26.2
//|                    "slot-release-decision longTriggered={} moved={} doubleClick={} nativeReleaseAllows={} replayScheduled={} carried={}",
// @target else
                    "slot-release-decision longTriggered={} moved={} nativeReleaseAllows={} replayScheduled={} carried={}",
// @target end
                    released.longTriggered,
                    released.moved,
// @target if minecraft_version=26.1.2,26.2
//|                    released.doubleClick,
// @target else
// @target end
                    this.nativeReleaseAllowsAction,
                    this.pendingSlotReplay != null,
                    stackId(this.host.visualCreativeTabEditor$menu().getCarried())
            );
            return false;
        }
        if (released.longTriggered) {
            return true;
        }
        switch (released.kind) {
            case NORMAL_TAB -> {
// @target if minecraft_version=26.1.2,26.2
//|                CreativeModeTab hit = tabAt(event.x(), event.y());
// @target else
                CreativeModeTab hit = tabAt(mouseX, mouseY);
// @target end
                CreativeModeTab before = this.host.visualCreativeTabEditor$selectedTab();
                trace(
                        "tab-click-release delegated-to-native requested={} localHit={} selectedBefore={} moved={} sameTarget={}",
                        tabId(released.tab),
                        tabId(hit),
                        tabId(before),
                        released.moved,
                        hit == released.tab
                );
                this.pendingNativeTabSelection = released.tab;
                this.nativeTabSelectionBefore = before;
                return false;
            }
            case NORMAL_ITEM -> { }
            case BLANK_TAB -> { }
            case EDIT_TAB -> CreativeTabRuntime.id(released.tab).ifPresent(draft()::toggleTabSelection);
            case EDIT_ITEM -> draft().toggleItemSelection(released.itemIndex);
        }
        return true;
    }

// @target if minecraft_version=26.1.2,26.2
//|    public void afterMouseReleased(MouseButtonEvent event) {
// @target else
    public void afterMouseReleased(double mouseX, double mouseY, int button) {
// @target end
        CreativeModeTab requestedTab = this.pendingNativeTabSelection;
        if (requestedTab != null) {
            CreativeModeTab before = this.nativeTabSelectionBefore;
            CreativeModeTab after = this.host.visualCreativeTabEditor$selectedTab();
            trace(
                    "tab-select-native-result requested={} selectedBefore={} selectedAfter={} changed={} success={} release=({}, {})",
                    tabId(requestedTab),
                    tabId(before),
                    tabId(after),
                    before != after,
                    after == requestedTab,
// @target if minecraft_version=26.1.2,26.2
//|                    event.x(),
//|                    event.y()
// @target else
                    mouseX,
                    mouseY
// @target end
            );
            this.pendingNativeTabSelection = null;
            this.nativeTabSelectionBefore = null;
        }
        if (this.nativeReleaseCleanup) {
            this.nativeReleaseCleanup = false;
            this.nativeReleaseAllowsAction = false;
            Press replay = this.pendingSlotReplay;
            this.pendingSlotReplay = null;
            if (replay != null) {
                replay(replay);
            }
        }
        if (this.mode == Mode.EDIT) {
            syncDraftCurrentTabToScreen("native-mouse-release");
        }
    }

// @target if minecraft_version=1.20.1
//|    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
// @target elif minecraft_version=26.1.2,26.2
//|    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
// @target else
    /** Updates long-press, paging and drag-hover state before deciding whether an overlay pass is needed. */
    public boolean prepareRender(int mouseX, int mouseY) {
// @target end
        this.pointerX = mouseX;
        this.pointerY = mouseY;
        triggerLongPress();
        updateVirtualTail("render");
        handlePageHover(mouseX, mouseY);
        if (this.mode == Mode.DRAG_ITEMS) {
            handleItemDragTabHover(mouseX, mouseY);
        }
// @target if minecraft_version=1.20.1,26.1.2,26.2
// @target else
        return isEditing();
    }

    /** Draws the active editor in the isolated foreground pass prepared by the screen mixin. */
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
// @target end
        if (!isEditing()) {
// @target if minecraft_version=26.1.2,26.2
//|            this.folders.render(graphics, mouseX, mouseY);
// @target else
// @target end
            return;
        }
// @target if minecraft_version=26.1.2,26.2
//|        graphics.nextStratum();
// @target else
// @target end
        if (isModal()) {
            if (isPicker()) {
                renderPickerBackdrop(graphics);
            } else {
                graphics.fill(0, 0, this.host.visualCreativeTabEditor$screenWidth(), this.host.visualCreativeTabEditor$screenHeight(), 0x99000000);
            }
        }
        if (this.mode == Mode.SAVING) {
            renderCenteredText(graphics,
                    this.host.visualCreativeTabEditor$font(),
                    Component.translatable("visual_creative_tab_editor.editor.saving"),
                    this.host.visualCreativeTabEditor$screenWidth() / 2,
                    this.host.visualCreativeTabEditor$screenHeight() / 2,
                    0xFFFFFFFF
            );
            return;
        }
        if (this.mode == Mode.CONFLICT) {
            renderCenteredText(graphics,
                    this.host.visualCreativeTabEditor$font(),
                    Component.translatable("visual_creative_tab_editor.editor.conflict"),
                    this.host.visualCreativeTabEditor$screenWidth() / 2,
                    this.host.visualCreativeTabEditor$screenHeight() / 2 - 10,
                    0xFFFFC7C7
            );
            renderButton(
                    graphics,
                    cancelRect(),
                    "visual_creative_tab_editor.editor.close_draft",
                    mouseX,
                    mouseY,
                    0xFF555555
            );
            return;
        }
        if (isPicker()) {
            renderInventoryPicker(graphics, mouseX, mouseY);
            return;
        }
        if (!isModal()) {
            renderControls(graphics, mouseX, mouseY);
            if (this.mode == Mode.EDIT || this.mode == Mode.DRAG_TABS || this.mode == Mode.DRAG_ITEMS) {
                renderTabChecks(graphics);
                TabPlusTarget tabPlus = tabPlusTarget();
                if (tabPlus != null) {
                    renderTabPlus(graphics, tabPlus, mouseX, mouseY);
                }
                renderItemChecks(graphics);
                if (currentType() == CreativeTabType.CATEGORY && screenMatchesDraftCurrentTab()) {
                    Rect itemPlus = itemPlusRect();
                    if (itemPlus != null) {
                        renderItemPlus(graphics, itemPlus);
                    }
                }
            }
        }
        if (this.mode == Mode.CONTEXT_TAB || this.mode == Mode.CONTEXT_ITEM) {
            renderContextTarget(graphics);
            renderContextMenu(graphics, mouseX, mouseY);
        } else if (this.mode == Mode.TAB_PROPERTIES) {
            renderTabProperties(graphics, mouseX, mouseY, partialTick);
        } else if (this.mode == Mode.SORT_MENU) {
            renderSortMenu(graphics, mouseX, mouseY);
        } else if (this.mode == Mode.GROUP_CONTROLS) {
            renderGroupControls(graphics, mouseX, mouseY);
        }
        if (isModal() && !isPicker()) {
            renderModalExitButtons(graphics, mouseX, mouseY);
        }
        if (this.mode == Mode.DRAG_TABS || this.mode == Mode.DRAG_ITEMS) {
            DragVisual visual = this.dragVisual;
            if (visual != null && !visual.icon.isEmpty()) {
                graphics.renderItem(
                        visual.icon,
                        Mth.floor(mouseX - visual.grabOffsetX),
                        Mth.floor(mouseY - visual.grabOffsetY)
                );
            }
        }
    }

    public boolean suppressesTooltips() {
        return isEditing() || this.folders.isOpen();
    }

    public boolean suppressesItemTooltip(@Nullable Slot hoveredSlot) {
        return this.folders.isOpen() || isEditing()
                && (!isPicker()
                || hoveredSlot == null
                || this.host.visualCreativeTabEditor$isCreativeSlot(hoveredSlot));
    }

// @target if minecraft_version=26.1.2,26.2
//|    public boolean keyPressed(KeyEvent event) {
//|        if (this.folders.isOpen()) { if (event.isEscape()) { this.folders.close(); } return true; }
//|        if (this.mode == Mode.EDIT && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
//|            if (event.key() == GLFW.GLFW_KEY_Y || (event.key() == GLFW.GLFW_KEY_Z && (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0)) {
// @target else
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.folders.isOpen()) { if (keyCode == GLFW.GLFW_KEY_ESCAPE) { this.folders.close(); } return true; }
        if (this.mode == Mode.EDIT && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (keyCode == GLFW.GLFW_KEY_Y || (keyCode == GLFW.GLFW_KEY_Z && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0)) {
// @target end
                redo();
                return true;
            }
// @target if minecraft_version=26.1.2,26.2
//|            if (event.key() == GLFW.GLFW_KEY_Z) { undo(); return true; }
// @target else
            if (keyCode == GLFW.GLFW_KEY_Z) { undo(); return true; }
// @target end
        }
        if (this.mode == Mode.GROUP_CONTROLS) {
// @target if minecraft_version=26.1.2,26.2
//|            if (event.isEscape()) {
// @target else
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
// @target end
                this.mode = Mode.EDIT;
            }
            return true;
        }
        if (this.mode != Mode.TAB_PROPERTIES) {
// @target if minecraft_version=26.1.2,26.2
//|            if (isEditing() && event.isEscape()) {
// @target else
            if (isEditing() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
// @target end
                if (this.mode != Mode.SAVING) {
                    cancelAndExit();
                }
                return true;
            }
            if (this.mode == Mode.EDIT || this.mode == Mode.DRAG_TABS || this.mode == Mode.DRAG_ITEMS) {
// @target if minecraft_version=26.1.2,26.2
//|                if (event.key() == GLFW.GLFW_KEY_PAGE_UP) {
// @target else
                if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
// @target end
                    switchPageFromKey(-1, "page-up");
                    return true;
                }
// @target if minecraft_version=26.1.2,26.2
//|                if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
// @target else
                if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
// @target end
                    switchPageFromKey(1, "page-down");
                    return true;
                }
            }
            return isEditing();
        }
// @target if minecraft_version=26.1.2,26.2
//|        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
// @target else
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
// @target end
            finishTitleEdit();
            return true;
        }
// @target if minecraft_version=26.1.2,26.2
//|        if (event.isEscape()) {
// @target else
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
// @target end
            this.host.visualCreativeTabEditor$hideTitleEditor();
            this.titleEditorInitialValue = null;
            this.mode = Mode.EDIT;
            return true;
        }
// @target if minecraft_version=26.1.2,26.2
//|        this.host.visualCreativeTabEditor$titleEditorKeyPressed(event);
// @target else
        this.host.visualCreativeTabEditor$titleEditorKeyPressed(keyCode, scanCode, modifiers);
// @target end
        return true;
    }

    private void switchPageFromKey(int direction, String reason) {
        CreativeTabClientPlatform.PageState before = CreativeTabClientPlatform.pageState(
                this.host.visualCreativeTabEditor$screen()
        );
        boolean switched = before.canMove(direction)
                && CreativeTabClientPlatform.switchToPage(
                        this.host.visualCreativeTabEditor$screen(),
                        before.index() + direction
                );
        if (switched && this.mode == Mode.EDIT) {
            syncDraftCurrentTabToScreen("keyboard-page-turn");
        }
        CreativeTabClientPlatform.PageState after = CreativeTabClientPlatform.pageState(
                this.host.visualCreativeTabEditor$screen()
        );
        trace(
                "page-key reason={} direction={} before={}/{} switched={} after={}/{} selected={}",
                reason,
                direction,
                before.index() + 1,
                before.count(),
                switched,
                after.index() + 1,
                after.count(),
                tabId(this.host.visualCreativeTabEditor$selectedTab())
        );
    }

// @target if minecraft_version=26.1.2,26.2
//|    public boolean charTyped(CharacterEvent event) {
// @target else
    public boolean charTyped(char codePoint, int modifiers) {
// @target end
        if (this.mode == Mode.TAB_PROPERTIES) {
// @target if minecraft_version=26.1.2,26.2
//|            this.host.visualCreativeTabEditor$titleEditorCharTyped(event);
// @target else
            this.host.visualCreativeTabEditor$titleEditorCharTyped(codePoint, modifiers);
// @target end
            return true;
        }
        return isEditing();
// @target if minecraft_version=26.1.2,26.2
//|    }
//|
//|    public boolean preeditUpdated(@Nullable PreeditEvent event) {
//|        if (this.mode != Mode.TAB_PROPERTIES) {
//|            return isEditing();
//|        }
//|        this.host.visualCreativeTabEditor$titleEditorPreeditUpdated(event);
//|        return true;
// @target else
// @target end
    }

    public void removed() {
        this.contextGroupId = null;
        this.contextMemberGroupId = null;
        this.contextGroupIcon = ItemStack.EMPTY;
        this.focusedGroupId = null;
        this.focusedGroupTabId = null;
        this.folders.close();
        if (this.draft != null) {
            trace(
                    "screen-removed-discard mode={} draftRevision={} currentTab={} tabs={} items={}",
                    this.mode,
                    this.draftRevision,
                    this.draft.currentTabId().orElse(null),
                    this.draft.tabCount(),
                    currentItemCount()
            );
        }
        this.press = null;
        this.dragVisual = null;
        this.pendingNativeTabSelection = null;
        this.nativeTabSelectionBefore = null;
        clearItemDragTabHover();
        clearPageHover("screen-removed");
        setVirtualTail(false, "screen-removed");
        this.draft = null;
        this.history = null;
        this.dragCheckpointed = false;
        this.mode = Mode.NORMAL;
        this.titleEditorInitialValue = null;
        this.host.visualCreativeTabEditor$hideTitleEditor();
        CreativeTabRuntime.clearPreview();
    }

// @target if minecraft_version=1.20.1
//|    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
//|        if (this.folders.scroll(delta)) { return true; }
// @target else
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.folders.scroll(vertical)) { return true; }
// @target end
        return isModal();
    }

    private void triggerLongPress() {
        Press active = this.press;
        if (active == null
                || active.cancelled
                || active.longTriggered
                || this.mode == Mode.DRAG_TABS
                || this.mode == Mode.DRAG_ITEMS) {
            return;
        }
        long elapsed = Util.getMillis() - active.startedAt;
        if (elapsed < LONG_PRESS_MILLIS) {
            return;
        }
        double distanceSquared = active.distanceSquared(this.pointerX, this.pointerY);
        if (active.moved || distanceSquared >= LONG_PRESS_SLOP_SQUARED) {
            if (!active.longPressBlockedLogged) {
                active.longPressBlockedLogged = true;
                trace(
                        "long-press-blocked mode={} kind={} elapsedMs={} moved={} distanceSq={} thresholdSq={}",
                        this.mode,
                        active.kind,
                        elapsed,
                        active.moved,
                        distanceSquared,
                        LONG_PRESS_SLOP_SQUARED
                );
            }
            return;
        }
        if (active.groupId != null && !itemGroups().containsGroup(active.groupId)) {
            active.cancelled = true;
            return;
        }
        active.longTriggered = true;
        if (active.groupId != null) {
            if (this.mode == Mode.NORMAL) {
                if (!CreativeTabClientState.canEdit()) { active.longTriggered = false; return; }
                beginEdit(Mode.EDIT, this.host.visualCreativeTabEditor$selectedTab());
            }
            ensureSearchOrderPrepared(this.host.visualCreativeTabEditor$selectedTab(), "group-long-press");
            this.contextX = (int) active.x + 12;
            this.contextY = (int) active.y - 8;
            setContextItemTarget(active.slot);
            this.contextGroupId = active.groupId;
            this.contextMemberGroupId = null;
            this.contextGroupIcon = draft().itemGroup(active.groupId).icon();
            draft().clearItemSelection();
            draft().selectItemGroup(active.groupId, true);
            this.contextItemIndex = -1;
            this.mode = Mode.CONTEXT_ITEM;
            return;
        }
        trace(
                "long-press-trigger mode={} kind={} elapsedMs={} press=({}, {}) pointer=({}, {})",
                this.mode,
                active.kind,
                elapsed,
                active.x,
                active.y,
                this.pointerX,
                this.pointerY
        );
        switch (active.kind) {
            case NORMAL_TAB -> {
                beginEdit(Mode.EDIT, this.host.visualCreativeTabEditor$selectedTab());
                this.contextTabId = CreativeTabRuntime.id(active.tab).orElse(null);
                this.contextX = (int) active.x + 12;
                this.contextY = (int) active.y - 8;
                setContextTabTarget(active.tab);
                this.mode = Mode.CONTEXT_TAB;
            }
            case NORMAL_ITEM -> {
                beginEdit(Mode.EDIT, this.host.visualCreativeTabEditor$selectedTab());
                ensureSearchOrderPrepared(
                        this.host.visualCreativeTabEditor$selectedTab(),
                        "normal-item-long-press"
                );
                active.itemIndex = itemIndexForSlot(active.slot);
                trace(
                        "item-press-map type={} slotIndex={} mappedIndex={} menuItems={} visibleItems={} draftItems={} accepted={} reason=normal-long-press",
                        currentType(),
                        active.slot == null ? -1 : active.slot.index,
                        active.itemIndex,
                        this.host.visualCreativeTabEditor$menu().items.size(),
                        this.host.visualCreativeTabEditor$selectedTab().getDisplayItems().size(),
                        currentItemCount(),
                        active.itemIndex >= 0
                );
                this.contextItemIndex = active.itemIndex;
                this.contextX = (int) active.x + 12;
                this.contextY = (int) active.y - 8;
                setContextItemTarget(active.slot);
                this.mode = Mode.CONTEXT_ITEM;
                if (contextActions().isEmpty()) { this.mode = Mode.EDIT; }
            }
            case BLANK_TAB -> beginEdit(Mode.EDIT, this.host.visualCreativeTabEditor$selectedTab());
            case EDIT_TAB -> {
                this.contextTabId = CreativeTabRuntime.id(active.tab).orElse(null);
                this.contextX = (int) active.x + 12;
                this.contextY = (int) active.y - 8;
                setContextTabTarget(active.tab);
                this.mode = Mode.CONTEXT_TAB;
            }
            case EDIT_ITEM -> {
                this.contextItemIndex = active.itemIndex;
                this.contextX = (int) active.x + 12;
                this.contextY = (int) active.y - 8;
                setContextItemTarget(active.slot);
                this.mode = Mode.CONTEXT_ITEM;
                if (contextActions().isEmpty()) { this.mode = Mode.EDIT; }
            }
        }
        if (this.mode == Mode.CONTEXT_TAB || this.mode == Mode.CONTEXT_ITEM) {
            traceContextGeometry();
        } else {
            trace("long-press-result kind={} mode={}", active.kind, this.mode);
        }
    }

    private void beginEdit(Mode editMode, CreativeModeTab selected) {
        this.dragVisual = null;
        this.pendingNativeTabSelection = null;
        this.nativeTabSelectionBefore = null;
        clearItemDragTabHover();
        clearPageHover("begin-edit");
        this.history = null;
        this.dragCheckpointed = false;
        var sourceCatalog = CreativeTabClientState.resolvedCatalog();
        this.history = CreativeTabHistory.open(CreativeTabClientState.contextFile("history"),
                new CreativeTabHistory.State(sourceCatalog, CreativeTabRuntime.id(selected).orElse(null)), CreativeTabClientState.lookup());
        this.draft = CreativeTabDraft.fromCatalog(this.history.anchor().catalog());
        this.draftRevision = CreativeTabClientState.revision();
        this.editorEntryTabId = CreativeTabRuntime.id(selected)
                .filter(id -> this.draft.definition(id).isPresent())
                .orElse(null);
        logDraftSanitization(sourceCatalog);
        CreativeTabRuntime.id(selected).ifPresent(id -> {
            if (this.draft.definition(id).isPresent()) {
                this.draft.setCurrentTab(id);
            }
        });
        ensureSearchOrderPrepared(selected, "begin-edit");
        this.mode = editMode;
        updateVirtualTail("begin-edit");
        trace(
                "begin-edit mode={} selectedTab={} revision={} tabs={}",
                editMode,
                tabId(selected),
                this.draftRevision,
                this.draft.tabCount()
        );
        preview();
    }

    private void setContextTabTarget(@Nullable CreativeModeTab tab) {
        this.contextGroupId = null;
        this.contextMemberGroupId = null;
        if (tab == null) {
            this.contextTargetX = this.contextX - 20;
            this.contextTargetY = this.contextY;
            trace("context-target type=tab tab=null target=({}, {})", this.contextTargetX, this.contextTargetY);
            return;
        }
        int tabX = this.host.visualCreativeTabEditor$tabX(tab);
        int tabY = this.host.visualCreativeTabEditor$tabY(tab);
        Rect icon = tabIconRect(tab);
        this.contextTargetX = icon.x;
        this.contextTargetY = icon.y;
        trace(
                "context-target type=tab tab={} leftTop=({}, {}) tabXY=({}, {}) iconOffsetY={} result=({}, {})",
                tabId(tab),
                this.host.visualCreativeTabEditor$left(),
                this.host.visualCreativeTabEditor$top(),
                tabX,
                tabY,
                tabY < 0 ? 13 : 3,
                this.contextTargetX,
                this.contextTargetY
        );
    }

    private void setContextItemTarget(@Nullable Slot slot) {
        this.contextGroupId = null;
        this.contextMemberGroupId = slot == null ? null : itemGroups().groupId(displayItemIndex(slot));
        if (slot == null) {
            this.contextTargetX = this.contextX - 20;
            this.contextTargetY = this.contextY;
            trace("context-target type=item slot=null target=({}, {})", this.contextTargetX, this.contextTargetY);
            return;
        }
        this.contextTargetX = this.host.visualCreativeTabEditor$left() + slot.x;
        this.contextTargetY = this.host.visualCreativeTabEditor$top() + slot.y;
        trace(
                "context-target type=item item={} slot={} slotXY=({}, {}) leftTop=({}, {}) result=({}, {})",
                itemId(slot),
                slot.index,
                slot.x,
                slot.y,
                this.host.visualCreativeTabEditor$left(),
                this.host.visualCreativeTabEditor$top(),
                this.contextTargetX,
                this.contextTargetY
        );
    }

    private Rect tabIconRect(CreativeModeTab tab) {
        int tabX = this.host.visualCreativeTabEditor$tabX(tab);
        int tabY = this.host.visualCreativeTabEditor$tabY(tab);
        return new Rect(
                this.host.visualCreativeTabEditor$left() + tabX + 5,
                this.host.visualCreativeTabEditor$top() + tabY
                        + (tab.row() == CreativeModeTab.Row.TOP ? 13 : 3),
                16,
                16
        );
    }

    private void beginPicker(Mode pickerMode) {
        this.pickerReturnTab = this.host.visualCreativeTabEditor$selectedTab();
        this.pickerReturnScrollRow = currentType() == CreativeTabType.CATEGORY
                ? this.host.visualCreativeTabEditor$menu()
                        .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset())
                : 0;
        this.mode = pickerMode;
        updateVirtualTail("picker-begin");
        trace(
                "picker-begin mode={} returnTab={} draftCurrent={} contextItemIndex={} contextItem={} items={} searchItems={} returnScrollRow={}",
                pickerMode,
                tabId(this.pickerReturnTab),
                draft().currentTabId().orElse(null),
                this.contextItemIndex,
                stackId(currentItemAt(this.contextItemIndex)),
                currentItemCount(),
                currentSearchItemCount(),
                this.pickerReturnScrollRow
        );
// @target if minecraft_version=1.20.1
//|        ResourceLocation inventoryId = new ResourceLocation("inventory");
// @target else
        ResourceLocation inventoryId = ResourceLocation.withDefaultNamespace("inventory");
// @target end
        CreativeModeTab inventory = CreativeTabRuntime
                .effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(tab -> CreativeTabRuntime.id(tab).filter(inventoryId::equals).isPresent())
                .filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY)
                .findFirst()
                .orElseGet(() -> CreativeTabRuntime
                        .effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                        .filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY)
                        .findFirst()
                        .orElse(null));
        if (inventory == null || inventory.getType() != CreativeModeTab.Type.INVENTORY) {
            trace(
                    "picker-open-failed mode={} resolved={} resolvedType={} reason=missing-valid-inventory-tab",
                    pickerMode,
                    tabId(inventory),
                    inventory == null ? "missing" : inventory.getType()
            );
            this.mode = Mode.EDIT;
            this.pickerReturnTab = null;
            preview();
            this.host.visualCreativeTabEditor$refreshScreen();
            updateVirtualTail("picker-open-failed");
            return;
        }
        CreativeTabRuntime.withForcedVisibility(
                inventory,
                () -> this.host.visualCreativeTabEditor$selectTab(inventory)
        );
        if (this.host.visualCreativeTabEditor$selectedTab() != inventory) {
            trace(
                    "picker-open-failed mode={} requested={} selectedAfter={} reason=loader-rejected-selection",
                    pickerMode,
                    tabId(inventory),
                    tabId(this.host.visualCreativeTabEditor$selectedTab())
            );
            if (this.pickerReturnTab != null) {
                this.host.visualCreativeTabEditor$selectTab(this.pickerReturnTab);
            }
            this.mode = Mode.EDIT;
            this.pickerReturnTab = null;
            preview();
            this.host.visualCreativeTabEditor$refreshScreen();
            updateVirtualTail("picker-selection-rejected");
            return;
        }
        trace(
                "picker-open mode={} requested={} selectedAfter={} selectedType={} panel=({},{} {}x{}) screen={}x{}",
                pickerMode,
                tabId(inventory),
                tabId(this.host.visualCreativeTabEditor$selectedTab()),
                this.host.visualCreativeTabEditor$selectedTab().getType(),
                this.host.visualCreativeTabEditor$left(),
                this.host.visualCreativeTabEditor$top(),
                this.host.visualCreativeTabEditor$imageWidth(),
                this.host.visualCreativeTabEditor$imageHeight(),
                this.host.visualCreativeTabEditor$screenWidth(),
                this.host.visualCreativeTabEditor$screenHeight()
        );
    }

    private void acceptPickedItem(ItemStack stack) {
        Mode pickerMode = this.mode;
        ItemStack picked = stack.copyWithCount(1);
        ResourceLocation createdTabId = null;
        int tabsBefore = draft().tabCount();
        int itemsBefore = currentItemCount();
        int searchItemsBefore = currentSearchItemCount();
        int requestedIndex = this.contextItemIndex;
        ItemStack previous = currentItemAt(requestedIndex);
        int existingPickedIndex = indexOfMatchingCurrentItem(picked, requestedIndex);
        CreativeTabDefinition tabTargetBefore = this.contextTabId == null
                ? null
                : draft().definition(this.contextTabId).orElse(null);
        ItemStack tabIconBefore = tabTargetBefore == null ? ItemStack.EMPTY : tabTargetBefore.icon();
        int resultIndex = -1;
        checkpoint();
        switch (pickerMode) {
            case PICK_REPLACEMENT -> resultIndex = draft().replaceItem(this.contextItemIndex, picked);
            case PICK_NEW_ITEM -> resultIndex = draft().addItem(picked);
            case PICK_TAB_ICON -> {
                if (this.contextGroupId == null) {
                    draft().setTabIcon(requireContextTab(), picked);
                } else {
                    draft().setItemGroupIcon(this.contextGroupId, picked);
                    this.contextGroupIcon = picked.copy();
                }
            }
            case PICK_NEW_TAB -> {
                createdTabId = draft().addCustomCategory(picked);
                this.contextTabId = createdTabId;
            }
            default -> { return; }
        }
        trace(
                "picker-accept-start mode={} picked={} returnTab={} createdTab={} tabTarget={} tabIconBefore={} tabIconAfter={} requestedIndex={} resultIndex={} previous={} existingPickedIndex={} tabsBefore={} tabsAfter={} itemsBefore={} itemsAfter={} searchBefore={} searchAfter={} selectedIndices={} draftCurrent={}",
                pickerMode,
                stackId(picked),
                tabId(this.pickerReturnTab),
                createdTabId,
                this.contextTabId,
                stackId(tabIconBefore),
                stackId(this.contextTabId == null
                        ? ItemStack.EMPTY
                        : draft().definition(this.contextTabId).map(CreativeTabDefinition::icon).orElse(ItemStack.EMPTY)),
                requestedIndex,
                resultIndex,
                stackId(previous),
                existingPickedIndex,
                tabsBefore,
                draft().tabCount(),
                itemsBefore,
                currentItemCount(),
                searchItemsBefore,
                currentSearchItemCount(),
                draft().selectedItemIndices(),
                draft().currentTabId().orElse(null)
        );
        preview();

        if (createdTabId != null) {
            selectCreatedTab(createdTabId);
        } else if (this.pickerReturnTab != null) {
            this.host.visualCreativeTabEditor$selectTab(this.pickerReturnTab);
            syncDraftCurrentTabToScreen("picker-return");
        }
        this.mode = Mode.EDIT;
        this.contextItemIndex = resultIndex >= 0 ? resultIndex : this.contextItemIndex;
        this.host.visualCreativeTabEditor$refreshScreen();
        syncDraftCurrentTabToScreen("picker-return-after-refresh");
        updateVirtualTail("picker-accept");
        refreshCurrentDraftItems(this.pickerReturnScrollRow, resultIndex, "picker-accept");
        this.pickerReturnTab = null;
        trace(
                "picker-accept-finish sourceMode={} mode={} selectedTab={} draftCurrent={} contextItemIndex={} contextItem={} tabs={} items={} searchItems={} menuItems={} runtimeDisplayItems={}",
                pickerMode,
                this.mode,
                tabId(this.host.visualCreativeTabEditor$selectedTab()),
                draft().currentTabId().orElse(null),
                this.contextItemIndex,
                stackId(currentItemAt(this.contextItemIndex)),
                draft().tabCount(),
                currentItemCount(),
                currentSearchItemCount(),
                this.host.visualCreativeTabEditor$menu().items.size(),
                this.host.visualCreativeTabEditor$selectedTab().getDisplayItems().size()
        );
    }

    private void selectCreatedTab(ResourceLocation createdTabId) {
        this.host.visualCreativeTabEditor$refreshScreen();
        CreativeModeTab createdTab = CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(tab -> CreativeTabRuntime.id(tab).filter(createdTabId::equals).isPresent())
                .findFirst()
                .orElse(null);
        boolean revealed = createdTab != null
                && CreativeTabClientPlatform.revealTab(this.host.visualCreativeTabEditor$screen(), createdTab);
        if (revealed) {
            this.host.visualCreativeTabEditor$selectTab(createdTab);
        }
        boolean selected = createdTab != null && this.host.visualCreativeTabEditor$selectedTab() == createdTab;
        if (selected) {
            draft().setCurrentTab(createdTabId);
        } else if (this.pickerReturnTab != null) {
            this.host.visualCreativeTabEditor$selectTab(this.pickerReturnTab);
            syncDraftCurrentTabToScreen("new-tab-selection-fallback");
        }
        trace(
                "new-tab-select createdTab={} resolved={} revealed={} selected={} selectedAfter={} draftCurrent={}",
                createdTabId,
                tabId(createdTab),
                revealed,
                selected,
                tabId(this.host.visualCreativeTabEditor$selectedTab()),
                draft().currentTabId().orElse(null)
        );
    }

    private boolean clickControl(double x, double y) {
        if (groupControlsAvailable() && buttonAt(x, y, groupControlsRect())) {
            String group = focusedItemGroup();
            this.groupControlTitle = group == null
                    ? Component.translatable("visual_creative_tab_editor.group.select") : draft().itemGroup(group).title();
            this.mode = Mode.GROUP_CONTROLS;
            return true;
        }
        if (buttonAt(x, y, undoRect())) {
            undo();
            return true;
        }
        if (buttonAt(x, y, redoRect())) { redo(); return true; }
        if (buttonAt(x, y, restoreRect())) {
            restoreDeletedTabs();
            return true;
        }
        if (buttonAt(x, y, defaultOrderRect())) {
            checkpoint();
            draft().restoreDefaultOrder(CreativeTabClientState.baseCatalog());
            refreshAfterHistoryChange();
            return true;
        }
        if (buttonAt(x, y, globalSaveRect())) {
            boolean saved = CreativeTabClientState.saveGlobal(draft().toCatalog());
            var player = this.host.visualCreativeTabEditor$minecraft().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable(saved
                        ? "visual_creative_tab_editor.editor.global_saved" : "visual_creative_tab_editor.editor.global_failed"));
            }
            return true;
        }
        if (buttonAt(x, y, saveRect())) {
            saveAndExit();
            return true;
        }
        if (buttonAt(x, y, cancelRect())) {
            cancelAndExit();
            return true;
        }
        if (buttonAt(x, y, deleteRect())) {
            if (currentType() == CreativeTabType.CATEGORY) {
                deleteItems();
            }
            deleteTabs();
            return true;
        }
        if (buttonAt(x, y, sortRect())
                && (currentType() == CreativeTabType.CATEGORY || currentType() == CreativeTabType.SEARCH)) {
            this.mode = Mode.SORT_MENU;
            return true;
        }
        return false;
    }

    private boolean clickContextMenu(double x, double y) {
        List<ContextAction> actions = contextActions();
        for (int index = 0; index < actions.size(); index++) {
            if (!buttonAt(x, y, contextActionRect(index))) { continue; }
            ContextAction action = actions.get(index);
            trace("context-action action={} tab={} group={} item={} selectedItems={}",
                    action, draft().currentTabId().orElse(null), this.contextGroupId,
                    this.contextItemIndex, draft().selectedItemCount());
            switch (action) {
                case MODIFY -> {
                    if (this.contextGroupId != null) {
                        openGroupProperties(this.contextGroupId);
                    } else if (this.mode == Mode.CONTEXT_ITEM) {
                        beginPicker(Mode.PICK_REPLACEMENT);
                    } else {
                        openTabProperties(draft().definition(requireContextTab()).orElseThrow());
                    }
                }
                case DELETE -> {
                    if (this.contextGroupId != null) {
                        checkpoint();
                        draft().dissolveItemGroup(this.contextGroupId);
                        this.contextGroupId = null;
                        this.mode = Mode.EDIT;
                        preview();
                        refreshCurrentDraftItems(0, "group-dissolve");
                    } else if (this.mode == Mode.CONTEXT_ITEM) {
                        selectContextItemIfNeeded();
                        deleteItems();
                        this.mode = Mode.EDIT;
                    } else {
                        if (!draft().isTabSelected(requireContextTab())) {
                            draft().clearTabSelection();
                            draft().setTabSelected(requireContextTab(), true);
                        }
                        deleteTabs();
                        this.mode = Mode.EDIT;
                    }
                }
                case MERGE -> {
                    checkpoint();
                    String created = draft().createItemGroup();
                    preview();
                    refreshCurrentDraftItems(0, "group-create");
                    openGroupProperties(created);
                }
                case DETACH -> {
                    selectContextItemIfNeeded();
                    checkpoint();
                    draft().detachSelectedItemsFromGroups();
                    this.mode = Mode.EDIT;
                    preview();
                    refreshCurrentDraftItems(0, "group-detach");
                }
            }
            return true;
        }
        saveAndExit();
        return true;
    }

    private void selectContextItemIfNeeded() {
        if (!draft().isItemSelected(this.contextItemIndex)) {
            draft().clearItemSelection();
            draft().setItemSelected(this.contextItemIndex, true);
        }
    }

    private boolean clickTabProperties(double x, double y) {
        Rect panel = propertiesPanel();
        Rect icon = new Rect(panel.x + 8, panel.y + 52, 76, BUTTON_HEIGHT);
        Rect done = new Rect(panel.right() - 52, panel.y + 52, 44, BUTTON_HEIGHT);
        if (this.host.visualCreativeTabEditor$isTitleEditorAt(x, y)) {
            return false;
        }
        if (buttonAt(x, y, icon)) {
            commitTabTitleIfChanged("change-icon");
            this.host.visualCreativeTabEditor$hideTitleEditor();
            this.titleEditorInitialValue = null;
            beginPicker(Mode.PICK_TAB_ICON);
            return true;
        }
        if (buttonAt(x, y, done)) {
            finishTitleEdit();
            return true;
        }
        if (!buttonAt(x, y, panel)) {
            finishTitleEdit();
            saveAndExit();
        }
        return true;
    }

    private void finishTitleEdit() {
        commitTabTitleIfChanged("finish-properties");
        preview();
        this.host.visualCreativeTabEditor$hideTitleEditor();
        this.titleEditorInitialValue = null;
        this.mode = Mode.EDIT;
    }

    private void openTabProperties(CreativeTabDefinition definition) {
        this.contextGroupId = null;
        this.mode = Mode.TAB_PROPERTIES;
        this.titleEditorInitialValue = definition.title().getString();
        this.host.visualCreativeTabEditor$showTitleEditor(this.titleEditorInitialValue);
    }

    private void openGroupProperties(String id) {
        this.focusedGroupId = id;
        this.focusedGroupTabId = draft().currentTabId().orElse(null);
        CreativeItemGroup group = draft().itemGroup(id);
        this.contextGroupId = id;
        this.contextMemberGroupId = null;
        this.contextTabId = draft().currentTabId().orElseThrow();
        this.contextGroupIcon = group.icon();
        this.mode = Mode.TAB_PROPERTIES;
        this.titleEditorInitialValue = group.title().getString();
        this.host.visualCreativeTabEditor$showTitleEditor(this.titleEditorInitialValue);
    }

    private boolean commitTabTitleIfChanged(String reason) {
        if (this.contextGroupId != null) {
            String next = this.host.visualCreativeTabEditor$titleEditorValue();
            if (Objects.equals(this.titleEditorInitialValue, next)) { return false; }
            checkpoint();
            draft().setItemGroupTitle(this.contextGroupId, Component.literal(next));
            this.titleEditorInitialValue = next;
            return true;
        }
        ResourceLocation tabId = requireContextTab();
        CreativeTabDefinition before = draft().definition(tabId).orElseThrow();
        String initial = this.titleEditorInitialValue == null
                ? before.title().getString()
                : this.titleEditorInitialValue;
        String next = this.host.visualCreativeTabEditor$titleEditorValue();
        if (Objects.equals(initial, next)) {
            trace(
                    "tab-title-preserved reason={} tab={} displayed={} component={}",
                    reason,
                    tabId,
                    next,
                    before.title()
            );
            return false;
        }
        checkpoint();
        draft().setTabTitle(tabId, next);
        this.titleEditorInitialValue = next;
        trace(
                "tab-title-change reason={} tab={} before={} after={}",
                reason,
                tabId,
                before.title(),
                next
        );
        return true;
    }

    private boolean clickSortMenu(double x, double y) {
        List<CreativeTabSortMode> modes = sortModes();
        Rect menu = sortMenuRect(modes.size());
        for (int index = 0; index < modes.size(); index++) {
            Rect row = new Rect(menu.x, menu.y + index * BUTTON_HEIGHT, menu.width, BUTTON_HEIGHT);
            if (buttonAt(x, y, row)) {
                int preferredRow = this.host.visualCreativeTabEditor$menu()
                        .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
                CreativeTabSortMode selected = modes.get(index);
                checkpoint();
                Comparator<ItemStack> comparator = itemComparator(selected);
                if (currentType() == CreativeTabType.SEARCH) {
                    draft().sortCurrentSearchItems(draft().currentItems(), comparator);
                } else if (currentType() == CreativeTabType.CATEGORY) {
                    draft().sortCurrentItems(comparator);
                }
                trace(
                        "sort-apply tab={} type={} mode={} itemsAfter={} searchAfter={}",
                        draft().currentTabId().orElse(null),
                        currentType(),
                        selected,
                        currentItemCount(),
                        currentSearchItemCount()
                );
                preview();
                this.mode = Mode.EDIT;
                this.host.visualCreativeTabEditor$refreshScreen();
                updateVirtualTail("sort-finish");
                refreshCurrentDraftItems(preferredRow, "sort-finish");
                return true;
            }
        }
        saveAndExit();
        return true;
    }

    private void deleteTabs() {
        int preferredRow = this.host.visualCreativeTabEditor$menu()
                .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
        Set<ResourceLocation> selected = new LinkedHashSet<>(draft().selectedTabIds());
        if (selected.isEmpty()) {
            return;
        }
        List<CreativeTabDefinition> remainingCategories = draft().tabs().stream()
                .filter(definition -> definition.type() == CreativeTabType.CATEGORY && !definition.hidden())
                .filter(definition -> !selected.contains(definition.id()))
                .toList();
        if (remainingCategories.isEmpty()) {
            draft().tabs().stream()
                    .filter(definition -> definition.type() == CreativeTabType.CATEGORY && !definition.hidden())
                    .map(CreativeTabDefinition::id)
                    .findFirst()
                    .ifPresent(selected::remove);
        }
        ResourceLocation currentId = draft().currentTabId().orElse(null);
        ResourceLocation fallbackId = currentId != null && selected.contains(currentId)
                ? draft().tabs().stream()
                        .filter(definition -> definition.type() == CreativeTabType.CATEGORY && !definition.hidden())
                        .map(CreativeTabDefinition::id)
                        .filter(id -> !selected.contains(id))
                        .findFirst()
                        .orElse(null)
                : null;
        checkpoint();
        draft().setTabsHidden(selected, true);
        draft().clearTabSelection();
        if (fallbackId != null) {
            draft().setCurrentTab(fallbackId);
        }
        boolean refreshSearch = fallbackId == null && currentType() == CreativeTabType.SEARCH;
        preview();
        if (refreshSearch) {
            CreativeTabRuntime.rebuildSearchContents();
        }
        if (fallbackId != null) {
            CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                    .filter(tab -> CreativeTabRuntime.id(tab).filter(fallbackId::equals).isPresent())
                    .findFirst()
                    .ifPresent(this.host::visualCreativeTabEditor$selectTab);
            trace(
                    "delete-tabs-current-fallback hiddenCurrent={} fallback={} selectedAfter={}",
                    currentId,
                    fallbackId,
                    tabId(this.host.visualCreativeTabEditor$selectedTab())
            );
        }
        this.host.visualCreativeTabEditor$refreshScreen();
        if (refreshSearch) {
            ensureSearchOrderPrepared(
                    this.host.visualCreativeTabEditor$selectedTab(),
                    "delete-tabs-search-refresh"
            );
            refreshCurrentDraftItems(preferredRow, "delete-tabs-search-refresh");
            trace(
                    "delete-tabs-search-refresh hiddenTabs={} preparedItems={} persistablePrefix={} row={}",
                    selected.size(),
                    currentItemCount(),
                    draft().currentSearchPreferenceCapacity(),
                    preferredRow
            );
        }
    }

    private void refreshSearchAfterTabMutation(String reason) {
        if (currentType() != CreativeTabType.SEARCH || draft().hasPreparedCurrentSearchItems()) {
            return;
        }
        int preferredRow = this.host.visualCreativeTabEditor$menu()
                .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
        CreativeTabRuntime.rebuildSearchContents();
        this.host.visualCreativeTabEditor$refreshScreen();
        ensureSearchOrderPrepared(this.host.visualCreativeTabEditor$selectedTab(), reason);
        refreshCurrentDraftItems(preferredRow, reason);
        trace(
                "search-refresh-after-tab-mutation reason={} preparedItems={} persistablePrefix={} row={}",
                reason,
                currentItemCount(),
                draft().currentSearchPreferenceCapacity(),
                preferredRow
        );
    }

    private void deleteItems() {
        if (currentType() != CreativeTabType.CATEGORY) {
            return;
        }
        if (draft().selectedItemIndices().isEmpty()) {
            return;
        }
        int row = this.host.visualCreativeTabEditor$menu()
                .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
        checkpoint();
        int removed = draft().deleteSelectedItems();
        preview();
        this.host.visualCreativeTabEditor$refreshScreen();
        refreshCurrentDraftItems(row, "delete-items");
        trace(
                "delete-items tab={} removed={} itemsAfter={} searchAfter={} row={}",
                draft().currentTabId().orElse(null),
                removed,
                currentItemCount(),
                currentSearchItemCount(),
                row
        );
    }

    private CreativeTabHistory.State currentEditState() {
        return new CreativeTabHistory.State(draft().toCatalog(), draft().currentTabId().orElse(null));
    }

    private void checkpoint() {
        if (this.mode == Mode.DRAG_ITEMS || this.mode == Mode.DRAG_TABS) {
            if (this.dragCheckpointed) { return; }
            this.dragCheckpointed = true;
        }
        if (this.history != null) { this.history.beforeChange(currentEditState()); }
    }

    private boolean canUndo() { return this.draft != null && this.history != null && this.history.hasUndo(); }
    private boolean canRedo() { return this.draft != null && this.history != null && this.history.hasRedo(); }

    private void undo() {
        if (canUndo()) { restoreHistoryState(this.history.undo(currentEditState())); }
    }

    private void redo() {
        if (canRedo()) { restoreHistoryState(this.history.redo(currentEditState())); }
    }

    private void restoreHistoryState(CreativeTabHistory.State state) {
        this.draft = CreativeTabDraft.fromCatalog(state.catalog());
        if (state.currentTab() != null && this.draft.definition(state.currentTab()).filter(tab -> !tab.hidden()).isPresent()) {
            this.draft.setCurrentTab(state.currentTab());
        }
        this.contextTabId = null;
        this.contextItemIndex = -1;
        this.mode = Mode.EDIT;
        refreshAfterHistoryChange();
    }

    private List<ResourceLocation> deletedTabIds() {
        if (this.draft == null) {
            return List.of();
        }
        CreativeTabCatalog baseline = CreativeTabClientState.baseCatalog();
        return draft().tabs().stream().filter(CreativeTabDefinition::hidden)
                .filter(tab -> baseline.definition(tab.id()).map(original -> !original.hidden()).orElse(true))
                .map(CreativeTabDefinition::id).toList();
    }

    private void restoreDeletedTabs() {
        List<ResourceLocation> deleted = deletedTabIds();
        if (deleted.isEmpty()) {
            return;
        }
        checkpoint();
        draft().setTabsHidden(deleted, false);
        refreshAfterHistoryChange();
    }

    private void refreshAfterHistoryChange() {
        preview();
        CreativeTabRuntime.rebuildSearchContents();
        ResourceLocation selected = draft().currentTabId().orElse(null);
        CreativeModeTab tab = CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(candidate -> CreativeTabRuntime.id(candidate).filter(id -> id.equals(selected)).isPresent())
                .findFirst().orElse(null);
        this.host.visualCreativeTabEditor$refreshScreen();
        if (tab != null) {
            CreativeTabClientPlatform.revealTab(this.host.visualCreativeTabEditor$screen(), tab);
            this.host.visualCreativeTabEditor$selectTab(tab);
        }
        ensureSearchOrderPrepared(this.host.visualCreativeTabEditor$selectedTab(), "restore-edit");
        updateVirtualTail("restore-edit");
        refreshCurrentDraftItems(0, "restore-edit");
    }

    private void preview() {
        if (this.history != null) { this.history.afterChange(draft().toCatalog()); }
        CreativeTabRuntime.setPreview(draft().toCatalog());
        CreativeTabClientPlatform.refreshTabLayout();
    }

    private void syncDraftCurrentTabToScreen(String reason) {
        if (this.draft == null) {
            return;
        }
        CreativeModeTab selected = this.host.visualCreativeTabEditor$selectedTab();
        ResourceLocation selectedId = CreativeTabRuntime.id(selected).orElse(null);
        if (selectedId == null || this.draft.definition(selectedId).isEmpty()) {
            return;
        }
        ResourceLocation before = this.draft.currentTabId().orElse(null);
        if (selectedId.equals(before)) {
            return;
        }
        this.draft.setCurrentTab(selectedId);
        ensureSearchOrderPrepared(selected, reason + "-search");
        trace(
                "draft-current-tab-sync reason={} before={} screenSelected={} selectedItemsCleared=true",
                reason,
                before,
                selectedId
        );
    }

    private void saveAndExit() {
        if (this.draft == null || this.mode == Mode.SAVING) {
            return;
        }
        Mode returnMode = saveReturnMode(this.mode);
        boolean restoredPicker = this.pickerReturnTab != null;
        restorePickerTab();
        if (this.mode == Mode.TAB_PROPERTIES && this.contextTabId != null) {
            commitTabTitleIfChanged("save-properties");
            preview();
        }
        trace(
                "save-submit mode={} returnMode={} baseRevision={} tabs={} currentTab={} items={} searchItems={}",
                this.mode,
                returnMode,
                this.draftRevision,
                this.draft.tabCount(),
                this.draft.currentTabId().orElse(null),
                currentItemCount(),
                currentSearchItemCount()
        );
        CreativeTabRuntime.preparePreviewHandoff();
        this.host.visualCreativeTabEditor$hideTitleEditor();
        this.press = null;
        this.saveReturnMode = returnMode;
        this.mode = Mode.SAVING;
        if (!CreativeTabClientState.submit(this.draft.toCatalog(), this.draftRevision, this::handleEditResult)) {
            CreativeTabRuntime.discardPreviewHandoff();
            this.mode = returnMode;
            trace("save-submit-rejected baseRevision={} reason=client-submit-false", this.draftRevision);
            if (restoredPicker) {
                this.mode = returnMode;
                this.host.visualCreativeTabEditor$refreshScreen();
            }
            if (this.host.visualCreativeTabEditor$minecraft().player != null) {
                this.host.visualCreativeTabEditor$minecraft().player.sendSystemMessage(
                        Component.translatable("visual_creative_tab_editor.editor.submit_failed")
                );
            }
            return;
        }
    }

    private void handleEditResult(EditResultPayload result) {
        if (this.mode != Mode.SAVING) {
            return;
        }
        if (result.success()) {
            trace("save-result success=true baseRevision={} newRevision={}", this.draftRevision, result.revision());
            if (this.history != null && this.draft != null
                    && !this.history.commit(currentEditState(), CreativeTabClientState.lookup())) {
                var player = this.host.visualCreativeTabEditor$minecraft().player;
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("visual_creative_tab_editor.editor.history_failed"));
                }
            }
            exitEditor(false);
            return;
        }
        CreativeTabRuntime.discardPreviewHandoff();
        if (result.revision() != this.draftRevision) {
            trace(
                    "save-result success=false baseRevision={} serverRevision={} conflict=true message={}",
                    this.draftRevision,
                    result.revision(),
                    result.message().getString()
            );
            this.mode = Mode.CONFLICT;
            this.saveReturnMode = null;
            return;
        }
        this.mode = this.saveReturnMode == null ? Mode.EDIT : this.saveReturnMode;
        trace(
                "save-result success=false baseRevision={} serverRevision={} conflict=false returnMode={} message={}",
                this.draftRevision,
                result.revision(),
                this.mode,
                result.message().getString()
        );
        this.saveReturnMode = null;
        this.titleEditorInitialValue = null;
        preview();
        this.host.visualCreativeTabEditor$refreshScreen();
    }

    private void cancelAndExit() {
        trace("editor-cancel mode={} draftRevision={} entryTab={}", this.mode, this.draftRevision, this.editorEntryTabId);
        exitEditor(true);
    }

    private Mode saveReturnMode(Mode source) {
        return Mode.EDIT;
    }

    private void exitEditor(boolean restoreEntryTab) {
        this.contextGroupId = null;
        this.contextMemberGroupId = null;
        this.contextGroupIcon = ItemStack.EMPTY;
        this.focusedGroupId = null;
        this.focusedGroupTabId = null;
        ResourceLocation restoreId = restoreEntryTab ? this.editorEntryTabId : null;
        restorePickerTab();
        this.press = null;
        this.dragVisual = null;
        this.pendingNativeTabSelection = null;
        this.nativeTabSelectionBefore = null;
        clearItemDragTabHover();
        clearPageHover("exit-editor");
        setVirtualTail(false, "exit-editor");
        this.draft = null;
        this.history = null;
        this.dragCheckpointed = false;
        this.mode = Mode.NORMAL;
        this.titleEditorInitialValue = null;
        this.contextTabId = null;
        this.contextItemIndex = -1;
        this.saveReturnMode = null;
        this.draftRevision = -1L;
        this.editorEntryTabId = null;
        this.host.visualCreativeTabEditor$hideTitleEditor();
        CreativeTabRuntime.clearPreview();
        CreativeTabClientPlatform.refreshTabLayout();
        this.host.visualCreativeTabEditor$refreshScreen();
        if (restoreId != null) {
            CreativeModeTab restore = CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                    .filter(tab -> CreativeTabRuntime.id(tab).filter(restoreId::equals).isPresent())
                    .findFirst()
                    .orElse(null);
            boolean revealed = restore != null
                    && CreativeTabClientPlatform.revealTab(this.host.visualCreativeTabEditor$screen(), restore);
            if (revealed) {
                this.host.visualCreativeTabEditor$selectTab(restore);
            }
            trace(
                    "editor-cancel-restore requested={} resolved={} revealed={} selectedAfter={}",
                    restoreId,
                    tabId(restore),
                    revealed,
                    tabId(this.host.visualCreativeTabEditor$selectedTab())
            );
        }
    }

    private void restorePickerTab() {
        if (this.pickerReturnTab == null) {
            return;
        }
        CreativeModeTab restore = this.pickerReturnTab;
        this.pickerReturnTab = null;
        this.host.visualCreativeTabEditor$selectTab(restore);
    }

    private void replay(Press released) {
        if (released.slot == null) {
            return;
        }
        trace(
                "slot-replay-before slotId={} slotIndex={} input={} slotStack={} carried={}",
                released.slotId,
                released.slot.index,
                released.input,
                stackId(released.slot.getItem()),
                stackId(this.host.visualCreativeTabEditor$menu().getCarried())
        );
        this.replayingSlotClick = true;
        try {
            this.host.visualCreativeTabEditor$replaySlotClick(released.slot, released.slotId, released.button, released.input);
        } finally {
            this.replayingSlotClick = false;
            trace(
                    "slot-replay-after slotId={} slotStack={} carried={}",
                    released.slotId,
                    stackId(released.slot.getItem()),
                    stackId(this.host.visualCreativeTabEditor$menu().getCarried())
            );
        }
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY) {
        renderButton(graphics, undoRect(), "visual_creative_tab_editor.editor.undo", mouseX, mouseY, 0xFF555555);
        renderButton(graphics, redoRect(), "visual_creative_tab_editor.editor.redo", mouseX, mouseY, 0xFF555555);
        renderButton(graphics, restoreRect(), "visual_creative_tab_editor.editor.restore", mouseX, mouseY, 0xFF555555);
        renderButton(graphics, defaultOrderRect(), "visual_creative_tab_editor.editor.default_order", mouseX, mouseY, 0xFF555555);
        renderButton(graphics, globalSaveRect(), "visual_creative_tab_editor.editor.global_save", mouseX, mouseY, 0xFF315B7A);
        renderButton(graphics, saveRect(), "visual_creative_tab_editor.editor.save", mouseX, mouseY, 0xFF2D6A3F);
        renderButton(graphics, cancelRect(), "visual_creative_tab_editor.editor.cancel", mouseX, mouseY, 0xFF555555);
        renderButton(graphics, deleteRect(), "visual_creative_tab_editor.editor.delete", mouseX, mouseY, 0xFF8B2F2F);
        if (currentType() == CreativeTabType.CATEGORY || currentType() == CreativeTabType.SEARCH) {
            renderButton(graphics, sortRect(), "visual_creative_tab_editor.editor.sort", mouseX, mouseY, 0xFF315B7A);
        }
        if (groupControlsAvailable()) {
            renderButton(graphics, groupControlsRect(), "visual_creative_tab_editor.group.controls", mouseX, mouseY, 0xFF315B7A);
        }
    }

    private boolean groupControlsAvailable() {
        return CreativeTabClientConfig.groupSimilarItems() && canReorderCurrentItems() && screenMatchesDraftCurrentTab();
    }

    private @Nullable String focusedItemGroup() {
        if (this.draft == null || !Objects.equals(this.focusedGroupTabId, draft().currentTabId().orElse(null))) {
            return null;
        }
        return this.focusedGroupId != null && itemGroups().containsGroup(this.focusedGroupId) ? this.focusedGroupId : null;
    }

    private boolean groupActionEnabled(GroupAction action) {
        String group = focusedItemGroup();
        return switch (action) {
            case NEW -> draft().selectedItemCount() >= 2;
            case EDIT, DELETE -> group != null;
            case ADD -> group != null && draft().selectedItemCount() > 0 && !draft().selectedItemsBelongToGroup(group);
            case DETACH -> draft().hasSelectedGroupedItems();
            case BACK -> true;
        };
    }

    private boolean clickGroupControls(double x, double y) {
        for (GroupAction action : GroupAction.values()) {
            if (!buttonAt(x, y, groupActionRect(action))) { continue; }
            if (!groupActionEnabled(action)) { return true; }
            String group = focusedItemGroup();
            switch (action) {
                case NEW -> {
                    checkpoint();
                    String created = draft().createItemGroup();
                    preview();
                    refreshCurrentDraftItems(0, "group-create");
                    openGroupProperties(created);
                    return true;
                }
                case EDIT -> {
                    openGroupProperties(Objects.requireNonNull(group));
                    return true;
                }
                case DELETE -> {
                    checkpoint();
                    draft().dissolveItemGroup(Objects.requireNonNull(group));
                    this.focusedGroupId = null;
                }
                case ADD -> {
                    checkpoint();
                    draft().assignSelectedItemsToGroup(Objects.requireNonNull(group));
                }
                case DETACH -> {
                    checkpoint();
                    draft().detachSelectedItemsFromGroups();
                }
                case BACK -> {
                    this.mode = Mode.EDIT;
                    return true;
                }
            }
            this.mode = Mode.EDIT;
            preview();
            refreshCurrentDraftItems(0, "group-controls");
            return true;
        }
        return true;
    }

    private void renderGroupControls(GuiGraphics graphics, int mouseX, int mouseY) {
        Rect panel = groupPanelRect();
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), 0xEE202020);
        graphics.renderOutline(panel.x, panel.y, panel.width, panel.height, 0xFFFFFFFF);
        var font = this.host.visualCreativeTabEditor$font();
        graphics.drawString(font, Component.translatable("visual_creative_tab_editor.group.controls"),
                panel.x + 8, panel.y + 6, 0xFFFFFFFF, false);
        String title = font.plainSubstrByWidth(this.groupControlTitle.getString(), panel.width - 16);
        graphics.drawString(font, title, panel.x + 8, panel.y + 18, 0xFFD0D0D0, false);
        for (GroupAction action : GroupAction.values()) {
            renderButton(graphics, groupActionRect(action), action.key(), mouseX, mouseY,
                    action == GroupAction.DELETE ? 0xFF8B2F2F : 0xFF555555, groupActionEnabled(action));
        }
    }

    private Rect groupControlsRect() {
        int x = this.host.visualCreativeTabEditor$left() + this.host.visualCreativeTabEditor$imageWidth() + 4;
        int width = Math.min(70, this.host.visualCreativeTabEditor$screenWidth() - x - 2);
        return new Rect(x, this.host.visualCreativeTabEditor$top() + 4, width, BUTTON_HEIGHT);
    }

    private Rect groupPanelRect() {
        int width = Math.min(186, this.host.visualCreativeTabEditor$screenWidth() - 16);
        return new Rect((this.host.visualCreativeTabEditor$screenWidth() - width) / 2,
                (this.host.visualCreativeTabEditor$screenHeight() - 144) / 2, width, 144);
    }

    private Rect groupActionRect(GroupAction action) {
        Rect panel = groupPanelRect();
        return new Rect(panel.x + 8, panel.y + 32 + action.ordinal() * 18, panel.width - 16, 16);
    }

    private void renderTabChecks(GuiGraphics graphics) {
        for (CreativeModeTab tab : CreativeTabClientPlatform.visibleTabs(this.host.visualCreativeTabEditor$screen())) {
            ResourceLocation id = CreativeTabRuntime.id(tab).orElse(null);
            if (id == null) {
                continue;
            }
            int x = this.host.visualCreativeTabEditor$left() + this.host.visualCreativeTabEditor$tabX(tab) + 17;
            int y = this.host.visualCreativeTabEditor$top() + this.host.visualCreativeTabEditor$tabY(tab) + 3;
            renderCheck(graphics, x, y, draft().isTabSelected(id));
        }
    }

    private void renderItemChecks(GuiGraphics graphics) {
        if (!canReorderCurrentItems() || !screenMatchesDraftCurrentTab()) {
            return;
        }
        for (Slot slot : this.host.visualCreativeTabEditor$menu().slots) {
            if (!this.host.visualCreativeTabEditor$isCreativeSlot(slot) || !slot.hasItem()) {
                continue;
            }
            String group = itemGroups().isHeader(displayItemIndex(slot))
                    ? itemGroups().groupId(displayItemIndex(slot)) : null;
            int itemIndex = group == null ? itemIndexForSlot(slot) : -1;
            if (group == null && itemIndex < 0) {
                continue;
            }
            renderCheck(
                    graphics,
                    this.host.visualCreativeTabEditor$left() + slot.x + 9,
                    this.host.visualCreativeTabEditor$top() + slot.y - 1,
                    group == null ? draft().isItemSelected(itemIndex) : draft().isItemGroupSelected(group)
            );
        }
    }

    private void renderCheck(GuiGraphics graphics, int x, int y, boolean selected) {
        graphics.fill(x, y, x + 8, y + 8, selected ? 0xFF2DAA4F : 0xCC222222);
        graphics.renderOutline(x, y, 8, 8, 0xFFFFFFFF);
        if (selected) {
// @target if minecraft_version=1.20.1
//|            graphics.drawString(this.host.visualCreativeTabEditor$font(), Component.literal("\u2713"), x + 1, y - 1, 0xFFFFFFFF, false);
// @target else
            graphics.drawString(this.host.visualCreativeTabEditor$font(), Component.literal("✓"), x + 1, y - 1, 0xFFFFFFFF, false);
// @target end
        }
    }

    private void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ContextAction> actions = contextActions();
        for (int index = 0; index < actions.size(); index++) {
            ContextAction action = actions.get(index);
            renderButton(graphics, contextActionRect(index), action.label(this.contextGroupId != null),
                    mouseX, mouseY, action == ContextAction.DELETE ? 0xFF8B2F2F : 0xFF315B7A);
        }
    }

    private void renderContextTarget(GuiGraphics graphics) {
        ItemStack stack = ItemStack.EMPTY;
        if (this.mode == Mode.CONTEXT_TAB && this.contextTabId != null) {
            stack = draft().definition(this.contextTabId).map(CreativeTabDefinition::icon).orElse(ItemStack.EMPTY);
        } else if (this.mode == Mode.CONTEXT_ITEM) {
            stack = this.contextGroupId == null ? draft().currentItemAt(this.contextItemIndex) : this.contextGroupIcon;
        }
        if (!stack.isEmpty()) {
            graphics.fill(this.contextTargetX - 2, this.contextTargetY - 2, this.contextTargetX + 18, this.contextTargetY + 18, 0xCC222222);
            graphics.renderItem(stack, this.contextTargetX, this.contextTargetY);
            graphics.renderOutline(this.contextTargetX - 2, this.contextTargetY - 2, 20, 20, 0xFFFFFFFF);
        }
    }

    private void renderTabProperties(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Rect panel = propertiesPanel();
        graphics.fill(panel.x, panel.y, panel.right(), panel.bottom(), 0xEE202020);
        graphics.renderOutline(panel.x, panel.y, panel.width, panel.height, 0xFFFFFFFF);
        graphics.drawString(this.host.visualCreativeTabEditor$font(), Component.translatable("visual_creative_tab_editor.editor.title"), panel.x + 8, panel.y + 7, 0xFFFFFFFF, false);
// @target if minecraft_version=26.1.2,26.2
//|        this.host.visualCreativeTabEditor$extractTitleEditor(graphics, mouseX, mouseY, partialTick);
// @target else
        this.host.visualCreativeTabEditor$renderTitleEditor(graphics, mouseX, mouseY, partialTick);
// @target end
        renderButton(graphics, new Rect(panel.x + 8, panel.y + 52, 76, BUTTON_HEIGHT), "visual_creative_tab_editor.editor.change_icon", mouseX, mouseY, 0xFF315B7A);
        renderButton(graphics, new Rect(panel.right() - 52, panel.y + 52, 44, BUTTON_HEIGHT), "visual_creative_tab_editor.editor.done", mouseX, mouseY, 0xFF2D6A3F);
    }

    private void renderSortMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        List<CreativeTabSortMode> modes = sortModes();
        Rect menu = sortMenuRect(modes.size());
        for (int index = 0; index < modes.size(); index++) {
            renderButton(
                    graphics,
                    new Rect(menu.x, menu.y + index * BUTTON_HEIGHT, menu.width, BUTTON_HEIGHT),
                    sortKey(modes.get(index)),
                    mouseX,
                    mouseY,
                    0xFF315B7A
            );
        }
    }

    private void renderInventoryPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                this.host.visualCreativeTabEditor$font(),
                Component.translatable("visual_creative_tab_editor.editor.select_item"),
                Math.max(4, this.host.visualCreativeTabEditor$left() + 8),
                Math.max(4, this.host.visualCreativeTabEditor$top() - 18),
                0xFFFFFFFF,
                false
        );
        renderModalExitButtons(graphics, mouseX, mouseY);
    }

    private void renderPickerBackdrop(GuiGraphics graphics) {
        int screenWidth = Math.max(0, this.host.visualCreativeTabEditor$screenWidth());
        int screenHeight = Math.max(0, this.host.visualCreativeTabEditor$screenHeight());
        int left = Mth.clamp(this.host.visualCreativeTabEditor$left(), 0, screenWidth);
        int top = Mth.clamp(this.host.visualCreativeTabEditor$top(), 0, screenHeight);
        int right = Mth.clamp(
                this.host.visualCreativeTabEditor$left() + Math.max(0, this.host.visualCreativeTabEditor$imageWidth()),
                0,
                screenWidth
        );
        int bottom = Mth.clamp(
                this.host.visualCreativeTabEditor$top() + Math.max(0, this.host.visualCreativeTabEditor$imageHeight()),
                0,
                screenHeight
        );
        if (left >= right || top >= bottom) {
            graphics.fill(0, 0, screenWidth, screenHeight, 0x99000000);
            return;
        }
        if (top > 0) {
            graphics.fill(0, 0, screenWidth, top, 0x99000000);
        }
        if (left > 0) {
            graphics.fill(0, top, left, bottom, 0x99000000);
        }
        if (right < screenWidth) {
            graphics.fill(right, top, screenWidth, bottom, 0x99000000);
        }
        if (bottom < screenHeight) {
            graphics.fill(0, bottom, screenWidth, screenHeight, 0x99000000);
        }
    }

    private void renderModalExitButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        renderButton(graphics, saveRect(), "visual_creative_tab_editor.editor.save", mouseX, mouseY, 0xFF2D6A3F);
        renderButton(graphics, cancelRect(), "visual_creative_tab_editor.editor.cancel", mouseX, mouseY, 0xFF555555);
    }

    private static void renderCenteredText(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                           Component text, int x, int y, int color) {
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }

    private void renderButton(GuiGraphics graphics, Rect rect, String key, int mouseX, int mouseY, int color) {
        boolean enabled = !key.endsWith(".undo") || canUndo();
        enabled &= !key.endsWith(".redo") || canRedo();
        enabled &= !key.endsWith(".restore") || !deletedTabIds().isEmpty();
        renderButton(graphics, rect, key, mouseX, mouseY, color, enabled);
    }

    private void renderButton(GuiGraphics graphics, Rect rect, String key, int mouseX, int mouseY,
                              int color, boolean enabled) {
        boolean hovered = enabled && rect.contains(mouseX, mouseY);
        boolean green = color == 0xFF2D6A3F;
        boolean red = color == 0xFF8B2F2F;
        int face = !enabled ? 0xFF777777 : green ? (hovered ? 0xFF4C9E35 : 0xFF3C8527)
                : red ? (hovered ? 0xFFC94D3F : 0xFFAA382C) : (hovered ? 0xFFE0E0E0 : 0xFFC6C6C6);
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), 0xFF1E1E1E);
        graphics.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, 0xFF454545);
        graphics.fill(rect.x + 2, rect.y + 2, rect.right() - 2, rect.bottom() - 3, face);
        graphics.fill(rect.x + 2, rect.y + 1, rect.right() - 2, rect.y + 2, enabled ? 0xFFEEEEEE : 0xFF999999);
        graphics.fill(rect.x + 1, rect.y + 2, rect.x + 2, rect.bottom() - 3, enabled ? 0xFFEEEEEE : 0xFF999999);
        if (hovered) {
            graphics.renderOutline(rect.x, rect.y, rect.width, rect.height, 0xFFFFFFFF);
        }
        var font = this.host.visualCreativeTabEditor$font();
        String label = font.plainSubstrByWidth(Component.translatable(key).getString(), Math.max(0, rect.width - 6));
        renderCenteredText(graphics, font, Component.literal(label), rect.x + rect.width / 2, rect.y + 4,
                !enabled ? 0xFFB0B0B0 : green || red ? 0xFFFFFFFF : 0xFF202020);
    }

    private void renderItemPlus(GuiGraphics graphics, Rect rect) {
        graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), 0x66333333);
        graphics.renderOutline(rect.x, rect.y, rect.width, rect.height, 0x88FFFFFF);
        renderCenteredText(graphics, this.host.visualCreativeTabEditor$font(), Component.literal("+"), rect.x + rect.width / 2, rect.y + 4, 0xAAFFFFFF);
    }

    private void renderTabPlus(
            GuiGraphics graphics,
            TabPlusTarget target,
            int mouseX,
            int mouseY
    ) {
        Rect sprite = target.spriteRect();
// @target if minecraft_version=1.20.1
//|        int textureY = target.row() == CreativeModeTab.Row.TOP ? 0 : 64;
//|        RenderSystem.enableBlend();
//|        graphics.blit(
//|                CREATIVE_TABS_LOCATION,
//|                sprite.x,
//|                sprite.y,
//|                target.column() * 26,
//|                textureY,
//|                sprite.width,
//|                sprite.height
// @target else
        String rowName = target.row() == CreativeModeTab.Row.TOP ? "top" : "bottom";
        ResourceLocation spriteId = ResourceLocation.withDefaultNamespace(
                "container/creative_inventory/tab_" + rowName + "_unselected_" + (target.column() + 1)
// @target end
        );
// @target if minecraft_version=1.20.1
// @target elif minecraft_version=26.1.2,26.2
//|        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteId, sprite.x, sprite.y, sprite.width, sprite.height);
// @target else
        graphics.blitSprite(spriteId, sprite.x, sprite.y, sprite.width, sprite.height);
// @target end
        int shade = target.hitRect().contains(mouseX, mouseY) ? 0x44333333 : 0x66333333;
        graphics.fill(sprite.x, sprite.y, sprite.right(), sprite.bottom(), shade);
        renderCenteredText(graphics,
                this.host.visualCreativeTabEditor$font(),
                Component.literal("+"),
                sprite.x + sprite.width / 2,
                sprite.y + 12,
                0xFFFFFFFF
        );
    }

    private CreativeModeTab tabAt(double x, double y) {
        for (CreativeModeTab tab : CreativeTabClientPlatform.visibleTabs(this.host.visualCreativeTabEditor$screen())) {
            Rect rect = new Rect(
                    this.host.visualCreativeTabEditor$left() + this.host.visualCreativeTabEditor$tabX(tab),
                    this.host.visualCreativeTabEditor$top() + this.host.visualCreativeTabEditor$tabY(tab),
                    27,
                    32
            );
            if (rect.contains(x, y)) {
                return tab;
            }
        }
        return null;
    }

    private @Nullable Slot creativeSlotAt(double x, double y) {
        Slot slot = slotAt(x, y);
        return slot != null && this.host.visualCreativeTabEditor$isCreativeSlot(slot) ? slot : null;
    }

    private @Nullable Slot slotAt(double x, double y) {
        for (Slot slot : this.host.visualCreativeTabEditor$menu().slots) {
            int absoluteX = this.host.visualCreativeTabEditor$left() + slot.x;
            int absoluteY = this.host.visualCreativeTabEditor$top() + slot.y;
            if (slot.isActive() && x >= absoluteX && x < absoluteX + 16 && y >= absoluteY && y < absoluteY + 16) {
                return slot;
            }
        }
        return null;
    }

    private CreativeItemGroups itemGroups() {
        return ((CreativeModeItemPickerMenuVirtualTail) this.host.visualCreativeTabEditor$menu())
                .visualCreativeTabEditor$itemGroups();
    }

    private boolean beginItemGroupPress(double x, double y) {
        Slot slot = creativeSlotAt(x, y);
        if (slot == null || !itemGroups().isHeader(displayItemIndex(slot))) { return false; }
        if (isPicker()) { return toggleItemGroup(x, y); }
        CreativeItemGroups groups = itemGroups();
        Press active = Press.item(this.mode == Mode.NORMAL ? PressKind.NORMAL_ITEM : PressKind.EDIT_ITEM,
                slot, groups.sourceIndex(displayItemIndex(slot)), x, y);
        active.groupId = groups.groupId(displayItemIndex(slot));
        this.focusedGroupId = active.groupId;
        this.focusedGroupTabId = CreativeTabRuntime.id(this.host.visualCreativeTabEditor$selectedTab()).orElse(null);
        active.toggleGroup = this.mode == Mode.NORMAL
                || x >= this.host.visualCreativeTabEditor$left() + slot.x + 10
                        && y >= this.host.visualCreativeTabEditor$top() + slot.y + 10;
        this.press = active;
        return true;
    }

    private boolean toggleItemGroup(double x, double y) {
        Slot slot = creativeSlotAt(x, y);
        if (slot == null) {
            return false;
        }
        int displayIndex = displayItemIndex(slot);
        CreativeItemGroups groups = itemGroups();
        if (!groups.isHeader(displayIndex)) {
            return false;
        }
        var menu = this.host.visualCreativeTabEditor$menu();
        int oldRow = menu.getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
        groups.toggle(displayIndex, menu.items);
        int maxRow = Math.max(0, Mth.positiveCeilDiv(groups.size() + (this.virtualTailEnabled ? 1 : 0), 9) - 5);
        float scroll = maxRow == 0 ? 0.0F : menu.getScrollForRowIndex(Math.min(oldRow, maxRow));
        this.host.visualCreativeTabEditor$setScrollOffset(scroll);
        menu.scrollTo(scroll);
        return true;
    }

    private @Nullable String itemGroupAt(double x, double y) {
        Slot slot = creativeSlotAt(x, y);
        return slot == null ? null : itemGroups().groupId(displayItemIndex(slot));
    }

    private void captureItemGroupDrop(Press active) {
        active.dropGroupId = itemGroupAt(this.pointerX, this.pointerY);
        active.dropX = this.pointerX;
        active.dropY = this.pointerY;
        active.dropTargetCaptured = true;
    }

    private void finishItemGroupDrop(Press active) {
        if (!CreativeTabClientConfig.groupSimilarItems() || creativeSlotAt(this.pointerX, this.pointerY) == null) { return; }
        String target = active.dropTargetCaptured && active.dropX == this.pointerX && active.dropY == this.pointerY
                ? active.dropGroupId : itemGroupAt(this.pointerX, this.pointerY);
        boolean changed = false;
        if (target != null && !(active.groupId != null && active.groupId.equals(target))
                && !draft().selectedItemsBelongToGroup(target)) {
            checkpoint();
            draft().assignSelectedItemsToGroup(target);
            changed = true;
        } else if (target == null && active.groupId == null && active.hadGroupedItems) {
            checkpoint();
            draft().detachSelectedItemsFromGroups();
            changed = true;
        }
        if (changed) {
            preview();
            int row = this.host.visualCreativeTabEditor$menu().getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
            refreshCurrentDraftItems(row, "group-drop");
        }
    }

    public void renderItemGroupBackground(GuiGraphics graphics, Slot slot) {
        if (!this.host.visualCreativeTabEditor$isCreativeSlot(slot)) { return; }
        CreativeItemGroups groups = itemGroups();
        int index = displayItemIndex(slot);
        if (groups.groupId(index) != null) {
            CreativeItemGroupRenderer.background(graphics, slot, groups.isHeader(index));
        }
    }

    public void renderItemGroupBadge(GuiGraphics graphics, Slot slot) {
        if (!this.host.visualCreativeTabEditor$isCreativeSlot(slot)) { return; }
        CreativeItemGroups groups = itemGroups();
        int index = displayItemIndex(slot);
        if (groups.isHeader(index)) {
            CreativeItemGroupRenderer.badge(graphics, slot, groups.isExpanded(index));
        }
    }

    private int displayItemIndex(Slot slot) {
        if (slot.index < 0 || slot.index >= 45) {
            return -1;
        }
        int row = this.host.visualCreativeTabEditor$menu()
                .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
        return row * 9 + slot.index;
    }

    private int absoluteItemIndex(Slot slot) {
        return itemGroups().sourceIndex(displayItemIndex(slot));
    }

    private int itemIndexForSlot(@Nullable Slot slot) {
        if (slot == null || !slot.hasItem()) {
            return -1;
        }
        if (itemGroups().isHeader(displayItemIndex(slot))) {
            return -1;
        }
        if (currentType() != CreativeTabType.SEARCH) {
            return absoluteItemIndex(slot);
        }
        if (this.draft == null) {
            return -1;
        }
        return draft().indexOfPreparedSearchItem(slot.getItem(), CreativeTabValidation.MAX_ITEMS_PER_TAB);
    }

    private int itemInsertionIndex(double x, double y) {
        Slot slot = creativeSlotAt(x, y);
        if (slot == null) {
            return -1;
        }
        int size = currentItemCount();
        if (currentType() == CreativeTabType.SEARCH) {
            int index = itemIndexForSlot(slot);
            if (index >= 0) {
                int absoluteX = this.host.visualCreativeTabEditor$left() + slot.x;
                return Math.min(index + (x >= absoluteX + 8 ? 1 : 0), size);
            }
            if (slot.hasItem()) {
                return -1;
            }
            List<ItemStack> menuItems = this.host.visualCreativeTabEditor$menu().items;
            for (int menuIndex = menuItems.size() - 1; menuIndex >= 0; menuIndex--) {
                int preparedIndex = indexOfMatchingPreparedSearchItem(menuItems.get(menuIndex));
                if (preparedIndex >= 0) {
                    return Math.min(preparedIndex + 1, size);
                }
            }
            return -1;
        }
        int index = absoluteItemIndex(slot);
        if (index < 0) {
            return -1;
        }
        int absoluteX = this.host.visualCreativeTabEditor$left() + slot.x;
        if (slot.hasItem() && x >= absoluteX + 8) {
            index++;
        }
        return Math.min(index, size);
    }

    private int indexOfMatchingPreparedSearchItem(ItemStack target) {
        if (this.draft == null) {
            return -1;
        }
        return draft().indexOfPreparedSearchItem(target, CreativeTabValidation.MAX_ITEMS_PER_TAB);
    }

    private boolean handleItemDragTabHover(double x, double y) {
        if (this.mode != Mode.DRAG_ITEMS) {
            clearItemDragTabHover();
            return false;
        }
        if (currentType() != CreativeTabType.CATEGORY) {
            clearItemDragTabHover();
            return false;
        }
        CreativeModeTab hoveredTab = tabAt(x, y);
        if (hoveredTab == null) {
            boolean overTabStrip = isInTabStrip(x, y);
            clearItemDragTabHover();
            return overTabStrip;
        }
        ResourceLocation targetId = CreativeTabRuntime.id(hoveredTab).orElse(null);
        ResourceLocation sourceId = draft().currentTabId().orElse(null);
        if (targetId == null || targetId.equals(sourceId)) {
            clearItemDragTabHover();
            return true;
        }

        long now = Util.getMillis();
        if (!targetId.equals(this.itemDragHoverTabId)) {
            this.itemDragHoverTabId = targetId;
            this.itemDragHoverStartedAt = now;
            this.itemDragHoverBlockedLogged = false;
            trace(
                    "item-drag-tab-hover-start source={} target={} selectedItems={} pointer=({}, {}) dwellMs={}",
                    sourceId,
                    targetId,
                    draft().selectedItemIndices().size(),
                    x,
                    y,
                    ITEM_DRAG_TAB_HOVER_MILLIS
            );
        }

        CreativeTabDefinition target = draft().definition(targetId).orElse(null);
        if (target == null || target.hidden() || target.type() != CreativeTabType.CATEGORY) {
            if (!this.itemDragHoverBlockedLogged) {
                this.itemDragHoverBlockedLogged = true;
                trace(
                        "item-drag-tab-hover-blocked target={} type={} hidden={} reason=not-editable-category",
                        targetId,
                        target == null ? "missing" : target.type(),
                        target != null && target.hidden()
                );
            }
            return true;
        }

        long elapsed = now - this.itemDragHoverStartedAt;
        if (elapsed < ITEM_DRAG_TAB_HOVER_MILLIS) {
            return true;
        }
        checkpoint();
        int moved;
        if (this.press != null && this.press.groupId != null) {
            CreativeTabDraft.GroupMove result = draft().moveItemGroupToTab(this.press.groupId, targetId);
            moved = result.movedItems();
            if (moved > 0) { this.press.groupId = result.groupId(); }
        } else {
            moved = draft().moveSelectedItemsToTab(targetId);
        }
        if (moved < 0) {
            trace(
                    "item-drag-tab-hover-blocked source={} target={} reason=target-capacity elapsedMs={}",
                    sourceId,
                    targetId,
                    elapsed
            );
            clearItemDragTabHover();
            return true;
        }
        if (moved <= 0) {
            trace(
                    "item-drag-tab-hover-blocked source={} target={} reason=no-selected-items elapsedMs={}",
                    sourceId,
                    targetId,
                    elapsed
            );
            clearItemDragTabHover();
            return true;
        }
        if (this.press != null) {
            this.press.dropTargetCaptured = false;
            int firstSelected = draft().selectedItemIndices().stream().findFirst().orElse(-1);
            this.press.originGroupId = firstSelected < 0 ? null : draft().itemGroupId(firstSelected);
        }
        preview();
        this.host.visualCreativeTabEditor$selectTab(hoveredTab);
        int focusIndex = draft().selectedItemIndices().stream().findFirst().orElse(-1);
        refreshCurrentDraftItems(0, focusIndex, "item-drag-tab-open");
        this.lastDragTarget = -1;
        trace(
                "item-drag-tab-hover-open source={} target={} movedItems={} elapsedMs={} selectedAfter={} pointer=({}, {})",
                sourceId,
                targetId,
                moved,
                elapsed,
                tabId(this.host.visualCreativeTabEditor$selectedTab()),
                x,
                y
        );
        clearItemDragTabHover();
        return true;
    }

    private void clearItemDragTabHover() {
        this.itemDragHoverTabId = null;
        this.itemDragHoverStartedAt = 0L;
        this.itemDragHoverBlockedLogged = false;
    }

    private void handlePageHover(double x, double y) {
        if (this.mode != Mode.DRAG_TABS && this.mode != Mode.DRAG_ITEMS) {
            clearPageHover("not-dragging");
            return;
        }
        int direction = pageHoverDirectionAt(x, y);
        if (direction == 0) {
            clearPageHover("pointer-left-edge");
            return;
        }
        CreativeTabClientPlatform.PageState page = CreativeTabClientPlatform.pageState(
                this.host.visualCreativeTabEditor$screen()
        );
        long now = Util.getMillis();
        if (direction != this.pageHoverDirection) {
            this.pageHoverDirection = direction;
            this.pageHoverOrigin = page.index();
            this.pageHoverStartedAt = now;
            this.pageHoverBlockedLogged = false;
            this.pageHoverTurnConsumed = false;
            trace(
                    "page-hover-start mode={} direction={} page={}/{} pointer=({}, {}) dwellMs={} reason=direction-change",
                    this.mode,
                    direction,
                    page.index() + 1,
                    page.count(),
                    x,
                    y,
                    PAGE_TURN_HOVER_MILLIS
            );
            return;
        }
        if (this.pageHoverTurnConsumed) {
            if (page.index() != this.pageHoverOrigin) {
                trace(
                        "page-hover-latched-page-change mode={} direction={} recordedPage={} actualPage={} count={} pointer=({}, {}) action=keep-latched",
                        this.mode,
                        direction,
                        this.pageHoverOrigin + 1,
                        page.index() + 1,
                        page.count(),
                        x,
                        y
                );
                this.pageHoverOrigin = page.index();
            }
            return;
        }
        if (page.index() != this.pageHoverOrigin) {
            int previousOrigin = this.pageHoverOrigin;
            this.pageHoverOrigin = page.index();
            this.pageHoverStartedAt = now;
            this.pageHoverBlockedLogged = false;
            trace(
                    "page-hover-rearm mode={} direction={} previousPage={} page={}/{} pointer=({}, {}) dwellMs={} reason=page-changed-before-turn",
                    this.mode,
                    direction,
                    previousOrigin + 1,
                    page.index() + 1,
                    page.count(),
                    x,
                    y,
                    PAGE_TURN_HOVER_MILLIS
            );
            return;
        }
        if (!page.canMove(direction)) {
            if (!this.pageHoverBlockedLogged) {
                this.pageHoverBlockedLogged = true;
                trace(
                        "page-turn-blocked mode={} direction={} page={}/{} reason=boundary pointer=({}, {})",
                        this.mode,
                        direction,
                        page.index() + 1,
                        page.count(),
                        x,
                        y
                );
            }
            return;
        }
        long elapsed = now - this.pageHoverStartedAt;
        if (elapsed < PAGE_TURN_HOVER_MILLIS) {
            return;
        }
        int target = page.index() + direction;
        boolean switched = CreativeTabClientPlatform.switchToPage(
                this.host.visualCreativeTabEditor$screen(),
                target
        );
        CreativeTabClientPlatform.PageState after = CreativeTabClientPlatform.pageState(
                this.host.visualCreativeTabEditor$screen()
        );
        if (switched) {
            clearItemDragTabHover();
            this.lastDragTarget = -1;
            this.pageHoverTurnConsumed = true;
        }
        trace(
                "page-turn mode={} direction={} from={} target={} switched={} after={}/{} elapsedMs={} pointer=({}, {}) latched={} rearm=leave-edge-or-change-direction",
                this.mode,
                direction,
                page.index(),
                target,
                switched,
                after.index() + 1,
                after.count(),
                elapsed,
                x,
                y,
                this.pageHoverTurnConsumed
        );
        this.pageHoverOrigin = after.index();
        this.pageHoverStartedAt = now;
        this.pageHoverBlockedLogged = !switched;
    }

    private int pageHoverDirectionAt(double x, double y) {
        int left = this.host.visualCreativeTabEditor$left();
        int top = this.host.visualCreativeTabEditor$top();
        int right = left + this.host.visualCreativeTabEditor$imageWidth();
        int bottom = top + this.host.visualCreativeTabEditor$imageHeight();
        if (y < top - 32 || y >= bottom + 32 || tabAt(x, y) != null || isEditorControlAt(x, y)
                || this.host.visualCreativeTabEditor$isInteractiveWidgetAt(x, y)) {
            return 0;
        }
        TabPlusTarget plus = tabPlusTarget();
        if (plus != null && plus.hitRect().contains(x, y)) {
            return 0;
        }
        if (x < left || x >= left && x < left + 9) {
            return -1;
        }
        if (x >= right || x >= right - 6 && x < right) {
            return 1;
        }
        return 0;
    }

    private boolean screenMatchesDraftCurrentTab() {
        if (this.draft == null) {
            return false;
        }
        ResourceLocation screenTab = CreativeTabRuntime.id(this.host.visualCreativeTabEditor$selectedTab()).orElse(null);
        return Objects.equals(screenTab, this.draft.currentTabId().orElse(null));
    }

    private boolean isEditorControlAt(double x, double y) {
        return buttonAt(x, y, saveRect())
                || buttonAt(x, y, cancelRect())
                || buttonAt(x, y, redoRect())
                || buttonAt(x, y, undoRect())
                || buttonAt(x, y, restoreRect())
                || buttonAt(x, y, defaultOrderRect())
                || buttonAt(x, y, globalSaveRect())
                || buttonAt(x, y, deleteRect())
                || buttonAt(x, y, sortRect())
                || groupControlsAvailable() && buttonAt(x, y, groupControlsRect())
                || isCreativeScrollbarAt(x, y);
    }

    private void clearPageHover(String reason) {
        if (this.pageHoverDirection != 0) {
            trace(
                    "page-hover-reset reason={} direction={} origin={} elapsedMs={}",
                    reason,
                    this.pageHoverDirection,
                    this.pageHoverOrigin,
                    Math.max(0L, Util.getMillis() - this.pageHoverStartedAt)
            );
        }
        this.pageHoverDirection = 0;
        this.pageHoverOrigin = -1;
        this.pageHoverStartedAt = 0L;
        this.pageHoverBlockedLogged = false;
        this.pageHoverTurnConsumed = false;
    }

    private int tabInsertionIndex(double x, double y) {
        CreativeModeTab tab = tabAt(x, y);
        if (tab == null) {
            List<CreativeTabDefinition> definitions = draft().tabs();
            List<Integer> visibleIndices = new ArrayList<>();
            for (CreativeModeTab visible : CreativeTabClientPlatform.visibleTabs(this.host.visualCreativeTabEditor$screen())) {
                ResourceLocation visibleId = CreativeTabRuntime.id(visible).orElse(null);
                if (visibleId == null || COMMON_TAB_IDS.contains(visibleId)) {
                    continue;
                }
                for (int index = 0; index < definitions.size(); index++) {
                    if (definitions.get(index).id().equals(visibleId)) {
                        visibleIndices.add(index);
                        break;
                    }
                }
            }
            if (visibleIndices.isEmpty()) {
                return definitions.size();
            }
            int left = this.host.visualCreativeTabEditor$left();
            int middle = left + this.host.visualCreativeTabEditor$imageWidth() / 2;
            return x < middle
                    ? visibleIndices.stream().mapToInt(Integer::intValue).min().orElse(0)
                    : visibleIndices.stream().mapToInt(Integer::intValue).max().orElse(definitions.size() - 1) + 1;
        }
        ResourceLocation id = CreativeTabRuntime.id(tab).orElse(null);
        if (id == null) {
            return draft().tabCount();
        }
        List<CreativeTabDefinition> tabs = draft().tabs();
        for (int index = 0; index < tabs.size(); index++) {
            if (tabs.get(index).id().equals(id)) {
                return index;
            }
        }
        return tabs.size();
    }

    private boolean isBlankEditorEntryArea(double x, double y) {
        int left = this.host.visualCreativeTabEditor$left();
        int top = this.host.visualCreativeTabEditor$top();
        int right = left + this.host.visualCreativeTabEditor$imageWidth();
        int bottom = top + this.host.visualCreativeTabEditor$imageHeight();
        boolean frame = x >= left && x < right && y >= top && y < bottom
                && (x < left + 9 || x >= right - 6 || y < top + 18 || y >= bottom - 6);
        boolean candidate = frame || isInTabStrip(x, y);
        if (!candidate) {
            return false;
        }
        boolean blocked = tabAt(x, y) != null
                || slotAt(x, y) != null
                || isCreativeScrollbarAt(x, y)
                || this.host.visualCreativeTabEditor$isPageButtonAt(x, y)
                || this.host.visualCreativeTabEditor$isInteractiveWidgetAt(x, y);
        trace(
                "blank-frame-hit-test x={} y={} candidate={} frame={} tabStrip={} blocked={} tab={} slot={} scrollbar={} pageButton={} widget={}",
                x,
                y,
                candidate,
                frame,
                isInTabStrip(x, y),
                blocked,
                tabId(tabAt(x, y)),
                slotAt(x, y) == null ? -1 : slotAt(x, y).index,
                isCreativeScrollbarAt(x, y),
                this.host.visualCreativeTabEditor$isPageButtonAt(x, y),
                this.host.visualCreativeTabEditor$isInteractiveWidgetAt(x, y)
        );
        return !blocked;
    }

    private boolean isInTabStrip(double x, double y) {
        int left = this.host.visualCreativeTabEditor$left();
        int top = this.host.visualCreativeTabEditor$top();
        boolean horizontal = x >= left && x < left + this.host.visualCreativeTabEditor$imageWidth();
        boolean vertical = (y >= top - 32 && y < top) || (y >= top + this.host.visualCreativeTabEditor$imageHeight() && y < top + this.host.visualCreativeTabEditor$imageHeight() + 32);
        return horizontal && vertical;
    }

    private boolean isCreativeScrollbarAt(double x, double y) {
        int left = this.host.visualCreativeTabEditor$left();
        int top = this.host.visualCreativeTabEditor$top();
        return x >= left + 175
                && x < left + 189
                && y >= top + 18
                && y < top + 130;
    }

    private CreativeTabType currentType() {
        return draft().currentTabType();
    }

    private int currentItemCount() {
        return this.draft == null ? -1 : this.draft.currentItemCount();
    }

    private int currentSearchItemCount() {
        return this.draft == null ? -1 : this.draft.currentSearchItemCount();
    }

    private ItemStack currentItemAt(int index) {
        if (this.draft == null) {
            return ItemStack.EMPTY;
        }
        return this.draft.currentItemAt(index);
    }

    private int indexOfMatchingCurrentItem(ItemStack target, int excludedIndex) {
        if (this.draft == null || target.isEmpty()) {
            return -1;
        }
        return this.draft.indexOfMatchingCurrentItem(target, excludedIndex);
    }

    private void updateVirtualTail(String reason) {
        boolean enabled = this.draft != null
                && this.mode != Mode.NORMAL
                && this.mode != Mode.SAVING
                && this.mode != Mode.CONFLICT
                && !isPicker()
                && currentType() == CreativeTabType.CATEGORY;
        setVirtualTail(enabled, reason);
    }

    private void setVirtualTail(boolean enabled, String reason) {
        CreativeModeItemPickerMenuVirtualTail access =
                (CreativeModeItemPickerMenuVirtualTail) this.host.visualCreativeTabEditor$menu();
        boolean before = access.visualCreativeTabEditor$hasVirtualTail();
        if (before == enabled && this.virtualTailEnabled == enabled) {
            return;
        }
        var menu = this.host.visualCreativeTabEditor$menu();
        int oldRow = menu.getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
        access.visualCreativeTabEditor$setVirtualTail(enabled);
        this.virtualTailEnabled = enabled;
        int logicalSize = itemGroups().size() + (enabled ? 1 : 0);
        int maxRow = Math.max(0, Mth.positiveCeilDiv(logicalSize, 9) - 5);
        int restoredRow = Mth.clamp(oldRow, 0, maxRow);
        float scroll = maxRow == 0 ? 0.0F : menu.getScrollForRowIndex(restoredRow);
        this.host.visualCreativeTabEditor$setScrollOffset(scroll);
        this.host.visualCreativeTabEditor$menu().scrollTo(scroll);
        trace(
                "virtual-tail-change reason={} before={} after={} menuItems={} oldRow={} restoredRow={} maxRow={} scroll={}",
                reason,
                before,
                enabled,
                this.host.visualCreativeTabEditor$menu().items.size(),
                oldRow,
                restoredRow,
                maxRow,
                scroll
        );
    }

    private void refreshCurrentDraftItems(int preferredRow, String reason) {
        refreshCurrentDraftItems(preferredRow, -1, reason);
    }

    private void refreshCurrentDraftItems(int preferredRow, int focusIndex, String reason) {
        if (this.draft == null || !canReorderCurrentItems()) {
            return;
        }
        List<ItemStack> expected = draft().currentItems();
        var menu = this.host.visualCreativeTabEditor$menu();
        List<ItemStack> previousMenuItems = new ArrayList<>(menu.items);
        Collection<ItemStack> runtime = this.host.visualCreativeTabEditor$selectedTab().getDisplayItems();
        List<ItemStack> displayed = expected;
        if (currentType() == CreativeTabType.SEARCH) {
// @target if minecraft_version=1.20.1
//|            Set<ItemStack> previousView = ItemStackLinkedSet.createTypeAndTagSet();
// @target else
            Set<ItemStack> previousView = ItemStackLinkedSet.createTypeAndComponentsSet();
// @target end
            previousView.addAll(previousMenuItems);
            displayed = new ArrayList<>(previousMenuItems.size());
            for (ItemStack stack : expected) {
                if (previousView.contains(stack)) {
                    displayed.add(stack.copyWithCount(1));
                }
            }
        }
        menu.items.clear();
        for (ItemStack stack : displayed) {
            menu.items.add(stack.copyWithCount(1));
        }
        CreativeItemGroups groups = itemGroups();
        groups.invalidate();
        groups.refresh(menu.items);
        updateVirtualTail(reason + "-refresh");
        int displayedFocusIndex = focusIndex;
        if (currentType() == CreativeTabType.SEARCH && focusIndex >= 0 && focusIndex < expected.size()) {
            displayedFocusIndex = indexOfMatching(displayed, expected.get(focusIndex));
        }
        if (displayedFocusIndex >= 0 && displayedFocusIndex < displayed.size()) {
            displayedFocusIndex = groups.reveal(displayedFocusIndex, menu.items);
        }
        int logicalSize = groups.size() + (this.virtualTailEnabled ? 1 : 0);
        int maxRow = Math.max(0, Mth.positiveCeilDiv(logicalSize, 9) - 5);
        int row = Mth.clamp(preferredRow, 0, maxRow);
        if (displayedFocusIndex >= 0 && displayedFocusIndex < groups.size()) {
            if (displayedFocusIndex < row * 9) {
                row = displayedFocusIndex / 9;
            } else if (displayedFocusIndex >= (row + 5) * 9) {
                row = displayedFocusIndex / 9 - 4;
            }
            row = Mth.clamp(row, 0, maxRow);
        }
        float scroll = maxRow == 0 ? 0.0F : menu.getScrollForRowIndex(row);
        this.host.visualCreativeTabEditor$setScrollOffset(scroll);
        menu.scrollTo(scroll);
        int mismatch = firstMismatch(expected, runtime);
        trace(
                "draft-menu-refresh reason={} tab={} expectedItems={} previousMenuItems={} menuItems={} runtimeItems={} firstMismatch={} row={}/{} focusIndex={} displayedFocusIndex={} scroll={} plusVisible={}",
                reason,
                draft().currentTabId().orElse(null),
                expected.size(),
                previousMenuItems.size(),
                menu.items.size(),
                runtime.size(),
                mismatch,
                row,
                maxRow,
                focusIndex,
                displayedFocusIndex,
                scroll,
                itemPlusRect() != null
        );
    }

    private static int firstMismatch(List<ItemStack> expected, Collection<ItemStack> actual) {
        Iterator<ItemStack> left = expected.iterator();
        Iterator<ItemStack> right = actual.iterator();
        int index = 0;
        while (left.hasNext() && right.hasNext()) {
// @target if minecraft_version=1.20.1
//|            if (!ItemStack.isSameItemSameTags(left.next(), right.next())) {
// @target else
            if (!ItemStack.isSameItemSameComponents(left.next(), right.next())) {
// @target end
                return index;
            }
            index++;
        }
        return left.hasNext() || right.hasNext() ? index : -1;
    }

    private static int indexOfMatching(List<ItemStack> items, ItemStack target) {
        for (int index = 0; index < items.size(); index++) {
// @target if minecraft_version=1.20.1
//|            if (ItemStack.isSameItemSameTags(items.get(index), target)) {
// @target else
            if (ItemStack.isSameItemSameComponents(items.get(index), target)) {
// @target end
                return index;
            }
        }
        return -1;
    }

    private boolean canReorderCurrentItems() {
        CreativeTabType type = currentType();
        return type == CreativeTabType.CATEGORY || type == CreativeTabType.SEARCH;
    }

    private void ensureSearchOrderPrepared(@Nullable CreativeModeTab selected, String reason) {
        if (this.draft == null || selected == null) {
            return;
        }
        ResourceLocation selectedId = CreativeTabRuntime.id(selected).orElse(null);
        CreativeTabDefinition definition = selectedId == null
                ? null
                : this.draft.definition(selectedId).orElse(null);
        if (definition == null || definition.type() != CreativeTabType.SEARCH) {
            return;
        }
        ResourceLocation currentBefore = this.draft.currentTabId().orElse(null);
        if (!selectedId.equals(currentBefore)) {
            this.draft.setCurrentTab(selectedId);
        }
        if (this.draft.hasPreparedCurrentSearchItems()) {
            trace(
                    "search-order-prepare-skip reason={} tab={} preparedItems={} persistedPreference={} currentBefore={}",
                    reason,
                    selectedId,
                    this.draft.currentItemCount(),
                    definition.itemCount(),
                    currentBefore
            );
            return;
        }
        Collection<ItemStack> visible = selected.getDisplayItems();
        List<ItemStack> preferredBefore = definition.items();
        this.draft.prepareCurrentSearchItems(visible);
        List<ItemStack> prepared = this.draft.currentItems();
        trace(
                "search-order-prepare reason={} tab={} visible={} prepared={} persistedPreference={} firstMismatch={} persistedChanged=false currentBefore={}",
                reason,
                selectedId,
                visible.size(),
                prepared.size(),
                preferredBefore.size(),
                firstMismatch(prepared, visible),
                currentBefore
        );
    }

    private void logDraftSanitization(CreativeTabCatalog source) {
        for (CreativeTabDefinition before : source.orderedDefinitions()) {
            CreativeTabDefinition after = draft().definition(before.id()).orElse(null);
            if (after == null) {
                continue;
            }
            int removedItems = before.itemCount() - after.itemCount();
            int removedSearchItems = before.searchItemCount() - after.searchItemCount();
            if (removedItems > 0 || removedSearchItems > 0) {
                trace(
                        "draft-sanitize tab={} itemsBefore={} itemsAfter={} removedDuplicates={} searchBefore={} searchAfter={} removedSearchDuplicates={}",
                        before.id(),
                        before.itemCount(),
                        after.itemCount(),
                        removedItems,
                        before.searchItemCount(),
                        after.searchItemCount(),
                        removedSearchItems
                );
            }
        }
    }

    private List<CreativeTabSortMode> sortModes() {
        List<CreativeTabSortMode> modes = new ArrayList<>(List.of(
                CreativeTabSortMode.TYPE,
                CreativeTabSortMode.ID_PATH,
                CreativeTabSortMode.LOCALIZED_NAME,
                CreativeTabSortMode.FULL_ID
        ));
        if (currentType() == CreativeTabType.SEARCH) {
            modes.add(CreativeTabSortMode.MOD_ID);
        }
        return modes;
    }

    private Comparator<ItemStack> itemComparator(CreativeTabSortMode mode) {
        if (mode != CreativeTabSortMode.TYPE) {
            return mode.comparator();
        }
        List<CreativeModeTab> categories = CreativeTabRuntime
                .effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
                .toList();
        List<CreativeModeTab> ordered = new ArrayList<>(categories);
        Map<CreativeModeTab, Integer> sourceOrder = new IdentityHashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            sourceOrder.put(ordered.get(index), index);
        }
        ordered.sort(Comparator.comparingInt(tab -> {
            ResourceLocation id = CreativeTabRuntime.id(tab).orElse(null);
            int known = id == null ? -1 : SEMANTIC_CATEGORY_ORDER.indexOf(id);
            return known >= 0
                    ? known
                    : SEMANTIC_CATEGORY_ORDER.size() + sourceOrder.getOrDefault(tab, Integer.MAX_VALUE / 2);
        }));

        Map<Item, CreativeTabSortMode.SemanticRank> ranks = new IdentityHashMap<>();
        List<String> groupTrace = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < ordered.size(); groupIndex++) {
            CreativeModeTab tab = ordered.get(groupIndex);
            int itemIndex = 0;
            int inserted = 0;
            for (ItemStack stack : tab.getDisplayItems()) {
                if (ranks.putIfAbsent(
                        stack.getItem(),
                        new CreativeTabSortMode.SemanticRank(groupIndex, itemIndex)
                ) == null) {
                    inserted++;
                }
                itemIndex++;
            }
            groupTrace.add(tabId(tab) + ':' + inserted);
        }
        trace(
                "sort-semantic-index groups={} rankedItems={} order={}",
                ordered.size(),
                ranks.size(),
                groupTrace
        );
        return mode.comparator(ranks);
    }

    private String sortKey(CreativeTabSortMode mode) {
        return "visual_creative_tab_editor.editor.sort." + mode.name().toLowerCase(java.util.Locale.ROOT);
    }

    private Rect sideActionRect(int row) {
        return new Rect(sideControlX(), this.host.visualCreativeTabEditor$top() + 4 + row * (BUTTON_HEIGHT + 3),
                BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private Rect undoRect() { return sideActionRect(2); }
    private Rect redoRect() { return sideActionRect(3); }
    private Rect restoreRect() { return sideActionRect(4); }
    private Rect defaultOrderRect() { return sideActionRect(5); }
    private Rect globalSaveRect() { return sideActionRect(6); }

    private Rect saveRect() {
        return new Rect(sideControlX(), this.host.visualCreativeTabEditor$top() + 4, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private Rect cancelRect() {
        return new Rect(sideControlX(), this.host.visualCreativeTabEditor$top() + BUTTON_HEIGHT + 7, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private Rect sortRect() {
        int x = this.host.visualCreativeTabEditor$left()
                + this.host.visualCreativeTabEditor$imageWidth()
                - 20
                - 54
                - 3;
        return new Rect(x, controlY(), 54, BUTTON_HEIGHT);
    }

    private Rect deleteRect() {
        return new Rect(this.host.visualCreativeTabEditor$left() + 23, controlY(), BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private int controlY() {
        return Math.max(2, this.host.visualCreativeTabEditor$top() - 53);
    }

    private int sideControlX() {
        int left = this.host.visualCreativeTabEditor$left();
        if (left >= BUTTON_WIDTH + 6) {
            return left - BUTTON_WIDTH - 4;
        }
        int right = left + this.host.visualCreativeTabEditor$imageWidth();
        if (this.host.visualCreativeTabEditor$screenWidth() - right >= BUTTON_WIDTH + 6) {
            return right + 4;
        }
        return 2;
    }

    private @Nullable TabPlusTarget tabPlusTarget() {
        CreativeTabClientPlatform.PageState page = CreativeTabClientPlatform.pageState(
                this.host.visualCreativeTabEditor$screen()
        );
        if (!page.isLast()) {
            return null;
        }
        long ordinaryCount = CreativeTabClientPlatform.visibleTabs(this.host.visualCreativeTabEditor$screen()).stream()
                .map(CreativeTabRuntime::id)
                .flatMap(java.util.Optional::stream)
                .filter(id -> !COMMON_TAB_IDS.contains(id))
                .count();
        if (ordinaryCount < ORDINARY_TABS_PER_PAGE) {
            int index = (int) ordinaryCount;
            CreativeModeTab.Row row = index < ORDINARY_TABS_PER_ROW
                    ? CreativeModeTab.Row.TOP
                    : CreativeModeTab.Row.BOTTOM;
            int column = index % ORDINARY_TABS_PER_ROW;
            int x = this.host.visualCreativeTabEditor$left() + column * 27;
            int hitY = this.host.visualCreativeTabEditor$top()
                    + (row == CreativeModeTab.Row.TOP ? -32 : this.host.visualCreativeTabEditor$imageHeight());
            int spriteY = this.host.visualCreativeTabEditor$top()
                    + (row == CreativeModeTab.Row.TOP ? -28 : this.host.visualCreativeTabEditor$imageHeight() - 4);
            return new TabPlusTarget(
                    new Rect(x, hitY, 27, 32),
                    new Rect(x, spriteY, 26, 32),
                    row,
                    column,
                    false
            );
        }

        int right = this.host.visualCreativeTabEditor$left() + this.host.visualCreativeTabEditor$imageWidth() + 3;
        boolean placeRight = right + 26 <= this.host.visualCreativeTabEditor$screenWidth();
        int x = placeRight ? right : Math.max(2, this.host.visualCreativeTabEditor$left() - 29);
        int hitY = this.host.visualCreativeTabEditor$top() - 32;
        int spriteY = this.host.visualCreativeTabEditor$top() - 28;
        int column = placeRight ? 6 : 0;
        return new TabPlusTarget(
                new Rect(x, hitY, 27, 32),
                new Rect(x, spriteY, 26, 32),
                CreativeModeTab.Row.TOP,
                column,
                true
        );
    }

    private @Nullable Rect itemPlusRect() {
        int size = itemGroups().size();
        int row = this.host.visualCreativeTabEditor$menu()
                .getRowIndexForScroll(this.host.visualCreativeTabEditor$scrollOffset());
        int relative = size - row * 9;
        if (relative < 0 || relative >= 45) {
            return null;
        }
        return new Rect(
                this.host.visualCreativeTabEditor$left() + 9 + (relative % 9) * 18,
                this.host.visualCreativeTabEditor$top() + 18 + (relative / 9) * 18,
                16,
                16
        );
    }

    private List<ContextAction> contextActions() {
        if (this.mode == Mode.CONTEXT_TAB || this.contextGroupId != null) {
            return List.of(ContextAction.MODIFY, ContextAction.DELETE);
        }
        List<ContextAction> actions = new ArrayList<>();
        if (currentType() == CreativeTabType.CATEGORY) {
            actions.add(ContextAction.MODIFY);
            actions.add(ContextAction.DELETE);
        }
        if (CreativeTabClientConfig.groupSimilarItems() && draft().selectedItemCount() >= 2) {
            actions.add(ContextAction.MERGE);
        }
        if (this.contextMemberGroupId != null) {
            actions.add(ContextAction.DETACH);
        }
        return actions;
    }

    private Rect contextActionRect(int index) {
        Rect menu = contextMenuRect();
        return new Rect(menu.x, menu.y + index * BUTTON_HEIGHT, menu.width, BUTTON_HEIGHT);
    }

    private Rect contextMenuRect() {
        int margin = 2;
        int gap = 4;
        int width = 60;
        List<ContextAction> actions = contextActions();
        for (ContextAction action : actions) {
            width = Math.max(width, this.host.visualCreativeTabEditor$font().width(
                    Component.translatable(action.label(this.contextGroupId != null))) + 12);
        }
        int height = BUTTON_HEIGHT * actions.size();
        int screenWidth = this.host.visualCreativeTabEditor$screenWidth();
        int screenHeight = this.host.visualCreativeTabEditor$screenHeight();
        Rect target = contextTargetRect();
        int panelLeft = this.host.visualCreativeTabEditor$left();
        int panelRight = panelLeft + this.host.visualCreativeTabEditor$imageWidth();
        int panelRightCandidate = panelRight + gap;
        int panelLeftCandidate = panelLeft - gap - width;
        int targetRightCandidate = target.right() + gap;
        int targetLeftCandidate = target.x - gap - width;
        int x;
        if (panelRightCandidate + width <= screenWidth - margin) {
            x = panelRightCandidate;
        } else if (panelLeftCandidate >= margin) {
            x = panelLeftCandidate;
        } else if (targetRightCandidate >= margin
                && targetRightCandidate + width <= screenWidth - margin) {
            x = targetRightCandidate;
        } else if (targetLeftCandidate >= margin
                && targetLeftCandidate + width <= screenWidth - margin) {
            x = targetLeftCandidate;
        } else {
            x = Mth.clamp(this.contextX, margin, Math.max(margin, screenWidth - margin - width));
        }
        int y = Mth.clamp(target.y, margin, Math.max(margin, screenHeight - margin - height));
        Rect menu = new Rect(x, y, width, height);
        if (!menu.intersects(target)) {
            return menu;
        }
        int below = target.bottom() + gap;
        int above = target.y - gap - height;
        x = Mth.clamp(target.x, margin, Math.max(margin, screenWidth - margin - width));
        if (below + height <= screenHeight - margin) {
            return new Rect(x, below, width, height);
        }
        if (above >= margin) {
            return new Rect(x, above, width, height);
        }
        return menu;
    }

    private Rect creativeUiColumnRect() {
        return new Rect(
                this.host.visualCreativeTabEditor$left(),
                0,
                this.host.visualCreativeTabEditor$imageWidth(),
                this.host.visualCreativeTabEditor$screenHeight()
        );
    }

    private Rect contextTargetRect() {
        return new Rect(this.contextTargetX - 2, this.contextTargetY - 2, 20, 20);
    }

    private Rect propertiesPanel() {
        int width = 190;
        int height = 80;
        return new Rect(
                (this.host.visualCreativeTabEditor$screenWidth() - width) / 2,
                (this.host.visualCreativeTabEditor$screenHeight() - height) / 2,
                width,
                height
        );
    }

    private Rect sortMenuRect(int rows) {
        int width = 112;
        return new Rect(
                (this.host.visualCreativeTabEditor$screenWidth() - width) / 2,
                (this.host.visualCreativeTabEditor$screenHeight() - rows * BUTTON_HEIGHT) / 2,
                width,
                rows * BUTTON_HEIGHT
        );
    }

    private static boolean buttonAt(double x, double y, Rect rect) {
        return rect.contains(x, y);
    }

    private boolean isEditing() {
        return this.mode != Mode.NORMAL;
    }

    private boolean isPicker() {
        return this.mode == Mode.PICK_REPLACEMENT
                || this.mode == Mode.PICK_NEW_ITEM
                || this.mode == Mode.PICK_TAB_ICON
                || this.mode == Mode.PICK_NEW_TAB;
    }

    private boolean isModal() {
        return this.mode == Mode.CONTEXT_TAB
                || this.mode == Mode.CONTEXT_ITEM
                || this.mode == Mode.TAB_PROPERTIES
                || this.mode == Mode.SORT_MENU
                || this.mode == Mode.GROUP_CONTROLS
                || this.mode == Mode.SAVING
                || this.mode == Mode.CONFLICT
                || isPicker();
    }

    private void tracePressStart(Press active) {
        trace(
                "press-start kind={} mode={} press=({}, {}) tab={} item={} slot={} startedAt={}",
                active.kind,
                this.mode,
                active.x,
                active.y,
                tabId(active.tab),
                itemId(active.slot),
                active.slot == null ? -1 : active.slot.index,
                active.startedAt
        );
    }

// @target if minecraft_version=26.1.2,26.2
//|    private void traceDragStart(Press active, MouseButtonEvent event, Mode dragMode) {
// @target else
    private void traceDragStart(Press active, double mouseX, double mouseY, Mode dragMode) {
// @target end
        if (active.dragStartedLogged) {
            return;
        }
        active.dragStartedLogged = true;
        this.dragVisual = createDragVisual(active);
// @target if minecraft_version=26.1.2,26.2
//|        double ghostX = this.dragVisual == null ? event.x() - 8.0D : event.x() - this.dragVisual.grabOffsetX;
//|        double ghostY = this.dragVisual == null ? event.y() - 8.0D : event.y() - this.dragVisual.grabOffsetY;
// @target else
        double ghostX = this.dragVisual == null ? mouseX - 8.0D : mouseX - this.dragVisual.grabOffsetX;
        double ghostY = this.dragVisual == null ? mouseY - 8.0D : mouseY - this.dragVisual.grabOffsetY;
// @target end
        trace(
                "drag-start mode={} sourceKind={} longTriggered={} currentTab={} currentType={} selectedItems={} tab={} item={} pointer=({}, {}) ghostTopLeft=({}, {}) grabOffset=({}, {}) contextTarget=({}, {})",
                dragMode,
                active.kind,
                active.longTriggered,
                draft().currentTabId().orElse(null),
                currentType(),
                draft().selectedItemIndices().size(),
                tabId(active.tab),
                itemId(active.slot),
// @target if minecraft_version=26.1.2,26.2
//|                event.x(),
//|                event.y(),
// @target else
                mouseX,
                mouseY,
// @target end
                ghostX,
                ghostY,
                this.dragVisual == null ? 8.0D : this.dragVisual.grabOffsetX,
                this.dragVisual == null ? 8.0D : this.dragVisual.grabOffsetY,
                this.contextTargetX,
                this.contextTargetY
        );
    }

    private @Nullable DragVisual createDragVisual(Press active) {
        ItemStack icon;
        Rect origin;
        if (active.tab != null) {
            ResourceLocation id = CreativeTabRuntime.id(active.tab).orElse(null);
            icon = id == null
                    ? active.tab.getIconItem()
                    : draft().definition(id).map(CreativeTabDefinition::icon).orElseGet(active.tab::getIconItem);
            origin = tabIconRect(active.tab);
        } else if (active.slot != null) {
            ItemStack current = draft().currentItemAt(active.itemIndex);
            icon = active.groupId != null ? draft().itemGroup(active.groupId).icon()
                    : current.isEmpty() ? active.slot.getItem() : current;
            origin = new Rect(
                    this.host.visualCreativeTabEditor$left() + active.slot.x,
                    this.host.visualCreativeTabEditor$top() + active.slot.y,
                    16,
                    16
            );
        } else {
            return null;
        }
        if (icon.isEmpty()) {
            return null;
        }
        return new DragVisual(
                icon.copyWithCount(1),
                Mth.floor(active.x) - origin.x,
                Mth.floor(active.y) - origin.y
        );
    }

    private void traceContextGeometry() {
        Rect menu = contextMenuRect();
        Rect target = contextTargetRect();
        Rect creativeUiColumn = creativeUiColumnRect();
        String placement = menu.x >= creativeUiColumn.right()
                ? "panel-right"
                : menu.right() <= creativeUiColumn.x ? "panel-left" : "panel-overlap-fallback";
        trace(
                "context-open mode={} requested=({}, {}) target={} menu={} targetOverlap={} creativeUiColumn={} creativeUiOverlap={} placement={} screen={}x{}",
                this.mode,
                this.contextX,
                this.contextY,
                target,
                menu,
                target.intersects(menu),
                creativeUiColumn,
                creativeUiColumn.intersects(menu),
                placement,
                this.host.visualCreativeTabEditor$screenWidth(),
                this.host.visualCreativeTabEditor$screenHeight()
        );
    }

    private static String tabId(@Nullable CreativeModeTab tab) {
        return tab == null
                ? "-"
                : CreativeTabRuntime.id(tab).map(ResourceLocation::toString).orElse("<unregistered>");
    }

    private static String itemId(@Nullable Slot slot) {
        if (slot == null || !slot.hasItem()) {
            return "-";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(slot.getItem().getItem());
        return id == null ? "<unregistered>" : id.toString();
    }

    private static String stackId(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return (id == null ? "<unregistered>" : id.toString()) + "x" + stack.getCount();
    }

    private static void trace(String message, Object... arguments) {
        VisualCreativeTabEditorConstants.LOGGER.debug("[EditorTrace] " + message, arguments);
    }

    private CreativeTabDraft draft() {
        return Objects.requireNonNull(this.draft, "editor draft");
    }

    private ResourceLocation requireContextTab() {
        return Objects.requireNonNull(this.contextTabId, "context tab");
    }

    private static int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 255) + 25);
        int green = Math.min(255, ((color >> 8) & 255) + 25);
        int blue = Math.min(255, (color & 255) + 25);
        return (color & 0xFF000000) | red << 16 | green << 8 | blue;
    }

    private enum GroupAction {
        NEW, EDIT, DELETE, ADD, DETACH, BACK;

        private String key() {
            return "visual_creative_tab_editor.group." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private enum ContextAction {
        MODIFY("visual_creative_tab_editor.editor.modify"),
        DELETE("visual_creative_tab_editor.editor.delete"),
        MERGE("visual_creative_tab_editor.group.create"),
        DETACH("visual_creative_tab_editor.group.detach");

        private final String key;

        ContextAction(String key) { this.key = key; }

        private String label(boolean group) {
            return this == DELETE && group ? "visual_creative_tab_editor.group.dissolve" : this.key;
        }
    }

    private enum Mode {
        NORMAL,
        EDIT,
        CONTEXT_TAB,
        CONTEXT_ITEM,
        TAB_PROPERTIES,
        SORT_MENU,
        GROUP_CONTROLS,
        PICK_REPLACEMENT,
        PICK_NEW_ITEM,
        PICK_TAB_ICON,
        PICK_NEW_TAB,
        DRAG_TABS,
        DRAG_ITEMS,
        SAVING,
        CONFLICT
    }

    private enum PressKind {
        NORMAL_TAB,
        NORMAL_ITEM,
        BLANK_TAB,
        EDIT_TAB,
        EDIT_ITEM
    }

    private static final class Press {
        private final PressKind kind;
        private final long startedAt = Util.getMillis();
        private final double x;
        private final double y;
        private final @Nullable CreativeModeTab tab;
        private final @Nullable Slot slot;
        private final int slotId;
        private final int button;
        private final @Nullable ClickType input;
        private int itemIndex;
        private @Nullable String groupId;
        private @Nullable String originGroupId;
        private boolean toggleGroup;
        private boolean hadGroupedItems;
        private @Nullable String dropGroupId;
        private boolean dropTargetCaptured;
        private double dropX;
        private double dropY;
// @target if minecraft_version=26.1.2,26.2
//|        private final boolean doubleClick;
// @target else
// @target end
        private boolean longTriggered;
        private boolean cancelled;
        private boolean moved;
        private boolean dragThresholdLogged;
        private boolean dragBlockedLogged;
        private boolean longPressBlockedLogged;
        private boolean dragStartedLogged;

        private Press(
                PressKind kind,
                double x,
                double y,
                @Nullable CreativeModeTab tab,
                @Nullable Slot slot,
                int slotId,
                int button,
                @Nullable ClickType input,
// @target if minecraft_version=26.1.2,26.2
//|                int itemIndex,
//|                boolean doubleClick
// @target else
                int itemIndex
// @target end
        ) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.tab = tab;
            this.slot = slot;
            this.slotId = slotId;
            this.button = button;
            this.input = input;
            this.itemIndex = itemIndex;
// @target if minecraft_version=26.1.2,26.2
//|            this.doubleClick = doubleClick;
// @target else
// @target end
        }

        private static Press tab(PressKind kind, CreativeModeTab tab, double x, double y) {
// @target if minecraft_version=26.1.2,26.2
//|            return new Press(kind, x, y, tab, null, -1, 0, null, -1, false);
// @target else
            return new Press(kind, x, y, tab, null, -1, 0, null, -1);
// @target end
        }

        private static Press item(PressKind kind, Slot slot, int itemIndex, double x, double y) {
// @target if minecraft_version=26.1.2,26.2
//|            return new Press(kind, x, y, null, slot, slot.index, 0, ClickType.PICKUP, itemIndex, false);
// @target else
            return new Press(kind, x, y, null, slot, slot.index, 0, ClickType.PICKUP, itemIndex);
// @target end
        }

        private static Press slot(
                PressKind kind,
                Slot slot,
                int slotId,
                int button,
                ClickType input,
                int itemIndex,
                double x,
// @target if minecraft_version=26.1.2,26.2
//|                double y,
//|                boolean doubleClick
// @target else
                double y
// @target end
        ) {
// @target if minecraft_version=26.1.2,26.2
//|            return new Press(kind, x, y, null, slot, slotId, button, input, itemIndex, doubleClick);
// @target else
            return new Press(kind, x, y, null, slot, slotId, button, input, itemIndex);
// @target end
        }

        private static Press blank(double x, double y) {
// @target if minecraft_version=26.1.2,26.2
//|            return new Press(PressKind.BLANK_TAB, x, y, null, null, -1, 0, null, -1, false);
// @target else
            return new Press(PressKind.BLANK_TAB, x, y, null, null, -1, 0, null, -1);
// @target end
        }

        private double distanceSquared(double mouseX, double mouseY) {
            double dx = mouseX - this.x;
            double dy = mouseY - this.y;
            return dx * dx + dy * dy;
        }
    }

    private record DragVisual(ItemStack icon, double grabOffsetX, double grabOffsetY) {
    }

    private record TabPlusTarget(
            Rect hitRect,
            Rect spriteRect,
            CreativeModeTab.Row row,
            int column,
            boolean external
    ) {
    }

    private record Rect(int x, int y, int width, int height) {
        private int right() {
            return this.x + this.width;
        }

        private int bottom() {
            return this.y + this.height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.right() && mouseY >= this.y && mouseY < this.bottom();
        }

        private boolean intersects(Rect other) {
            return this.x < other.right()
                    && this.right() > other.x
                    && this.y < other.bottom()
                    && this.bottom() > other.y;
        }
    }
}
