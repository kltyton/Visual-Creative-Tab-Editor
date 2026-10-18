package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves native vertical scrolling across the scroll API signatures. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class NativeEditorScrollMixin {

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseScrolled(
            double mouseX,
            double mouseY,
            double delta,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().mouseScrolled(mouseX, mouseY, 0.0D, delta)) {
            callback.setReturnValue(true);
        }
    }
}
