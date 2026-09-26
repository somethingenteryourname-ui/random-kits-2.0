package com.customabilities;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum AbilityType {

    WEB_LAUNCHER("web-launcher", "Web Launcher", Material.WHITE_DYE, ChatColor.WHITE, 5,
            "Right-click to shoot a giant cobweb.",
            "Traps whatever it hits in a 3x3x2 web cage."),

    ENERGY_COOKIE("energy-cookie", "Energy Cookie", Material.COOKIE, ChatColor.GOLD, 5,
            "Right-click for Strength II.",
            "Stays active through a totem pop."),

    SWITCHER_BALL("switcher-ball", "Switcher Ball", Material.SNOWBALL, ChatColor.AQUA, 6,
            "Throw it at a player to swap places."),

    LANDMINE("landmine", "Landmine", Material.LIGHT_WEIGHTED_PRESSURE_PLATE, ChatColor.RED, 3,
            "Place it and wait for someone to step on it.",
            "Blows a 6x6 hole in the ground."),

    EVADE("evade", "Evade", Material.FEATHER, ChatColor.GREEN, 4,
            "Right-click to slip behind the nearest player.");

    private final String id;
    private final String displayName;
    private final Material material;
    private final ChatColor color;
    private final double defaultCooldown;
    private final String[] lore;

    AbilityType(String id, String displayName, Material material, ChatColor color,
                double defaultCooldown, String... lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.color = color;
        this.defaultCooldown = defaultCooldown;
        this.lore = lore;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Material material() { return material; }
    public ChatColor color() { return color; }
    public double defaultCooldown() { return defaultCooldown; }
    public String[] lore() { return lore; }

    public static AbilityType fromId(String id) {
        for (AbilityType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
