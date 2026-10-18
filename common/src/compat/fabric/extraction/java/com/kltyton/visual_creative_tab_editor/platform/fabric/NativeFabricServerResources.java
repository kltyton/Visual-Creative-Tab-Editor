package com.kltyton.visual_creative_tab_editor.platform.fabric;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;

/** Registers the tab listener with the Fabric data-resource API of this target. */
public final class NativeFabricServerResources {
    private static final ResourceId ID = ResourceId.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID, "creative_tabs");
    private NativeFabricServerResources() {}
    public static void register() {
        net.fabricmc.fabric.api.resource.v1.DataResourceLoader.get().registerReloadListener(
                NativeIds.unwrap(ID), com.kltyton.visual_creative_tab_editor.server.CreativeTabReloadListener::new);
    }
}
