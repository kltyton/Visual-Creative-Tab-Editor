package com.kltyton.visual_creative_tab_editor.client.editor;

import com.kltyton.visual_creative_tab_editor.VisualCreativeTabEditorConstants;
// @target if minecraft_version=1.20.1
//|import com.mojang.blaze3d.systems.RenderSystem;
// @target else
// @target end
import net.minecraft.client.gui.GuiGraphics;
// @target if minecraft_version=26.1.2,26.2
//|import net.minecraft.client.renderer.RenderPipelines;
// @target else
// @target end
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

/** Renders Bedrock-style group cells separately from ordinary creative slots. */
public final class CreativeItemGroupRenderer {
    private static final ResourceLocation HEADER = sprite("icon_slot");
    private static final ResourceLocation MEMBER = sprite("item_slot");
    private static final ResourceLocation PLUS = sprite("plus");
    private static final ResourceLocation MINUS = sprite("minus");

    private CreativeItemGroupRenderer() {}

    public static void background(GuiGraphics graphics, Slot slot, boolean header) {
        draw(graphics, header ? HEADER : MEMBER, slot.x - 1, slot.y - 1, 18);
    }

    public static void badge(GuiGraphics graphics, Slot slot, boolean expanded) {
// @target if minecraft_version=26.1.2,26.2
//|        graphics.nextStratum();
// @target else
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 350.0F);
        try {
// @target end
            draw(graphics, expanded ? MINUS : PLUS, slot.x, slot.y, 16);
// @target if minecraft_version=26.1.2,26.2
// @target else
        } finally {
            graphics.pose().popPose();
        }
// @target end
    }

    private static ResourceLocation sprite(String name) {
// @target if minecraft_version=1.20.1
//|        return new ResourceLocation(VisualCreativeTabEditorConstants.MOD_ID,
//|                "textures/gui/sprites/container/item_groups/" + name + ".png");
// @target else
        return ResourceLocation.fromNamespaceAndPath(VisualCreativeTabEditorConstants.MOD_ID,
                "container/item_groups/" + name);
// @target end
    }

    private static void draw(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int size) {
// @target if minecraft_version=1.20.1
//|        RenderSystem.enableBlend();
//|        graphics.blit(sprite, x, y, 0, 0, size, size, size, size);
// @target elif minecraft_version=26.1.2,26.2
//|        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size);
// @target else
        graphics.blitSprite(sprite, x, y, size, size);
// @target end
    }
}
