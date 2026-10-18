package com.kltyton.visual_creative_tab_editor.client.editor.render;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
import com.kltyton.visual_creative_tab_editor.data.ResourceId;
import com.kltyton.visual_creative_tab_editor.platform.registry.NativeIds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.CreativeModeTab;
import com.mojang.blaze3d.systems.RenderSystem;

/** Native texture and foreground handling for Minecraft 1.20.1. */
public final class NativeEditorGraphics extends LegacyEditorGraphics {
    private static final java.util.Map<String, ResourceId> GROUP_TEXTURES = java.util.stream.Stream
            .of("icon_slot", "item_slot", "plus", "minus").collect(java.util.stream.Collectors.toUnmodifiableMap(
                    name -> name, name -> ResourceId.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID,
                            "textures/gui/sprites/container/item_groups/" + name + ".png")));

    public NativeEditorGraphics(GuiGraphics graphics) { super(graphics); }
    public void groupSprite(String name, int x, int y, int size) {
        var sprite = NativeIds.unwrap(GROUP_TEXTURES.get(name));
        RenderSystem.enableBlend();
        graphics.blit(sprite, x, y, 0, 0, size, size, size, size);
    }
    public void tabSprite(CreativeModeTab.Row row, int column, int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        graphics.blit(NativeIds.unwrap(ResourceId.withDefaultNamespace("textures/gui/container/creative_inventory/tabs.png")),
                x, y, column * 26, row == CreativeModeTab.Row.TOP ? 0 : 64, width, height);
    }
    public void categoryTab(int edge, boolean selected, int x, int y) {
        RenderSystem.enableBlend();
        graphics.blit(NativeIds.unwrap(ResourceId.withDefaultNamespace("textures/gui/advancements/tabs.png")),
                x, y, edge * 32, selected ? 92 : 64, 32, 28);
    }
    public void overlay(Runnable draw) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 500.0F);
        try { draw.run(); }
        finally { graphics.flush(); graphics.pose().popPose(); }
    }
}
