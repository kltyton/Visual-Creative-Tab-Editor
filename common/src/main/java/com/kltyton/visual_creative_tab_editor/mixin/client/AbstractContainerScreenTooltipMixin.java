package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @target if minecraft_version=26.1.2,26.2
//|/** Cancels tooltip submission before loader tooltip events see an invalid synthetic list. */
// @target else
/** Cancels tooltip rendering before loader tooltip events see an invalid synthetic list. */
// @target end
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenTooltipMixin {
// @target if minecraft_version=26.1.2,26.2
//|    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
// @target else
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
// @target end
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
