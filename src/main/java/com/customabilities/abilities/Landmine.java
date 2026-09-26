package com.customabilities.abilities;

import com.customabilities.AbilityType;
import com.customabilities.BlockKey;
import com.customabilities.CustomAbilities;
import com.customabilities.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Landmine: a gold pressure plate that digs a 6x6x1 hole when a player steps on it,
 * when it gets broken, or when the block under it gets broken. Mines are saved to
 * landmines.yml so they survive restarts.
 */
public final class Landmine implements Listener {

    private final CustomAbilities plugin;
    private final Set<String> mines = new HashSet<>();
    private final File file;

    public Landmine(CustomAbilities plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "landmines.yml");
        if (file.exists()) {
            mines.addAll(YamlConfiguration.loadConfiguration(file).getStringList("mines"));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("mines", new ArrayList<>(mines));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save landmines.yml: " + e.getMessage());
        }
    }

    private boolean isMine(Block block) {
        return mines.contains(BlockKey.of(block));
    }

    private boolean disarm(Block block) {
        boolean removed = mines.remove(BlockKey.of(block));
        if (removed) save();
        return removed;
    }

    // ---- Placing ----

    /** Blocks placing another landmine while the cooldown is running. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceCheckCooldown(BlockPlaceEvent event) {
        if (plugin.items().getType(event.getItemInHand()) != AbilityType.LANDMINE) return;
        if (!plugin.cooldowns().isReady(event.getPlayer(), AbilityType.LANDMINE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.items().getType(event.getItemInHand()) != AbilityType.LANDMINE) return;
        mines.add(BlockKey.of(event.getBlockPlaced()));
        save();
        plugin.startCooldown(event.getPlayer(), AbilityType.LANDMINE);
        Messages.actionBar(event.getPlayer(), ChatColor.RED + "Landmine armed.");
        event.getPlayer().playSound(event.getBlockPlaced().getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 0.6f);
    }

    // ---- Triggers ----

    /** A player steps on the plate. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onStep(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null || !isMine(block)) return;
        if (block.getType() != Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            disarm(block); // stale entry, the plate isn't there anymore
            return;
        }
        event.setCancelled(true);
        detonate(block);
    }

    /** The mine itself, or the block holding it up, gets broken. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isMine(block)) {
            disarm(block);
            event.setDropItems(false);
            Location location = block.getLocation();
            Bukkit.getScheduler().runTask(plugin, () -> dig(location));
            return;
        }
        Block above = block.getRelative(BlockFace.UP);
        if (isMine(above)) {
            detonate(above);
        }
    }

    /** Explosions that hit the mine or the block under it set it off too. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        if (mines.isEmpty()) return;
        Set<Block> triggered = new LinkedHashSet<>();
        for (Block block : blocks) {
            if (isMine(block)) triggered.add(block);
            Block above = block.getRelative(BlockFace.UP);
            if (isMine(above)) triggered.add(above);
        }
        if (triggered.isEmpty()) return;
        blocks.removeAll(triggered);
        for (Block mine : triggered) {
            detonate(mine);
        }
    }

    // ---- Boom ----

    private void detonate(Block mine) {
        if (!disarm(mine)) return;
        if (mine.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            mine.setType(Material.AIR, false);
        }
        Location location = mine.getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> dig(location));
    }

    /** Removes a 6x6 area, 1 block deep, directly under the mine. */
    private void dig(Location mineLocation) {
        World world = mineLocation.getWorld();
        if (world == null) return;

        int size = Math.max(1, plugin.getConfig().getInt("landmine.hole-size", 6));
        int depth = Math.max(1, plugin.getConfig().getInt("landmine.hole-depth", 1));
        int min = -((size - 1) / 2); // for 6: -2 .. +3
        int max = min + size - 1;
        int baseX = mineLocation.getBlockX();
        int baseY = mineLocation.getBlockY();
        int baseZ = mineLocation.getBlockZ();

        List<Block> chained = new ArrayList<>();
        for (int dy = 1; dy <= depth; dy++) {
            int y = baseY - dy;
            if (y < world.getMinHeight()) break;
            for (int dx = min; dx <= max; dx++) {
                for (int dz = min; dz <= max; dz++) {
                    Block block = world.getBlockAt(baseX + dx, y, baseZ + dz);
                    Block above = block.getRelative(BlockFace.UP);
                    if (isMine(above)) {
                        // Another landmine loses its ground: set it off too (chain reaction).
                        above.setType(Material.AIR, false);
                        chained.add(above);
                    }
                    if (canDestroy(block)) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        Location center = mineLocation.clone().add(0.5, 0, 0.5);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.LARGE_SMOKE, center, 40, size / 3.0, 0.5, size / 3.0, 0.02);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);

        double damage = plugin.getConfig().getDouble("landmine.damage", 0);
        if (damage > 0) {
            for (Entity entity : world.getNearbyEntities(center, size / 2.0, 2, size / 2.0)) {
                if (entity instanceof LivingEntity living) {
                    living.damage(damage);
                }
            }
        }

        for (Block other : chained) {
            if (!disarm(other)) continue;
            Location otherLocation = other.getLocation();
            Bukkit.getScheduler().runTaskLater(plugin, () -> dig(otherLocation), 3L);
        }
    }

    /** Skips air, unbreakable blocks (bedrock, barriers, portals) and containers (so chests aren't deleted). */
    private boolean canDestroy(Block block) {
        Material type = block.getType();
        if (type.isAir()) return false;
        if (type.getHardness() < 0) return false;
        return !(block.getState() instanceof Container);
    }
}
