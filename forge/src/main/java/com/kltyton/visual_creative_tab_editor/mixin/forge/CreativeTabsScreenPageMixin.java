package com.kltyton.visual_creative_tab_editor.mixin.forge;

import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies data order to the Forge page view while retaining native special-tab anchors. */
@Mixin(value = CreativeTabsScreenPage.class, remap = false)
public abstract class CreativeTabsScreenPageMixin {
    @Inject(method = "getVisibleTabs", at = @At("RETURN"), cancellable = true)
    private void visualCreativeTabEditor$orderVisibleTabs(
            CallbackInfoReturnable<List<CreativeModeTab>> callback
    ) {
        callback.setReturnValue(visualCreativeTabEditor$ordered(callback.getReturnValue()));
    }

    @Inject(method = "getDefaultTab", at = @At("RETURN"), cancellable = true)
    private void visualCreativeTabEditor$dataDrivenDefault(
            CallbackInfoReturnable<CreativeModeTab> callback
    ) {
        if (CreativeTabRuntime.catalog().isEmpty()) {
            return;
        }
        List<CreativeModeTab> visible = ((CreativeTabsScreenPage) (Object) this).getVisibleTabs();
        if (!visible.isEmpty()) {
            callback.setReturnValue(visible.getFirst());
        }
    }

    @Unique
    private static List<CreativeModeTab> visualCreativeTabEditor$ordered(List<CreativeModeTab> input) {
        if (CreativeTabRuntime.catalog().isEmpty() || input.size() < 2) {
            return input;
        }
        List<CreativeModeTab> ordered = new ArrayList<>(input);
        Map<CreativeModeTab, Integer> nativeOrder = new HashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            nativeOrder.put(ordered.get(index), index);
        }
        Map<ResourceLocation, Integer> configuredOrder = new HashMap<>();
        var definitions = CreativeTabRuntime.catalog().orderedDefinitions();
        for (int index = 0; index < definitions.size(); index++) {
            configuredOrder.put(definitions.get(index).id(), index);
        }
        ordered.sort(java.util.Comparator
                .comparingInt((CreativeModeTab tab) -> CreativeTabRuntime.id(tab)
                        .map(id -> configuredOrder.getOrDefault(id, Integer.MAX_VALUE))
                        .orElse(Integer.MAX_VALUE))
                .thenComparingInt(tab -> nativeOrder.getOrDefault(tab, Integer.MAX_VALUE)));
        return List.copyOf(ordered);
    }
}
