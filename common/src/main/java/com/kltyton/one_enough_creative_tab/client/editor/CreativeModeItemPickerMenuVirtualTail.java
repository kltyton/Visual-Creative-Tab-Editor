package com.kltyton.one_enough_creative_tab.client.editor;

/**
 * Controls whether the creative item grid reserves one empty virtual tail slot.
 */
public interface CreativeModeItemPickerMenuVirtualTail {
    void oneEnoughCreativeTab$setVirtualTail(boolean enabled);

    boolean oneEnoughCreativeTab$hasVirtualTail();
}
