package com.kltyton.visual_creative_tab_editor.client.editor;

/**
 * Controls whether the creative item grid reserves one empty virtual tail slot.
 */
public interface CreativeModeItemPickerMenuVirtualTail {
    void visualCreativeTabEditor$setVirtualTail(boolean enabled);

    boolean visualCreativeTabEditor$hasVirtualTail();
}
