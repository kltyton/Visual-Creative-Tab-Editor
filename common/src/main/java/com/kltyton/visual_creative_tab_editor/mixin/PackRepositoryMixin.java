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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
    @Inject(method = "reload", at = @At("TAIL"))
    private void visualCreativeTabEditor$pinAfterReload(CallbackInfo callback) {
        this.visualCreativeTabEditor$restorePinnedBoundaries();
    }

    @Inject(method = "setSelected", at = @At("TAIL"))
    private void visualCreativeTabEditor$pinAfterSetSelected(
            Collection<String> selectedNames,
            CallbackInfo callback
    ) {
        this.visualCreativeTabEditor$restorePinnedBoundaries();
    }

    private void visualCreativeTabEditor$restorePinnedBoundaries() {
        PackRepository repository = (PackRepository) (Object) this;
        List<Pack> selected = new ArrayList<>(repository.getSelectedPacks());
        List<Pack> ordered = new ArrayList<>(selected);
        moveToBoundary(ordered, repository.getPack(PinnedWorldPackSource.DEFAULT_PACK_ID), false);
        moveToBoundary(ordered, repository.getPack(PinnedWorldPackSource.PLAYER_PACK_ID), true);
        if (selected.equals(ordered)) {
            return;
        }
        for (Pack pack : selected) {
            repository.removePack(pack.getId());
        }
        for (Pack pack : ordered) {
            repository.addPack(pack.getId());
        }
    }

    private static void moveToBoundary(List<Pack> packs, Pack pinned, boolean top) {
        if (pinned == null) {
            return;
        }

        packs.removeIf(pack -> pack.getId().equals(pinned.getId()));
        if (top) {
            packs.add(pinned);
        } else {
            packs.add(0, pinned);
        }
    }
}
