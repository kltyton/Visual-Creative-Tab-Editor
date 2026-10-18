package com.kltyton.visual_creative_tab_editor.client.editor.input;

import net.minecraft.world.inventory.ContainerInput;

/** Preserves the native inventory action across the shared editor state. */
public final class NativeSlotInput {
    private NativeSlotInput() {}
    public record Handle(ContainerInput value) {}
    public static SlotInput wrap(ContainerInput value) {
        if (value == ContainerInput.PICKUP) { return SlotInput.PICKUP; }
        if (value == ContainerInput.QUICK_MOVE) { return SlotInput.QUICK_MOVE; }
        return new SlotInput(new Handle(value));
    }
    public static ContainerInput unwrap(SlotInput input) { return input.handle().value(); }
    static SlotInput pickup() { return new SlotInput(new Handle(ContainerInput.PICKUP)); }
    static SlotInput quickMove() { return new SlotInput(new Handle(ContainerInput.QUICK_MOVE)); }
}
