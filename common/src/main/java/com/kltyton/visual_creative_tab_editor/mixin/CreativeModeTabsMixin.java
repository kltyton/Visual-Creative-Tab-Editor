package com.kltyton.visual_creative_tab_editor.mixin;

import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.stream.Stream;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTabs.class)
public abstract class CreativeModeTabsMixin {
    @Inject(method = "streamAllTabs", at = @At("RETURN"), cancellable = true)
    private static void visualCreativeTabEditor$useRuntimeCatalog(CallbackInfoReturnable<Stream<CreativeModeTab>> callback) {
        callback.setReturnValue(CreativeTabRuntime.effectiveTabs(callback.getReturnValue()));
    }

    @Inject(method = "getDefaultTab", at = @At("RETURN"), cancellable = true)
    private static void visualCreativeTabEditor$chooseVisibleDefault(CallbackInfoReturnable<CreativeModeTab> callback) {
        if (!callback.getReturnValue().shouldDisplay()) {
            CreativeModeTabs.tabs().stream().findFirst().ifPresent(callback::setReturnValue);
        }
    }

    @Inject(method = "buildAllTabContents", at = @At("TAIL"))
    private static void visualCreativeTabEditor$rebuildSearch(
            CreativeModeTab.ItemDisplayParameters parameters,
            CallbackInfo callback
    ) {
        CreativeTabRuntime.refreshContents(parameters);
    }
}
