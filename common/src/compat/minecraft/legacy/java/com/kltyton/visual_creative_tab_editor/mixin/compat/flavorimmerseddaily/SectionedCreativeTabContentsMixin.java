package com.kltyton.visual_creative_tab_editor.mixin.compat.flavorimmerseddaily;

import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabContents;
import java.util.Collection;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Captures section contents before Flavor Immersed Daily adds transient banner markers. */
@Pseudo
@Mixin(targets = "com.flavor_immersed_daily.creative.SectionedCreativeModeTab", remap = false)
public abstract class SectionedCreativeTabContentsMixin implements CreativeTabContents {
    @Override
    public Collection<ItemStack> visualCreativeTabEditor$catalogDisplayItems() {
        return visualCreativeTabEditor$backingDisplayItems();
    }
}
