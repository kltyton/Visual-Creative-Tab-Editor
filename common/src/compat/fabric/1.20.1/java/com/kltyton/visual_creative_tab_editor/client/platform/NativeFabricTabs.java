package com.kltyton.visual_creative_tab_editor.client.platform;

import java.util.Collection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;

/** Native Fabric tab page fields and bootstrap registry access. */
public final class NativeFabricTabs {
    private NativeFabricTabs() {}
    public static Collection<CreativeModeTab> commonTabs() { return FabricCreativeGuiComponents.COMMON_GROUPS; }
    public static int tabsPerPage() { return 10; }
    public static void setPage(CreativeModeTab tab, int page) { ((FabricItemGroup) tab).setPage(page); }
    public static int page(CreativeModeTab tab) { return ((FabricItemGroup) tab).getPage(); }
    public static CreativeModeTab vanillaTab(ResourceKey<CreativeModeTab> key) { return BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(key); }
    public static String identifier(ResourceKey<CreativeModeTab> key) { return key.location().toString(); }
}
