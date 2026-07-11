package com.kltyton.visual_creative_tab_editor;

/** Loader-independent bootstrap for shared mod code. */
public final class VisualCreativeTabEditorCommon {
    private VisualCreativeTabEditorCommon() {
    }

    /** Initializes the shared mod bootstrap. */
    public static void initialize() {
        VisualCreativeTabEditorConstants.LOGGER.info("Initializing {}", VisualCreativeTabEditorConstants.MOD_NAME);
    }
}
