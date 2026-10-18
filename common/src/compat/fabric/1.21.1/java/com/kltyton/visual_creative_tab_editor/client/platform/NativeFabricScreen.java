package com.kltyton.visual_creative_tab_editor.client.platform;

import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;

/** Native page operations retain Fabric's selected-tab correction callbacks. */
public final class NativeFabricScreen {
    private NativeFabricScreen() {}
    public static int current(CreativeModeInventoryScreen screen) { return ((FabricCreativeInventoryScreen) screen).getCurrentPage(); }
    public static int count(CreativeModeInventoryScreen screen) { return ((FabricCreativeInventoryScreen) screen).getPageCount(); }
    public static int page(CreativeModeInventoryScreen screen, CreativeModeTab tab) {
        return ((FabricCreativeInventoryScreen) screen).getPage(tab);
    }
    public static List<CreativeModeTab> visible(CreativeModeInventoryScreen screen) {
        return ((FabricCreativeInventoryScreen) screen).getItemGroupsOnPage(current(screen));
    }
    public static boolean switchTo(CreativeModeInventoryScreen screen, int target) {
        return ((FabricCreativeInventoryScreen) screen).switchToPage(target);
    }
}
