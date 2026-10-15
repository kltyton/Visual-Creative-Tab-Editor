package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws group cell backgrounds and badges in the native slot pass. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenItemGroupMixin {
// @target if minecraft_version=26.1.2,26.2
//|    @Inject(method = "extractSlot", at = @At("HEAD"))
// @target else
    @Inject(method = "renderSlot", at = @At("HEAD"))
// @target end
    private void visualCreativeTabEditor$renderGroupBackground(
            GuiGraphics graphics, Slot slot,
// @target if minecraft_version=26.1.2,26.2
//|            int mouseX, int mouseY,
// @target else
// @target end
            CallbackInfo callback
    ) {
        if ((Object) this instanceof CreativeTabEditorHost host) {
            host.visualCreativeTabEditor$renderItemGroupBackground(graphics, slot);
        }
    }

// @target if minecraft_version=26.1.2,26.2
//|    @Inject(method = "extractSlot", at = @At("TAIL"))
// @target else
    @Inject(method = "renderSlot", at = @At("TAIL"))
// @target end
    private void visualCreativeTabEditor$renderGroupBadge(
            GuiGraphics graphics, Slot slot,
// @target if minecraft_version=26.1.2,26.2
//|            int mouseX, int mouseY,
// @target else
// @target end
            CallbackInfo callback
    ) {
        if ((Object) this instanceof CreativeTabEditorHost host) {
            host.visualCreativeTabEditor$renderItemGroupBadge(graphics, slot);
        }
    }
}
