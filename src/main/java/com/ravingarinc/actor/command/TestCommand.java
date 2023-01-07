package com.ravingarinc.actor.command;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public class TestCommand extends BaseCommand {
    private final ProtocolManager manager;

    public TestCommand(final ProtocolManager manager) {
        super("testcommand");
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        LivingEntity entity = null;
        final Player player = (Player) sender;
        for (final Entity e : player.getNearbyEntities(4, 4, 4)) {
            if (e.equals(player)) {
                continue;
            }
            if (e instanceof LivingEntity livingEntity) {
                entity = livingEntity;
                break;
            }
        }
        if (entity == null) {
            sender.sendMessage("No entity found!");
            return true;
        }
        // Note that Citizens NPCs can be moved by these packets, however they sorta glitch out a little bit.

        final LivingEntity finalEntity = entity;
        final int id = finalEntity.getEntityId();
        final Vector3 oldLoc = new Vector3(finalEntity.getLocation());
        final Vector3 newLoc = new Vector3(finalEntity.getLocation().add(2, 0, 2));

        AsyncHandler.runAsynchronously(() -> {
            final PacketContainer packet = manager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE);
            packet.getIntegers().write(0, id);
            packet.getShorts()
                    .write(0, calculateChange(newLoc.getX(), oldLoc.getX()))
                    .write(1, calculateChange(newLoc.getY(), oldLoc.getY()))
                    .write(2, calculateChange(newLoc.getZ(), oldLoc.getZ()));
            packet.getBooleans().write(0, true);
            try {
                manager.sendServerPacket(player, packet);
            } catch (final InvocationTargetException e) {
                I.log(Level.WARNING, "Error", e);
            }
        });
        return true;
    }

    private short calculateChange(final double current, final double prev) {
        return (short) ((current * 32 - prev * 32) * 128);
    }
}
