package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.util.Vector3;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Actor<T extends Entity> {

    protected final UUID uuid;
    protected final T entity;
    protected final int id;

    protected final ProtocolManager manager;

    protected Vector3 spawnLocation;
    protected Location bukkitLocation;


    public Actor(final UUID uuid, final T entity, final Location spawnLocation, final ProtocolManager manager) {
        this.uuid = uuid;
        this.entity = entity;
        this.id = entity.getEntityId();
        this.manager = manager;
        this.spawnLocation = new Vector3(spawnLocation);
        this.bukkitLocation = spawnLocation;
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

    /**
     * Creates a list of packets required to show an actor to a player. This differs based on the entity hence
     * why there may be different packets
     */
    public abstract List<PacketContainer> getShowPackets(Vector3 location);

    public abstract void update(Player player);

    public void updateName(final String displayName) {
        this.entity.setCustomName(displayName);
        this.entity.setCustomNameVisible(true);
    }

    public PacketContainer getHidePacket() {
        final PacketContainer container = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        final List<Integer> list = new ArrayList<>();
        list.add(id);
        container.getIntLists().write(0, list);
        return container;
    }
}
