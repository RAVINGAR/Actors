package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.AsynchronousException;
import com.ravingarinc.actor.api.async.ConcurrentKeyedQueue;
import com.ravingarinc.actor.api.async.KeyedRunnable;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.ActorFactory;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.selector.Selectable;
import com.ravingarinc.actor.npc.selector.SelectionFailException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public abstract class Actor<T extends Entity> implements Selectable {

    /**
     * The internal UUID used during loading and saving of actors.
     */
    protected final UUID uuid;
    protected final T entity;
    protected final ActorFactory.Type<?> type;
    /**
     * The entity id which is dependent on the spawn cycle of the actor.
     */
    protected final int id;
    protected final Map<UUID, Player> viewers;
    protected final Map<String, String> appliedArguments;
    protected final ConcurrentKeyedQueue<KeyedRunnable> syncUpdates;
    protected final AtomicReference<String> name;
    protected final AtomicBoolean isInvuln;
    protected Vector3 spawnLocation;

    protected AtomicReference<Vector3> currentLocation;

    public Actor(final ActorFactory.Type<?> type, final UUID uuid, final T entity, final Vector3 spawnLocation) {
        this.syncUpdates = new ConcurrentKeyedQueue<>(ConcurrentKeyedQueue.Mode.IGNORE);
        this.name = new AtomicReference<>("Actor");
        this.uuid = uuid;
        this.type = type;
        this.entity = entity;
        this.id = entity.getEntityId();
        this.spawnLocation = spawnLocation;
        this.currentLocation = new AtomicReference<>(spawnLocation);
        this.viewers = new ConcurrentHashMap<>();
        this.appliedArguments = new HashMap<>();
        this.isInvuln = new AtomicBoolean(true);
        this.setInvuln(true);
    }

    public boolean isInvuln() {
        return this.isInvuln.getAcquire();
    }

    public void setInvuln(final boolean isInvuln) {
        this.isInvuln.setRelease(isInvuln);
        syncUpdate(Update.INVULN, () -> getEntity().setInvulnerable(isInvuln()));
    }

    public ActorFactory.Type<?> getType() {
        return type;
    }

    public Vector3 getLocation() {
        return currentLocation.getAcquire();
    }

    public void setLocation(final Vector3 location) {
        this.currentLocation.setRelease(location);
    }

    public void applyArguments(final Argument... arguments) {
        for (final Argument argument : arguments) {
            final String arg = argument.consume(this);
            if (arg != null) {
                final String prefix = argument.getPrefix();
                appliedArguments.put(prefix, prefix + " " + arg);
            }
        }
    }

    /**
     * Gets a list of the applied arguments to this actor
     * These arguments are in the form of '--arg value'
     *
     * @return A list of arguments, possibly empty
     */
    public List<String> getAppliedArguments() {
        return new ArrayList<>(appliedArguments.values());
    }

    public Vector3 getSpawnLocation() {
        return spawnLocation;
    }

    public int getId() {
        return id;
    }

    public UUID getUUID() {
        return uuid;
    }

    public T getEntity() {
        return entity;
    }

    public String getName() {
        return name.getAcquire();
    }

    public void addViewer(final Player player) {
        this.viewers.put(player.getUniqueId(), player);
    }

    public void removeViewer(final Player player) {
        this.viewers.remove(player.getUniqueId());
    }

    /**
     * Get a copy of all current viewers.
     *
     * @return Collection of viewers
     */
    public Collection<Player> getViewers() {
        return new HashSet<>(viewers.values());
    }

    public abstract PacketContainer getSpawnPacket(Vector3 location, ActorManager manager);

    /**
     * Refresh the appearance of an actor
     */
    public abstract void update(ActorManager actorManager);

    /**
     * Show an actor after it comes into view
     */
    public abstract void spawn(ActorManager actorManager, Player viewer);

    /**
     * Spawn an actor for the first time
     */
    public abstract void create(ActorManager actorManager);

    public void updateName(final String displayName) {
        this.name.setRelease(displayName);
        syncUpdate(Update.NAME, () -> {
            final Entity entity = getEntity();
            entity.setCustomName(getName());
            entity.setCustomNameVisible(true);
        });
    }

    public PacketContainer getRemovePacket(final ActorManager manager) {
        final PacketContainer container = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        final List<Integer> list = new ArrayList<>();
        list.add(id);
        container.getIntLists().write(0, list);
        return container;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Actor<?> actor = (Actor<?>) o;
        return uuid.equals(actor.uuid);
    }

    public void syncUpdate(final String key, final Runnable runnable) {
        syncUpdates.add(new KeyedRunnable(key) {
            @Override
            public void run() {
                runnable.run();
            }
        });
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * Applies any queued sync updates to this actor. This method blocks and is expected
     * to be called from an async context
     */
    @Sync.AsyncOnly
    @Blocking
    public void apply() {
        try {
            AsyncHandler.executeBlockingSyncComputation(() -> {
                while (!syncUpdates.isEmpty()) {
                    syncUpdates.poll().run();
                }
                return true;
            });
        } catch (final AsynchronousException e) {
            I.log(Level.SEVERE, "Encountered issues applying synchronised updates to actor!", e);
        }
    }

    @Override
    public void onSelect(final Player selector) throws SelectionFailException {
    }

    @Override
    public void onUnselect(final Player selector) throws SelectionFailException {
    }

    protected static class Update {
        public final static String NAME = "name_update";
        public final static String LOCATION = "location_update";
        public final static String INVULN = "invuln_update";
    }
}
