package ru.kledrock.mod;

import org.bukkit.plugin.java.JavaPlugin;
import ru.kledrock.mod.api.ApiServer;
import ru.kledrock.mod.database.DatabaseManager;
import ru.kledrock.mod.commands.*;
import ru.kledrock.mod.listeners.PlayerListener;

public class KledrockMod extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ApiServer apiServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        if (getConfig().getBoolean("api.enabled")) {
            apiServer = new ApiServer(this);
            apiServer.start();
        }

        getLogger().info("KledrockMod успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (apiServer != null) {
            apiServer.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("KledrockMod выключен.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}