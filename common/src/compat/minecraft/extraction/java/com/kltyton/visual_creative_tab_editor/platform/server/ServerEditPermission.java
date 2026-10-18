package com.kltyton.visual_creative_tab_editor.platform.server;

import net.minecraft.server.level.ServerPlayer;

/** Native gamemaster permission used for edit requests and permission snapshots. */
public final class ServerEditPermission {
    private ServerEditPermission() {}
    public static boolean canEdit(ServerPlayer player) {
        return player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }
}
