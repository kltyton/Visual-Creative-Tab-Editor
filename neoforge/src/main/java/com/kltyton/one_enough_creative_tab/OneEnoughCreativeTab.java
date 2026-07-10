package com.kltyton.one_enough_creative_tab;

import net.neoforged.fml.common.Mod;

/** NeoForge loader entrypoint. */
@Mod(OneEnoughCreativeTabConstants.MOD_ID)
public final class OneEnoughCreativeTab {
    /** Creates and initializes the NeoForge mod entrypoint. */
    public OneEnoughCreativeTab() {
        OneEnoughCreativeTabCommon.initialize();
    }
}
