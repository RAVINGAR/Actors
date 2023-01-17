package com.ravingarinc.actor.command;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Command argument registry for global access. Behaves as a singleton utility class.
 */
public class Registry {
    public static final String ACTOR_ARGS = "actor-args";
    private static final Map<String, Map<String, Argument>> argumentTypes = new HashMap<>();

    public static Optional<Argument> getArgument(final String group, final String prefix) {
        final Map<String, Argument> map = argumentTypes.get(group);
        if (map == null) {
            throw new IllegalArgumentException("Unknown argument registry group '" + group + "'!");
        }
        return Optional.ofNullable(map.get(prefix));
    }

    public static Map<String, Argument> getArgumentTypes(final String group) {
        final Map<String, Argument> map = argumentTypes.get(group);
        if (map == null) {
            throw new IllegalArgumentException("Unknown argument registry group '" + group + "'!");
        }
        return map;
    }

    public static void registerArgument(final String group, final String prefix, final int minArgs, final @Nullable Supplier<List<String>> tabCompletions, final BiFunction<Object, String[], String> consumer) {
        final Map<String, Argument> map = argumentTypes.computeIfAbsent(group, (g) -> new HashMap<>());
        map.put(prefix, new Argument(prefix, minArgs, tabCompletions, consumer, null));
    }

    public static void registerArgument(final String group, final String prefix, final int minArgs, final BiFunction<Object, String[], String> consumer) {
        Registry.registerArgument(group, prefix, minArgs, null, consumer);
    }

    public static Argument[] parseArguments(final String group, final int index, final String[] args) throws Argument.InvalidArgumentException {
        final Map<String, Argument> map = argumentTypes.get(group);
        if (map == null) {
            throw new IllegalArgumentException("Unknown argument registry group '" + group + "'!");
        }
        final List<Argument> arguments = new ArrayList<>();
        Argument lastArg = null;
        final List<String> lastStrings = new ArrayList<>();
        for (int i = index; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (lastArg != null) {
                    arguments.add(lastArg.createArgument(lastStrings.toArray(new String[0])));
                    lastStrings.clear();
                }
                lastArg = map.get(args[i]);
            } else if (lastArg != null) {
                lastStrings.add(args[i]);
            }
            if (i + 1 == args.length && lastArg != null) {
                arguments.add(lastArg.createArgument(lastStrings.toArray(new String[0])));
                lastStrings.clear();
            }
        }
        return arguments.toArray(new Argument[0]);
    }
}
