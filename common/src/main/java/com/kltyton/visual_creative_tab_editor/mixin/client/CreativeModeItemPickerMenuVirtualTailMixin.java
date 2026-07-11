package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeModeItemPickerMenuVirtualTail;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class CreativeModeItemPickerMenuVirtualTailMixin implements CreativeModeItemPickerMenuVirtualTail {
    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Unique
    private boolean visualCreativeTabEditor$virtualTail;

    @Override
    @Unique
    public void visualCreativeTabEditor$setVirtualTail(boolean enabled) {
        this.visualCreativeTabEditor$virtualTail = enabled;
    }

    @Override
    @Unique
    public boolean visualCreativeTabEditor$hasVirtualTail() {
        return this.visualCreativeTabEditor$virtualTail;
    }

    @Inject(method = "calculateRowCount", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$includeVirtualTailInRowCount(CallbackInfoReturnable<Integer> callback) {
        if (this.visualCreativeTabEditor$virtualTail) {
            callback.setReturnValue(Mth.positiveCeilDiv(this.items.size() + 1, 9) - 5);
        }
    }

    @Inject(method = "canScroll", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$includeVirtualTailInScrollCheck(CallbackInfoReturnable<Boolean> callback) {
        if (this.visualCreativeTabEditor$virtualTail) {
            callback.setReturnValue(this.items.size() + 1 > 45);
        }
    }
}
