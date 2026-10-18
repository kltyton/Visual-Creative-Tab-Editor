package com.kltyton.visual_creative_tab_editor.platform.registry;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import net.minecraft.resources.ResourceLocation;

/** Typed identifier boundary for Minecraft's ResourceLocation API. */
public final class NativeIds {
    private NativeIds() {}
    public record Handle(ResourceLocation value) {}
    public static ResourceId wrap(ResourceLocation value) { return value == null ? null : new ResourceId(new Handle(value)); }
    public static ResourceLocation unwrap(ResourceId value) { return value.handle().value(); }
    public static ResourceId create(String namespace, String path) { return wrap(ResourceLocation.fromNamespaceAndPath(namespace, path)); }
    public static ResourceId parse(String value) { return wrap(ResourceLocation.parse(value)); }
    public static ResourceId tryParse(String value) { return wrap(ResourceLocation.tryParse(value)); }
}
