package com.ravingarinc.actor.command;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class Argument {
    private final String prefix;
    private final int minArgs;
    private final String[] args;

    private final Supplier<List<String>> tabCompletions;
    private final BiConsumer<Object, String[]> consumer;

    /**
     * An argument for a command. This will transform a given type of object based on the command.
     *
     * @param prefix   The prefix in the format --
     * @param minArgs  The passed args.length must be equal to or greater than this value
     * @param consumer The consumer of the object
     * @param args     Can be null, however if not null it is expected this contains all arguments after the preceding
     *                 --arg (as specified by prefix) but up to the next --arg
     */
    public Argument(final String prefix, final int minArgs, final @Nullable Supplier<List<String>> tabCompletions, final BiConsumer<Object, String[]> consumer, final String[] args) {
        this.prefix = prefix;
        this.minArgs = minArgs;
        this.consumer = consumer;
        this.args = args;
        this.tabCompletions = tabCompletions;
    }

    /**
     * Consume the value only if args is not null and its length is equal to or exceeds minArgs.
     *
     * @param value The value
     */
    public void consume(final Object value) {
        if (args == null) {
            throw new IllegalArgumentException("Cannot consume arguments as this Argument object does not have any args!");
        }
        consumer.accept(value, args);
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
    public Argument createArgument(final String[] args) throws InvalidArgumentException {
        if (args.length < minArgs) {
            throw new InvalidArgumentException();
        }
        return new Argument(prefix, minArgs, tabCompletions, consumer, args);
    }

    public static class InvalidArgumentException extends Exception {
        public InvalidArgumentException() {
            super("Incorrect amount of arguments!");
        }
    }
}
