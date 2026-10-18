package com.kltyton.visual_creative_tab_editor.client.paging;

import java.util.List;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;

/** Typed access to the loader-owned creative page list and page objects. */
public final class NativePageAccess implements CreativeTabPaging.Access<CreativeTabsScreenPage> {
    public static final NativePageAccess INSTANCE = new NativePageAccess();
    private NativePageAccess() {}
    public List<CreativeTabsScreenPage> pages(CreativeModeInventoryScreen screen) {
        return screen.pages;
    }
    public CreativeTabsScreenPage current(CreativeModeInventoryScreen screen) { return screen.getCurrentPage(); }
    public void select(CreativeModeInventoryScreen screen, CreativeTabsScreenPage page) { screen.setCurrentPage(page); }
    public List<CreativeModeTab> visible(CreativeTabsScreenPage page) { return page.getVisibleTabs(); }
    public CreativeModeTab defaultTab(CreativeTabsScreenPage page) { return page.getDefaultTab(); }
    public void resize(CreativeModeInventoryScreen screen) { com.kltyton.visual_creative_tab_editor.client.platform.NativeScreenLifecycle.refresh(screen); }
}
