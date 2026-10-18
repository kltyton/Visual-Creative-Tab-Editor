package com.kltyton.visual_creative_tab_editor.mixin;

import com.kltyton.visual_creative_tab_editor.pack.PinnedWorldPackSource;
import java.nio.file.Path;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPacksSource.class)
public abstract class ServerPacksSourceMixin {
    @Inject(method = "createPackRepository(Ljava/nio/file/Path;Lnet/minecraft/world/level/validation/DirectoryValidator;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At("RETURN"), cancellable = true)
    private static void visualCreativeTabEditor$appendPinnedWorldPacks(
            Path datapackDirectory,
            DirectoryValidator validator,
            CallbackInfoReturnable<PackRepository> callback
    ) {
        PackRepository original = callback.getReturnValue();
        original.sources.add(new PinnedWorldPackSource(datapackDirectory));
    }
}
