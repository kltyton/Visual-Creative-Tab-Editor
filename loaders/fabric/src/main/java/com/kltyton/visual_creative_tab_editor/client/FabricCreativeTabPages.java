package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
// @target if minecraft_version=1.20.1
//|import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
//|import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
//|import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
// @target elif minecraft_version=26.1.2,26.2
//|import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
//|import net.fabricmc.fabric.impl.creativetab.FabricCreativeModeTabImpl;
// @target else
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroupImpl;
// @target end
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/** Assigns Fabric pages to native and runtime-created data-driven tabs. */
public final class FabricCreativeTabPages {
// @target if minecraft_version=1.20.1
//|    private static final int TABS_PER_PAGE = 10;
// @target else
// @target end
    private static final List<ResourceKey<CreativeModeTab>> VANILLA_TABS = List.of(
            CreativeModeTabs.BUILDING_BLOCKS,
            CreativeModeTabs.COLORED_BLOCKS,
            CreativeModeTabs.NATURAL_BLOCKS,
            CreativeModeTabs.FUNCTIONAL_BLOCKS,
            CreativeModeTabs.REDSTONE_BLOCKS,
            CreativeModeTabs.HOTBAR,
            CreativeModeTabs.SEARCH,
            CreativeModeTabs.TOOLS_AND_UTILITIES,
            CreativeModeTabs.COMBAT,
            CreativeModeTabs.FOOD_AND_DRINKS,
            CreativeModeTabs.INGREDIENTS,
            CreativeModeTabs.SPAWN_EGGS,
            CreativeModeTabs.OP_BLOCKS,
            CreativeModeTabs.INVENTORY
    );
    private static final Map<CreativeModeTab, NativePosition> VANILLA_BASELINE = new IdentityHashMap<>();
    private static volatile int lastLoggedSignature = Integer.MIN_VALUE;

    private FabricCreativeTabPages() {
    }

// @target if minecraft_version=1.20.1
//|    /** Restores the 1.21+ shared-tab behavior missing from Fabric API 0.92. */
//|    public static void includeOperatorTabInCommonGroups() {
//|        FabricCreativeGuiComponents.COMMON_GROUPS.add(
//|                BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.OP_BLOCKS)
//|        );
//|    }
//|
// @target else
// @target end
    public static synchronized void repack() {
        captureVanillaBaseline();
        List<CreativeModeTab> ordinary = CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
// @target if minecraft_version=26.1.2,26.2
//|                .filter(tab -> !FabricCreativeGuiComponents.COMMON_TABS.contains(tab))
// @target else
                .filter(tab -> !FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab))
// @target end
                .toList();
        int visibleIndex = 0;
        int hiddenIndex = 0;
        int signature = 1;
        for (CreativeModeTab tab : ordinary) {
            signature = 31 * signature + System.identityHashCode(tab);
            signature = 31 * signature + (tab.shouldDisplay() ? 1 : 0);
            if (!tab.shouldDisplay()) {
// @target if minecraft_version=1.20.1
//|                ((FabricItemGroup) tab).setPage(0);
// @target elif minecraft_version=26.1.2,26.2
//|                ((FabricCreativeModeTabImpl) tab).fabric_setPage(0);
// @target else
                ((FabricItemGroupImpl) tab).fabric_setPage(0);
// @target end
                tab.row = CreativeModeTab.Row.TOP;
                tab.column = 7 + hiddenIndex++;
                continue;
            }
// @target if minecraft_version=1.20.1
//|            int pageIndex = visibleIndex % TABS_PER_PAGE;
//|            ((FabricItemGroup) tab).setPage(visibleIndex / TABS_PER_PAGE);
// @target elif minecraft_version=26.1.2,26.2
//|            int pageIndex = visibleIndex % FabricCreativeModeTabImpl.TABS_PER_PAGE;
//|            ((FabricCreativeModeTabImpl) tab).fabric_setPage(visibleIndex / FabricCreativeModeTabImpl.TABS_PER_PAGE);
// @target else
            int pageIndex = visibleIndex % FabricItemGroupImpl.TABS_PER_PAGE;
            ((FabricItemGroupImpl) tab).fabric_setPage(visibleIndex / FabricItemGroupImpl.TABS_PER_PAGE);
// @target end
            tab.row = pageIndex < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            tab.column = pageIndex % 5;
            visibleIndex++;
        }
        if (signature != lastLoggedSignature) {
            lastLoggedSignature = signature;
            VisualCreativeTabEditorConstants.LOGGER.debug(
                    "[EditorTrace] fabric-tab-pages-repacked ordinary={} visible={} hidden={} pages={}",
                    ordinary.size(),
                    visibleIndex,
                    hiddenIndex,
// @target if minecraft_version=1.20.1
//|                    Math.max(1, (visibleIndex + TABS_PER_PAGE - 1) / TABS_PER_PAGE)
// @target elif minecraft_version=26.1.2,26.2
//|                    Math.max(1, (visibleIndex + FabricCreativeModeTabImpl.TABS_PER_PAGE - 1)
//|                            / FabricCreativeModeTabImpl.TABS_PER_PAGE)
// @target else
                    Math.max(1, (visibleIndex + FabricItemGroupImpl.TABS_PER_PAGE - 1)
                            / FabricItemGroupImpl.TABS_PER_PAGE)
// @target end
            );
        }
    }

