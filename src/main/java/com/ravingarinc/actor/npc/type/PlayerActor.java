package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.ActorFactory;
import com.ravingarinc.actor.npc.ActorManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class PlayerActor extends LivingActor {
    private final WrappedGameProfile gameProfile;

    private PlayerInfoData data = null;


    public PlayerActor(final UUID uuid, final LivingEntity entity, final Vector3 spawnLocation) {
        super(ActorFactory.PLAYER, uuid, entity, spawnLocation);
        this.gameProfile = new WrappedGameProfile(uuid, getName());
    }

    @Override
    public void update(final ActorManager manager) {
        final Player[] viewers = getViewers().toArray(new Player[0]);
        manager.queue(() -> manager.sendPacket(viewers, getRemovePacket(manager)));
        manager.queue(() -> manager.sendPacket(viewers, getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, manager)));
        manager.queue(() -> manager.sendPacket(viewers, getSpawnPacket(getLocation(), manager)));
        manager.queueLater(() -> manager.sendPacket(viewers, getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, manager)), 10L);
    }

    @Override
    public void spawn(final ActorManager manager, final Vector3 location, final Player viewer) {
        manager.queue(() -> {
            addViewer(viewer);
            manager.sendPackets(viewer,
                    getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, manager),
                    getSpawnPacket(location, manager));
        });
        manager.queueLater(() -> manager.sendPacket(viewer, getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, manager)), 10L);

    }

    @Override
    public void create(final ActorManager manager) {
        final Player[] viewers = getViewers().toArray(new Player[0]);
        manager.queue(() -> {
            final PacketContainer[] packets = new PacketContainer[3];
            packets[0] = (getRemovePacket(manager));
            packets[1] = (getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, manager));
            packets[2] = (getSpawnPacket(spawnLocation, manager));
            manager.sendPackets(viewers, packets);
        });
        manager.queueLater(() -> {
            manager.sendPacket(viewers, getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, manager));
            manager.cacheActor(id, uuid, this);
        }, 5L);
    }

    @Override
    public PacketContainer getSpawnPacket(final Vector3 location, final ActorManager manager) {
        final PacketContainer spawnPacket = manager.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawnPacket.getIntegers().write(0, id);
        spawnPacket.getUUIDs().write(0, uuid);
        spawnPacket.getDoubles()
                .write(0, location.x)
                .write(1, location.y)
                .write(2, location.z);
        spawnPacket.getBytes()
                .write(0, (byte) 0)
                .write(1, (byte) 0);

        return spawnPacket;
    }

    public WrappedGameProfile getWrappedProfile() {
        return gameProfile;
    }

    @Override
    public void updateName(final @NotNull String displayName) {
        this.name.setRelease(displayName);
        // todo hologram thingies
        data = null;
    }

    private PacketContainer getPlayerInfoPacket(final EnumWrappers.PlayerInfoAction action, final ActorManager manager) {
        final PacketContainer infoPacket = manager.createPacket(PacketType.Play.Server.PLAYER_INFO, true);

        infoPacket.getPlayerInfoAction().write(0, action);
        final List<PlayerInfoData> list = infoPacket.getPlayerInfoDataLists().readSafely(0);
        if (list == null) {
            return null;
        }

        list.add(getPlayerInfoData());
        infoPacket.getPlayerInfoDataLists().write(0, list);

        return infoPacket;
    }

    public PlayerInfoData getPlayerInfoData() {
        if (data == null) {
            data = new PlayerInfoData(
                    gameProfile,
                    100,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(getName()));
        }
        return data;
    }
}
