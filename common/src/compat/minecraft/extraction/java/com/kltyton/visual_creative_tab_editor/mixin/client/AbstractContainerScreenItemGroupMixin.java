package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws group cell backgrounds and badges in the native slot pass. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenItemGroupMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void visualCreativeTabEditor$renderGroupBackground(
            GuiGraphicsExtractor graphics, Slot slot,
            int mouseX, int mouseY,
            CallbackInfo callback
    ) {
        if ((Object) this instanceof CreativeTabEditorHost host) {
            host.visualCreativeTabEditor$renderItemGroupBackground(new com.kltyton.visual_creative_tab_editor.client.editor.render.NativeEditorGraphics(graphics), slot);
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void visualCreativeTabEditor$renderGroupBadge(
            GuiGraphicsExtractor graphics, Slot slot,
            int mouseX, int mouseY,
            CallbackInfo callback
    ) {
        if ((Object) this instanceof CreativeTabEditorHost host) {
            host.visualCreativeTabEditor$renderItemGroupBadge(new com.kltyton.visual_creative_tab_editor.client.editor.render.NativeEditorGraphics(graphics), slot);
        }
    }
}
