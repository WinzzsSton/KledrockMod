package ru.kledrock.mod.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
}