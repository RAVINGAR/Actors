package com.ravingarinc.actor.playback.path;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.AsyncHandler;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.playback.PathingAgent;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.playback.PlaybackBuilder;
import com.ravingarinc.actor.playback.api.LivePlayback;
import com.ravingarinc.actor.playback.api.Movement;
import com.ravingarinc.api.Sync;
import com.ravingarinc.api.Vector3;
import org.bukkit.entity.Player;

import java.util.UUID;

import static com.ravingarinc.actor.playback.PathingManager.PACKET_VALID_META;

public class Path extends LivePlayback {

    public Path(final PathingAgent agent) {
        super(agent);
    }

    private Vector3 location() {
        return location(null);
    }

    public void addFrame(final int id, final Vector3 current, final Vector3 terminal, final Vector3 next, final float speed) {
        addFrame(new PathMovement(id, current, terminal, next, speed));
    }

    @Override
    @Sync.SyncOnly
    public void start(final PathingManager manager) {
        synchronise(manager);
        final Actor<?> actor = agent.getActor();
        manager.queue(actor.getUUID(), () -> queueNextFrame(manager, actor));
    }

    @Override
    @Sync.SyncOnly
    public void stop(final PathingManager manager) {
        synchronise(manager);
    }

    @Override
    @Sync.SyncOnly
    public void reset(final PathingManager manager) {
        current().reset();
        reset(); //synchronized
        synchronise(manager);
    }

    @Override
    public PlaybackBuilder getBuilder(final PathingManager manager) {
        return new PathMaker(manager, this);
    }

    private void queueNextFrame(final PathingManager manager, final Actor<?> actor) {
        final UUID uuid = actor.getUUID();
        final Movement movement = current();
        manager.sendPacket(actor.getViewers().toArray(new Player[0]), movement.getPackets(manager));
        movement.increment();
        for (int i = movement.iteration(); i < movement.max() - 1; i++) {
            manager.queue(uuid, () -> {
                manager.sendPacket(actor.getViewers().toArray(new Player[0]), movement.getPackets(manager));
                movement.increment();
            });
        }
        manager.queue(uuid, () -> {
            movement.increment();
            final Vector3 location = location();
            actor.setLocation(location);
            manager.sendPacket(actor.getViewers().toArray(new Player[0]), getTeleportPacket(manager, actor.getId(), location));
            AsyncHandler.runSynchronously(() -> actor.getEntity().teleport(location.toBukkitLocation()));
            movement.reset();
            next();
            queueNextFrame(manager, actor);
        });
    }

    private void synchronise(final PathingManager manager) {
        final Actor<?> actor = agent.getActor();
        final Vector3 location = location();
        actor.setLocation(location);
        manager.sendPacket(actor.getViewers().toArray(new Player[0]), getTeleportPacket(manager, actor.getId(), location));
        actor.getEntity().teleport(location.toBukkitLocation());
    }

    private PacketContainer getTeleportPacket(final PathingManager manager, final int id, final Vector3 location) {
        return manager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT, (packet) -> {
            packet.getIntegers().write(0, id);
            packet.getDoubles()
                    .write(0, location.x)
                    .write(1, location.y)
                    .write(2, location.z);
            packet.getBytes()
                    .write(0, current().pitch())
                    .write(1, current().yaw());
            packet.getBooleans().write(0, true);
            packet.setMeta(PACKET_VALID_META, true);
        });
    }
}
