package com.customabilities;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Creates the ability items and recognises them via a hidden data tag. */
public final class ItemManager {

    private final NamespacedKey abilityKey;

    public ItemManager(CustomAbilities plugin) {
        this.abilityKey = new NamespacedKey(plugin, "ability");
    }

    public ItemStack create(AbilityType type, int amount) {
        ItemStack item = new ItemStack(type.material(), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.color() + "" + ChatColor.BOLD + type.displayName());
            List<String> lore = new ArrayList<>();
            for (String line : type.lore()) {
                lore.add(ChatColor.GRAY + line);
            }
            meta.setLore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, type.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Returns the ability this item represents, or null for normal items. */
    public AbilityType getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String id = meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        return id == null ? null : AbilityType.fromId(id);
    }
}
