package com.kltyton.visual_creative_tab_editor.client.editor.input;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;

/** Binds editor operations to the native input signatures of this Minecraft API. */
public final class NativeEditorInput {
    private NativeEditorInput() {}
    public record Key(KeyEvent event) implements EditorInput.Key {
        public int key() { return event.key(); }
        public int modifiers() { return event.modifiers(); }
        public boolean press(EditBox editor) { return editor.keyPressed(event); }
    }
    public record Character(CharacterEvent event) implements EditorInput.Character {
        public boolean type(EditBox editor) { return editor.charTyped(event); }
    }
    public record Preedit(PreeditEvent event) implements EditorInput.Preedit {
        public boolean update(EditBox editor) { return editor.preeditUpdated(event); }
    }
    public record Pointer(MouseButtonEvent event) implements EditorInput.Pointer {
        public double x() { return event.x(); }
        public double y() { return event.y(); }
        public int button() { return event.button(); }
        public boolean hasShiftDown() { return event.hasShiftDown(); }
        public boolean drag(EditBox editor, double deltaX, double deltaY) { return editor.mouseDragged(event, deltaX, deltaY); }
        public boolean release(EditBox editor) { return editor.mouseReleased(event); }
    }
}
