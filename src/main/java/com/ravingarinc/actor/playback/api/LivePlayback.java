package com.ravingarinc.actor.playback.api;

import com.ravingarinc.actor.playback.PathingAgent;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.playback.PlaybackBuilder;
import com.ravingarinc.api.Vector3;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class LivePlayback implements Playback {
    protected final PathingAgent agent;

    protected final List<Movement> movements;
    protected int lastFrame;

    public LivePlayback(final PathingAgent agent) {
        this.agent = agent;
        this.movements = new ArrayList<>();
        this.lastFrame = 0;
    }

    public PathingAgent getAgent() {
        return agent;
    }

    public List<Movement> getFrames() {
        return Collections.unmodifiableList(movements);
    }

    public synchronized Movement current() {
        return movements.get(lastFrame);
    }

    @Override
    public synchronized Vector3 location(@Nullable final Player viewer) {
        return current().location(viewer);
    }

    public synchronized Movement next() {
        lastFrame++;

        if (lastFrame >= movements.size()) {
            lastFrame = 0;
        }
        return movements.get(lastFrame);
    }

    public synchronized void reset() {
        lastFrame = 0;
    }

    public synchronized void clearFrames() {
        this.movements.clear();
        this.lastFrame = 0;
    }

    public synchronized void addFrame(final Movement movement) {
        this.movements.add(movement);
    }

    public abstract PlaybackBuilder getBuilder(PathingManager manager);
}
