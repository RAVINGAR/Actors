package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.collect.Multimap;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerActor extends Actor<LivingEntity> {

    private final PlayerProfile profile;
    private final WrappedGameProfile gameProfile;
    private String name;

    private PlayerInfoData data = null;


    public PlayerActor(final UUID uuid, final PlayerProfile profile, final LivingEntity entity, final Location spawnLocation, final ProtocolManager manager) {
        super(uuid, entity, spawnLocation, manager);
        this.name = "Actor";
        this.profile = profile;
        this.gameProfile = new WrappedGameProfile(uuid, name);
    }

    public void updateProfile(final String value, final String signature) {
        I.log(Level.WARNING, "Updating profile!");

        final Multimap<String, WrappedSignedProperty> properties = gameProfile.getProperties();
        properties.clear();
        properties.put("textures", new WrappedSignedProperty("textures", value, signature));

        profile.setProperty(new ProfileProperty("textures", value, signature));
        profile.complete(false);

        data = null;
    }

    public WrappedGameProfile getWrappedProfile() {
        return gameProfile;
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    @Override
    public void updateName(final String displayName) {
        this.name = displayName;
        // todo hologram thingies
        data = null;
    }


    @Override
    public List<PacketContainer> getShowPackets(final Vector3 location) {
        final List<PacketContainer> packets = new ArrayList<>();
        packets.add(getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
        packets.add(getPlayerSpawnPacket(location));
        packets.add(getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));
        return packets;
    }

    private PacketContainer getPlayerInfoPacket(final EnumWrappers.PlayerInfoAction action) {
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

    private PacketContainer getPlayerSpawnPacket(final Vector3 location) {
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
    public List<PacketContainer> getUpdatePackets() {
        final List<PacketContainer> packets = new ArrayList<>();
        packets.add(getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
        packets.add(getPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));
        return packets;
    }
}
