package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.client.editor.render.NativeEditorGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Positions tabs and joins category rails behind native filter widgets. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class NativeCreativeLayoutMixin {
    @Inject(method = "renderBg", at = @At("HEAD"))
    private void visualCreativeTabEditor$arrangeBeforeBackground(CallbackInfo callback) {
        ((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$arrangeDataDrivenTabs();
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void visualCreativeTabEditor$categoryFrame(GuiGraphics graphics, float partialTick,
            int mouseX, int mouseY, CallbackInfo callback) {
        ((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$renderCategoryFrame(new NativeEditorGraphics(graphics));
    }
}
