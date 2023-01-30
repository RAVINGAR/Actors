package com.ravingarinc.actor.playback.api;

import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.playback.PathingAgent;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.playback.PlaybackBuilder;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class LivePlayback<T extends Frame> implements Playback {
    protected final PathingAgent agent;

    protected final List<T> frames;
    protected int lastFrame;

    public LivePlayback(final PathingAgent agent) {
        this.agent = agent;
        this.frames = new ArrayList<>();
        this.lastFrame = 0;
    }

    public PathingAgent getAgent() {
        return agent;
    }

    public List<T> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public synchronized T current() {
        return frames.get(lastFrame);
    }

    @Override
    public synchronized Vector3 location(@Nullable final Player viewer) {
        return current().location(viewer);
    }

    public synchronized T next() {
        lastFrame++;

        if (lastFrame >= frames.size()) {
            lastFrame = 0;
        }
        return frames.get(lastFrame);
    }

    public synchronized void reset() {
        lastFrame = 0;
    }

    public synchronized void clearFrames() {
        this.frames.clear();
        this.lastFrame = 0;
    }

    public synchronized void addFrame(final T frame) {
        this.frames.add(frame);
    }

    public abstract PlaybackBuilder getBuilder(PathingManager manager);
}
