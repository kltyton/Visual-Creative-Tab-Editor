package com.kltyton.visual_creative_tab_editor.data;

import net.minecraft.world.item.CreativeModeTab;

import java.util.Locale;

/** The data-facing type of a creative tab. */
public enum CreativeTabType {
    CATEGORY,
    SEARCH,
    HOTBAR,
    INVENTORY;

    /** Parses the lower-case JSON name of a tab type. */
    public static CreativeTabType parse(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown creative tab type: " + name, exception);
        }
    }

    /** Returns the lower-case JSON name. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Converts this data type to Minecraft's runtime type. */
    public CreativeModeTab.Type toVanilla() {
        return CreativeModeTab.Type.valueOf(name());
    }

    /** Converts Minecraft's runtime type to its data representation. */
    public static CreativeTabType fromVanilla(CreativeModeTab.Type type) {
        return valueOf(type.name());
    }
}
