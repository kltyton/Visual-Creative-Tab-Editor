package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels tooltip rendering before loader tooltip events see an invalid synthetic list. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenTooltipMixin {
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$suppressEditorTooltip(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            CallbackInfo callback
    ) {
        if ((Object) this instanceof CreativeTabEditorHost host
                && host.visualCreativeTabEditor$suppressesTooltips()) {
            callback.cancel();
        }
    }
}
