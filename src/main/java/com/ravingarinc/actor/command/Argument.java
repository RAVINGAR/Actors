package com.ravingarinc.actor.command;

import com.ravingarinc.actor.api.TriFunction;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class Argument {
    private final CommandSender sender;
    private final String prefix;
    private final int minArgs;
    private final String[] args;

    private final Supplier<List<String>> tabCompletions;
    private final TriFunction<CommandSender, Object, String[], String> consumer;

    /**
     * An argument for a command. This will transform a given type of object based on the command.
     *
     * @param prefix   The prefix in the format '--'
     * @param minArgs  The passed args.length must be equal to or greater than this value
     * @param consumer The consumer of the object. This may be executed async or sync.
     * @param args     Can be null, however if not null it is expected this contains all arguments after the preceding
     *                 --arg (as specified by prefix) but up to the next --arg
     */
    public Argument(final CommandSender sender, final String prefix, final int minArgs, final @Nullable Supplier<List<String>> tabCompletions, final TriFunction<CommandSender, Object, String[], String> consumer, final String[] args) {
        this.sender = sender;
        this.prefix = prefix;
        this.minArgs = minArgs;
        this.consumer = consumer;
        this.args = args;
        this.tabCompletions = tabCompletions;
    }


    /**
     * Consume the value only if args is not null and its length is equal to or exceeds minArgs.
     * This should only ever be called by the actor through {@link com.ravingarinc.actor.npc.type.Actor#applyArguments(Argument...)}
     *
     * @param value The value
     * @return The final applied argument string for storing in a database with the prefix already appended.
     */
    @Nullable
    public String consume(final Object value) {
        if (args == null) {
            throw new IllegalArgumentException("Cannot consume arguments as this Argument object does not have any args!");
        }
        return consumer.apply(sender, value, args);
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        if (args == null) {
            throw new IllegalArgumentException("Cannot get arguments as this Argument object does not have any args!");
        }
        final StringBuilder builder = new StringBuilder(prefix);
        for (final String arg : args) {
            builder.append(" ");
            builder.append(arg);
        }
        return builder.toString();
    }

    @Nullable
    public List<String> getTabCompletions() {
        if (tabCompletions == null) {
            return null;
        }
        return tabCompletions.get();
    }

    /**
     * Creates a filled argument.
     *
     * @param args It is expected this contains all arguments after the preceding
     *             --arg (as specified by prefix) but up to the next --arg
     * @return The filled argument
     */
    public Argument createArgument(final CommandSender sender, final String[] args) throws InvalidArgumentException {
        if (args.length < minArgs) {
            throw new InvalidArgumentException();
        }
        return new Argument(sender, prefix, minArgs, tabCompletions, consumer, args);
    }

    public static class InvalidArgumentException extends Exception {
        public InvalidArgumentException() {
            super("Incorrect amount of arguments!");
        }
    }
}
