package com.kltyton.visual_creative_tab_editor.mixin.compat.createpropulsion;

import com.kltyton.visual_creative_tab_editor.client.CreativeTabClientState;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeModeItemPickerMenuVirtualTail;
import com.kltyton.visual_creative_tab_editor.client.editor.CreativeTabEditorHost;
import com.kltyton.visual_creative_tab_editor.runtime.CreativeTabRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps Propulsion's section banners aligned with grouped items and the native creative scroll position. */
@Pseudo
@Mixin(targets = "dev.propulsionteam.propulsionsimulated.client.PropulsionCreativeTabRenderer", remap = false)
public abstract class PropulsionCreativeTabRendererMixin {
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private static Integer visualCreativeTabEditor$sectionRow(Integer row,
            CreativeModeInventoryScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (row == null) { return null; }
        CreativeTabEditorHost host = (CreativeTabEditorHost) screen;
        var baseline = CreativeTabRuntime.id(host.visualCreativeTabEditor$selectedTab())
                .flatMap(CreativeTabClientState.baseCatalog()::definition).orElse(null);
        return ((CreativeModeItemPickerMenuVirtualTail) host.visualCreativeTabEditor$menu())
                .visualCreativeTabEditor$itemGroups().projectReservedRow(row, baseline);
    }

    // This pure static-field read is the renderer's only scroll input; using the native menu row retains its drawing callbacks.
    @Redirect(method = "render", at = @At(value = "FIELD",
            target = "Ldev/simulated_team/simulated/registrate/simulated_tab/SimulatedCreativeTab;CURRENT_ROW:I"))
    private static int visualCreativeTabEditor$scrollRow(
            CreativeModeInventoryScreen screen, GuiGraphics graphics, int mouseX, int mouseY) {
        CreativeTabEditorHost host = (CreativeTabEditorHost) screen;
        return host.visualCreativeTabEditor$menu().getRowIndexForScroll(host.visualCreativeTabEditor$scrollOffset());
    }
}
