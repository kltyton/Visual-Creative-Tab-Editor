package com.kltyton.visual_creative_tab_editor.client.editor.input;

import net.minecraft.client.gui.components.EditBox;


/** Binds editor operations to the native input signatures of this Minecraft API. */
public final class NativeEditorInput {
    private NativeEditorInput() {}
    public record Key(int key, int scanCode, int modifiers) implements EditorInput.Key {
        public boolean press(EditBox editor) { return editor.keyPressed(key, scanCode, modifiers); }
    }
    public record Character(char codePoint, int modifiers) implements EditorInput.Character {
        public boolean type(EditBox editor) { return editor.charTyped(codePoint, modifiers); }
    }
    public record Pointer(double x, double y, int button, boolean hasShiftDown) implements EditorInput.Pointer {
        public boolean drag(EditBox editor, double deltaX, double deltaY) { return editor.mouseDragged(x, y, button, deltaX, deltaY); }
        public boolean release(EditBox editor) { return editor.mouseReleased(x, y, button); }
    }
}
