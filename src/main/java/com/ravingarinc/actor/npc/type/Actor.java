package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.ActorFactory;
import com.ravingarinc.actor.npc.ActorManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Actor<T extends Entity> {

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
    protected Vector3 spawnLocation;

    public Actor(final ActorFactory.Type<?> type, final UUID uuid, final T entity, final Vector3 spawnLocation) {
        this.uuid = uuid;
        this.type = type;
        this.entity = entity;
        this.id = entity.getEntityId();
        this.spawnLocation = spawnLocation;
        this.viewers = new ConcurrentHashMap<>();
        this.appliedArguments = new HashMap<>();
    }

    public ActorFactory.Type<?> getType() {
        return type;
    }

    public void applyArgument(final Argument argument) {
        final String arg = argument.consume(this);
        if (arg != null) {
            final String prefix = argument.getPrefix();
            appliedArguments.put(prefix, prefix + " " + arg);
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
        return entity.getName();
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
    public abstract void spawn(ActorManager actorManager, Vector3 location, Player viewer);

    /**
     * Spawn an actor for the first time
     */
    public abstract void create(ActorManager actorManager);

    public void updateName(final String displayName) {
        this.entity.setCustomName(displayName);
        this.entity.setCustomNameVisible(true);
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

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
