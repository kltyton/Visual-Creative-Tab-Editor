package com.kltyton.visual_creative_tab_editor.client.editor.render;

/** Native pixel drawing adapted from mcui-oreui's Ore palette and control bevels. */
public final class EditorTheme {
    public static final int SCRIM = 0x99000000;
    public static final int PANEL = 0xFF48494A;
    public static final int SURFACE = 0xFF58585A;
    public static final int SURFACE_HOVER = 0xFF6D6D6E;
    public static final int HEADER = 0xFFE6E8EB;
    public static final int BORDER = 0xFF1E1E1F;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFFD0D1D4;
    public static final int TEXT_DARK = 0xFF1E1E1F;
    public static final int TEXT_DISABLED = 0xFF48494A;
    public static final int ACCENT = 0xFF3C8527;

    public enum Tone {
        NORMAL(0xFFD0D1D4, 0xFFB1B2B5, 0xFF58585A, TEXT_DARK),
        PRIMARY(ACCENT, 0xFF2A641C, 0xFF1D4D13, TEXT),
        DANGER(0xFFC33636, 0xFFC02D2D, 0xFFAD1D1D, TEXT);

        private final int face;
        private final int hover;
        private final int shadow;
        private final int text;

        Tone(int face, int hover, int shadow, int text) {
            this.face = face;
            this.hover = hover;
            this.shadow = shadow;
            this.text = text;
        }
    }

    private EditorTheme() {}

    public static void panel(EditorGraphics graphics, int x, int y, int width, int height, int headerHeight) {
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x66000000);
        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 2, PANEL);
        graphics.fill(x + 1, y + 1, x + width - 1, y + headerHeight, HEADER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, TEXT);
        graphics.fill(x + 1, y + headerHeight, x + width - 1, y + headerHeight + 1, BORDER);
    }

    public static int button(EditorGraphics graphics, int x, int y, int width, int height,
                             Tone tone, boolean enabled, boolean hovered) {
        int face = !enabled ? 0xFFD0D1D4 : hovered ? tone.hover : tone.face;
        int edge = !enabled ? 0xFF8C8D90 : hovered ? TEXT : BORDER;
        int shadow = enabled ? tone.shadow : 0xFFB1B2B5;
        graphics.fill(x, y, x + width, y + height, edge);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, shadow);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 3, face);
        if (enabled) {
            int highlight = tone == Tone.NORMAL ? 0x99FFFFFF : 0x33FFFFFF;
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, highlight);
            graphics.fill(x + 1, y + 2, x + 2, y + height - 3, highlight);
            graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 3, 0x1AFFFFFF);
        }
        return enabled ? tone.text : TEXT_DISABLED;
    }

    public static void row(EditorGraphics graphics, int x, int y, int width, int height,
                           boolean focused, boolean hovered) {
        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hovered ? SURFACE_HOVER : SURFACE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0x26FFFFFF);
        if (focused) { graphics.fill(x + 1, y + 1, x + 3, y + height - 1, ACCENT); }
    }

    public static void inset(EditorGraphics graphics, int x, int y, int width, int height, boolean focused) {
        graphics.fill(x, y, x + width, y + height, focused ? ACCENT : BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF313233);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, SURFACE_HOVER);
    }

    public static void checkbox(EditorGraphics graphics, int x, int y, int size, boolean selected, boolean hovered) {
        graphics.fill(x, y, x + size, y + size, BORDER);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1,
                selected ? (hovered ? Tone.PRIMARY.hover : ACCENT) : hovered ? 0xFFB1B2B5 : 0xFF8C8D90);
        graphics.fill(x + 1, y + 1, x + size - 1, y + 2, 0x33FFFFFF);
        if (selected) {
            int px = x + (size - 8) / 2;
            int py = y + (size - 8) / 2;
            graphics.fill(px + 2, py + 4, px + 3, py + 6, TEXT);
            graphics.fill(px + 3, py + 5, px + 4, py + 6, TEXT);
            graphics.fill(px + 4, py + 4, px + 5, py + 5, TEXT);
            graphics.fill(px + 5, py + 3, px + 6, py + 4, TEXT);
            graphics.fill(px + 6, py + 2, px + 7, py + 3, TEXT);
        }
    }

}
