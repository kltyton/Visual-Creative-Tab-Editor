package com.kltyton.visual_creative_tab_editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared identity and logging constants for the mod. */
public final class VisualCreativeTabEditorConstants {
    /** The mod namespace used by both loaders. */
    public static final String MOD_ID = "visual_creative_tab_editor";

    /** Previous namespace accepted only when reading worlds created before the project rename. */
    public static final String LEGACY_MOD_ID = "one_enough_creative_tab";

    /** The human-readable mod name. */
    public static final String MOD_NAME = "Visual Creative Tab Editor";

    /** The shared mod logger. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private VisualCreativeTabEditorConstants() {
    }
}
