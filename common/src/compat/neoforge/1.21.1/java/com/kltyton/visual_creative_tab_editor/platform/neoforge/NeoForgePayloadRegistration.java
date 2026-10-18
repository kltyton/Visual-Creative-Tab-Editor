package com.kltyton.visual_creative_tab_editor.platform.neoforge;

import com.kltyton.visual_creative_tab_editor.network.EditChunkPayload;
import com.kltyton.visual_creative_tab_editor.network.EditResultPayload;
import com.kltyton.visual_creative_tab_editor.network.SnapshotChunkPayload;
import com.kltyton.visual_creative_tab_editor.server.CreativeTabServerManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.kltyton.visual_creative_tab_editor.client.VisualCreativeTabEditorNeoForgeClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

/** Registers the native optional protocol and preserves its physical-side handlers. */
public final class NeoForgePayloadRegistration {
    private static final String NETWORK_VERSION = "4";
    private NeoForgePayloadRegistration() {}


    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION).optional();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            VisualCreativeTabEditorNeoForgeClient.registerPayloads(registrar);
        } else {
            registrar.playToClient(
                    com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK,
                    com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.SNAPSHOT_CHUNK_CODEC,
                    NeoForgePayloadRegistration::rejectMisroutedClientboundPayload
            );
            registrar.playToClient(
                    com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT,
                    com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_RESULT_CODEC,
                    NeoForgePayloadRegistration::rejectMisroutedClientboundPayload
            );
        }
        registrar.playToServer(
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK,
                com.kltyton.visual_creative_tab_editor.network.NativePayloadTypes.EDIT_CHUNK_CODEC,
                (payload, context) -> CreativeTabServerManager.handleEditChunk((ServerPlayer) context.player(), payload)
        );
    }


    private static void rejectMisroutedClientboundPayload(
            CustomPacketPayload payload,
            IPayloadContext context
    ) {
        throw new IllegalStateException(
                "Clientbound payload " + payload.type().id() + " was delivered on a dedicated server"
        );
    }
}
