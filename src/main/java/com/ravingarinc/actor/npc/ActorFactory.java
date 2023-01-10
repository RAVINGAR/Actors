package com.ravingarinc.actor.npc;

import com.comphenix.protocol.ProtocolManager;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.AsynchronousException;
import com.ravingarinc.actor.api.async.Thread;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Pair;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class ActorFactory {
    private final static String ACTOR_META = "actor-meta";
    private final ProtocolManager manager;
    private final RavinPlugin plugin;

    public ActorFactory(final RavinPlugin plugin, final ProtocolManager manager) {
        this.manager = manager;
        this.plugin = plugin;
    }

    public static UUID transformToVersion(final UUID uuid, final int version) {
        final String string = uuid.toString();
        final String builder = string.substring(0, 14) + version + string.substring(15);
        return UUID.fromString(builder);
    }

    @Thread.AsyncOnly
    @Blocking
    public Optional<Actor<?>> buildActor(final UUID uuid, final EntityType type, final Location location, final Argument[] args) {
        try {
            return Optional.ofNullable(switch (type) {
                case PLAYER -> buildPlayerActor(uuid, location, args);
                default -> null;
            });
        } catch (final AsynchronousException exception) {
            I.log(Level.SEVERE, "Encountered exception whilst building actor!", exception);
        }
        return Optional.empty();
    }

    @Thread.AsyncOnly
    @Blocking
    private Actor<?> buildPlayerActor(final UUID uuid, final Location location, final Argument[] args) throws AsynchronousException {
        final Pair<Object[], List<Player>> computations = AsyncHandler.executeBlockingSyncComputation(() -> {
            final Object[] array = new Object[2];
            final World world = location.getWorld();
            final LivingEntity entity = (LivingEntity) world.spawnEntity(location, EntityType.HUSK, false);
            //entity.setAI(false);
            //entity.setInvulnerable(true);
            array[0] = Bukkit.createProfileExact(entity.getUniqueId(), "Actor");
            array[1] = entity;

            return new Pair<>(array, world.getPlayers());
        });

        final Object[] array = computations.getLeft();
        final LivingEntity entity = (LivingEntity) array[1];
        final PlayerActor actor = new PlayerActor(entity.getUniqueId(), (PlayerProfile) array[0], entity, location, manager);
        computations.getRight().forEach(actor::addViewer);
        for (final Argument arg : args) {
            arg.consume(actor);
        }
        return actor;
    }
}
