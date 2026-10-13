package com.kltyton.visual_creative_tab_editor.mixin.fabric;

// @target if minecraft_version=1.20.1
//|import com.kltyton.visual_creative_tab_editor.client.FabricCreativeTabPages;
//|import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
//|import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
// @target elif minecraft_version=26.1.2,26.2
//|import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
// @target else
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
// @target end
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @target if minecraft_version=1.20.1
//|/** Makes Fabric API 0.92's page buttons follow the effective data-driven tab catalog. */
// @target else
/** Makes Fabric's page controls tolerate the one-frame tab-cache invalidation used during reloads. */
// @target end
@Mixin(value = CreativeModeInventoryScreen.class, priority = 2000)
public abstract class FabricCreativeModeInventoryScreenMixin {
// @target if minecraft_version=1.20.1
//|    @Inject(method = "fabric_isButtonVisible", at = @At("HEAD"), cancellable = true, remap = false)
//|    private void visualCreativeTabEditor$useRuntimePageCountForVisibility(
//|            FabricCreativeGuiComponents.Type type,
// @target else
    @Inject(method = "hasAdditionalPages", at = @At("HEAD"), cancellable = true, remap = false)
    private void visualCreativeTabEditor$guardInvalidatedTabCache(
// @target end
            CallbackInfoReturnable<Boolean> callback
    ) {
// @target if minecraft_version=1.20.1
//|        callback.setReturnValue(FabricCreativeTabPages.pageCount() > 1);
//|    }
//|
//|    @Inject(method = "fabric_isButtonEnabled", at = @At("HEAD"), cancellable = true, remap = false)
//|    private void visualCreativeTabEditor$useRuntimePageCountForNavigation(
//|            FabricCreativeGuiComponents.Type type,
//|            CallbackInfoReturnable<Boolean> callback
//|    ) {
//|        int current = ((CreativeGuiExtensions) this).fabric_currentPage();
//|        callback.setReturnValue(type == FabricCreativeGuiComponents.Type.NEXT
//|                ? current + 1 < FabricCreativeTabPages.pageCount()
//|                : current > 0);
// @target else
        callback.setReturnValue(FabricCreativeGuiComponents.getPageCount() > 1);
// @target end
    }
}
