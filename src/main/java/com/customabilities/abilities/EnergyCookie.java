package com.customabilities.abilities;

import com.customabilities.AbilityType;
import com.customabilities.CustomAbilities;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Energy Cookie: right-click for Strength II that survives popping a totem. */
public final class EnergyCookie implements Listener {

    private record Boost(long endsAtMillis, int amplifier) {}

    private final CustomAbilities plugin;
    private final Map<UUID, Boost> boosts = new HashMap<>();

    public EnergyCookie(CustomAbilities plugin) {
        this.plugin = plugin;
    }

    public boolean use(Player player) {
        int level = Math.max(1, plugin.getConfig().getInt("energy-cookie.strength-level", 2));
        double seconds = plugin.getConfig().getDouble("energy-cookie.duration-seconds", 5);
        int ticks = Math.max(1, (int) Math.round(seconds * 20));
        int amplifier = level - 1;

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, amplifier, false, true, true));
        boosts.put(player.getUniqueId(), new Boost(System.currentTimeMillis() + ticks * 50L, amplifier));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.6f, 1.2f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.1);
        return true;
    }

    /**
     * A totem wipes all effects right after this event, so the remaining
     * cookie Strength is put back one tick later.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Boost boost = boosts.get(player.getUniqueId());
        if (boost == null) {
            return;
        }
        long remainingMillis = boost.endsAtMillis() - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            boosts.remove(player.getUniqueId());
            return;
        }
        int remainingTicks = (int) Math.ceil(remainingMillis / 50.0);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, remainingTicks,
                        boost.amplifier(), false, true, true));
            }
        });
    }

    // Safety net: the ability cookie should never actually be eaten.
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (plugin.items().getType(event.getItem()) == AbilityType.ENERGY_COOKIE) {
            event.setCancelled(true);
        }
    }
}
