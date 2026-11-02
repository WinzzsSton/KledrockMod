package ru.kledrock.mod.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.kledrock.mod.KledrockMod;
import ru.kledrock.mod.utils.MessageUtil;

import java.util.Arrays;

public class KickCommand implements CommandExecutor {
    private final KledrockMod plugin;

    public KickCommand(KledrockMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kledrockmod.kick")) {
            sender.sendMessage("§cУ вас нет прав.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cИспользование: /kick <ник> [причина]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден в сети.");
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Не указана";
        
        plugin.getDatabaseManager().addPunishment(target.getName(), "KICK", reason, sender.getName(), 0);
        target.kickPlayer("§cВы были кикнуты.\n§eПричина: " + reason);
        
        MessageUtil.broadcastSimple("КИКНУЛ", target.getName(), reason, sender.getName());
        return true;
    }
}