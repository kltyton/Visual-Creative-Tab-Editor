package com.kltyton.visual_creative_tab_editor.platform.registry;

import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import net.minecraft.resources.Identifier;

/** Typed identifier boundary for Minecraft's Identifier API. */
public final class NativeIds {
    private NativeIds() {}
    public record Handle(Identifier value) {}
    public static ResourceId wrap(Identifier value) { return value == null ? null : new ResourceId(new Handle(value)); }
    public static Identifier unwrap(ResourceId value) { return value.handle().value(); }
    public static ResourceId create(String namespace, String path) { return wrap(Identifier.fromNamespaceAndPath(namespace, path)); }
    public static ResourceId parse(String value) { return wrap(Identifier.parse(value)); }
    public static ResourceId tryParse(String value) { return wrap(Identifier.tryParse(value)); }
}
