package com.customabilities;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

/** Handles right-clicking any of the ability items and sends it to the right ability. */
public final class AbilityListener implements Listener {

    private final CustomAbilities plugin;

    public AbilityListener(CustomAbilities plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        AbilityType type = plugin.items().getType(event.getItem());
        if (type == null || type == AbilityType.LANDMINE) {
            return; // landmines are just placed like a normal block
        }

        // Stop the vanilla action (placing the cobweb, eating the cookie, throwing a plain snowball...)
        event.setCancelled(true);
        Player player = event.getPlayer();

        // If the main hand already holds an ability item, ignore the off-hand event.
        if (hand == EquipmentSlot.OFF_HAND) {
            AbilityType mainHand = plugin.items().getType(player.getInventory().getItemInMainHand());
            if (mainHand != null && mainHand != AbilityType.LANDMINE) {
                return;
            }
        }

        if (plugin.cooldowns().isReady(player, type)) {
            boolean used = switch (type) {
                case WEB_LAUNCHER -> plugin.webLauncher().use(player);
                case ENERGY_COOKIE -> plugin.energyCookie().use(player);
                case SWITCHER_BALL -> plugin.switcherBall().use(player);
                case EVADE -> plugin.evade().use(player);
                default -> false;
            };
            if (used) {
                plugin.startCooldown(player, type);
            }
        }
        player.updateInventory();
    }

    /** Stops the items' vanilla uses on mobs (white dye on sheep, cookies on parrots...). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getHand() == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        AbilityType type = plugin.items().getType(item);
        if (type != null && type != AbilityType.LANDMINE) {
            event.setCancelled(true);
        }
    }
}
