package com.ravingarinc.actor.npc.skin;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.jetbrains.annotations.Async;
import org.mineskin.data.Skin;

import java.util.LinkedList;
import java.util.List;

public class ActorSkin {
    private final String name;
    private final List<PlayerActor> linkedActors;
    private String uuid;

    private String profileName; // Todo check if this is the same as name
    private String value;
    private String signature;
    private String url;
    private long timestamp;

    public ActorSkin(final String name) {
        this.name = name;
        this.linkedActors = new LinkedList<>();
    }

    public void setValues(final Skin skin) {
        this.uuid = skin.uuid;
        this.value = skin.data.texture.value;
        this.profileName = skin.name;
        this.signature = skin.data.texture.signature;
        this.url = skin.data.texture.url;
        this.timestamp = skin.timestamp;
    }

    public void linkActor(final PlayerActor actor) {
        linkedActors.add(actor);
        applyToProfile(actor);
    }

    public void unlinkActor(final PlayerActor actor) {
        linkedActors.remove(actor);
        unapplyFromProfile(actor);
    }

    /**
     * Apply this skin to all player actors
     */
    @Async.Execute
    public void apply(final ActorManager manager) {
        linkedActors.forEach(actor -> {
            applyToProfile(actor);
            manager.processActorUpdate(actor);
        });
    }

    private void applyToProfile(final PlayerActor actor) {
        final WrappedGameProfile gameProfile = actor.getGameProfile();
        final Multimap<String, WrappedSignedProperty> properties = gameProfile.getProperties();

        properties.get("textures").clear();
        properties.get("textures").add(new WrappedSignedProperty("textures", value, signature));

        // todo, do we need to consider the spigot game profile or can we even just use that??
    }

    private void unapplyFromProfile(final PlayerActor actor) {
        final WrappedGameProfile gameProfile = actor.getGameProfile();
        final Multimap<String, WrappedSignedProperty> properties = gameProfile.getProperties();
        properties.get("textures").clear();
    }

    private JsonObject toJsonObject() {
        // todo do we need?
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", timestamp);
        jsonObject.addProperty("profileId", uuid);
        jsonObject.addProperty("profileName", profileName);

        final JsonObject textureObject = new JsonObject();
        final JsonObject skin = new JsonObject();
        skin.addProperty("url", url);
        final JsonObject meta = new JsonObject();
        meta.addProperty("model", "");
        skin.add("metadata", meta);
        textureObject.add("SKIN", skin);

        jsonObject.add("textures", textureObject);
        return jsonObject;
    }

    /**
     * Removes this skin from any player actors it is connected to. This is normally called if a skin request timed out
     * or if something else went wrong!
     */
    @Async.Execute
    public void discard(final ActorManager manager) {
        linkedActors.forEach(actor -> {
            unapplyFromProfile(actor);
            manager.processActorUpdate(actor);
        });
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ActorSkin actorSkin = (ActorSkin) o;
        return uuid.equals(actorSkin.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    public String getName() {
        return name;
    }
}