// @target if minecraft_version=1.20.1
//|    /** Returns every effective tab visible on the current Fabric page, including the shared special tabs. */
//|    public static List<CreativeModeTab> tabsOnPage(int page) {
//|        return CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
//|                .filter(CreativeModeTab::shouldDisplay)
//|                .filter(tab -> FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab)
//|                        || ((FabricItemGroup) tab).getPage() == page)
//|                .toList();
//|    }
//|
//|    /** Returns the page containing a tab; Fabric's shared special tabs belong to the current page. */
//|    public static int pageOf(CreativeModeTab tab, int currentPage) {
//|        return FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab)
//|                ? currentPage
//|                : ((FabricItemGroup) tab).getPage();
//|    }
//|
//|    /** Computes the page count from the effective runtime catalog rather than only registered groups. */
//|    public static int pageCount() {
//|        return CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
//|                .filter(CreativeModeTab::shouldDisplay)
//|                .filter(tab -> !FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab))
//|                .mapToInt(tab -> ((FabricItemGroup) tab).getPage())
//|                .max()
//|                .orElse(0) + 1;
//|    }
//|
//|    /** Uses Fabric's native previous/next operations to preserve its selected-tab correction behavior. */
//|    public static boolean switchToPage(CreativeGuiExtensions screen, int targetPage) {
//|        int pageCount = pageCount();
//|        if (targetPage < 0 || targetPage >= pageCount) {
//|            return false;
//|        }
//|        int previous = screen.fabric_currentPage();
//|        while (previous != targetPage) {
//|            if (previous < targetPage) {
//|                screen.fabric_nextPage();
//|            } else {
//|                screen.fabric_previousPage();
//|            }
//|            int current = screen.fabric_currentPage();
//|            if (current == previous) {
//|                return false;
//|            }
//|            previous = current;
//|        }
//|        return true;
//|    }
//|
//|    /** Restores native coordinates before the runtime catalog is packed again. */
//|    public static synchronized void restoreVanillaBaselineBeforeRepack() {
// @target else
    /** Restores the registered vanilla layout before Fabric paginates and validates it. */
    public static synchronized void restoreVanillaBaselineForValidation() {
// @target end
        captureVanillaBaseline();
        VANILLA_BASELINE.forEach((tab, position) -> position.apply(tab));
    }

    private static void captureVanillaBaseline() {
        if (!VANILLA_BASELINE.isEmpty()) {
            return;
        }
        for (ResourceKey<CreativeModeTab> key : VANILLA_TABS) {
// @target if minecraft_version=26.1.2,26.2
//|            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(key);
// @target else
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(key);
// @target end
            VANILLA_BASELINE.put(tab, NativePosition.capture(tab));
            VisualCreativeTabEditorConstants.LOGGER.debug(
                    "[EditorTrace] fabric-vanilla-baseline-tab id={} row={} column={} page=0",
// @target if minecraft_version=26.1.2,26.2
//|                    key.identifier(),
// @target else
                    key.location(),
// @target end
                    tab.row(),
                    tab.column()
            );
        }
        VisualCreativeTabEditorConstants.LOGGER.debug(
                "[EditorTrace] fabric-vanilla-baseline-captured tabs={} page=0",
                VANILLA_BASELINE.size()
        );
    }

    private record NativePosition(CreativeModeTab.Row row, int column) {
        private static NativePosition capture(CreativeModeTab tab) {
            /*
// @target if minecraft_version=1.20.1
//|             * Fabric initializes its page field to -1 and getPage() throws until its bootstrap
//|             * collector has paginated registered tabs. Every vanilla tab belongs to page zero, so
//|             * the baseline records only native coordinates and restores page zero without reading
//|             * the potentially uninitialized field.
// @target elif minecraft_version=26.1.2,26.2
//|             * Fabric initializes its page field to -1 and fabric_getPage() deliberately throws until
//|             * Fabric's buildAllTabContents TAIL injection has paginated the tabs for the first time.
//|             * Every registered vanilla tab is assigned to page zero by that pagination pass, so the
//|             * pre-validation baseline must record only native row/column coordinates and restore page
//|             * zero without attempting to read the uninitialized field.
// @target else
             * Fabric initializes its page field to -1 and fabric_getPage() deliberately throws until
             * Fabric's updateEntries TAIL injection has paginated the tabs for the first time.
             * Every registered vanilla tab is assigned to page zero by that pagination pass, so the
             * pre-validation baseline must record only native row/column coordinates and restore page
             * zero without attempting to read the uninitialized field.
// @target end
             */
            return new NativePosition(tab.row(), tab.column());
        }

        private void apply(CreativeModeTab tab) {
            tab.row = this.row;
            tab.column = this.column;
// @target if minecraft_version=1.20.1
//|            ((FabricItemGroup) tab).setPage(0);
// @target elif minecraft_version=26.1.2,26.2
//|            ((FabricCreativeModeTabImpl) tab).fabric_setPage(0);
// @target else
            ((FabricItemGroupImpl) tab).fabric_setPage(0);
// @target end
        }
    }
}
