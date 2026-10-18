package com.kltyton.visual_creative_tab_editor.platform.registry;

/** The prototype-binding phase exists only in the extraction-era item registry. */
public final class NativeReloadReadiness {
    private NativeReloadReadiness() {}
    public static boolean shouldDefer() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.listElements()
                .map(net.minecraft.core.Holder::areComponentsBound).anyMatch(bound -> !bound);
    }
}
