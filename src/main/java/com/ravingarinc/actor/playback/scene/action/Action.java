package com.ravingarinc.actor.playback.scene.action;

import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.playback.PathingManager;
import org.bukkit.entity.Player;

public abstract class Action {
    protected final String identifier;

    public Action(final String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public abstract void apply(PathingManager manager, Actor<?> actor, Player... viewers);
}
