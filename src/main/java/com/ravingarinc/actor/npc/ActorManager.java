package com.ravingarinc.actor.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.BiMap;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.selector.SelectorManager;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.skin.SkinClient;
import com.ravingarinc.actor.storage.sql.ActorDatabase;
import com.ravingarinc.api.I;
import com.ravingarinc.api.Sync;
import com.ravingarinc.api.Vector3;
import com.ravingarinc.api.concurrent.BlockingRunner;
import com.ravingarinc.api.concurrent.DelayedFutureTask;
import com.ravingarinc.api.module.Module;
import com.ravingarinc.api.module.ModuleLoadException;
import com.ravingarinc.api.module.RavinPlugin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Async;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages all currently loaded actors and is responsible for creating new ones. Any tasks, especially packet sending
 * should be done through this manager through the method {@link #queue(Runnable)} as to prevent synchronisation issues
 */
public class ActorManager extends Module {

    private final BiMap<Integer, UUID, Actor<?>> cachedActors;
    private BlockingRunner<FutureTask<Void>> runner;
    private BlockingRunner<DelayedFutureTask<Void>> delayedRunner;
    private SkinClient client;
    private ProtocolManager manager;

    private ActorDatabase database;

    private PathingManager pathingManager;

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
        pathingManager = plugin.getModule(PathingManager.class);

        runner = new BlockingRunner<>(new LinkedBlockingQueue<>());
        runner.runTaskAsynchronously(plugin);

        delayedRunner = new BlockingRunner<>(new DelayQueue<>());
        delayedRunner.runTaskAsynchronously(plugin);
    }

    @Override
    public void cancel() {
        delayedRunner.blockUntilCancelled((runnable) -> new DelayedFutureTask<>(runnable, null, 0));
        runner.blockUntilCancelled((runnable) -> new FutureTask<>(runnable, null));

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
            // todo load pathes into pathing manager
            saveLater(actor);
        });
    }

    public void deleteActor(final int id) {
        final Actor<?> actor = getActor(id);
        if (actor == null) {
            return;
        }
        deleteActor(actor);
    }

    public void deleteActor(final UUID uuid) {
        final Actor<?> actor = getActor(uuid);
        if (actor == null) {
            return;
        }
        deleteActor(actor);
    }

    /**
     * Deletes an actor
     *
     * @param actor The actor to delete
     */
    public void deleteActor(final Actor<?> actor) {
        plugin.getModule(SelectorManager.class).removeActorSelection(actor);
        plugin.getModule(PathingManager.class).removeAgent(actor);
        if (actor instanceof PlayerActor playerActor) {
            client.unlinkActorAll(playerActor);
        }
        database.deleteActor(actor);
        cachedActors.removeBoth(actor.getId(), actor.getUUID());
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
        return cachedActors.getFirst(id);
    }

    @SuppressWarnings("all")
    public Actor<?> getActor(UUID uuid) {
        return cachedActors.getSecond(uuid);
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
     * Queues a runnable on an asynchronous runner and returns a future representing the completion state of the
     * runnable
     *
     * @param runnable The runnable
     * @return The future
     */
    public FutureTask<Void> queue(final Runnable runnable) {
        final FutureTask<Void> future = new FutureTask<>(runnable, null);
        this.runner.queue(future);
        return future;
    }

    /**
     * Queue a runnable to be added later the asynchronous runner.
     *
     * @param runnable The runnable
     * @param delay    The delay in ticks
     * @return The future task
     */
    public FutureTask<Void> queueLater(final Runnable runnable, final long delay) {
        final FutureTask<Void> future = new FutureTask<>(runnable, null);
        delayedRunner.queue(new DelayedFutureTask<>(() -> this.runner.queue(future), null, delay * 1000 / 20));
        return future;
    }
}
