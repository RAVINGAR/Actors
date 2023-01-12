package com.ravingarinc.actor.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class CommandOption {
    private final Map<String, CommandOption> options;
    private final int requiredArgs;
    private final CommandOption parent;
    private final Map<String, Argument> argumentTypes;
    private BiFunction<CommandSender, String[], Boolean> function;
    private BiFunction<CommandSender, String[], List<String>> tabCompletions;

    public CommandOption(final CommandOption parent, final int requiredArgs, final BiFunction<CommandSender, String[], Boolean> function) {
        this.parent = parent;
        this.function = function;
        this.options = new LinkedHashMap<>();
        this.tabCompletions = null;
        this.requiredArgs = requiredArgs;
        this.argumentTypes = new LinkedHashMap<>();
    }

    public void setFunction(final BiFunction<CommandSender, String[], Boolean> function) {
        this.function = function;
    }

    /**
     * Adds an option for this command. An option can have suboptions since this method returns the command option
     * created.
     *
     * @param key          The key of the option
     * @param requiredArgs args.length must be greater than or equal to this number to search for sub options in this option.
     * @param function     The function to execute
     * @return The new command option
     */
    public CommandOption addOption(final String key, final int requiredArgs, final BiFunction<CommandSender, String[], Boolean> function) {
        final CommandOption option = new CommandOption(this, requiredArgs, function);
        this.options.put(key, option);
        return option;
    }

    public CommandOption addOption(final String key, final CommandOption option) {
        this.options.put(key, option);
        return option;
    }

    public CommandOption getParent() {
        return parent;
    }

    /**
     * If options is empty then provide these tab completions
     *
     * @param tabCompletions The function to use for tab completions
     */
    public CommandOption buildTabCompletions(final BiFunction<CommandSender, String[], List<String>> tabCompletions) {
        this.tabCompletions = tabCompletions;
        return this;
    }


    /**
     * Executes a command option. If this command option has children it will search through for a specified key
     * and if one is found it will search through that option. The function will be accepted if no option is available
     *
     * @param player The player using the command
     * @param args   The args
     * @return true if command was run successfully, or false if not
     */
    public boolean execute(@NotNull final CommandSender player, final String[] args, final int index) {
        if (args.length >= requiredArgs) {
            final CommandOption option = args.length == index ? null : options.get(args[index].toLowerCase());
            if (option == null) {
                return function.apply(player, args);
            } else {
                return option.execute(player, args, index + 1);
            }
        }
        return false;
    }

    @Nullable
    public List<String> getTabCompletion(@NotNull final CommandSender sender, @NotNull final String[] args, final int index) {
        if (tabCompletions == null) {
            if (args.length == index + 1) {
                return options.isEmpty()
                        ? null
                        : options.keySet().stream().toList();
            } else {
                final CommandOption option = options.get(args[index]);
                if (option != null) {
                    return option.getTabCompletion(sender, args, index + 1);
                }
            }
        } else {
            return tabCompletions.apply(sender, args);
        }

        return null;
    }
}