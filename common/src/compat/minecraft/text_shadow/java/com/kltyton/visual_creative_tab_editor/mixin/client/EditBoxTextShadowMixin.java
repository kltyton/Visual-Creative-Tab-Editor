package com.kltyton.visual_creative_tab_editor.mixin.client;

import com.kltyton.visual_creative_tab_editor.client.editor.ShadowlessTextInput;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EditBox.class)
public abstract class EditBoxTextShadowMixin implements ShadowlessTextInput {
    @Unique private boolean visualCreativeTabEditor$shadowless;

    @Override
    public void visualCreativeTabEditor$disableTextShadow() {
        this.visualCreativeTabEditor$shadowless = true;
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int visualCreativeTabEditor$formatted(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color) {
        return graphics.drawString(font, text, x, y, color, !this.visualCreativeTabEditor$shadowless);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private int visualCreativeTabEditor$component(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        return graphics.drawString(font, text, x, y, color, !this.visualCreativeTabEditor$shadowless);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
    private int visualCreativeTabEditor$string(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        return graphics.drawString(font, text, x, y, color, !this.visualCreativeTabEditor$shadowless);
    }
}
