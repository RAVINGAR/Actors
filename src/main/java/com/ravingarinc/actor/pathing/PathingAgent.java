package com.ravingarinc.actor.pathing;

import com.ravingarinc.actor.pathing.type.Path;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for actor related movement patterns. An actor always has a start location otherwise known as their
 * spawn location. This is a final location that cannot be changed without a command. Additionally they will have a
 * current location which represents their current location. This can change and may mean that through subsequent reloads
 * or saves, an actors position may be different from what is expected from the spawn location.
 */
public class PathingAgent {
    private final List<Path> pathList;
    private final AtomicBoolean isMoving;
    private Path selectedPath;

    public PathingAgent() {
        isMoving = new AtomicBoolean(false);
        pathList = new LinkedList<>();
        selectedPath = null;
    }

    public void addPath(final Path path) {
        pathList.add(path);
    }

    public Path removePath(final int index) {
        return pathList.remove(index);
    }

    public int getAmountOfPaths() {
        return pathList.size();
    }

    /**
     * True if path was selected, or false if not
     */
    public boolean trySelectPath(final int index) {
        final Path path = pathList.get(index);
        if (path == null) {
            return false;
        }
        if (selectedPath != null) {
            selectedPath.reset();
        }
        selectedPath = path;
        return true;
    }

    public void start() {
        if (selectedPath == null) {
            throw new IllegalStateException("Cannot start() as no path is selected for Actor!");
        }
        this.isMoving.setRelease(true);
    }

    public void stop() {
        if (selectedPath == null) {
            throw new IllegalStateException("Cannot stop() as no path is selected for Actor!");
        }
        this.isMoving.setRelease(false);
    }

    public void reset() {
        if (selectedPath == null) {
            throw new IllegalStateException("Cannot reset() as no path is selected for Actor!");
        }
        selectedPath.reset();
    }

    public boolean isMoving() {
        return isMoving.getAcquire();
    }


}
