package com.ravingarinc.actor.pathing;

import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.type.Path;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Base class for actor related movement patterns. An actor always has a start location otherwise known as their
 * spawn location. This is a final location that cannot be changed without a command. Additionally they will have a
 * current location which represents their current location. This can change and may mean that through subsequent reloads
 * or saves, an actors position may be different from what is expected from the spawn location.
 */
public class PathingAgent {
    private final List<Path> pathList;
    private final Actor<?> actor;
    private Path selectedPath;

    public PathingAgent(final Actor<?> actor) {
        pathList = new LinkedList<>();
        selectedPath = null;
        this.actor = actor;
    }

    public Actor<?> getActor() {
        return actor;
    }

    public void addPath(final Path path) {
        pathList.add(path);
    }

    public Path removePath(final int index) {
        return pathList.remove(index);
    }

    public void removePath(final Path path) {
        pathList.remove(path);
    }

    @NotNull
    public Path getPath(final int index) {
        return pathList.get(index);
    }

    public int getAmountOfPaths() {
        return pathList.size();
    }

    /**
     * True if path was selected, or false if not
     */
    public void selectPath(final int index) {
        selectedPath = pathList.get(index);
    }

    public void reset() {
        selectedPath.current().resetIteration();
        selectedPath.reset();
    }

    @NotNull
    protected Path getSelectedPath() {
        if (selectedPath == null) {
            throw new IllegalStateException("Cannot get selected path as it was null!");
        }
        return selectedPath;
    }

    public boolean hasSelectedPath() {
        return selectedPath != null;
    }
}
