package com.kltyton.one_enough_creative_tab.mixin.client;

import com.kltyton.one_enough_creative_tab.client.editor.CreativeModeItemPickerMenuVirtualTail;
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
    private boolean oneEnoughCreativeTab$virtualTail;

    @Override
    @Unique
    public void oneEnoughCreativeTab$setVirtualTail(boolean enabled) {
        this.oneEnoughCreativeTab$virtualTail = enabled;
    }

    @Override
    @Unique
    public boolean oneEnoughCreativeTab$hasVirtualTail() {
        return this.oneEnoughCreativeTab$virtualTail;
    }

    @Inject(method = "calculateRowCount", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$includeVirtualTailInRowCount(CallbackInfoReturnable<Integer> callback) {
        if (this.oneEnoughCreativeTab$virtualTail) {
            callback.setReturnValue(Mth.positiveCeilDiv(this.items.size() + 1, 9) - 5);
        }
    }

    @Inject(method = "canScroll", at = @At("HEAD"), cancellable = true)
    private void oneEnoughCreativeTab$includeVirtualTailInScrollCheck(CallbackInfoReturnable<Boolean> callback) {
        if (this.oneEnoughCreativeTab$virtualTail) {
            callback.setReturnValue(this.items.size() + 1 > 45);
        }
    }
}
