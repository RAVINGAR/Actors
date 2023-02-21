package com.ravingarinc.actor.playback;

import com.ravingarinc.actor.playback.api.LivePlayback;
import com.ravingarinc.actor.playback.path.Path;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PathFactory {
    public static final Type<Path> FIXED = new Type<>("fixed", Path::new);

    private static final Map<String, Type<?>> pathTypes = new LinkedHashMap<>();

    static {
        pathTypes.put(FIXED.getKey(), FIXED);
    }

    @Nullable
    public static LivePlayback build(final String pathKey, final PathingAgent agent) {
        final Type<?> type = pathTypes.get(pathKey.toLowerCase());
        if (type == null) {
            return null;
        }
        return type.build(agent);
    }

    public static List<String> getTypes() {
        return new ArrayList<>(pathTypes.keySet());
    }

    public static class Type<T extends LivePlayback> {
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
