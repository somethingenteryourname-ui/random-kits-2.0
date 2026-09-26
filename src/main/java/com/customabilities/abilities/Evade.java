package com.customabilities.abilities;

import com.customabilities.CustomAbilities;
import com.customabilities.Messages;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Evade: teleports you behind the nearest player that has enough room behind them. */
public final class Evade {

    private static final double PLAYER_HALF_WIDTH = 0.3;
    private static final double PLAYER_HEIGHT = 1.8;

    private final CustomAbilities plugin;

    public Evade(CustomAbilities plugin) {
        this.plugin = plugin;
    }

    /** Returns true only if the player actually moved (so the cooldown only starts on success). */
    public boolean use(Player player) {
        double radius = plugin.getConfig().getDouble("evade.search-radius", 10);
        double distance = plugin.getConfig().getDouble("evade.distance-behind", 1.2);
        Location origin = player.getLocation();

        List<Player> candidates = new ArrayList<>();
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || other.isDead() || other.getGameMode() == GameMode.SPECTATOR) continue;
            if (other.getLocation().distanceSquared(origin) <= radius * radius) {
                candidates.add(other);
            }
        }
        if (candidates.isEmpty()) {
            fail(player, "No players nearby to evade behind.");
            return false;
        }
        candidates.sort(Comparator.comparingDouble(other -> other.getLocation().distanceSquared(origin)));

        for (Player target : candidates) {
            Location destination = spotBehind(target, distance);
            if (destination == null) continue;

            World world = player.getWorld();
            world.spawnParticle(Particle.CLOUD, origin.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
            if (player.isInsideVehicle()) player.leaveVehicle();
            player.teleport(destination);
            player.setFallDistance(0);
            world.spawnParticle(Particle.CLOUD, destination.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
            world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);
            return true;
        }

        fail(player, "Not enough space behind anyone nearby.");
        return false;
    }

    private void fail(Player player, String message) {
        Messages.actionBar(player, ChatColor.RED + message);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
    }

    /** The spot directly behind the target, facing their back, or null if there's no room. */
    private Location spotBehind(Player target, double distance) {
        Location t = target.getLocation();
        double yaw = Math.toRadians(t.getYaw());
        // Minecraft's facing direction is (-sin(yaw), cos(yaw)); behind is the opposite.
        Location destination = t.clone().add(Math.sin(yaw) * distance, 0, -Math.cos(yaw) * distance);
        destination.setYaw(t.getYaw());
        destination.setPitch(0f);
        return hasRoom(destination) ? destination : null;
    }

    /** Checks that a player-sized hitbox at this spot doesn't collide with any blocks. */
    private boolean hasRoom(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        BoundingBox box = new BoundingBox(
                loc.getX() - PLAYER_HALF_WIDTH, loc.getY(), loc.getZ() - PLAYER_HALF_WIDTH,
                loc.getX() + PLAYER_HALF_WIDTH, loc.getY() + PLAYER_HEIGHT, loc.getZ() + PLAYER_HALF_WIDTH);
        if (box.getMinY() < world.getMinHeight() || box.getMaxY() > world.getMaxHeight()) return false;

        for (int x = floor(box.getMinX()); x <= floor(box.getMaxX()); x++) {
            for (int y = floor(box.getMinY()); y <= floor(box.getMaxY()); y++) {
                for (int z = floor(box.getMinZ()); z <= floor(box.getMaxZ()); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.LAVA || type == Material.FIRE || type == Material.SOUL_FIRE) {
                        return false; // don't evade into danger
                    }
                    for (BoundingBox part : block.getCollisionShape().getBoundingBoxes()) {
                        if (part.clone().shift(x, y, z).overlaps(box)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
