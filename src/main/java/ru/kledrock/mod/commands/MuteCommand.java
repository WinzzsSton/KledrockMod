package ru.kledrock.mod.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.kledrock.mod.KledrockMod;
import ru.kledrock.mod.utils.MessageUtil;

import java.util.Arrays;

public class MuteCommand implements CommandExecutor {
    private final KledrockMod plugin;

    public MuteCommand(KledrockMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kledrockmod.mute")) {
            sender.sendMessage("§cУ вас нет прав.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cИспользование: /mute <ник> [время] [причина]");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        long duration = 0;
        String durationStr = "НАВСЕГДА";
        String reason = "Не указана";

        if (args.length >= 2) {
            String timeStr = args[1].toLowerCase();
            try {
                if (timeStr.endsWith("h")) {
                    duration = Long.parseLong(timeStr.replace("h", "")) * 3600000L;
                    durationStr = timeStr.toUpperCase();
                } else if (timeStr.endsWith("m")) {
                    duration = Long.parseLong(timeStr.replace("m", "")) * 60000L;
                    durationStr = timeStr.toUpperCase();
                }
            } catch (NumberFormatException ignored) {}
        }
        
        if (args.length >= 3) {
            reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        }

        plugin.getDatabaseManager().addPunishment(targetName, "MUTE", reason, sender.getName(), duration);

        if (target != null) target.sendMessage("§cВы были замучены. Причина: " + reason);
        
        MessageUtil.broadcastModeration("ЗАМУТИЛ", targetName, durationStr, reason, sender.getName(), Sound.BLOCK_ANVIL_LAND);
        return true;
    }
}