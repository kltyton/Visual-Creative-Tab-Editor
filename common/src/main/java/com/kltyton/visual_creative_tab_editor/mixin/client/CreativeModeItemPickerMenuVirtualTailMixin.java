package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeModeItemPickerMenuVirtualTail;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeItemGroups;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import com.kltyton.visual_creative_tab_editor.data.CreativeTabDefinition;
import com.kltyton.visual_creative_tab_editor.data.itemgroup.CreativeItemGrouping;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Maps the visible creative grid and its scroll range to the current item groups. */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class CreativeModeItemPickerMenuVirtualTailMixin implements CreativeModeItemPickerMenuVirtualTail {
    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Unique
    private boolean visualCreativeTabEditor$virtualTail;

    @Unique
    private CreativeItemGroups visualCreativeTabEditor$groups;

    @Override
    @Unique
    public CreativeItemGroups visualCreativeTabEditor$itemGroups() {
        if (this.visualCreativeTabEditor$groups == null) {
            this.visualCreativeTabEditor$groups = new CreativeItemGroups();
        }
        if (this.visualCreativeTabEditor$groups.tab() != null) {
            this.visualCreativeTabEditor$groups.setGrouping(CreativeTabRuntime.definition(this.visualCreativeTabEditor$groups.tab())
                    .map(CreativeTabDefinition::grouping).orElse(CreativeItemGrouping.EMPTY));
        }
        this.visualCreativeTabEditor$groups.refresh(this.items);
        return this.visualCreativeTabEditor$groups;
    }

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
        CreativeItemGroups groups = this.visualCreativeTabEditor$itemGroups();
        if (groups.hasProjection() || this.visualCreativeTabEditor$virtualTail) {
            callback.setReturnValue(Mth.positiveCeilDiv(groups.size() + (this.visualCreativeTabEditor$virtualTail ? 1 : 0), 9) - 5);
        }
    }

    @Inject(method = "canScroll", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$includeVirtualTailInScrollCheck(CallbackInfoReturnable<Boolean> callback) {
        CreativeItemGroups groups = this.visualCreativeTabEditor$itemGroups();
        if (groups.hasProjection() || this.visualCreativeTabEditor$virtualTail) {
            callback.setReturnValue(groups.size() + (this.visualCreativeTabEditor$virtualTail ? 1 : 0) > 45);
        }
    }

    @Inject(method = "scrollTo", at = @At("TAIL"))
    private void visualCreativeTabEditor$displayItemGroups(float scroll, CallbackInfo callback) {
        CreativeItemGroups groups = this.visualCreativeTabEditor$itemGroups();
        if (!groups.hasProjection()) {
            return;
        }
        CreativeModeInventoryScreen.ItemPickerMenu menu = (CreativeModeInventoryScreen.ItemPickerMenu) (Object) this;
        int first = menu.getRowIndexForScroll(scroll) * 9;
        for (int slot = 0; slot < 45; slot++) {
            int displayIndex = first + slot;
            menu.slots.get(slot).set(displayIndex < groups.size()
                    ? groups.displayStack(displayIndex, this.items) : ItemStack.EMPTY);
        }
    }
}
