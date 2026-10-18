package com.kltyton.visual_creative_tab_editor.data;

import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import java.util.Objects;

/** Resource identity backed by the selected Minecraft identifier implementation. */
public final class ResourceId implements Comparable<ResourceId> {
    private final NativeIds.Handle handle;

    public ResourceId(NativeIds.Handle handle) { this.handle = Objects.requireNonNull(handle, "handle"); }
    public NativeIds.Handle handle() { return this.handle; }
    public static ResourceId fromNamespaceAndPath(String namespace, String path) { return NativeIds.create(namespace, path); }
    public static ResourceId withDefaultNamespace(String path) { return fromNamespaceAndPath("minecraft", path); }
    public static ResourceId parse(String value) { return NativeIds.parse(value); }
    public static ResourceId tryParse(String value) { return NativeIds.tryParse(value); }
    public String getNamespace() { return handle.value().getNamespace(); }
    public String getPath() { return handle.value().getPath(); }
    @Override public int compareTo(ResourceId other) { return handle.value().compareTo(other.handle.value()); }
    @Override public boolean equals(Object other) { return other instanceof ResourceId id && handle.equals(id.handle); }
    @Override public int hashCode() { return handle.hashCode(); }
    @Override public String toString() { return handle.value().toString(); }
}
