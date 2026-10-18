package com.kltyton.visual_creative_tab_editor.client.editor.input;

import net.minecraft.client.gui.components.EditBox;

/** Editor input retains the complete native event until it reaches the text widget. */
public final class EditorInput {
    private EditorInput() {}
    public interface Key {
        int key();
        int modifiers();
        boolean press(EditBox editor);
        default boolean isEscape() { return key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE; }
    }
    public interface Character { boolean type(EditBox editor); }
    public interface Preedit { boolean update(EditBox editor); }
    public interface Pointer {
        double x();
        double y();
        int button();
        boolean hasShiftDown();
        boolean drag(EditBox editor, double deltaX, double deltaY);
        boolean release(EditBox editor);
    }
}
