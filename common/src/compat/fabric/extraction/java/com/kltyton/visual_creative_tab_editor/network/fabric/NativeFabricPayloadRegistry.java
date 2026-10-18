package com.kltyton.visual_creative_tab_editor.network.fabric;

import com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Binds the stable editor codecs to Fabric's native direction registries. */
public final class NativeFabricPayloadRegistry {
    private NativeFabricPayloadRegistry() {}
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(NativePayloadTypes.SNAPSHOT_CHUNK, NativePayloadTypes.SNAPSHOT_CHUNK_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NativePayloadTypes.EDIT_CHUNK, NativePayloadTypes.EDIT_CHUNK_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NativePayloadTypes.EDIT_RESULT, NativePayloadTypes.EDIT_RESULT_CODEC);
    }
}
