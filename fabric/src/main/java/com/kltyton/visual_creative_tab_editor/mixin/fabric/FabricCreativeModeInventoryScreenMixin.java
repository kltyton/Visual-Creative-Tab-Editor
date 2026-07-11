package com.kltyton.visual_creative_tab_editor.mixin.fabric;

import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Fabric's page controls tolerate the one-frame tab-cache invalidation used during reloads. */
@Mixin(value = CreativeModeInventoryScreen.class, priority = 500)
public abstract class FabricCreativeModeInventoryScreenMixin {
    @Inject(method = "hasAdditionalPages", at = @At("HEAD"), cancellable = true, remap = false)
    private void visualCreativeTabEditor$guardInvalidatedTabCache(
            CallbackInfoReturnable<Boolean> callback
    ) {
        callback.setReturnValue(FabricCreativeGuiComponents.getPageCount() > 1);
    }
}
