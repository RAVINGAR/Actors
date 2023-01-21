package com.ravingarinc.actor.pathing;

import com.ravingarinc.actor.pathing.type.Path;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PathFactory {
    public static final Type<Path> SCHEDULED_PATH = new Type<>("scheduled", Path::new);

    private static final Map<String, Type<?>> pathTypes = new LinkedHashMap<>();

    static {
        pathTypes.put(SCHEDULED_PATH.getKey(), SCHEDULED_PATH);
    }

    @Nullable
    public static Path build(final String pathKey, final PathingAgent agent) {
        final Type<?> type = pathTypes.get(pathKey.toLowerCase());
        if (type == null) {
            return null;
        }
        return type.build(agent);
    }

    public static List<String> getTypes() {
        return new ArrayList<>(pathTypes.keySet());
    }

    public static class Type<T extends Path> {
        private final Function<PathingAgent, T> function;
        private final String key;

        public Type(final String key, final Function<PathingAgent, T> function) {
            this.key = key;
            this.function = function;
        }

        public T build(final PathingAgent agent) {
            return function.apply(agent);
        }

        public String getKey() {
            return key;
        }
    }
}
