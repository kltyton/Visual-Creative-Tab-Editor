package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/** Assigns Fabric pages to native and runtime-created data-driven tabs. */
public final class FabricCreativeTabPages {
    private static final int TABS_PER_PAGE = 10;
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

    /** Restores the 1.21+ shared-tab behavior missing from Fabric API 0.92. */
    public static void includeOperatorTabInCommonGroups() {
        FabricCreativeGuiComponents.COMMON_GROUPS.add(
                BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.OP_BLOCKS)
        );
    }

    public static synchronized void repack() {
        captureVanillaBaseline();
        List<CreativeModeTab> ordinary = CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(tab -> !FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab))
                .toList();
        int visibleIndex = 0;
        int hiddenIndex = 0;
        int signature = 1;
        for (CreativeModeTab tab : ordinary) {
            signature = 31 * signature + System.identityHashCode(tab);
            signature = 31 * signature + (tab.shouldDisplay() ? 1 : 0);
            if (!tab.shouldDisplay()) {
                ((FabricItemGroup) tab).setPage(0);
                tab.row = CreativeModeTab.Row.TOP;
                tab.column = 7 + hiddenIndex++;
                continue;
            }
            int pageIndex = visibleIndex % TABS_PER_PAGE;
            ((FabricItemGroup) tab).setPage(visibleIndex / TABS_PER_PAGE);
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
                    Math.max(1, (visibleIndex + TABS_PER_PAGE - 1) / TABS_PER_PAGE)
            );
        }
    }

    /** Returns every effective tab visible on the current Fabric page, including the shared special tabs. */
    public static List<CreativeModeTab> tabsOnPage(int page) {
        return CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(CreativeModeTab::shouldDisplay)
                .filter(tab -> FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab)
                        || ((FabricItemGroup) tab).getPage() == page)
                .toList();
    }

    /** Returns the page containing a tab; Fabric's shared special tabs belong to the current page. */
    public static int pageOf(CreativeModeTab tab, int currentPage) {
        return FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab)
                ? currentPage
                : ((FabricItemGroup) tab).getPage();
    }

    /** Computes the page count from the effective runtime catalog rather than only registered groups. */
    public static int pageCount() {
        return CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(CreativeModeTab::shouldDisplay)
                .filter(tab -> !FabricCreativeGuiComponents.COMMON_GROUPS.contains(tab))
                .mapToInt(tab -> ((FabricItemGroup) tab).getPage())
                .max()
                .orElse(0) + 1;
    }

    /** Uses Fabric's native previous/next operations to preserve its selected-tab correction behavior. */
    public static boolean switchToPage(CreativeGuiExtensions screen, int targetPage) {
        int pageCount = pageCount();
        if (targetPage < 0 || targetPage >= pageCount) {
            return false;
        }
        int previous = screen.fabric_currentPage();
        while (previous != targetPage) {
            if (previous < targetPage) {
                screen.fabric_nextPage();
            } else {
                screen.fabric_previousPage();
            }
            int current = screen.fabric_currentPage();
            if (current == previous) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    /** Restores native coordinates before the runtime catalog is packed again. */
    public static synchronized void restoreVanillaBaselineBeforeRepack() {
        captureVanillaBaseline();
        VANILLA_BASELINE.forEach((tab, position) -> position.apply(tab));
    }

    private static void captureVanillaBaseline() {
        if (!VANILLA_BASELINE.isEmpty()) {
            return;
        }
        for (ResourceKey<CreativeModeTab> key : VANILLA_TABS) {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(key);
            VANILLA_BASELINE.put(tab, NativePosition.capture(tab));
            VisualCreativeTabEditorConstants.LOGGER.debug(
                    "[EditorTrace] fabric-vanilla-baseline-tab id={} row={} column={} page=0",
                    key.location(),
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
             * Fabric initializes its page field to -1 and getPage() throws until its bootstrap
             * collector has paginated registered tabs. Every vanilla tab belongs to page zero, so
             * the baseline records only native coordinates and restores page zero without reading
             * the potentially uninitialized field.
             */
            return new NativePosition(tab.row(), tab.column());
        }

        private void apply(CreativeModeTab tab) {
            tab.row = this.row;
            tab.column = this.column;
            ((FabricItemGroup) tab).setPage(0);
        }
    }
}
