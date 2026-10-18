package com.kltyton.visual_creative_tab_editor.client.editor.input;

import net.minecraft.world.inventory.ClickType;

/** Preserves the native inventory action across the shared editor state. */
public final class NativeSlotInput {
    private NativeSlotInput() {}
    public record Handle(ClickType value) {}
    public static SlotInput wrap(ClickType value) {
        if (value == ClickType.PICKUP) { return SlotInput.PICKUP; }
        if (value == ClickType.QUICK_MOVE) { return SlotInput.QUICK_MOVE; }
        return new SlotInput(new Handle(value));
    }
    public static ClickType unwrap(SlotInput input) { return input.handle().value(); }
    static SlotInput pickup() { return new SlotInput(new Handle(ClickType.PICKUP)); }
    static SlotInput quickMove() { return new SlotInput(new Handle(ClickType.QUICK_MOVE)); }
}
