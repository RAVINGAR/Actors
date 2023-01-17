package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.ActorFactory;
import com.ravingarinc.actor.npc.ActorManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LivingActor extends Actor<LivingEntity> {
    protected String name;

    protected boolean isInvuln;

    public LivingActor(final ActorFactory.Type<?> type, final UUID uuid, final LivingEntity entity, final Vector3 spawnLocation) {
        super(type, uuid, entity, spawnLocation);
        this.name = "Actor";
        this.isInvuln = true;
    }

    @Override
    public void updateName(@NotNull final String displayName) {
        this.name = displayName;
        // todo hologram thingies
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PacketContainer getSpawnPacket(final Vector3 location, final ActorManager manager) {
        return null;
    }

    @Override
    public void update(final ActorManager actorManager) {

    }

    @Override
    public void spawn(final ActorManager actorManager, final Vector3 location, final Player viewer) {

    }

    @Override
    public void create(final ActorManager actorManager) {

    }
}
