package com.kltyton.visual_creative_tab_editor.platform.time;

/** Uses Minecraft's monotonic clock for editor gesture timing. */
public final class NativeClock {
    private NativeClock() {}
    public static long millis() { return net.minecraft.util.Util.getMillis(); }
}
