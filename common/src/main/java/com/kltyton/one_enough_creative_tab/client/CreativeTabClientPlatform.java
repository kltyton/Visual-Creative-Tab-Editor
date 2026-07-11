package com.kltyton.one_enough_creative_tab.client;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/** Loader-specific current-page view used by the shared editor. */
public final class CreativeTabClientPlatform {
    private static volatile boolean preserveNativeTabPositions;
    private static final AtomicReference<VisibleTabsProvider> VISIBLE_TABS = new AtomicReference<>(
            screen -> CreativeModeTabs.tabs()
    );
    private static final AtomicReference<ScreenRefresher> SCREEN_REFRESHER = new AtomicReference<>(
            screen -> screen.resize(screen.width, screen.height)
    );
    private static final AtomicReference<Runnable> TAB_LAYOUT_REFRESHER = new AtomicReference<>(() -> { });
    private static final AtomicReference<TabRevealer> TAB_REVEALER = new AtomicReference<>((screen, tab) -> true);
    private static final AtomicReference<PageNavigator> PAGE_NAVIGATOR = new AtomicReference<>(new PageNavigator() {
        @Override
        public PageState state(CreativeModeInventoryScreen screen) {
            return new PageState(0, 1);
        }

        @Override
        public boolean switchTo(CreativeModeInventoryScreen screen, int targetIndex) {
            return targetIndex == 0;
        }
    });

    private CreativeTabClientPlatform() {
    }

    public static void installVisibleTabsProvider(VisibleTabsProvider provider) {
        VISIBLE_TABS.set(Objects.requireNonNull(provider, "provider"));
    }

    public static List<CreativeModeTab> visibleTabs(CreativeModeInventoryScreen screen) {
        return VISIBLE_TABS.get().visibleTabs(screen).stream()
                .filter(CreativeModeTab::shouldDisplay)
                .toList();
    }

    /** Prevents the shared screen layer from mutating loader-owned tab row/column fields. */
    public static void preserveNativeTabPositions() {
        preserveNativeTabPositions = true;
    }

    public static boolean preservesNativeTabPositions() {
        return preserveNativeTabPositions;
    }

    public static void installScreenRefresher(ScreenRefresher refresher) {
        SCREEN_REFRESHER.set(Objects.requireNonNull(refresher, "refresher"));
    }

    public static void refreshScreen(CreativeModeInventoryScreen screen) {
        SCREEN_REFRESHER.get().refresh(Objects.requireNonNull(screen, "screen"));
    }

    public static void installTabLayoutRefresher(Runnable refresher) {
        TAB_LAYOUT_REFRESHER.set(Objects.requireNonNull(refresher, "refresher"));
    }

    public static void refreshTabLayout() {
        TAB_LAYOUT_REFRESHER.get().run();
    }

    public static void installTabRevealer(TabRevealer revealer) {
        TAB_REVEALER.set(Objects.requireNonNull(revealer, "revealer"));
    }

    /** Makes the loader-owned page containing {@code tab} current before vanilla selects it. */
    public static boolean revealTab(CreativeModeInventoryScreen screen, CreativeModeTab tab) {
        return TAB_REVEALER.get().reveal(
                Objects.requireNonNull(screen, "screen"),
                Objects.requireNonNull(tab, "tab")
        );
    }

    public static void installPageNavigator(PageNavigator navigator) {
        PAGE_NAVIGATOR.set(Objects.requireNonNull(navigator, "navigator"));
    }

    public static PageState pageState(CreativeModeInventoryScreen screen) {
        PageState state = PAGE_NAVIGATOR.get().state(Objects.requireNonNull(screen, "screen"));
        return new PageState(state.index(), state.count());
    }

    public static boolean switchToPage(CreativeModeInventoryScreen screen, int targetIndex) {
        PageState state = pageState(screen);
        if (targetIndex < 0 || targetIndex >= state.count()) {
            return false;
        }
        return PAGE_NAVIGATOR.get().switchTo(Objects.requireNonNull(screen, "screen"), targetIndex);
    }

    public record PageState(int index, int count) {
        public PageState {
            count = Math.max(1, count);
        }

        public boolean canMove(int delta) {
            if (!isValid()) {
                return false;
            }
            int target = this.index + delta;
            return target >= 0 && target < this.count;
        }

        public boolean isLast() {
            return isValid() && this.index == this.count - 1;
        }

        public boolean isValid() {
            return this.index >= 0 && this.index < this.count;
        }
    }

    @FunctionalInterface
    public interface VisibleTabsProvider {
        List<CreativeModeTab> visibleTabs(CreativeModeInventoryScreen screen);
    }

    @FunctionalInterface
    public interface ScreenRefresher {
        void refresh(CreativeModeInventoryScreen screen);
    }

    @FunctionalInterface
    public interface TabRevealer {
        boolean reveal(CreativeModeInventoryScreen screen, CreativeModeTab tab);
    }

    public interface PageNavigator {
        PageState state(CreativeModeInventoryScreen screen);

        boolean switchTo(CreativeModeInventoryScreen screen, int targetIndex);
    }
}
