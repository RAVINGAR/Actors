package com.ravingarinc.api.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class BaseCommand extends CommandOption implements CommandExecutor {

    public BaseCommand(final String identifier, final String permission) {
        super(identifier, null, permission, "", 1, (p, s) -> false);
    }

    public BaseCommand(final String identifier, final String permission, final String description, final int requiredArgs, final BiFunction<CommandSender, String[], Boolean> function) {
        super(identifier, null, permission, description, requiredArgs, function);
    }

    public void register(final JavaPlugin plugin) {
        final PluginCommand command = plugin.getCommand(identifier);
        Objects.requireNonNull(command, "Command /" + identifier + " was not registered correctly!");
        command.setExecutor(this);
        command.setTabCompleter(new CommandCompleter(this));
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        if (hasPermission(sender)) {
            return execute(sender, args, 0);
        }
        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        return true;
    }

    public static class CommandCompleter implements TabCompleter {
        private final BaseCommand command;

        public CommandCompleter(final BaseCommand command) {
            this.command = command;
        }

        @Override
        public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
            return this.command.getTabCompletion(sender, args, 0);
        }
    }
}
