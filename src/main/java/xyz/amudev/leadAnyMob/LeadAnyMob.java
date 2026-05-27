package xyz.amudev.leadAnyMob;

import org.bukkit.plugin.java.JavaPlugin;

public final class LeadAnyMob extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new EventHandler(this), this);

        getComponentLogger().info("LeadAnyMob has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        getComponentLogger().info("LeadAnyMob has been disabled.");
    }
}