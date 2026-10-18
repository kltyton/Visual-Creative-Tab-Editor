package com.kltyton.visual_creative_tab_editor.platform;

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

    public static List<CreativeModeTab> complete(List<CreativeModeTab> registryOrder, List<CreativeModeTab> nativeOrder) {
        List<CreativeModeTab> ordered = new java.util.ArrayList<>(nativeOrder);
        for (CreativeModeTab tab : registryOrder) {
            if (!ordered.contains(tab)) { ordered.add(tab); }
        }
        return ordered;
    }

    public static void install(Function<List<CreativeModeTab>, List<CreativeModeTab>> provider) {
        PROVIDER.set(Objects.requireNonNull(provider, "provider"));
    }

    public static List<CreativeModeTab> apply(List<CreativeModeTab> registryOrder) {
        return List.copyOf(PROVIDER.get().apply(List.copyOf(registryOrder)));
    }
}
