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

public class WarnCommand implements CommandExecutor {
    private final KledrockMod plugin;

    public WarnCommand(KledrockMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kledrockmod.warn")) {
            sender.sendMessage("§cУ вас нет прав.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cИспользование: /warn <ник> [причина]");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Не указана";

        plugin.getDatabaseManager().addPunishment(targetName, "WARN", reason, sender.getName(), 0);

        if (target != null) target.sendMessage("§cВы получили предупреждение. Причина: " + reason);

        MessageUtil.broadcastSimple("ПРЕДУПРЕДИЛ", targetName, reason, sender.getName(), Sound.ENTITY_VILLAGER_NO);

        int activeWarns = plugin.getDatabaseManager().getActiveWarns(targetName);
        int threshold = plugin.getConfig().getInt("warns.ban-threshold", 3);

        if (activeWarns >= threshold) {
            long banDurationHours = plugin.getConfig().getLong("warns.ban-duration", 24);
            long banDurationMillis = banDurationHours * 3600000L;
            
            plugin.getDatabaseManager().addPunishment(targetName, "BAN", "Авто-бан: " + threshold + " предупреждения", "Система", banDurationMillis);
            
            MessageUtil.broadcastModeration("ЗАБАНИЛ", targetName, banDurationHours + "Ч", "Достигнут лимит варнов", "Система", Sound.ENTITY_ENDER_DRAGON_GROWL);
            
            if (target != null) {
                target.kickPlayer("§cВы автоматически забанены за " + threshold + " предупреждения.\n§eСрок: " + banDurationHours + "ч.");
            }
        }
        return true;
    }
}