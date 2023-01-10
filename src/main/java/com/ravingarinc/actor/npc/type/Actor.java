package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.util.Vector3;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
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

    /**
     * The entity id which is dependent on the spawn cycle of the actor.
     */
    protected final int id;

    protected final ProtocolManager manager;
    protected final Map<UUID, Player> viewers;
    protected Vector3 spawnLocation;
    protected Location bukkitLocation;

    public Actor(final UUID uuid, final T entity, final Location spawnLocation, final ProtocolManager manager) {
        this.uuid = uuid;
        this.entity = entity;
        this.id = entity.getEntityId();
        this.manager = manager;
        this.spawnLocation = new Vector3(spawnLocation);
        this.bukkitLocation = spawnLocation;
        this.viewers = new ConcurrentHashMap<>();
    }

    public Location getBukkitLocation() {
        return bukkitLocation;
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

    public abstract PacketContainer getPreSpawnPacket();

    public abstract PacketContainer getSpawnPacket(Vector3 location);

    public abstract PacketContainer getHidePacket();

    public void updateName(final String displayName) {
        this.entity.setCustomName(displayName);
        this.entity.setCustomNameVisible(true);
    }

    public PacketContainer getRemovePacket() {
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
