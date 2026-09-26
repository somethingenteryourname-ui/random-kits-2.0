package com.customabilities;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CooldownManager {

    /** For each player and ability: {startedAtMillis, readyAtMillis}. */
    private final Map<UUID, Map<AbilityType, long[]>> cooldowns = new HashMap<>();

    /** True if the ability can be used. Otherwise shows the time left on the action bar. */
    public boolean isReady(Player player, AbilityType type) {
        Map<AbilityType, long[]> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return true;
        }
        long[] entry = playerCooldowns.get(type);
        long now = System.currentTimeMillis();
        if (entry == null || now >= entry[1]) {
            return true;
        }
        // Minecraft sometimes fires two right-click events for one click; stay quiet for those.
        if (now - entry[0] > 150) {
            double secondsLeft = (entry[1] - now) / 1000.0;
            Messages.actionBar(player, ChatColor.RED + type.displayName() + " recharging: "
                    + ChatColor.WHITE + String.format(Locale.US, "%.1fs", secondsLeft));
        }
        return false;
    }

    public void start(Player player, AbilityType type, double seconds) {
        long now = System.currentTimeMillis();
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(AbilityType.class))
                .put(type, new long[]{now, now + (long) (seconds * 1000)});
    }
}
