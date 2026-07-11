package com.kltyton.one_enough_creative_tab.platform;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.minecraft.world.item.CreativeModeTab;

/** Loader hook for the native pre-data-pack tab order used by default snapshots. */
public final class CreativeTabNativeOrder {
    private static final AtomicReference<Function<List<CreativeModeTab>, List<CreativeModeTab>>> PROVIDER =
            new AtomicReference<>(List::copyOf);

    private CreativeTabNativeOrder() {
    }

    public static void install(Function<List<CreativeModeTab>, List<CreativeModeTab>> provider) {
        PROVIDER.set(Objects.requireNonNull(provider, "provider"));
    }

    public static List<CreativeModeTab> apply(List<CreativeModeTab> registryOrder) {
        return List.copyOf(PROVIDER.get().apply(List.copyOf(registryOrder)));
    }
}
