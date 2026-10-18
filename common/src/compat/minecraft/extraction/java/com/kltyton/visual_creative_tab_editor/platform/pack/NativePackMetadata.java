package com.kltyton.visual_creative_tab_editor.platform.pack;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

/** Encodes the native data-pack format in Minecraft's expected metadata shape. */
public final class NativePackMetadata {
    private NativePackMetadata() {}
    public static void writeFormat(JsonObject pack) {
        var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
        var encoded = new com.google.gson.JsonArray();
        encoded.add(format.major());
        encoded.add(format.minor());
        pack.add("min_format", encoded.deepCopy());
        pack.add("max_format", encoded.deepCopy());
    }
}
