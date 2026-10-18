package com.kltyton.visual_creative_tab_editor.client.platform;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/** Reinitializes the creative screen through its native resize signature. */
public final class NativeScreenLifecycle {
    private NativeScreenLifecycle() {}
    public static void refresh(CreativeModeInventoryScreen screen) {
        screen.resize(net.minecraft.client.Minecraft.getInstance(), screen.width, screen.height);
    }
}
