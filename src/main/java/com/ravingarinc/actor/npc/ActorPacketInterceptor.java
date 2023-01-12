package com.ravingarinc.actor.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.type.Actor;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActorPacketInterceptor extends Module {
    private final Set<PacketType> spawnPackets;
    private final BukkitScheduler scheduler;
    private ActorManager actorManager;
    private ProtocolManager protocolManager;

    public ActorPacketInterceptor(final RavinPlugin plugin) {
        super(ActorPacketInterceptor.class, plugin, ActorManager.class);
        scheduler = plugin.getServer().getScheduler();
        spawnPackets = new HashSet<>();
        spawnPackets.add(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
        spawnPackets.add(PacketType.Play.Server.SPAWN_ENTITY);
    }

    @Override
    protected void load() throws ModuleLoadException {
        actorManager = plugin.getModule(ActorManager.class);
        protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, spawnPackets) {
            @Override
            public void onPacketSending(final PacketEvent event) {
                final PacketContainer container = event.getPacket();
                final int id = container.getIntegers().readSafely(0);
                final Actor<?> actor = actorManager.getActor(id);
                if (actor == null) {
                    // Actor may exist but isn't loaded. How do we fix this? by getting bitches!
                    return;
                }
                event.setCancelled(true);
                final StructureModifier<Double> doubles = container.getDoubles();
                final Vector3 location = new Vector3(doubles.read(0), doubles.read(1), doubles.read(2));
                scheduler.runTaskAsynchronously(plugin, () -> actorManager.processActorSpawn(actor, event.getPlayer(), location));
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, PacketType.Play.Server.ENTITY_DESTROY) {
            @Override
            public void onPacketSending(final PacketEvent event) {
                if (!event.isCancelled()) {
                    final PacketContainer packet = event.getPacket();
                    final List<Integer> list = packet.getIntLists().readSafely(0);
                    if (list == null) {
                        return;
                    }
                    scheduler.runTaskAsynchronously(plugin, () -> actorManager.processOnActorDestroy(event.getPlayer(), list));
                    // Todo when we have events for MANUALLY showing and hiding an actor, this must be different to listening for
                    //   these events.
                }
            }
        });
    }

    @Override
    public void cancel() {
        protocolManager.removePacketListeners(plugin);
    }
}
