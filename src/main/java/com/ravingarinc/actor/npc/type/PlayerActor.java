package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.ActorFactory;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.UUID;

public class PlayerActor extends Actor<LivingEntity> {
    private final WrappedGameProfile gameProfile;
    private String name;

    private PlayerInfoData data = null;


    public PlayerActor(final UUID uuid, final LivingEntity entity, final Vector3 spawnLocation) {
        super(ActorFactory.PLAYER, uuid, entity, spawnLocation);
        this.name = "Actor";
        this.gameProfile = new WrappedGameProfile(uuid, name);
    }

    @Override
    public PacketContainer getPreSpawnPacket(final ProtocolManager manager) {
        return getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, manager);
    }

    @Override
    public PacketContainer getSpawnPacket(final Vector3 location, final ProtocolManager manager) {
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

    @Override
    public PacketContainer getHidePacket(final ProtocolManager manager) {
        return getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, manager);
    }

    public WrappedGameProfile getWrappedProfile() {
        return gameProfile;
    }

    @Override
    public void updateName(final String displayName) {
        this.name = displayName;
        // todo hologram thingies
        data = null;
    }

    private PacketContainer getPlayerInfoPacket(final EnumWrappers.PlayerInfoAction action, final ProtocolManager manager) {
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
                    WrappedChatComponent.fromText(name));
        }
        return data;
    }
}
