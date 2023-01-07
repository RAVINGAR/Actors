package com.ravingarinc.actor.npc;

import com.comphenix.protocol.ProtocolManager;
import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public class ActorFactory {

    private final ProtocolManager manager;
    private final RavinPlugin plugin;

    public ActorFactory(final RavinPlugin plugin, final ProtocolManager manager) {
        this.manager = manager;
        this.plugin = plugin;
    }

    public Optional<Actor<?>> buildActor(final UUID uuid, final EntityType type, final Location location, final Argument[] args) {
        return Optional.ofNullable(switch (type) {
            case PLAYER -> buildPlayerActor(uuid, location, args);
            default -> null;
        });
    }

    private Actor<?> buildPlayerActor(final UUID uuid, final Location location, final Argument[] args) {
        final LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.HUSK, false);
        entity.setAI(false);
        entity.setInvulnerable(true);
        final PlayerActor actor = new PlayerActor(plugin.getServer().createPlayerProfile(uuid, ""), entity, location, manager);
        for (final Argument arg : args) {
            arg.consume(actor);
        }
        return actor;
    }
}
