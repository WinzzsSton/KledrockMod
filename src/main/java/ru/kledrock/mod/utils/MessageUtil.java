package ru.kledrock.mod.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MessageUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void broadcastModeration(String action, String target, String duration, String reason, String moderator, Sound sound) {
        String format = "<bold><gradient:#ff4b4b:#ff9a44>[Модерация]</gradient></bold> " +
                "<white><moderator></white> <red>" + action + "</red> " +
                "<white><target></white>" +
                (duration != null && !duration.isEmpty() ? " <yellow>НА <duration></yellow>" : "") +
                " <gray>ПО ПРИЧИНЕ</gray> <white><reason></white>";

        Component message = MM.deserialize(format,
                Placeholder.unparsed("moderator", moderator),
                Placeholder.unparsed("target", target),
                Placeholder.unparsed("duration", duration == null ? "" : duration),
                Placeholder.unparsed("reason", reason)
        );

        Bukkit.getServer().sendMessage(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 0.2f, 1.0f);
        }
    }

    public static void broadcastSimple(String action, String target, String reason, String moderator, Sound sound) {
        broadcastModeration(action, target, null, reason, moderator, sound);
    }

    public static void broadcastPardon(String action, String target, String moderator) {
        String format = "<bold><gradient:#55ff55:#00aa00>[Модерация]</gradient></bold> " +
                "<white><moderator></white> <green>" + action + "</green> " +
                "<white><target></white>";

        Component message = MM.deserialize(format,
                Placeholder.unparsed("moderator", moderator),
                Placeholder.unparsed("target", target)
        );

        Bukkit.getServer().sendMessage(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.2f, 1.0f);
        }
    }
}