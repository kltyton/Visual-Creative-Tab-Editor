package com.kltyton.visual_creative_tab_editor.mixin.fabric;

import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @target if minecraft_version=1.20.1
//|/** Re-paginates data-driven and unregistered runtime tabs for Fabric API 0.92's page fields. */
// @target else
/** Re-paginates data-driven and unregistered runtime tabs for Fabric's native page API. */
// @target end
@Mixin(value = CreativeModeTabs.class, priority = 2000)
public abstract class FabricCreativeModeTabsMixin {
    @Inject(method = "buildAllTabContents", at = @At("HEAD"))
// @target if minecraft_version=1.20.1
//|    private static void visualCreativeTabEditor$restoreFabricBaseline(
// @target else
    private static void visualCreativeTabEditor$restoreFabricValidationLayout(
// @target end
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
// @target if minecraft_version=1.20.1
//|        FabricCreativeTabPages.restoreVanillaBaselineBeforeRepack();
// @target else
        FabricCreativeTabPages.restoreVanillaBaselineForValidation();
// @target end
    }

// @target if minecraft_version=1.20.1
//|    // Fabric's bootstrap paginator only sees registered groups. Runtime-only tabs are repacked
//|    // after each content rebuild so its native page buttons can discover them through tabs().
// @target else
    // Fabric's own paginator uses the default injector order (1000) and validates registered tabs.
    // Runtime-only tabs are repacked afterwards because Fabric cannot see them in the registry.
// @target end
    @Inject(method = "buildAllTabContents", at = @At("TAIL"), order = 1100)
    private static void visualCreativeTabEditor$repackFabricPages(
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
        FabricCreativeTabPages.repack();
    }
}
