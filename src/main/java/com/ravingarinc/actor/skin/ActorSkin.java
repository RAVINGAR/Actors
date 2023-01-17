package com.ravingarinc.actor.skin;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Multimap;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ActorSkin {
    private final List<PlayerActor> linkedActors;
    private final UUID uuid;
    private String name;
    private String value;
    private String signature;

    public ActorSkin(@NotNull final String name, @NotNull final UUID uuid, @Nullable final String value, @Nullable final String signature) {
        this.name = name;
        this.uuid = uuid;
        this.value = value;
        this.signature = signature;
        this.linkedActors = new LinkedList<>();
    }

    @NotNull
    public UUID getUUID() {
        return uuid;
    }

    @Nullable
    public String getSignature() {
        return signature;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    /**
     * Updates an internal value and returns true if the value was changed, or null if it was not.
     */
    public boolean updateTexture(final String value, final String signature) {
        if ((value == null || value.equalsIgnoreCase(this.value)) && (signature == null || signature.equalsIgnoreCase(this.signature))) {
            return false;
        }
        this.value = value;
        this.signature = signature;
        return true;
    }

    /**
     * Updates an internal value and returns true if the value was changed, or null if it was not.
     */
    @Contract(value = "null -> false")
    public boolean updateName(final String name) {
        if (name == null || name.equalsIgnoreCase(this.name)) {
            return false;
        }
        this.name = name;
        return true;
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
            manager.updateActor(actor);
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
            manager.updateActor(actor);
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
        return name.equals(actorSkin.name) && Objects.equals(uuid, actorSkin.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uuid);
    }

    public String getName() {
        return name;
    }
}
