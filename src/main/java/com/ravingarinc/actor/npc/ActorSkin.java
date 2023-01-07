package com.ravingarinc.actor.npc;

import java.util.UUID;

public class ActorSkin {
    private final UUID uuid;
    private final String value;
    private final String signature;
    private final String url;

    public ActorSkin(final UUID uuid, final String value, final String signature, final String url) {
        this.uuid = uuid;
        this.value = value;
        this.signature = signature;
        this.url = url;
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
}
