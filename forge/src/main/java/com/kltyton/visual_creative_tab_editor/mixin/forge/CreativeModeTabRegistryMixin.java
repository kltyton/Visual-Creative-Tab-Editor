package com.kltyton.visual_creative_tab_editor.mixin.forge;

import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.List;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Projects the effective data-driven order into Forge's native creative-tab paging list. */
@Mixin(value = CreativeModeTabRegistry.class, remap = false)
public abstract class CreativeModeTabRegistryMixin {
    @Inject(method = "getSortedCreativeModeTabs", at = @At("RETURN"), cancellable = true)
    private static void visualCreativeTabEditor$applyEffectiveOrder(
            CallbackInfoReturnable<List<CreativeModeTab>> callback
    ) {
        List<CreativeModeTab> ordered = CreativeTabRuntime.effectiveTabs(callback.getReturnValue().stream())
                .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
                .filter(tab -> CreativeTabRuntime.definition(tab).map(definition -> !definition.hidden()).orElse(true))
                .toList();
        callback.setReturnValue(ordered);
    }
}
