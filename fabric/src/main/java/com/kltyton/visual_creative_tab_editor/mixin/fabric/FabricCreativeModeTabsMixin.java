package com.kltyton.visual_creative_tab_editor.mixin.fabric;

import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-paginates data-driven and unregistered runtime tabs for Fabric API 0.92's page fields. */
@Mixin(value = CreativeModeTabs.class, priority = 2000)
public abstract class FabricCreativeModeTabsMixin {
    @Inject(method = "buildAllTabContents", at = @At("HEAD"))
    private static void visualCreativeTabEditor$restoreFabricBaseline(
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
        FabricCreativeTabPages.restoreVanillaBaselineBeforeRepack();
    }

    // Fabric's bootstrap paginator only sees registered groups. Runtime-only tabs are repacked
    // after each content rebuild so its native page buttons can discover them through tabs().
    @Inject(method = "buildAllTabContents", at = @At("TAIL"), order = 1100)
    private static void visualCreativeTabEditor$repackFabricPages(
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
        FabricCreativeTabPages.repack();
    }
}
