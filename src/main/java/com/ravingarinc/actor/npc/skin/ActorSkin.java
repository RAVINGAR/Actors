package com.ravingarinc.actor.npc.skin;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Multimap;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.mineskin.data.Skin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

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

    @Sync.AsyncOnly
    public void setValues(final Skin skin) {
        if (skin == null) {
            throw new IllegalArgumentException("Skin was null!");
        }
        this.uuid = skin.data.uuid;
        this.value = skin.data.texture.value;
        this.signature = skin.data.texture.signature;
        this.url = skin.data.texture.url;
    }

    @Sync.AsyncOnly
    public void linkActor(final PlayerActor actor) {
        linkedActors.add(actor);
        applyToProfile(actor);
    }

    @Sync.AsyncOnly
    public void unlinkActor(final PlayerActor actor) {
        linkedActors.remove(actor);
        unapplyFromProfile(actor);
    }

    /**
     * Apply this skin to all player actors
     */
    @Sync.AsyncOnly
    public void apply(final ActorManager manager) {
        new ArrayList<>(linkedActors).forEach(actor -> {
            applyToProfile(actor);
            manager.processActorUpdate(actor);
        });
    }

    @Sync.AsyncOnly
    private void applyToProfile(final PlayerActor actor) {
        final Multimap<String, WrappedSignedProperty> properties = actor.getWrappedProfile().getProperties();
        properties.clear();
        properties.put("textures", new WrappedSignedProperty("textures", value, signature));
    }

    @Sync.AsyncOnly
    private void unapplyFromProfile(final PlayerActor actor) {
        final Multimap<String, WrappedSignedProperty> properties = actor.getWrappedProfile().getProperties();
        properties.clear();
    }

    /**
     * Removes this skin from any player actors it is connected to. This is normally called if a skin request timed out
     * or if something else went wrong!
     */
    @Sync.AsyncOnly
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
