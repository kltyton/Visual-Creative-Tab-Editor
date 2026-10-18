package com.kltyton.visual_creative_tab_editor.mixin.forge;

import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies data order to a Forge page while retaining its native special-tab anchors. */
@Mixin(value = CreativeTabsScreenPage.class, remap = false)
public abstract class CreativeTabsScreenPageMixin {
    @Inject(method = "getVisibleTabs", at = @At("RETURN"), cancellable = true)
    private void visualCreativeTabEditor$orderVisibleTabs(
            CallbackInfoReturnable<List<CreativeModeTab>> callback
    ) {
        callback.setReturnValue(com.kltyton.visual_creative_tab_editor.runtime.CreativeTabOrdering.ordered(callback.getReturnValue()));
    }

    @Inject(method = "getDefaultTab", at = @At("RETURN"), cancellable = true)
    private void visualCreativeTabEditor$dataDrivenDefault(
            CallbackInfoReturnable<CreativeModeTab> callback
    ) {
        com.kltyton.visual_creative_tab_editor.runtime.CreativeTabOrdering.defaultTab(
                () -> ((CreativeTabsScreenPage) (Object) this).getVisibleTabs()).ifPresent(callback::setReturnValue);
    }
}
