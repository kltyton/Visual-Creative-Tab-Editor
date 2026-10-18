package com.kltyton.visual_creative_tab_editor.mixin.forge;

import com.kltyton.visual_creative_tab_editor.client.ForgeCreativeTabPagesView;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes Forge's patch-added page list without an Accessor or per-frame reflection.
 *
 * <p>The packaged AT makes {@code pages} public at runtime, but ForgeGradle adds this
 * field after its source AT pass, so direct Java access cannot compile.</p>
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenPagesMixin implements ForgeCreativeTabPagesView {
    @Shadow(remap = false)
    @Final
    private List<CreativeTabsScreenPage> pages;

    @Override
    public List<CreativeTabsScreenPage> visualCreativeTabEditor$pages() {
        return this.pages;
    }
}
