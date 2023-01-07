package com.ravingarinc.actor.npc;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.type.Actor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Async;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class ActorManager extends Module {
    private final static String ACTOR_META = "actor-meta";

    private final Map<Integer, Actor<?>> cachedActors;
    private ActorFactory factory;
    private ActorTaskRunner runner;
    private ProtocolManager manager;

    /**
     * The constructor for a Module, should only ever be called by {@link Module#initialise(RavinPlugin, Class)}.
     * Implementations of Managers should have one public constructor with a EldenRhym object parameter.
     * The implementing constructor CANNOT call {@link RavinPlugin#getModule(Class)} otherwise potential issues
     * may occur. This must be done in {@link this#load()}.
     *
     * @param plugin The owning plugin
     */
    public ActorManager(final RavinPlugin plugin) {
        super(ActorManager.class, plugin);
        this.cachedActors = new ConcurrentHashMap<>();
    }

    @Override
    protected void load() throws ModuleLoadException {
        manager = ProtocolLibrary.getProtocolManager();
        factory = new ActorFactory(plugin, manager);
        runner = new ActorTaskRunner();
        runner.runTaskTimerAsynchronously(plugin, 0, 1);
    }

    public void createActor(final EntityType type, final Location location, final Argument[] args) {
        createActor(transformToVersion(UUID.randomUUID(), 2), type, location, args);
    }

    public void createActor(final UUID uuid, final EntityType type, final Location location, final Argument[] args) {
        factory.buildActor(uuid, type, location, args).ifPresent(this::spawnActor);
    }

    /**
     * Load existing actor from the database.
     *
     * @param uuid
     */
    public void loadActor(final UUID uuid) {

        // todo load from SQL then do something liek such with the following params
        //   uuid, type, location and args must be loaded from the SQL database
        //  createActor(uuid, type, location, args)
    }

    @Async.Schedule
    public void spawnActor(final Actor<?> actor) {
        final List<Player> players = actor.getBukkitLocation().getWorld().getPlayers();
        queueTask(() -> {
            // It is fine to hide from all players initially since this is only done when spawning for the first time.
            final List<PacketContainer> packets = actor.getShowPackets(actor.getSpawnLocation());
            packets.add(0, actor.getHidePacket()); // Insert first so it gets sent first
            packets.forEach(packet -> {
                for (final Player player : players) {
                    try {
                        manager.sendServerPacket(player, packet);
                    } catch (final InvocationTargetException e) {
                        I.log(Level.WARNING, "Encountered issue sending server packet to player!");
                    }
                }
            });
            // This is done AFTER the packets have been sent such that Actor Packet Interceptor does not spawn the entity
            // twice!
            cachedActors.put(actor.getId(), actor);
        });
    }

    /**
     * Show a given actor to a specific location at the given location. This queues an asynchronous execution.
     *
     * @param actor    The actor
     * @param player   The player
     * @param location The location
     */
    @Async.Schedule
    public void showActor(final Actor<?> actor, final Player player, final Vector3 location) {
        queueTask(() -> actor.getShowPackets(location).forEach(packet -> {
            try {
                manager.sendServerPacket(player, packet);
            } catch (final InvocationTargetException e) {
                I.log(Level.WARNING, "Encountered issue sending server packet to player!", e);
            }
        }));
    }

    /**
     * Hides an actor from a specific player. This queues an asynchronous execution
     *
     * @param actor  The actor
     * @param player The player
     */
    public void hideActor(final Actor<?> actor, final Player player) {
        queueTask(() -> {
            try {
                manager.sendServerPacket(player, actor.getHidePacket());
            } catch (final InvocationTargetException e) {
                I.log(Level.SEVERE, "Encountered issue sending server packet to player!", e);
            }
        });
    }

    private UUID transformToVersion(final UUID uuid, final int version) {
        final String string = uuid.toString();
        final String builder = string.substring(0, 14) + version + string.substring(15);
        return UUID.fromString(builder);
    }

    public Actor<?> getActor(final int id) {
        return cachedActors.get(id);
    }

    /**
     * Queue an actor related task on an asynchronous runnable. This will be run as async.
     *
     * @param runnable The runnable
     */
    @Async.Schedule
    private void queueTask(final Runnable runnable) {
        AsyncHandler.runAsynchronously(() -> this.runner.tasks.add(runnable));
    }

    @Override
    public void cancel() {
        cachedActors.values().forEach(actor -> {
            actor.getEntity().remove();
        });
        runner.cancel();
    }

    private static class ActorTaskRunner extends BukkitRunnable {
        private final Queue<Runnable> tasks;

        public ActorTaskRunner() {
            tasks = new ConcurrentLinkedQueue<>();
        }

        @Override
        public void run() {
            while (!tasks.isEmpty()) {
                tasks.poll().run();
            }
        }
    }
}
