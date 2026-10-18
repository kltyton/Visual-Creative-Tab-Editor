package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Draws the configured full texture where the legacy screen otherwise rebuilds a vanilla suffix. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class NativeCreativeBackgroundMixin {
    @Redirect(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 0))
    private void visualCreativeTabEditor$renderDataDrivenBackground(GuiGraphics graphics, ResourceLocation nativeBackground,
            int x, int y, int u, int v, int width, int height) {
        ResourceLocation background = CreativeTabRuntime.definition(
                ((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$selectedTab())
                .map(definition -> NativeIds.unwrap(definition.layout().background())).orElse(nativeBackground);
        graphics.blit(background, x, y, u, v, width, height);
    }
}
