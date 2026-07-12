package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits the editor after the complete screen, tooltip, and subtitle extraction pass. */
@Mixin(value = Screen.class, priority = 100)
public abstract class ScreenEditorOverlayMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void visualCreativeTabEditor$extractEditorOverlayLast(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        if ((Object) this instanceof CreativeTabEditorHost host) {
            graphics.nextStratum();
            host.visualCreativeTabEditor$renderEditorOverlay(graphics, mouseX, mouseY, partialTick);
        }
    }
}
