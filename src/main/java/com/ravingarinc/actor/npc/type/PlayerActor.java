package com.ravingarinc.actor.npc.type;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;

public class PlayerActor extends Actor<LivingEntity> {

    private final PlayerProfile profile;
    private final WrappedGameProfile gameProfile;
    private String name;
    private String url = null;
    private PlayerInfoData data = null;

    public PlayerActor(final PlayerProfile profile, final LivingEntity entity, final Location spawnLocation, final ProtocolManager manager) {
        super(profile.getUniqueId(), entity, spawnLocation, manager);
        this.name = profile.getName();
        this.profile = profile;
        this.gameProfile = new WrappedGameProfile(uuid, "");
    }

    /**
     * Updates the skin for this player. The updated player profile is not sent to the client from this method
     *
     * @param url The url of the skin
     */
    public void updateSkin(final String url) {
        this.url = url;
        final PlayerTextures playerTextures = profile.getTextures();
        try {
            playerTextures.setSkin(new URL(url));
        } catch (final MalformedURLException exception) {
            I.log(Level.WARNING, "Could not update skin due to invalid URL!");
            return;
        }
        profile.setTextures(playerTextures);

        final PlayerInfoData data = getPlayerInfoData();
        final WrappedGameProfile gameProfile = data.getProfile();
        final Multimap<String, WrappedSignedProperty> properties = gameProfile.getProperties();

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", playerTextures.getTimestamp());
        jsonObject.addProperty("profileId", profile.getUniqueId().toString());
        jsonObject.addProperty("profileName", profile.getName());

        final JsonObject textureObject = new JsonObject();
        final JsonObject skin = new JsonObject();
        skin.addProperty("url", url);
        final JsonObject meta = new JsonObject();
        meta.addProperty("model", playerTextures.getSkinModel().name().toLowerCase());
        skin.add("metadata", meta);
        textureObject.add("SKIN", skin);

        jsonObject.add("textures", textureObject);

        I.log(Level.WARNING, " DEBUG Json Block looking like " + jsonObject);
        final String encoded = Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8));

        properties.get("textures").clear();
        properties.get("textures").add(new WrappedSignedProperty("textures", encoded, null));
    }

    public WrappedGameProfile getGameProfile() {
        return gameProfile;
    }

    @Override
    public void updateName(final String displayName) {
        this.name = displayName;
        this.data = null;
        getPlayerInfoData();
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

    private PlayerInfoData getPlayerInfoData() {
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
    public void update(final Player player) {
    }
}
