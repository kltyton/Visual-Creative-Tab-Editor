package com.kltyton.visual_creative_tab_editor.mixin;

import com.kltyton.visual_creative_tab_editor.pack.PinnedWorldPackSource;
import java.nio.file.Path;
// @target if minecraft_version=26.1.2,26.2
// @target else
import java.util.ArrayList;
import java.util.List;
// @target end
import net.minecraft.server.packs.repository.PackRepository;
// @target if minecraft_version=26.1.2,26.2
// @target else
import net.minecraft.server.packs.repository.RepositorySource;
// @target end
import net.minecraft.server.packs.repository.ServerPacksSource;
// @target if minecraft_version=1.20.1
// @target else
import net.minecraft.world.level.validation.DirectoryValidator;
// @target end
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPacksSource.class)
public abstract class ServerPacksSourceMixin {
// @target if minecraft_version=1.20.1
//|    @Inject(method = "createPackRepository(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At("RETURN"), cancellable = true)
// @target else
    @Inject(method = "createPackRepository(Ljava/nio/file/Path;Lnet/minecraft/world/level/validation/DirectoryValidator;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At("RETURN"), cancellable = true)
// @target end
    private static void visualCreativeTabEditor$appendPinnedWorldPacks(
            Path datapackDirectory,
// @target if minecraft_version=1.20.1
// @target else
            DirectoryValidator validator,
// @target end
            CallbackInfoReturnable<PackRepository> callback
    ) {
        PackRepository original = callback.getReturnValue();
// @target if minecraft_version=26.1.2,26.2
//|        original.sources.add(new PinnedWorldPackSource(datapackDirectory));
// @target else
        List<RepositorySource> sources = new ArrayList<>(original.sources);
        sources.add(new PinnedWorldPackSource(datapackDirectory));
        callback.setReturnValue(new PackRepository(sources.toArray(RepositorySource[]::new)));
// @target end
    }
}
