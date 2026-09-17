package dev.pat.gotobed;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

final class PlayerLookup {

    private PlayerLookup() {
    }

    static @Nullable OfflinePlayer findKnown(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (isKnown(cached)) {
            return cached;
        }
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null && player.getName().equalsIgnoreCase(name) && isKnown(player)) {
                return player;
            }
        }
        return null;
    }

    static String displayName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name == null || name.isBlank() ? fallback : name;
    }

    private static boolean isKnown(@Nullable OfflinePlayer player) {
        return player != null && (player.isOnline() || player.hasPlayedBefore());
    }
}
