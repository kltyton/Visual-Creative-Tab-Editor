package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import com.kltyton.visual_creative_tab_editor.client.platform.NativeFabricTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/** Assigns Fabric pages to native and runtime-created data-driven tabs. */
public final class FabricCreativeTabPages {
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

    public static synchronized void repack() {
        captureVanillaBaseline();
        List<CreativeModeTab> ordinary = CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(tab -> !NativeFabricTabs.commonTabs().contains(tab))
                .toList();
        int visibleIndex = 0;
        int hiddenIndex = 0;
        int signature = 1;
        for (CreativeModeTab tab : ordinary) {
            signature = 31 * signature + System.identityHashCode(tab);
            signature = 31 * signature + (tab.shouldDisplay() ? 1 : 0);
            if (!tab.shouldDisplay()) {
                NativeFabricTabs.setPage(tab, 0);
                tab.row = CreativeModeTab.Row.TOP;
                tab.column = 7 + hiddenIndex++;
                continue;
            }
            int pageIndex = visibleIndex % NativeFabricTabs.tabsPerPage();
            NativeFabricTabs.setPage(tab, visibleIndex / NativeFabricTabs.tabsPerPage());
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
                    Math.max(1, (visibleIndex + NativeFabricTabs.tabsPerPage() - 1)
                            / NativeFabricTabs.tabsPerPage())
            );
        }
    }

    /** Restores the registered vanilla layout before Fabric paginates and validates it. */
    public static synchronized void restoreVanillaBaselineForValidation() {
        captureVanillaBaseline();
        VANILLA_BASELINE.forEach((tab, position) -> position.apply(tab));
    }

    private static void captureVanillaBaseline() {
        if (!VANILLA_BASELINE.isEmpty()) {
            return;
        }
        for (ResourceKey<CreativeModeTab> key : VANILLA_TABS) {
            CreativeModeTab tab = NativeFabricTabs.vanillaTab(key);
            VANILLA_BASELINE.put(tab, NativePosition.capture(tab));
            VisualCreativeTabEditorConstants.LOGGER.debug(
                    "[EditorTrace] fabric-vanilla-baseline-tab id={} row={} column={} page=0",
                    NativeFabricTabs.identifier(key),
                    tab.row(),
                    tab.column()
            );
        }
        VisualCreativeTabEditorConstants.LOGGER.debug(
                "[EditorTrace] fabric-vanilla-baseline-captured tabs={} page=0",
                VANILLA_BASELINE.size()
        );
    }



    /** Restores the 1.21+ shared-tab behavior missing from Fabric API 0.92. */
    public static void includeOperatorTabInCommonGroups() {
        NativeFabricTabs.commonTabs().add(
                NativeFabricTabs.vanillaTab(CreativeModeTabs.OP_BLOCKS)
        );
    }

    /** Returns every effective tab visible on the current Fabric page, including the shared special tabs. */
    public static List<CreativeModeTab> tabsOnPage(int page) {
        return CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(CreativeModeTab::shouldDisplay)
                .filter(tab -> NativeFabricTabs.commonTabs().contains(tab)
                        || NativeFabricTabs.page(tab) == page)
                .toList();
    }

    /** Returns the page containing a tab; Fabric's shared special tabs belong to the current page. */
    public static int pageOf(CreativeModeTab tab, int currentPage) {
        return NativeFabricTabs.commonTabs().contains(tab)
                ? currentPage
                : NativeFabricTabs.page(tab);
    }

    /** Computes the page count from the effective runtime catalog rather than only registered groups. */
    public static int pageCount() {
        return CreativeTabRuntime.effectiveTabs(BuiltInRegistries.CREATIVE_MODE_TAB.stream())
                .filter(CreativeModeTab::shouldDisplay)
                .filter(tab -> !NativeFabricTabs.commonTabs().contains(tab))
                .mapToInt(tab -> NativeFabricTabs.page(tab))
                .max()
                .orElse(0) + 1;
    }
    private record NativePosition(CreativeModeTab.Row row, int column) {
        private static NativePosition capture(CreativeModeTab tab) {
            // The page field is not initialized until native pagination finishes.
            return new NativePosition(tab.row(), tab.column());
        }

        private void apply(CreativeModeTab tab) {
            tab.row = this.row;
            tab.column = this.column;
            NativeFabricTabs.setPage(tab, 0);
        }
    }
}
