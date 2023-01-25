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
import com.ravingarinc.actor.api.ModuleListener;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.type.Actor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ActorListener extends ModuleListener {
    private final Set<PacketType> spawnPackets;
    private final BukkitScheduler scheduler;
    private ActorManager actorManager;
    private ProtocolManager protocolManager;

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
                final Player player = event.getPlayer();
                I.log(Level.WARNING, "Actor Spawn Packet!");
                scheduler.runTaskAsynchronously(plugin, () -> actor.spawn(actorManager, location, player));
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
                                I.log(Level.WARNING, "Actor Destroy Packet!");
                                actorManager.queue(() -> actor.removeViewer(player));
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

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        //actorManager.filterActors(actor ->
        //        actor.getSpawnLocation().getWorldName().equalsIgnoreCase(player.getWorld().getName()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(final EntityDamageEvent event) {
        // handle on actor damage
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
