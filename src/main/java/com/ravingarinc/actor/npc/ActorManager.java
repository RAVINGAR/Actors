package com.ravingarinc.actor.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.BiMap;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.BlockingRunner;
import com.ravingarinc.actor.api.async.DelayedFutureTask;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import com.ravingarinc.actor.skin.ActorSkin;
import com.ravingarinc.actor.skin.SkinClient;
import com.ravingarinc.actor.storage.sql.ActorDatabase;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Async;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages all currently loaded actors and is responsible for creating new ones. Any tasks, especially packet sending
 * should be done through this manager through the method {@link #queue(Runnable)} as to prevent synchronisation issues
 */
public class ActorManager extends Module {

    private final BiMap<Integer, UUID, Actor<?>> cachedActors;
    private BlockingRunner<FutureTask<?>> runner;
    private BlockingRunner<DelayedFutureTask> delayedRunner;
    private SkinClient client;
    private ProtocolManager manager;

    private ActorDatabase database;

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
        this.cachedActors = new BiMap<>(Integer.class, UUID.class);
    }

    public static UUID transformToVersion(final UUID uuid, final int version) {
        final String string = uuid.toString();
        final String builder = string.substring(0, 14) + version + string.substring(15);
        return UUID.fromString(builder);
    }


    @Override
    protected void load() throws ModuleLoadException {
        client = plugin.getModule(SkinClient.class);
        database = plugin.getModule(ActorDatabase.class);
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

        cachedActors.values().forEach(actor -> actor.getEntity().remove());
        cachedActors.clear();
    }

    @Sync.AsyncOnly
    public void createNewActor(final String type, final Vector3 location, final Argument[] args) {
        createActor(type, transformToVersion(UUID.randomUUID(), 2), location, args);
    }

    @Sync.AsyncOnly
    public void createActor(final String type, final UUID uuid, final Vector3 location, final Argument[] args) {
        ActorFactory.build(type, uuid, location, args).ifPresent(actor -> {
            actor.create(this);
            saveLater(actor);
        });
    }

    public Set<Actor<?>> filterActors(final Predicate<? super Actor<?>> filter) {
        return cachedActors.values().stream().filter(filter).collect(Collectors.toUnmodifiableSet());
    }

    public Collection<Actor<?>> getActors() {
        return Collections.unmodifiableCollection(cachedActors.values());
    }

    public void cacheActor(final int id, final UUID uuid, final Actor<?> actor) {
        cachedActors.put(id, uuid, actor);
    }

    @SuppressWarnings("all")
    public Actor<?> getActor(int id) {
        return cachedActors.get(id);
    }

    @SuppressWarnings("all")
    public Actor<?> getActor(UUID uuid) {
        return cachedActors.get(uuid);
    }

    /**
     * Called from an asynchronous context
     *
     * @param actor The actor
     */
    @Async.Execute
    public void updateActor(final Actor<?> actor) {
        actor.apply();
        actor.update(this);
        saveLater(actor);
    }

    public void saveLater(final Actor<?> actor) {
        queueLater(() -> {
            if (getActor(actor.getUUID()) != null) {
                database.saveActor(actor);
            }
        }, 40L);
    }

    public void sendPacket(final Player[] viewers, final PacketContainer packet) {
        try {
            for (final Player player : viewers) {
                manager.sendServerPacket(player, packet);
            }
        } catch (final InvocationTargetException exception) {
            I.log(Level.SEVERE, "Encountered issue sending server packet to player!", exception);
        }
    }

    public void sendPacket(final Player player, final PacketContainer packet) {
        try {
            manager.sendServerPacket(player, packet);
        } catch (final InvocationTargetException exception) {
            I.log(Level.SEVERE, "Encountered issue sending packet '" + packet.getType().toString() + "' to player!", exception);
        }
    }

    public void sendPackets(final Player[] viewers, final PacketContainer[] packets) {
        try {
            for (final PacketContainer packet : packets) {
                for (final Player player : viewers) {
                    manager.sendServerPacket(player, packet);
                }
            }
        } catch (final InvocationTargetException exception) {
            I.log(Level.SEVERE, "Encountered issue sending server packet to player!", exception);
        }
    }

    public void sendPackets(final Player receiver, final PacketContainer... packets) {
        try {
            for (final PacketContainer packet : packets) {
                manager.sendServerPacket(receiver, packet);
            }
        } catch (final InvocationTargetException exception) {
            I.log(Level.SEVERE, "Encountered issue sending server packet to player!", exception);
        }
    }

    public PacketContainer createPacket(final PacketType type) {
        return createPacket(type, true);
    }

    public PacketContainer createPacket(final PacketType type, final boolean forceDefaults) {
        return manager.createPacket(type, forceDefaults);
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
                updateActor(actor);
            } catch (ExecutionException | InterruptedException e) {
                I.log(Level.WARNING, "Encountered exception on ActorSkinChange request!", e);
            } catch (final TimeoutException e) {
                I.log(Level.WARNING, "ActorSkinChange request timed out!", e);
            }
        });
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
    public FutureTask<Object> queueLater(final Runnable runnable, final long delay) {
        return queueLater(runnable, true, delay);
    }

    public FutureTask<Object> queueLater(final Runnable runnable, final Object result, final long delay) {
        final FutureTask<Object> future = new FutureTask<>(runnable, result);
        delayedRunner.queue(new DelayedFutureTask(() -> this.runner.queue(future), result, delay * 1000 / 20));
        return future;
    }
}
