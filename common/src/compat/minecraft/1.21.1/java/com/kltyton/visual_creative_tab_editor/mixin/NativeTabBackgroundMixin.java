package com.kltyton.visual_creative_tab_editor.mixin;

import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Binds native background access and the registered-tab contents hook. */
@Mixin(CreativeModeTab.class)
public abstract class NativeTabBackgroundMixin {
    private static final String VANILLA_BACKGROUND_PREFIX = "textures/gui/container/creative_inventory/tab_";


    @Inject(method = "getBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$getBackground(CallbackInfoReturnable<ResourceLocation> callback) {
        CreativeTabRuntime.definition((CreativeModeTab) (Object) this)
                .ifPresent(definition -> callback.setReturnValue(com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds.unwrap(definition.layout().background())));
    }
    @Inject(method = "buildContents", at = @At("TAIL"))
    private void visualCreativeTabEditor$applyRegisteredContents(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo callback) {
        ((CreativeTabContents) (Object) this).visualCreativeTabEditor$applyRegisteredContents(parameters);
    }
}
