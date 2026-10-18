package com.kltyton.visual_creative_tab_editor.client.paging;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientPlatform;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;

/** Shared page selection, stale-page repair and resize restoration for native page objects. */
public final class CreativeTabPaging {
    private CreativeTabPaging() {}
    public interface Access<P> {
        List<P> pages(CreativeModeInventoryScreen screen);
        P current(CreativeModeInventoryScreen screen);
        void select(CreativeModeInventoryScreen screen, P page);
        List<CreativeModeTab> visible(P page);
        CreativeModeTab defaultTab(P page);
        void resize(CreativeModeInventoryScreen screen);
    }
    public static <P> void install(Access<P> access) {
        CreativeTabClientPlatform.preserveNativeTabPositions();
        CreativeTabClientPlatform.installVisibleTabsProvider(screen -> access.visible(access.current(screen)));
        CreativeTabClientPlatform.installTabRevealer((screen, tab) -> access.pages(screen).stream()
                .filter(page -> access.visible(page).contains(tab)).findFirst()
                .map(page -> { access.select(screen, page); return true; }).orElse(false));
        CreativeTabClientPlatform.installPageNavigator(new CreativeTabClientPlatform.PageNavigator() {
            public CreativeTabClientPlatform.PageState state(CreativeModeInventoryScreen screen) {
                List<P> pages = access.pages(screen);
                if (pages.isEmpty()) { return new CreativeTabClientPlatform.PageState(0, 1); }
                int current = pages.indexOf(access.current(screen));
                if (current < 0) {
                    CreativeModeTab selected = CreativeModeInventoryScreen.selectedTab;
                    P repaired = pages.stream().filter(page -> access.visible(page).contains(selected))
                            .findFirst().orElse(pages.get(0));
                    access.select(screen, repaired);
                    List<CreativeModeTab> visible = access.visible(repaired);
                    if (!visible.isEmpty() && !visible.contains(selected)) { screen.selectTab(visible.get(0)); }
                    current = pages.indexOf(repaired);
                }
                return new CreativeTabClientPlatform.PageState(current, pages.size());
            }
            public boolean switchTo(CreativeModeInventoryScreen screen, int targetIndex) {
                List<P> pages = access.pages(screen);
                if (targetIndex < 0 || targetIndex >= pages.size()) { return false; }
                P page = pages.get(targetIndex);
                access.select(screen, page);
                CreativeModeTab selected = CreativeModeInventoryScreen.selectedTab;
                List<CreativeModeTab> visible = access.visible(page);
                if (!visible.isEmpty() && !visible.contains(selected)) { screen.selectTab(access.defaultTab(page)); }
                return access.current(screen) == page;
            }
        });
        CreativeTabClientPlatform.installScreenRefresher(screen -> {
            int pageIndex = Math.max(0, access.pages(screen).indexOf(access.current(screen)));
            access.resize(screen);
            List<P> pages = access.pages(screen);
            if (!pages.isEmpty()) { access.select(screen, pages.get(Math.min(pageIndex, pages.size() - 1))); }
        });
    }
}
