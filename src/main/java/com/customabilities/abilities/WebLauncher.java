package com.customabilities.abilities;

import com.customabilities.BlockKey;
import com.customabilities.CustomAbilities;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Web Launcher: shoots a big 3x3x2 cobweb. Anything it touches gets caged in cobwebs.
 * Cage blocks ignore explosions (TNT, crystals, beds, anchors...) but break normally with a sword.
 */
public final class WebLauncher implements Listener {

    private final CustomAbilities plugin;
    private final NamespacedKey projectileKey;
    private final Set<String> cageBlocks = new HashSet<>();

    public WebLauncher(CustomAbilities plugin) {
        this.plugin = plugin;
        this.projectileKey = new NamespacedKey(plugin, "web_projectile");
    }

    public boolean use(Player shooter) {
        double speed = plugin.getConfig().getDouble("web-launcher.projectile-speed", 1.5);
        Snowball ball = shooter.launchProjectile(Snowball.class,
                shooter.getEyeLocation().getDirection().multiply(speed));
        ball.setItem(new ItemStack(Material.COBWEB));
        ball.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);

        // A 3x3x2 cobweb model that rides along with the projectile.
        BlockDisplay display = shooter.getWorld().spawn(ball.getLocation(), BlockDisplay.class, d -> {
            d.setBlock(Material.COBWEB.createBlockData());
            d.setTransformation(new Transformation(
                    new Vector3f(-1.5f, -1f, -1.5f), new AxisAngle4f(),
                    new Vector3f(3f, 2f, 3f), new AxisAngle4f()));
            d.setPersistent(false);
        });
        ball.addPassenger(display);

        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.6f);
        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.8f, 1.5f);

        // Give the projectile a real 3x3x2 hitbox by checking around it every tick.
        new BukkitRunnable() {
            private int age = 0;

            @Override
            public void run() {
                if (!ball.isValid() || ++age > 200) {
                    display.remove();
                    if (ball.isValid()) {
                        ball.remove();
                    }
                    cancel();
                    return;
                }
                for (Entity nearby : ball.getNearbyEntities(1.5, 1.0, 1.5)) {
                    if (isValidTarget(nearby, shooter)) {
                        trap(nearby);
                        display.remove();
                        ball.remove();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.getPersistentDataContainer().has(projectileKey, PersistentDataType.BYTE)) {
            return;
        }
        for (Entity passenger : projectile.getPassengers()) {
            passenger.remove();
        }
        Player shooter = projectile.getShooter() instanceof Player p ? p : null;
        Entity hit = event.getHitEntity();
        if (hit != null && isValidTarget(hit, shooter)) {
            trap(hit);
        } else {
            projectile.getWorld().spawnParticle(Particle.BLOCK, projectile.getLocation(), 30,
                    0.6, 0.6, 0.6, Material.COBWEB.createBlockData());
        }
    }

    private boolean isValidTarget(Entity entity, Player shooter) {
        if (!(entity instanceof LivingEntity living) || living.isDead()) return false;
        if (entity.equals(shooter) || entity instanceof ArmorStand) return false;
        return !(entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR);
    }

    /** Fills the air in a 3x3 area, 2 blocks tall, around the target (from their feet up) with cobwebs. */
    private void trap(Entity target) {
        Block feet = target.getLocation().getBlock();
        List<Block> placed = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = feet.getRelative(x, y, z);
                    if (block.getType().isAir()) {
                        block.setType(Material.COBWEB);
                        cageBlocks.add(BlockKey.of(block));
                        placed.add(block);
                    }
                }
            }
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SPIDER_HURT, 1f, 0.7f);
        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 60,
                1, 1, 1, Material.COBWEB.createBlockData());

        int duration = plugin.getConfig().getInt("web-launcher.cage-duration-seconds", 5);
        if (duration > 0 && !placed.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Block block : placed) {
                    if (cageBlocks.remove(BlockKey.of(block)) && block.getType() == Material.COBWEB) {
                        block.setType(Material.AIR);
                    }
                }
            }, duration * 20L);
        }
    }

    private boolean isCage(Block block) {
        return block.getType() == Material.COBWEB && cageBlocks.contains(BlockKey.of(block));
    }

    // ---- Explosion immunity ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) { // TNT, end crystals, creepers, ...
        if (!cageBlocks.isEmpty()) {
            event.blockList().removeIf(this::isCage);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) { // beds, respawn anchors
        if (!cageBlocks.isEmpty()) {
            event.blockList().removeIf(this::isCage);
        }
    }

    // Pistons would otherwise destroy the cobwebs
    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isCage)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isCage)) {
            event.setCancelled(true);
        }
    }

    // Breaking normally (e.g. with a sword) works as usual; just stop tracking the block.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        cageBlocks.remove(BlockKey.of(event.getBlock()));
    }
}
