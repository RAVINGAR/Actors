package com.ravingarinc.actor.playback;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.MapRunner;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.playback.path.PathMaker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PathingManager extends Module {
    public static final String PACKET_VALID_META = "actor_valid_packet";
    private final Map<UUID, PathingAgent> agents;
    private final Set<PacketType> movePackets;
    private ProtocolManager manager;
    private MapRunner<FutureTask<Void>> runner;
    private ActorManager actorManager;


    public PathingManager(final RavinPlugin plugin) {
        super(PathingManager.class, plugin, ActorManager.class);
        this.agents = new ConcurrentHashMap<>();

        movePackets = new HashSet<>();
        movePackets.add(PacketType.Play.Server.REL_ENTITY_MOVE);
        movePackets.add(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
    }

    @Override
    protected void load() throws ModuleLoadException {
        // the general idea is that two runners exist at the same time and are synchronised through a delayed queue.
        // the async runner will send packets every tick, meanwhile the sync runner will actually teleport the entity
        // itself on the sync thread every lets say 20 ticks.
        manager = ProtocolLibrary.getProtocolManager();
        actorManager = plugin.getModule(ActorManager.class);
        runner = new MapRunner<>();
        runner.runTaskTimerAsynchronously(plugin, 0L, 1L);

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, movePackets) {
            @Override
            public void onPacketSending(final PacketEvent event) {
                if (!event.isCancelled()) {
                    final PacketContainer packet = event.getPacket();
                    final Integer id = packet.getIntegers().readSafely(0);
                    if (id == null) {
                        return;
                    }

                    //todo test if storing ids in a local hashset is quicker than doing this!
                    if (packet.getMeta(PACKET_VALID_META).isEmpty() && actorManager.getActor(id) != null) {
                        event.setCancelled(true);
                    }
                }
            }
        });
    }

    @NotNull
    public PathingAgent getAgentOrCreate(final Actor<?> actor) {
        return agents.computeIfAbsent(actor.getUUID(), uuid -> new PathingAgent(actor));
    }

    @Nullable
    public PathingAgent getAgentOrNull(final Actor<?> actor) {
        return agents.get(actor.getUUID());
    }

    public void savePath(final PathMaker path) {
        //todo databse queries here as well
        actorManager.queue(path::save);
    }

    public void cancelForActor(final UUID uuid) {
        runner.cancelFor(uuid);
    }

    public FutureTask<Void> queue(final UUID uuid, final Runnable runnable) {
        final FutureTask<Void> future = new FutureTask<>(runnable, null);
        runner.add(uuid, future);
        return future;
    }

    public PacketContainer createPacket(final PacketType type, final Consumer<PacketContainer> consumer) {
        final PacketContainer packet = manager.createPacket(type);
        consumer.accept(packet);
        return packet;
    }

    public void syncIfMoving(final Actor<?> actor, final Player viewer) {
        final UUID uuid = actor.getUUID();
        final PathingAgent agent = agents.get(uuid);
        if (agent == null) {
            return;
        }
        if (runner.has(uuid)) {
            // assert path is selected
            final Vector3 location = agent.location(viewer);
            if (location != null) {
                actor.setLocation(location);
            }
        }
    }

    public void sendPacket(final Player[] viewers, final PacketContainer[] packets) {
        try {
            for (final Player player : viewers) {
                for (final PacketContainer packet : packets) {
                    manager.sendServerPacket(player, packet);
                }
            }
        } catch (final InvocationTargetException exception) {
            I.log(Level.SEVERE, "Encountered issue sending server packet to player!", exception);
        }
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

    @Override
    public void cancel() {
        runner.cancel();
        agents.clear();
        manager.removePacketListeners(plugin);
    }
}
