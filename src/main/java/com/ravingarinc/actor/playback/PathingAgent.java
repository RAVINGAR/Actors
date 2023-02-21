package com.ravingarinc.actor.playback;

import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.playback.api.LivePlayback;
import com.ravingarinc.actor.playback.api.Playback;
import com.ravingarinc.actor.playback.path.PathMaker;
import com.ravingarinc.api.Sync;
import com.ravingarinc.api.Vector3;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Base class for actor related movement patterns. An actor always has a start location otherwise known as their
 * spawn location. This is a final location that cannot be changed without a command. Additionally they will have a
 * current location which represents their current location. This can change and may mean that through subsequent reloads
 * or saves, an actors position may be different from what is expected from the spawn location.
 */
public class PathingAgent implements Playback {
    private final List<LivePlayback> pathList;
    private final Actor<?> actor;
    private LivePlayback selectedPath;

    private List<PathMaker> makers;

    public PathingAgent(final Actor<?> actor) {
        pathList = new LinkedList<>();
        selectedPath = null;
        this.actor = actor;
    }

    public Actor<?> getActor() {
        return actor;
    }

    public void addPath(final LivePlayback path) {
        pathList.add(path);
    }

    public LivePlayback removePath(final int index) {
        return pathList.remove(index);
    }

    public void removePath(final LivePlayback path) {
        pathList.remove(path);
    }

    @NotNull
    public LivePlayback getPath(final int index) {
        return pathList.get(index);
    }

    public int getAmountOfPaths() {
        return pathList.size();
    }

    /**
     * True if path was selected, or false if not
     */
    public void select(final PathingManager manager, final int index) {
        reset(manager);
        selectedPath = pathList.get(index);
    }

    @NotNull
    protected LivePlayback getSelectedPath() {
        if (selectedPath == null) {
            throw new IllegalStateException("Cannot get selected path as it was null!");
        }
        return selectedPath;
    }

    @Override
    @Sync.SyncOnly
    public void start(final PathingManager manager) {
        if (selectedPath == null) {
            return;
        }
        selectedPath.start(manager);
    }

    @Override
    @Sync.SyncOnly
    public void stop(final PathingManager manager) {
        if (selectedPath == null) {
            return;
        }
        manager.cancelForActor(actor.getUUID());
        selectedPath.stop(manager);
    }

    @Override
    @Sync.SyncOnly
    public void reset(final PathingManager manager) {
        if (selectedPath == null) {
            return;
        }
        manager.cancelForActor(actor.getUUID());
        selectedPath.reset(manager);
    }

    @Override
    @Nullable
    public Vector3 location(final Player viewer) {
        if (selectedPath == null) {
            return null;
        }
        return selectedPath.location(viewer);
    }
}
