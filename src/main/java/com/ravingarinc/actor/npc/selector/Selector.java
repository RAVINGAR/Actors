package com.ravingarinc.actor.npc.selector;

import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.PathFactory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Selector {
    // idk todo maybe change this to an interface that selects an object but im lazy right now so
    private final Player player;
    private @Nullable Actor<?> selectedActor;

    private @Nullable PathFactory.Type<?> selectedPath;

    private Mode mode;

    public Selector(final Player player) {
        this.player = player;
        this.mode = Mode.ACTOR;
        this.selectedActor = null;
    }

    public void select(final Actor<?> actor) {
        selectedActor = actor;
    }

    @Nullable
    public Actor<?> getSelection() {
        return selectedActor;
    }

    public void unselect() {
        selectedActor = null;
        selectedPath = null;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(final Mode mode) {
        this.mode = mode;
    }

    public void selectPath(final PathFactory.Type<?> type) {
        this.selectedPath = type;
    }

    @Nullable
    public PathFactory.Type<?> getSelectedPath() {
        return selectedPath;
    }

    public enum Mode {
        ACTOR,
        PATH
    }
}
