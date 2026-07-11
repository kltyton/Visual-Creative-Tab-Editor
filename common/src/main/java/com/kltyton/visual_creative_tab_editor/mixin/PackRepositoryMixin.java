package com.kltyton.visual_creative_tab_editor.mixin;

import com.kltyton.visual_creative_tab_editor.pack.PinnedWorldPackSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Inject(method = "rebuildSelected", at = @At("RETURN"), cancellable = true)
    private void visualCreativeTabEditor$pinBoundaryPacks(
            Collection<String> selectedNames,
            CallbackInfoReturnable<List<Pack>> callback
    ) {
        List<Pack> ordered = new ArrayList<>(callback.getReturnValue());
        moveToBoundary(ordered, PinnedWorldPackSource.DEFAULT_PACK_ID, false);
        moveToBoundary(ordered, PinnedWorldPackSource.PLAYER_PACK_ID, true);
        callback.setReturnValue(List.copyOf(ordered));
    }

    private static void moveToBoundary(List<Pack> packs, String id, boolean top) {
        Pack pinned = packs.stream().filter(pack -> pack.getId().equals(id)).findFirst().orElse(null);
        if (pinned == null) {
            return;
        }

        packs.remove(pinned);
        if (top) {
            packs.add(pinned);
        } else {
            packs.addFirst(pinned);
        }
    }
}
