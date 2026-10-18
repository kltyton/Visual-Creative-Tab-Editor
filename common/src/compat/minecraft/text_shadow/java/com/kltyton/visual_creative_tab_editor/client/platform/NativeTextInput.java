package com.kltyton.visual_creative_tab_editor.client.platform;

import net.minecraft.client.gui.components.EditBox;

/** Native shadow control for the active loader and text widget. */
public final class NativeTextInput {
    private NativeTextInput() {}
    public static void disableShadow(EditBox editor) {
        ((com.kltyton.visual_creative_tab_editor.client.editor.ShadowlessTextInput) editor).visualCreativeTabEditor$disableTextShadow();
    }
}
