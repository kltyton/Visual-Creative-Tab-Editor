package com.kltyton.one_enough_creative_tab.mixin.neoforge;

import com.kltyton.one_enough_creative_tab.runtime.CreativeTabRuntime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies data order to the page view while retaining NeoForge's native special-tab anchors. */
@Mixin(value = CreativeTabsScreenPage.class, remap = false)
public abstract class CreativeTabsScreenPageMixin {
    @Inject(method = "getVisibleTabs", at = @At("RETURN"), cancellable = true)
    private void oneEnoughCreativeTab$orderVisibleTabs(
            CallbackInfoReturnable<List<CreativeModeTab>> callback
    ) {
        callback.setReturnValue(oneEnoughCreativeTab$ordered(callback.getReturnValue()));
    }

    @Inject(method = "getDefaultTab", at = @At("RETURN"), cancellable = true)
    private void oneEnoughCreativeTab$dataDrivenDefault(
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
    private static List<CreativeModeTab> oneEnoughCreativeTab$ordered(List<CreativeModeTab> input) {
        if (CreativeTabRuntime.catalog().isEmpty() || input.size() < 2) {
            return input;
        }
        List<CreativeModeTab> ordered = new ArrayList<>(input);
        Map<CreativeModeTab, Integer> nativeOrder = new HashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            nativeOrder.put(ordered.get(index), index);
        }
        Map<Identifier, Integer> configuredOrder = new HashMap<>();
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
