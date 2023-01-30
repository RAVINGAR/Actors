package com.ravingarinc.actor.pathing.type;

import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.pathing.PathingAgent;
import com.ravingarinc.actor.pathing.PathingManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Path {
    private final PathingAgent agent;

    private final List<Frame> frames;
    private int lastFrame;

    private @Nullable PathMaker pathMaker = null;

    public Path(final PathingAgent agent) {
        this.agent = agent;
        this.frames = new ArrayList<>();
        this.lastFrame = 0;
    }

    public PathingAgent getAgent() {
        return agent;
    }

    public List<Frame> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public synchronized Frame current() {
        return frames.get(lastFrame);
    }

    public Vector3 location() {
        final Frame frame = current();
        final int i = frame.getIteration();
        final int factor = frame.getFactor();
        if (i == 0) {
            return frame.getInitial();
        } else if (i < factor) {
            final Vector3 initial = frame.getInitial();
            final Vector3 terminal = frame.getTerminal();
            terminal.sub(initial);
            terminal.scale((double) i / factor);
            initial.add(terminal);
            return initial;
        } else {
            return frame.getTerminal();
        }
    }

    public synchronized Frame next() {
        lastFrame++;

        if (lastFrame >= frames.size()) {
            lastFrame = 0;
        }
        return frames.get(lastFrame);
    }

    public synchronized void reset() {
        lastFrame = 0;
    }

    @NotNull
    public PathMaker getPathMaker(final PathingManager manager) {
        if (pathMaker == null) {
            pathMaker = new PathMaker(manager, this);
        }
        return pathMaker;
    }

    public void resetPathMaker() {
        pathMaker = null;
    }

    public synchronized void savePathMaker() {
        if (pathMaker != null) {
            // We can assert that points is greater than or equal to 3

            // assert that PathMaker actually adds the first point as the last point before this method is called.
            final Iterator<Vector3> iterator = this.pathMaker.getPoints().iterator();
            reset();
            resetPathMaker();
            this.frames.clear();

            final int id = agent.getActor().getId();
            final Vector3 first = iterator.next();
            final Vector3 second = iterator.next();
            Vector3 previous = first;
            Vector3 terminal = second;
            do {
                final Vector3 next = iterator.next();
                frames.add(new Frame(id, previous, terminal, next, 0.1f));
                previous = terminal;
                terminal = next;
            } while (iterator.hasNext());
            frames.add(new Frame(id, previous, terminal, second, 0.1f));
        }
    }
}
