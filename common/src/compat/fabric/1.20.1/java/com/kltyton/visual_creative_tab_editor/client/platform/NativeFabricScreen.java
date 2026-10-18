package com.kltyton.visual_creative_tab_editor.client.platform;

import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;

/** Native page operations retain Fabric's selected-tab correction callbacks. */
public final class NativeFabricScreen {
    private NativeFabricScreen() {}
    public static int current(CreativeModeInventoryScreen screen) { return ((CreativeGuiExtensions) screen).fabric_currentPage(); }
    public static int count(CreativeModeInventoryScreen screen) { return FabricCreativeTabPages.pageCount(); }
    public static int page(CreativeModeInventoryScreen screen, CreativeModeTab tab) {
        return FabricCreativeTabPages.pageOf(tab, current(screen));
    }
    public static List<CreativeModeTab> visible(CreativeModeInventoryScreen screen) {
        return FabricCreativeTabPages.tabsOnPage(current(screen));
    }
    public static boolean switchTo(CreativeModeInventoryScreen screen, int target) {
        if (target < 0 || target >= count(screen)) { return false; }
        CreativeGuiExtensions nativeScreen = (CreativeGuiExtensions) screen;
        int previous = nativeScreen.fabric_currentPage();
        while (previous != target) {
            if (previous < target) { nativeScreen.fabric_nextPage(); } else { nativeScreen.fabric_previousPage(); }
            int current = nativeScreen.fabric_currentPage();
            if (current == previous) { return false; }
            previous = current;
        }
        return true;
    }
}
