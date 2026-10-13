package com.kltyton.visual_creative_tab_editor.client;

import net.minecraft.client.gui.components.EditBox;

/** Uses the native text-shadow setting where the loader supplies it. */
public final class CreativeTabTextInput {
    private CreativeTabTextInput() {
    }

    public static void disableShadow(EditBox editor) {
// @target if loader=neoforge
//|        editor.setTextShadow(false);
// @target elif minecraft_version=26.1.2,26.2
//|        editor.setTextShadow(false);
// @target else
        ((com.kltyton.visual_creative_tab_editor.client.editor.ShadowlessTextInput) editor)
                .visualCreativeTabEditor$disableTextShadow();
// @target end
    }
}
