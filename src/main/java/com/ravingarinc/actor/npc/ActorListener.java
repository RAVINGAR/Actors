package com.ravingarinc.actor.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.ModuleListener;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.PathingManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActorListener extends ModuleListener {
    private final Set<PacketType> spawnPackets;
    private final BukkitScheduler scheduler;
    private ActorManager actorManager;
    private ProtocolManager protocolManager;

    private PathingManager pathingManager;

    public ActorListener(final RavinPlugin plugin) {
        super(ActorListener.class, plugin, ActorManager.class);
        scheduler = plugin.getServer().getScheduler();
        spawnPackets = new HashSet<>();
        spawnPackets.add(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
        spawnPackets.add(PacketType.Play.Server.SPAWN_ENTITY);
    }

    @Override
    public void load() throws ModuleLoadException {
        actorManager = plugin.getModule(ActorManager.class);
        protocolManager = ProtocolLibrary.getProtocolManager();
        pathingManager = plugin.getModule(PathingManager.class);

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
                pathingManager.syncIfMoving(actor);
                event.setCancelled(true);
                final Player player = event.getPlayer();
                scheduler.runTaskAsynchronously(plugin, () -> actor.spawn(actorManager, player));
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
                    final Player player = event.getPlayer();

                    scheduler.runTaskAsynchronously(plugin, () -> {
                        for (final int id : list) {
                            final Actor<?> actor = actorManager.getActor(id);
                            if (actor != null) {
                                actorManager.queue(() -> actor.removeViewer(player));
                                break;
                            }
                        }
                    });
                    // Todo when we have events for MANUALLY showing and hiding an actor, this must be different to listening for
                    //   these events.
                }
            }
        });
        super.load();
    }

    @Override
    public void cancel() {
        super.cancel();
        protocolManager.removePacketListeners(plugin);
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event) {

    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        final Actor<?> actor = actorManager.getActor(event.getEntity().getEntityId());
        if (actor == null) {
            return;
        }
        if (actor.isInvuln()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event) {
        // handle on actor death
    }
}
