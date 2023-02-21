package com.ravingarinc.actor.npc;

import com.ravingarinc.actor.api.AsyncFunction;
import com.ravingarinc.actor.api.AsyncHandler;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import com.ravingarinc.api.I;
import com.ravingarinc.api.Pair;
import com.ravingarinc.api.Sync;
import com.ravingarinc.api.Vector3;
import com.ravingarinc.api.concurrent.AsynchronousException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class ActorFactory {
    public static final Type<PlayerActor> PLAYER = new Type<>("player", (uuid, location, args) -> {
        final Pair<LivingEntity, List<Player>> computations = AsyncHandler.executeBlockingSyncComputation(() -> {
            final Location locale = location.toBukkitLocation();
            final World world = locale.getWorld();
            final LivingEntity entity = (LivingEntity) world.spawnEntity(locale, EntityType.WOLF, false);
            entity.setAI(false);

            return new Pair<>(entity, world.getPlayers());
        });
        final PlayerActor actor = new PlayerActor(uuid, computations.getLeft(), location);
        computations.getRight().forEach(actor::addViewer);
        actor.applyArguments(args);
        return actor;
    });

    private static final Map<String, Type<?>> ACTOR_TYPES = new LinkedHashMap<>();

    static {
        ACTOR_TYPES.put(PLAYER.getKey(), PLAYER);
    }

    public static List<String> getTypes() {
        return new ArrayList<>(ACTOR_TYPES.keySet());
    }

    protected static <T extends Actor<?>> Optional<T> build(final Type<T> type, final UUID uuid, final Vector3 location, final Argument[] arguments) {
        try {
            return Optional.ofNullable(type.build(uuid, location, arguments));
        } catch (final AsynchronousException e) {
            I.log(Level.WARNING, "Could not build actor!", e);
        }
        return Optional.empty();
    }

    /**
     * Builds an actor based on the given type. If no type exists for the provided string value, then an empty optional
     * will be returned
     *
     * @param type      The type
     * @param uuid      The uuid for this actor
     * @param location  The location to initially spawn it
     * @param arguments Any other arguments
     * @return An optional which may or may not contain the given actor.
     */
    protected static Optional<? extends Actor<?>> build(final String type, final UUID uuid, final Vector3 location, final Argument[] arguments) {
        final Type<?> actorType = ACTOR_TYPES.get(type.toLowerCase());
        if (actorType == null) {
            return Optional.empty();
        }
        return build(actorType, uuid, location, arguments);
    }

    public static class Type<T extends Actor<?>> {
        private final String key;
        private final AsyncFunction<UUID, Vector3, Argument[], T> function;


        public Type(final String key, @Sync.AsyncOnly final AsyncFunction<UUID, Vector3, Argument[], T> function) {
            this.key = key;
            this.function = function;
        }

        public String getKey() {
            return key;
        }

        @Sync.AsyncOnly
        public T build(final UUID uuid, final Vector3 location, final Argument[] args) throws AsynchronousException {
            return function.apply(uuid, location, args);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Type<?> actorType = (Type<?>) o;
            return key.equals(actorType.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}
