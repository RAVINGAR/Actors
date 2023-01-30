package com.ravingarinc.actor.playback.scene.action;

import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.playback.PathingManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MainhandChangeAction extends Action {
    private final ItemStack item;

    public MainhandChangeAction(final ItemStack item) {
        super("mainhand_change");
        this.item = item;
    }

    @Override
    public void apply(final PathingManager manager, final Actor<?> actor, final Player... viewers) {

    }
}
