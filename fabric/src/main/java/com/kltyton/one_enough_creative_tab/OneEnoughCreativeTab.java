package com.kltyton.one_enough_creative_tab;

import net.fabricmc.api.ModInitializer;

/** Fabric loader entrypoint. */
public final class OneEnoughCreativeTab implements ModInitializer {
    /** Creates the Fabric mod entrypoint. */
    public OneEnoughCreativeTab() {
    }

    @Override
    public void onInitialize() {
        OneEnoughCreativeTabCommon.initialize();
    }
}
