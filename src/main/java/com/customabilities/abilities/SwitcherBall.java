package com.customabilities.abilities;

import com.customabilities.AbilityType;
import com.customabilities.CustomAbilities;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

/** Switcher Ball: a snowball that swaps places with the player it hits. Hits on anything else do nothing. */
public final class SwitcherBall implements Listener {

    private final CustomAbilities plugin;
    private final NamespacedKey projectileKey;

    public SwitcherBall(CustomAbilities plugin) {
        this.plugin = plugin;
        this.projectileKey = new NamespacedKey(plugin, "switcher_ball");
    }

    public boolean use(Player player) {
        Snowball ball = player.launchProjectile(Snowball.class);
        ball.setItem(plugin.items().create(AbilityType.SWITCHER_BALL, 1));
        ball.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.8f);
        return true;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.getPersistentDataContainer().has(projectileKey, PersistentDataType.BYTE)) {
            return;
        }
        if (!(projectile.getShooter() instanceof Player thrower)) {
            return;
        }
        if (!(event.getHitEntity() instanceof Player target) || target.equals(thrower)) {
            return; // only works on players
        }
        if (target.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // Swap on the next tick so it doesn't clash with the hit itself.
        Bukkit.getScheduler().runTask(plugin, () -> swap(thrower, target));
    }

    private void swap(Player a, Player b) {
        if (!a.isOnline() || !b.isOnline() || a.isDead() || b.isDead()) return;
        if (!a.getWorld().equals(b.getWorld())) return;

        Location aLoc = a.getLocation();
        Location bLoc = b.getLocation();

        // Swap positions; each player keeps the direction they were looking.
        Location aDest = bLoc.clone();
        aDest.setYaw(aLoc.getYaw());
        aDest.setPitch(aLoc.getPitch());
        Location bDest = aLoc.clone();
        bDest.setYaw(bLoc.getYaw());
        bDest.setPitch(bLoc.getPitch());

        if (a.isInsideVehicle()) a.leaveVehicle();
        if (b.isInsideVehicle()) b.leaveVehicle();
        a.teleport(aDest);
        b.teleport(bDest);
        a.setFallDistance(0);
        b.setFallDistance(0);

        for (Location loc : new Location[]{aDest, bDest}) {
            loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.5);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        }
    }
}
