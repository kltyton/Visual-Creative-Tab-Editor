package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Fabric identity adapter for the shared creative-tab data reload listener. */
public final class FabricCreativeTabReloadListener extends CreativeTabReloadListener
        implements IdentifiableResourceReloadListener {
    private final ResourceLocation id;

    public FabricCreativeTabReloadListener(ResourceLocation id) {
        /*
         * Fabric API 0.92 has no registry-aware reload-listener factory. The 1.20.1 JSON codec
         * resolves item ids through BuiltInRegistries and only requires a non-null provider, so
         * the frozen built-in access is sufficient during both initial and subsequent reloads.
         */
        super(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        this.id = id;
    }

    @Override
    public ResourceLocation getFabricId() {
        return this.id;
    }
}
