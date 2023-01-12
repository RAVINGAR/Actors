package com.ravingarinc.actor.npc;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.BlockingRunner;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.factory.ActorFactory;
import com.ravingarinc.actor.npc.skin.ActorSkin;
import com.ravingarinc.actor.npc.skin.SkinClient;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Manages all currently loaded actors and is responsible for creating new ones. Any tasks, especially packet sending
 * should be done through this manager through the method {@link #queueAsync(Runnable)} as to prevent synchronisation issues
 */
public class ActorManager extends Module {

    private final Map<Integer, Actor<?>> cachedActors;

    private final BukkitScheduler scheduler;
    private BlockingRunner<FutureTask<?>> runner;
    private BlockingRunner<DelayedFutureTask> delayedRunner;
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
        client = plugin.getModule(SkinClient.class);
        manager = ProtocolLibrary.getProtocolManager();

        runner = new BlockingRunner<>(new LinkedBlockingQueue<>());
        runner.runTaskAsynchronously(plugin);

        delayedRunner = new BlockingRunner<>(new DelayQueue<>());
        delayedRunner.runTaskAsynchronously(plugin);
    }

    @Override
    public void cancel() {
        delayedRunner.cancel();
        runner.cancel();

        cachedActors.values().forEach(actor -> {
            actor.getEntity().remove();
        });
    }

    @Sync.AsyncOnly
    public void createActor(final String type, final Vector3 location, final Argument[] args) {
        createActor(type, transformToVersion(UUID.randomUUID(), 2), location, args);
    }

    @Sync.AsyncOnly
    public void createActor(final String type, final UUID uuid, final Vector3 location, final Argument[] args) {
        ActorFactory.build(type, uuid, location, args).ifPresent(this::spawnActor);
    }

    /**
     * Load existing actor from the database.
     *
     * @param uuid the existing UUID
     */
    public void loadActor(final UUID uuid) {

        // todo load from SQL then do something liek such with the following params
        //   uuid, type, location and args must be loaded from the SQL database
        //  createActor(uuid, type, location, args)
    }

    @Sync.AsyncOnly
    public void spawnActor(final Actor<?> actor) {
        queue(() -> {
            final List<PacketContainer> packets = new ArrayList<>();
            packets.add(actor.getRemovePacket(manager));
            packets.add(actor.getPreSpawnPacket(manager));
            packets.add(actor.getSpawnPacket(actor.getSpawnLocation(), manager));
            final Collection<Player> viewers = actor.getViewers();
            packets.forEach(packet -> {
                for (final Player viewer : viewers) {
                    try {
                        manager.sendServerPacket(viewer, packet);
                    } catch (final InvocationTargetException e) {
                        I.log(Level.WARNING, "Encountered issue sending server packet to player!", e);
                    }
                }
            });
            // This is done AFTER the packets have been sent such that Actor Packet Interceptor does not spawn the entity
            // twice!
        });
        queueLater(() -> {
            final Collection<Player> viewers = actor.getViewers();
            final PacketContainer packet = actor.getHidePacket(manager);
            viewers.forEach(viewer -> {
                try {
                    manager.sendServerPacket(viewer, packet);
                } catch (final InvocationTargetException e) {
                    I.log(Level.WARNING, "Encountered issue sending server packet to player!", e);
                }
            });
            cachedActors.put(actor.getId(), actor);
        }, 5L);
    }

    /**
     * Show a given actor to a specific location at the given location.
     *
     * @param actor    The actor
     * @param player   The player
     * @param location The location
     */
    @Sync.AsyncOnly
    public void processActorSpawn(final Actor<?> actor, final Player player, final Vector3 location) {
        queue(() -> {
            actor.addViewer(player);
            try {
                manager.sendServerPacket(player, actor.getPreSpawnPacket(manager));
                manager.sendServerPacket(player, actor.getSpawnPacket(location, manager));
            } catch (final InvocationTargetException e) {
                I.log(Level.WARNING, "Encountered issue sending server packet to player!", e);
                actor.removeViewer(player);
            }
        });

        queueLater(() -> {
            try {
                manager.sendServerPacket(player, actor.getHidePacket(manager));
            } catch (final InvocationTargetException e) {
                I.log(Level.WARNING, "Encountered issue sending server packet to player!", e);
                actor.removeViewer(player);
            }
        }, 5L);
    }

    @Sync.AsyncOnly
    public void processOnActorDestroy(final Player player, final List<Integer> ids) {
        for (final Integer id : ids) {
            if (id != null) {
                final Actor<?> actor = cachedActors.get(id);
                if (actor != null) {
                    queue(() -> actor.removeViewer(player));
                }
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
        final Collection<Player> viewers = actor.getViewers();
        queue(() -> viewers.forEach(player -> sendPacket(player, actor.getPreSpawnPacket(manager))));
        queueLater(() -> viewers.forEach(player -> sendPacket(player, actor.getHidePacket(manager))), 5L);
    }

    private void sendPacket(final Player receiver, final PacketContainer packet) {
        try {
            manager.sendServerPacket(receiver, packet);
        } catch (final InvocationTargetException exception) {
            I.log(Level.SEVERE, "Encountered issue sending server packet to player!", exception);
        }
    }

    /**
     * Process a request to change an actor's skin. The actor must be a player
     *
     * @param actor The actor
     * @param skin  The skin to apply
     */
    @Sync.AsyncOnly
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
                manager.sendServerPacket(player, actor.getRemovePacket(manager));
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
    public void queueAsync(final Runnable runnable) {
        AsyncHandler.runAsynchronously(() -> queue(runnable));
    }

    /**
     * Queue an actor related task on an asynchronous runnable. This is run on the same thread that called it.
     *
     * @param result The expected result
     */
    @Sync.AsyncOnly
    public <T> FutureTask<T> queue(final T result, final Runnable runnable) {
        final FutureTask<T> future = new FutureTask<>(runnable, result);
        this.runner.queue(future);
        return future;
    }

    /**
     * Queues a runnable on an asynchronous runner and returns a future representing the completion state of the
     * runnable
     *
     * @param runnable The runnable
     * @return The future
     */
    @Sync.AsyncOnly
    public FutureTask<?> queue(final Runnable runnable) {
        return queue(true, runnable);
    }

    /**
     * Queue a runnable to be added later the asynchronous runner.
     *
     * @param runnable The runnable
     * @param delay    The delay in ticks
     * @return The future task
     */
    @Sync.AsyncOnly
    public FutureTask<?> queueLater(final Runnable runnable, final long delay) {
        final FutureTask<?> future = new FutureTask<>(runnable, true);
        delayedRunner.queue(new DelayedFutureTask(future, (task) -> this.runner.queue(future), delay * 1000 / 20));
        return future;
    }

    private static class DelayedFutureTask implements Delayed, Runnable {
        private final long readyTime;

        private final FutureTask<?> task;
        private final Consumer<FutureTask<?>> consumer;

        public DelayedFutureTask(final FutureTask<?> task, final Consumer<FutureTask<?>> consumer, final long delay) {
            this.task = task;
            this.consumer = consumer;
            this.readyTime = System.currentTimeMillis() + delay;
        }

        @Override
        public void run() {
            consumer.accept(task);
        }

        @Override
        public long getDelay(@NotNull final TimeUnit unit) {
            return unit.convert(this.readyTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NotNull final Delayed other) {
            if (other instanceof DelayedFutureTask that) {
                return (int) (this.readyTime - that.readyTime);
            }
            throw new IllegalArgumentException("Cannot compare DelayedEvent to generic Delayed object!");
        }
    }
}
