package com.ravingarinc.actor.pathing;

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
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.type.Frame;
import com.ravingarinc.actor.pathing.type.Path;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

public class PathingManager extends Module {
    public static final String PACKET_VALID_META = "actor_valid_packet";
    private final Map<UUID, PathingAgent> agents;
    private final BukkitScheduler scheduler;
    private final Set<PacketType> movePackets;
    private ProtocolManager manager;
    private MapRunner<FutureTask<Void>> runner;
    private ActorManager actorManager;


    public PathingManager(final RavinPlugin plugin) {
        super(PathingManager.class, plugin, ActorManager.class);
        this.agents = new ConcurrentHashMap<>();
        this.scheduler = plugin.getServer().getScheduler();

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

    public void savePath(final Path path) {
        //todo databse queries here as well
        actorManager.queue(path::savePathMaker);
    }

    public void select(final Actor<?> actor, final int index) {
        final PathingAgent agent = agents.get(actor.getUUID());
        if (agent == null) {
            I.log(Level.WARNING, "Attempted to start actor movement but no agent was found!");
            return;
        }
        reset(actor, agent);
        agent.selectPath(index);
    }

    public void reset(final Actor<?> actor) {
        final PathingAgent agent = agents.get(actor.getUUID());
        if (agent == null) {
            I.log(Level.WARNING, "Attempted to reset actor movement but no agent was found!");
            return;
        }
        reset(actor, agent);
    }

    public void reset(final Actor<?> actor, final PathingAgent agent) {
        runner.cancelFor(actor.getUUID());
        if (agent.hasSelectedPath()) {
            agent.reset();
            synchronise(actor, agent.getSelectedPath());
        }

    }

    public FutureTask<Void> queue(final UUID uuid, final Runnable runnable) {
        final FutureTask<Void> future = new FutureTask<>(runnable, null);
        runner.add(uuid, future);
        return future;
    }

    @Sync.SyncOnly
    public void start(final Actor<?> actor) {
        final PathingAgent agent = agents.get(actor.getUUID());
        if (agent == null) {
            I.log(Level.WARNING, "Attempted to start actor movement but no agent was found!");
            return;
        }
        if (agent.hasSelectedPath()) {
            synchronise(actor, agent.getSelectedPath());
            runner.add(actor.getUUID(), new FutureTask<>(() -> queueNextFrame(actor, agent), null));
        }
    }

    @Sync.SyncOnly
    public void synchronise(final Actor<?> actor, final Path path) {
        actor.setLocation(path.location());
        sendPacket(actor.getViewers().toArray(new Player[0]), constructFixedTeleportPacket(actor.getId(), actor.getLocation()));
        actor.getEntity().teleport(actor.getLocation().toBukkitLocation());
    }

    public void queueNextFrame(final Actor<?> actor, final PathingAgent agent) {
        final UUID uuid = actor.getUUID();
        final Path path = agent.getSelectedPath();
        final Frame frame = path.current();
        sendPacket(actor.getViewers().toArray(new Player[0]), frame.getPackets(manager));
        frame.increment();
        for (int i = frame.getIteration(); i < frame.getFactor() - 1; i++) {
            runner.add(uuid, new FutureTask<>(() -> {
                sendPacket(actor.getViewers().toArray(new Player[0]), frame.getPackets(manager));
                frame.increment();
            }, null));
        }
        runner.add(uuid, new FutureTask<>(() -> {
            frame.increment();
            actor.setLocation(path.location());
            sendPacket(actor.getViewers().toArray(new Player[0]), constructFixedTeleportPacket(actor.getId(), actor.getLocation()));
            scheduler.runTask(plugin, () -> actor.getEntity().teleport(actor.getLocation().toBukkitLocation()));
            frame.resetIteration();
            path.next();
            queueNextFrame(actor, agent);
        }, null));
    }

    public PacketContainer constructFixedTeleportPacket(final int id, final Vector3 location) {
        final PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, id);
        packet.getDoubles()
                .write(0, location.x)
                .write(1, location.y)
                .write(2, location.z);
        packet.getBytes()
                .write(0, (byte) 0)
                .write(1, (byte) 0);
        packet.getBooleans().write(0, true);
        packet.setMeta(PACKET_VALID_META, true);

        return packet;
    }

    public void syncIfMoving(final Actor<?> actor) {
        final UUID uuid = actor.getUUID();
        final PathingAgent agent = agents.get(uuid);
        if (agent == null) {
            return;
        }
        if (runner.has(uuid)) {
            // assert path is selected
            actor.setLocation(agent.getSelectedPath().location());
        }
    }

    @Sync.SyncOnly
    public void stop(final Actor<?> actor) {
        final UUID uuid = actor.getUUID();
        final PathingAgent agent = agents.get(uuid);
        if (agent == null) {
            I.log(Level.WARNING, "Attempted to start actor movement but no agent was found!");
            return;
        }
        runner.cancelFor(uuid);
        if (agent.hasSelectedPath()) {
            synchronise(actor, agent.getSelectedPath());
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
