package ru.kledrock.mod.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import ru.kledrock.mod.KledrockMod;

public class PlayerListener implements Listener {
    private final KledrockMod plugin;

    public PlayerListener(KledrockMod plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (plugin.getDatabaseManager().isMuted(event.getPlayer().getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВы заглушены и не можете писать в чат.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.getDatabaseManager().isBanned(event.getPlayer().getName())) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "§cВы забанены на этом сервере.");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getDatabaseManager().addStat("deaths");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getDatabaseManager().addStat("joins");
    }
}