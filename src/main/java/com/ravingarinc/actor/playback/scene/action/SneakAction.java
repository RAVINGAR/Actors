package com.ravingarinc.actor.playback.scene.action;

import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import com.ravingarinc.actor.playback.PathingManager;
import org.bukkit.entity.Player;

public class SneakAction extends Action {
    private final boolean isSneaking;

    public SneakAction(final boolean isSneaking) {
        super("sneak");
        this.isSneaking = isSneaking;
    }

    @Override
    public void apply(final PathingManager manager, final Actor<?> actor, final Player... viewers) {
        if (actor instanceof PlayerActor playerActor) {

        }
    }
}
