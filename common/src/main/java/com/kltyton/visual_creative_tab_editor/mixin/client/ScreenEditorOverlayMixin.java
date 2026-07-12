package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Renders the editor after the complete screen and deferred-tooltip pass. */
@Mixin(value = Screen.class, priority = 100)
public abstract class ScreenEditorOverlayMixin {
    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void visualCreativeTabEditor$renderEditorOverlayLast(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        if ((Object) this instanceof CreativeTabEditorHost host) {
            host.visualCreativeTabEditor$renderEditorOverlay(graphics, mouseX, mouseY, partialTick);
        }
    }
}
