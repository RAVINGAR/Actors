package com.ravingarinc.actor.npc.skin;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.AsynchronousException;
import com.ravingarinc.actor.api.async.Thread;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.mineskin.data.Skin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ActorSkin {
    private final String name;
    private final List<PlayerActor> linkedActors;
    private UUID uuid;
    private String value;
    private String signature;
    private String url;

    public ActorSkin(final String name) {
        this.name = name;
        this.linkedActors = new LinkedList<>();
    }

    @Thread.AsyncOnly
    public void setValues(final Skin skin) {
        if (skin == null) {
            throw new IllegalArgumentException("Skin was null!");
        }
        this.uuid = skin.data.uuid;
        this.value = skin.data.texture.value;
        this.signature = skin.data.texture.signature;
        this.url = skin.data.texture.url;
    }

    @Thread.AsyncOnly
    public void linkActor(final PlayerActor actor) {
        linkedActors.add(actor);
        applyToProfile(actor);
        I.log(Level.WARNING, "Debug -> Linking actor!");
    }

    @Thread.AsyncOnly
    public void unlinkActor(final PlayerActor actor) {
        linkedActors.remove(actor);
        unapplyFromProfile(actor);
        I.log(Level.WARNING, "Debug -> Unlinking actor!");
    }

    /**
     * Apply this skin to all player actors
     */
    @Thread.AsyncOnly
    public void apply(final ActorManager manager) {
        new ArrayList<>(linkedActors).forEach(actor -> {
            applyToProfile(actor);
            manager.processActorUpdate(actor);
        });
    }

    @Thread.AsyncOnly
    private void applyToProfile(final PlayerActor actor) {
        try {
            AsyncHandler.executeBlockingSyncComputation(() -> {
                actor.updateProfile(value, signature);
                return true;
            });
        } catch (final AsynchronousException e) {
            I.log(Level.WARNING, "Error", e);
        }

        /*
        AsyncHandler.runSynchronously(() -> {
        // todo this execution is lazy and we shouldn't just be creating threads everywhere

            final PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(url));
            } catch (final MalformedURLException e) {
                I.log(Level.WARNING, "Could not apply actor skin to profile!", e);
            }
            profile.setTextures(textures);
        });*/

    }

    @Thread.AsyncOnly
    private void unapplyFromProfile(final PlayerActor actor) {
        final WrappedGameProfile gameProfile = actor.getWrappedProfile();
        final Multimap<String, WrappedSignedProperty> properties = gameProfile.getProperties();
        properties.get("textures").clear();

        I.logIfDebug(() -> "Debug -> Unapplying from profile!");
    }

    private JsonObject toJsonObject() {
        // todo do we need?
        final JsonObject jsonObject = new JsonObject();
        //jsonObject.addProperty("timestamp", timestamp);
        //jsonObject.addProperty("profileId", uuid);
        //jsonObject.addProperty("profileName", profileName);

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
    @Thread.AsyncOnly
    public void discard(final ActorManager manager) {
        new ArrayList<>(linkedActors).forEach(actor -> {
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
