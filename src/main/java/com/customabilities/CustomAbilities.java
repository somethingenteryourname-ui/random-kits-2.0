package com.customabilities;

import com.customabilities.abilities.EnergyCookie;
import com.customabilities.abilities.Evade;
import com.customabilities.abilities.Landmine;
import com.customabilities.abilities.SwitcherBall;
import com.customabilities.abilities.WebLauncher;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomAbilities extends JavaPlugin {

    private ItemManager items;
    private CooldownManager cooldowns;
    private WebLauncher webLauncher;
    private EnergyCookie energyCookie;
    private SwitcherBall switcherBall;
    private Landmine landmine;
    private Evade evade;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        items = new ItemManager(this);
        cooldowns = new CooldownManager();
        webLauncher = new WebLauncher(this);
        energyCookie = new EnergyCookie(this);
        switcherBall = new SwitcherBall(this);
        landmine = new Landmine(this);
        evade = new Evade(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new AbilityListener(this), this);
        pm.registerEvents(webLauncher, this);
        pm.registerEvents(energyCookie, this);
        pm.registerEvents(switcherBall, this);
        pm.registerEvents(landmine, this);

        AbilitiesCommand command = new AbilitiesCommand(this);
        PluginCommand pluginCommand = getCommand("abilities");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("CustomAbilities enabled.");
    }

    @Override
    public void onDisable() {
        if (landmine != null) {
            landmine.save();
        }
    }

    /** Starts the configured cooldown for an ability (and the hotbar sweep if enabled). */
    public void startCooldown(Player player, AbilityType type) {
        double seconds = getConfig().getDouble("cooldowns." + type.id(), type.defaultCooldown());
        if (seconds <= 0) {
            return;
        }
        cooldowns.start(player, type, seconds);
        if (getConfig().getBoolean("show-cooldown-overlay", true)) {
            player.setCooldown(type.material(), (int) Math.round(seconds * 20));
        }
    }

    public ItemManager items() { return items; }
    public CooldownManager cooldowns() { return cooldowns; }
    public WebLauncher webLauncher() { return webLauncher; }
    public EnergyCookie energyCookie() { return energyCookie; }
    public SwitcherBall switcherBall() { return switcherBall; }
    public Landmine landmine() { return landmine; }
    public Evade evade() { return evade; }
}
