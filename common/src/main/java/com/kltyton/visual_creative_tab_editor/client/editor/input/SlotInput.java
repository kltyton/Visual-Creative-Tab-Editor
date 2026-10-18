package com.kltyton.visual_creative_tab_editor.client.editor.input;

/** A retained native inventory click, including actions the editor passes through. */
public record SlotInput(NativeSlotInput.Handle handle) {
    public static final SlotInput PICKUP = NativeSlotInput.pickup();
    public static final SlotInput QUICK_MOVE = NativeSlotInput.quickMove();
}
