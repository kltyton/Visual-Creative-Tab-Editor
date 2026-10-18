package com.kltyton.visual_creative_tab_editor.platform.pack;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

/** Encodes the native data-pack format in Minecraft's expected metadata shape. */
public final class NativePackMetadata {
    private NativePackMetadata() {}
    public static void writeFormat(JsonObject pack) {
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
    }
}
