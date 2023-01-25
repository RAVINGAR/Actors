package com.ravingarinc.actor.npc.selector;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class Selector {
    private @Nullable Selectable value;

    private @Nullable Selectable lastSelection;
    private final Player player;
    public Selector(Player player) {
        this.player = player;
        this.value = null;
        this.lastSelection = null;
    }

    public void select(Selectable value) throws SelectionFailException {
        if(this.value != null) {
            this.lastSelection = this.value;
        }
        value.onSelect(player);
        this.value = value;
    }

    public void unselect(boolean resumeLastSelection) throws SelectionFailException {
        Selectable oldLastSelection = lastSelection;
        if(value != null) {
            this.value.onUnselect(player);
            this.lastSelection = this.value;
        }
        if(resumeLastSelection) {
            if(oldLastSelection != null) {
                oldLastSelection.onSelect(player);
            }
            this.value = oldLastSelection;
        }
        else {
            this.value = null;
        }
    }

    public Player getPlayer() {
        return player;
    }

    @Nullable
    public Selectable getSelection() {
        return this.value;
    }

}
