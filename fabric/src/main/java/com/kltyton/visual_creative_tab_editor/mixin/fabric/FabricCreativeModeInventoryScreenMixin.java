package com.kltyton.visual_creative_tab_editor.mixin.fabric;

import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Fabric API 0.92's page buttons follow the effective data-driven tab catalog. */
@Mixin(value = CreativeModeInventoryScreen.class, priority = 2000)
public abstract class FabricCreativeModeInventoryScreenMixin {
    @Inject(method = "fabric_isButtonVisible", at = @At("HEAD"), cancellable = true, remap = false)
    private void visualCreativeTabEditor$useRuntimePageCountForVisibility(
            FabricCreativeGuiComponents.Type type,
            CallbackInfoReturnable<Boolean> callback
    ) {
        callback.setReturnValue(FabricCreativeTabPages.pageCount() > 1);
    }

    @Inject(method = "fabric_isButtonEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private void visualCreativeTabEditor$useRuntimePageCountForNavigation(
            FabricCreativeGuiComponents.Type type,
            CallbackInfoReturnable<Boolean> callback
    ) {
        int current = ((CreativeGuiExtensions) this).fabric_currentPage();
        callback.setReturnValue(type == FabricCreativeGuiComponents.Type.NEXT
                ? current + 1 < FabricCreativeTabPages.pageCount()
                : current > 0);
    }
}
