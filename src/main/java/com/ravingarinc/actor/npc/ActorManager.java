package com.ravingarinc.actor.npc;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.Thread;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.skin.ActorSkin;
import com.ravingarinc.actor.npc.skin.SkinClient;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Async;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Manages all currently loaded actors and is responsible for creating new ones. Any tasks, especially packet sending
 * should be done through this manager through the method {@link #queueAsync(Runnable)} as to prevent synchronisation issues
 */
public class ActorManager extends Module {

    private final Map<Integer, Actor<?>> cachedActors;
    private final BukkitScheduler scheduler;
    private ActorFactory factory;
    private ActorTaskRunner runner;
    private SkinClient client;
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
        this.scheduler = plugin.getServer().getScheduler();
    }

    public static UUID transformToVersion(final UUID uuid, final int version) {
        final String string = uuid.toString();
        final String builder = string.substring(0, 14) + version + string.substring(15);
        return UUID.fromString(builder);
    }

    @Override
    protected void load() throws ModuleLoadException {
        manager = ProtocolLibrary.getProtocolManager();
        client = new SkinClient(plugin);
        factory = new ActorFactory(plugin, manager);
        runner = new ActorTaskRunner();
        runner.runTaskTimerAsynchronously(plugin, 0, 1);
    }

    @Async.Schedule
    public void createActor(final EntityType type, final Location location, final Argument[] args) {
        createActor(transformToVersion(UUID.randomUUID(), 2), type, location, args);
    }

    @Async.Schedule
    public void createActor(final UUID uuid, final EntityType type, final Location location, final Argument[] args) {
        AsyncHandler.runAsynchronously(() -> factory.buildActor(uuid, type, location, args).ifPresent(this::spawnActor));
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

    @Thread.AsyncOnly
    public void spawnActor(final Actor<?> actor) {
        queue(() -> {
            I.log(Level.WARNING, "Debug -> Spawning Actor for First Time");
            final List<PacketContainer> packets = actor.getShowPackets(actor.getSpawnLocation());
            final Collection<Player> viewers = actor.getViewers();
            packets.add(0, actor.getHidePacket()); // Insert first so it gets sent first
            packets.forEach(packet -> {
                for (final Player player : viewers) {
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
    public void processActorSpawn(final Actor<?> actor, final Player player, final Vector3 location) {
        queueAsync(() -> {
            I.log(Level.WARNING, "Debug -> Listener for Spawn Actor");
            actor.addViewer(player);
            actor.getShowPackets(location).forEach(packet -> {
                try {
                    manager.sendServerPacket(player, packet);
                } catch (final InvocationTargetException e) {
                    I.log(Level.WARNING, "Encountered issue sending server packet to player!", e);
                    actor.removeViewer(player);
                }
            });
        });
    }

    @Async.Schedule
    public void processOnActorDestroy(final Player player, final List<Integer> ids) {
        for (final Integer id : ids) {
            if (id != null && cachedActors.containsKey(id)) {
                queueAsync(() -> {
                    I.log(Level.WARNING, "Debug -> Listening for Destroy Actor with id ");
                    cachedActors.get(id).removeViewer(player);
                });
            }
        }
    }

    /**
     * Called from an asynchronous context
     *
     * @param actor
     */
    @Async.Execute
    public void processActorUpdate(final Actor<?> actor) {
        queue(() -> {
            final List<PacketContainer> packets = actor.getUpdatePackets();
            final Collection<Player> viewers = actor.getViewers();
            packets.forEach(packet -> viewers.forEach(player -> {
                try {
                    manager.sendServerPacket(player, packet);
                } catch (final InvocationTargetException exception) {
                    I.log(Level.SEVERE, "Encountered issue sending server packet to player!", exception);
                }
            }));
        });
    }

    /**
     * Process a request to change an actor's skin. The actor must be a player
     *
     * @param actor The actor
     * @param skin  The skin to apply
     */
    @Thread.AsyncOnly
    public void processActorSkinChange(final PlayerActor actor, final ActorSkin skin) {
        final FutureTask<?> future = queue(() -> client.unlinkActorAll(actor));
        AsyncHandler.runAsynchronously(() -> {
            skin.linkActor(actor);
            try {
                future.get(1000, TimeUnit.MILLISECONDS);
                processActorUpdate(actor);
            } catch (ExecutionException | InterruptedException e) {
                I.log(Level.WARNING, "Encountered exception on ActorSkinChange request!", e);
            } catch (final TimeoutException e) {
                I.log(Level.WARNING, "ActorSkinChange request timed out!", e);
            }
        });
    }

    /**
     * Hides an actor from a specific player. This queues an asynchronous execution
     *
     * @param actor  The actor
     * @param player The player
     */
    public void hideActor(final Actor<?> actor, final Player player) {
        queueAsync(() -> {
            try {
                manager.sendServerPacket(player, actor.getHidePacket());
            } catch (final InvocationTargetException e) {
                I.log(Level.SEVERE, "Encountered issue sending server packet to player!", e);
            }
        });
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
    private void queueAsync(final Runnable runnable) {
        AsyncHandler.runAsynchronously(() -> queue(runnable));
    }

    /**
     * Queue an actor related task on an asynchronous runnable. This is run on the same thread that called it.
     *
     * @param runnable The runnable
     */
    @Thread.AsyncOnly
    private <T> FutureTask<T> queue(final T result, final Runnable runnable) {
        final FutureTask<T> future = new FutureTask<>(runnable, result);
        this.runner.tasks.add(future);
        return future;
    }

    /**
     * Queues a runnable on an asynchronous runner and returns a future representing the completion state of the
     * runnable
     *
     * @param runnable The runnable
     * @return The future
     */
    @Thread.AsyncOnly
    private FutureTask<?> queue(final Runnable runnable) {
        return queue(true, runnable);
    }

    @Override
    public void cancel() {
        cachedActors.values().forEach(actor -> {
            actor.getEntity().remove();
        });
        runner.cancel();
    }

    private static class ActorTaskRunner extends BukkitRunnable {
        private final Queue<FutureTask<?>> tasks;

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
