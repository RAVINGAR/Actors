package com.ravingarinc.actor.npc;

import com.ravingarinc.actor.api.AsyncFunction;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.AsynchronousException;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Pair;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class ActorFactory {
    public static final Type<PlayerActor> PLAYER = new Type<>("player", (uuid, location, args) -> {
        final Pair<Object[], List<Player>> computations = AsyncHandler.executeBlockingSyncComputation(() -> {
            final Location locale = location.toBukkitLocation();
            final Object[] array = new Object[2];
            final World world = locale.getWorld();
            final LivingEntity entity = (LivingEntity) world.spawnEntity(locale, EntityType.HUSK, false);
            //entity.setAI(false);
            //entity.setInvulnerable(true);
            array[1] = entity;

            return new Pair<>(array, world.getPlayers());
        });

        final Object[] array = computations.getLeft();
        final LivingEntity entity = (LivingEntity) array[1];
        final PlayerActor actor = new PlayerActor(uuid, entity, location);
        computations.getRight().forEach(actor::addViewer);
        for (final Argument arg : args) {
            actor.applyArgument(arg);
        }
        return actor;
    });

    private static final Map<String, Type<?>> actorTypes = new HashMap<>();

    static {
        actorTypes.put(PLAYER.getKey(), PLAYER);
    }

    public static List<String> getTypes() {
        return new ArrayList<>(actorTypes.keySet());
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
        final Type<?> actorType = actorTypes.get(type.toLowerCase());
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
