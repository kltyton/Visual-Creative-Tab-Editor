package com.kltyton.visual_creative_tab_editor;

import com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

/** Fabric identity adapter for the shared creative-tab data reload listener. */
public final class FabricCreativeTabReloadListener extends CreativeTabReloadListener
        implements IdentifiableResourceReloadListener {
    private final ResourceLocation id;

    public FabricCreativeTabReloadListener(ResourceLocation id, HolderLookup.Provider registries) {
        super(registries);
        this.id = id;
    }

    @Override
    public ResourceLocation getFabricId() {
        return this.id;
    }
}
