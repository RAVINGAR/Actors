package com.ravingarinc.actor.npc;

import com.ravingarinc.actor.npc.type.PlayerActor;
import org.mineskin.data.Skin;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ActorSkin {
    private final String name;
    private UUID uuid;
    private String value;
    private String signature;
    private String url;

    private final List<PlayerActor> linkedActors;

    public ActorSkin(final String name) {
        this.name = name;
        this.linkedActors = new LinkedList<>();
    }

    public void loadSkinValues(final Skin skin) {
        this.uuid = skin.data.uuid;
        this.value = skin.data.texture.value;
        this.signature = skin.data.texture.signature;
        this.url = skin.data.texture.url;

        apply();
    }

    public void linkActor(final PlayerActor actor) {
        linkedActors.add(actor);
    }

    public void removeActor(final PlayerActor actor) {
        linkedActors.remove(actor);
    }

    /**
     * Apply this skin to all player actors
     */
    public void apply() {
        linkedActors.forEach(actor -> {
            //todo DO THIS
        });
    }

    /**
     * Removes this skin from any player actors it is connected to. This is normally called if a skin request timed out
     * or if something else went wrong!
     */
    public void discard() {

    }

    public String getSignature() {
        return signature;
    }

    public String getValue() {
        return value;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUrl() {
        return url;
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
