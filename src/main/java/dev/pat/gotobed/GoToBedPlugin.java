package dev.pat.gotobed;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GoToBedPlugin extends JavaPlugin {

    private GoToBedConfig goToBedConfig;
    private GoToBedService service;
    private ParentSkins skins;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        var resource = getResource("config.yml");
        if (resource == null) {
            getLogger().severe("config.yml fehlt in der JAR");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        getConfig().setDefaults(defaults);
        getConfig().options().copyDefaults(true);
        saveConfig();

        try {
            skins = ParentSkins.load(this);
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Eltern-Skins konnten nicht geladen werden", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        goToBedConfig = new GoToBedConfig(this);
        service = new GoToBedService(this, goToBedConfig, new AssignmentStore(this));
        service.restore();
        getServer().getPluginManager().registerEvents(service, this);
        new GoToBedCommand(this, goToBedConfig, service).register();
        getLogger().info("GoToBed aktiv.");
    }

    ParentSkins skins() {
        return skins;
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.shutdown();
        }
    }
}
