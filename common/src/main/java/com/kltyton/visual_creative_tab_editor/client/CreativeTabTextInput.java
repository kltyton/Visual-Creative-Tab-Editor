package com.kltyton.visual_creative_tab_editor.client;

import net.minecraft.client.gui.components.EditBox;

/** Uses the shadow setting exposed by the selected client API. */
public final class CreativeTabTextInput {
    private CreativeTabTextInput() {}
    public static void disableShadow(EditBox editor) {
        com.kltyton.visual_creative_tab_editor.client.platform.NativeTextInput.disableShadow(editor);
    }
}
