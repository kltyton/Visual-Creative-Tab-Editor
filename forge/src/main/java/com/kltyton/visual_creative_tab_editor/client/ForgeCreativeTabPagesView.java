package com.kltyton.visual_creative_tab_editor.client;

import java.util.List;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;

/** Forge-only view of the creative inventory's native page list. */
public interface ForgeCreativeTabPagesView {
    List<CreativeTabsScreenPage> visualCreativeTabEditor$pages();
}
