package com.kltyton.visual_creative_tab_editor.platform;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** Loader hook for resolving the real native creative-tab background. */
public final class CreativeTabNativeBackground {
    private static final String VANILLA_BACKGROUND_PREFIX = "textures/gui/container/creative_inventory/tab_";
    private static final AtomicReference<Function<CreativeModeTab, ResourceLocation>> PROVIDER =
            new AtomicReference<>(CreativeTabNativeBackground::resolveVanillaBackground);

    private CreativeTabNativeBackground() {
    }

    public static void install(Function<CreativeModeTab, ResourceLocation> provider) {
        PROVIDER.set(Objects.requireNonNull(provider, "provider"));
    }

    public static ResourceLocation resolve(CreativeModeTab tab) {
        Objects.requireNonNull(tab, "tab");
        return Objects.requireNonNull(PROVIDER.get().apply(tab), "resolved background");
    }

    private static ResourceLocation resolveVanillaBackground(CreativeModeTab tab) {
        return new ResourceLocation(
                "minecraft",
                VANILLA_BACKGROUND_PREFIX + tab.getBackgroundSuffix()
        );
    }
}
