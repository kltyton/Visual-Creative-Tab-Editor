package com.kltyton.one_enough_creative_tab;

/** Loader-independent bootstrap for shared mod code. */
public final class OneEnoughCreativeTabCommon {
    private OneEnoughCreativeTabCommon() {
    }

    /** Initializes the shared mod bootstrap. */
    public static void initialize() {
        OneEnoughCreativeTabConstants.LOGGER.info("Initializing {}", OneEnoughCreativeTabConstants.MOD_NAME);
    }
}
