package com.kltyton.visual_creative_tab_editor.client;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.creativetab.FabricCreativeModeTabImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
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
                .filter(tab -> !FabricCreativeGuiComponents.COMMON_TABS.contains(tab))
                .toList();
        int visibleIndex = 0;
        int hiddenIndex = 0;
        int signature = 1;
        for (CreativeModeTab tab : ordinary) {
            signature = 31 * signature + System.identityHashCode(tab);
            signature = 31 * signature + (tab.shouldDisplay() ? 1 : 0);
            if (!tab.shouldDisplay()) {
                ((FabricCreativeModeTabImpl) tab).fabric_setPage(0);
                tab.row = CreativeModeTab.Row.TOP;
                tab.column = 7 + hiddenIndex++;
                continue;
            }
            int pageIndex = visibleIndex % FabricCreativeModeTabImpl.TABS_PER_PAGE;
            ((FabricCreativeModeTabImpl) tab).fabric_setPage(visibleIndex / FabricCreativeModeTabImpl.TABS_PER_PAGE);
            tab.row = pageIndex < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM;
            tab.column = pageIndex % 5;
            visibleIndex++;
        }
        if (signature != lastLoggedSignature) {
            lastLoggedSignature = signature;
            VisualCreativeTabEditorConstants.LOGGER.info(
                    "[EditorTrace] fabric-tab-pages-repacked ordinary={} visible={} hidden={} pages={}",
                    ordinary.size(),
                    visibleIndex,
                    hiddenIndex,
                    Math.max(1, (visibleIndex + FabricCreativeModeTabImpl.TABS_PER_PAGE - 1)
                            / FabricCreativeModeTabImpl.TABS_PER_PAGE)
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
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(key);
            VANILLA_BASELINE.put(tab, NativePosition.capture(tab));
            VisualCreativeTabEditorConstants.LOGGER.debug(
                    "[EditorTrace] fabric-vanilla-baseline-tab id={} row={} column={} page=0",
                    key.identifier(),
                    tab.row(),
                    tab.column()
            );
        }
        VisualCreativeTabEditorConstants.LOGGER.info(
                "[EditorTrace] fabric-vanilla-baseline-captured tabs={} page=0",
                VANILLA_BASELINE.size()
        );
    }

    private record NativePosition(CreativeModeTab.Row row, int column) {
        private static NativePosition capture(CreativeModeTab tab) {
            /*
             * Fabric initializes its page field to -1 and fabric_getPage() deliberately throws until
             * Fabric's buildAllTabContents TAIL injection has paginated the tabs for the first time.
             * Every registered vanilla tab is assigned to page zero by that pagination pass, so the
             * pre-validation baseline must record only native row/column coordinates and restore page
             * zero without attempting to read the uninitialized field.
             */
            return new NativePosition(tab.row(), tab.column());
        }

        private void apply(CreativeModeTab tab) {
            tab.row = this.row;
            tab.column = this.column;
            ((FabricCreativeModeTabImpl) tab).fabric_setPage(0);
        }
    }
}
