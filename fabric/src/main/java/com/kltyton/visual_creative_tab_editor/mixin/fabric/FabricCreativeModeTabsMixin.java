package com.kltyton.visual_creative_tab_editor.mixin.fabric;

import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-paginates data-driven and unregistered runtime tabs for Fabric's native page API. */
@Mixin(value = CreativeModeTabs.class, priority = 500)
public abstract class FabricCreativeModeTabsMixin {
    @Inject(method = "buildAllTabContents", at = @At("HEAD"))
    private static void visualCreativeTabEditor$restoreFabricValidationLayout(
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
        FabricCreativeTabPages.restoreVanillaBaselineForValidation();
    }

    // Fabric's own paginator uses the default injector order (1000) and validates registered tabs.
    // Runtime-only tabs are repacked afterwards because Fabric cannot see them in the registry.
    @Inject(method = "buildAllTabContents", at = @At("TAIL"), order = 1100)
    private static void visualCreativeTabEditor$repackFabricPages(
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
        FabricCreativeTabPages.repack();
    }
}
