package ru.kledrock.mod.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.kledrock.mod.KledrockMod;
import ru.kledrock.mod.utils.MessageUtil;

public class UnmuteCommand implements CommandExecutor {
    private final KledrockMod plugin;

    public UnmuteCommand(KledrockMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kledrockmod.mute")) {
            sender.sendMessage("§cУ вас нет прав.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cИспользование: /unmute <ник>");
            return true;
        }

        plugin.getDatabaseManager().removePunishment(args[0], "MUTE");
        MessageUtil.broadcastPardon("РАЗМУТИЛ", args[0], sender.getName());
        return true;
    }
}