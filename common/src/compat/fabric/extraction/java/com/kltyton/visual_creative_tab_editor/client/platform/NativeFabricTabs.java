package com.kltyton.visual_creative_tab_editor.client.platform;

import java.util.Collection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.fabric.impl.creativetab.FabricCreativeModeTabImpl;
import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;

/** Native Fabric tab page fields and bootstrap registry access. */
public final class NativeFabricTabs {
    private NativeFabricTabs() {}
    public static Collection<CreativeModeTab> commonTabs() { return FabricCreativeGuiComponents.COMMON_TABS; }
    public static int tabsPerPage() { return FabricCreativeModeTabImpl.TABS_PER_PAGE; }
    public static void setPage(CreativeModeTab tab, int page) { ((FabricCreativeModeTabImpl) tab).fabric_setPage(page); }
    public static int page(CreativeModeTab tab) { return ((FabricCreativeModeTabImpl) tab).fabric_getPage(); }
    public static CreativeModeTab vanillaTab(ResourceKey<CreativeModeTab> key) { return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(key); }
    public static String identifier(ResourceKey<CreativeModeTab> key) { return key.identifier().toString(); }
}
