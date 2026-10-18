package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.client.editor.input.SlotInput;
import com.kltyton.visual_creative_tab_editor.client.editor.input.NativeSlotInput;
import com.kltyton.visual_creative_tab_editor.client.editor.input.NativeEditorInput;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forwards native input events without dropping widget or inventory action data. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class NativeEditorInputMixin {
    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ClickType input);
    public void visualCreativeTabEditor$replaySlotClick(Slot slot, int slotId, int button, SlotInput input) {
        this.slotClicked(slot, slotId, button, NativeSlotInput.unwrap(input));
    }


    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseClicked(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().mouseClicked(new NativeEditorInput.Pointer(mouseX, mouseY, button, net.minecraft.client.gui.screens.Screen.hasShiftDown()), false)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$slotClicked(
            @Nullable Slot slot,
            int slotId,
            int button,
            ClickType input,
            CallbackInfo callback
    ) {
        if (((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().slotClicked(slot, slotId, button, NativeSlotInput.wrap(input))) {
            callback.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().mouseDragged(new NativeEditorInput.Pointer(mouseX, mouseY, button, net.minecraft.client.gui.screens.Screen.hasShiftDown()), deltaX, deltaY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$mouseReleasedHead(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().mouseReleased(new NativeEditorInput.Pointer(mouseX, mouseY, button, net.minecraft.client.gui.screens.Screen.hasShiftDown()))) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void visualCreativeTabEditor$mouseReleasedReturn(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> callback
    ) {
        ((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().afterMouseReleased(new NativeEditorInput.Pointer(mouseX, mouseY, button, net.minecraft.client.gui.screens.Screen.hasShiftDown()));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$keyPressed(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().keyPressed(new NativeEditorInput.Key(keyCode, scanCode, modifiers))) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void visualCreativeTabEditor$charTyped(
            char codePoint,
            int modifiers,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (((CreativeTabEditorHost) (Object) this).visualCreativeTabEditor$editor().charTyped(new NativeEditorInput.Character(codePoint, modifiers))) {
            callback.setReturnValue(true);
        }
    }
}
