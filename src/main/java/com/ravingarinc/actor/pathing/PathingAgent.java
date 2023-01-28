package com.ravingarinc.actor.pathing;

import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.type.Path;
import org.jetbrains.annotations.NotNull;

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
    private final PathingManager manager;

    private final Actor<?> actor;

    public PathingAgent(Actor<?> actor, PathingManager manager) {
        isMoving = new AtomicBoolean(false);
        pathList = new LinkedList<>();
        selectedPath = null;
        this.manager = manager;
        this.actor = actor;
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
    public void trySelectPath(final int index) {
        if (selectedPath != null) {
            selectedPath.reset();
        }
        selectedPath = pathList.get(index);
    }

    public void start() {
        if (selectedPath == null) {
            throw new IllegalStateException("Cannot start() as no path is selected for Actor!");
        }
        this.isMoving.setRelease(true);

        move();
    }

    public void move() {
        long time = System.currentTimeMillis();
        Vector3 current = selectedPath.current();
        selectedPath.next();
        Vector3 post = selectedPath.current();

        double dX = post.x - current.x;
        double dY = post.y - current.y;
        double dZ = post.z - current.z;


    }

    public void stop() {
        if (selectedPath == null) {
            throw new IllegalStateException("Cannot stop() as no path is selected for Actor!");
        }
        this.isMoving.setRelease(false);


    }

    public void reset() {
        stop();
        selectedPath.reset();
    }

    public boolean isMoving() {
        return isMoving.getAcquire();
    }
}
