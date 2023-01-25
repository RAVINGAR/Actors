package com.ravingarinc.actor.pathing.type;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.pathing.PathingAgent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Path {
    private final PathingAgent agent;
    private final List<Vector3> points;

    private int lastPoint;

    private final boolean reverseLoopback = true;

    private boolean isReversing = false;

    private @Nullable PathMaker pathMaker = null;

    public Path(final PathingAgent agent) {
        this.agent = agent;
        this.points = new ArrayList<>();
        this.lastPoint = 0;
    }

    public PathingAgent getAgent() {
        return agent;
    }

    public synchronized void addPoint(final double x, final double y, final double z) {
        if (agent.isMoving()) {
            throw new IllegalStateException("Cannot add point whilst Actor is moving!");
        }
        this.points.add(new Vector3(x, y, z));
    }

    public synchronized void setPoint(int index, final double x, double y, double z) {
        if (agent.isMoving()) {
            throw new IllegalStateException("Cannot add point whilst Actor is moving!");
        }
        this.points.remove(index);
        this.points.add(index, new Vector3(x, y, z));
    }

    public synchronized void removePoint(final int index) {
        if (agent.isMoving()) {
            throw new IllegalStateException("Cannot add point whilst Actor is moving!");
        }
        if (points.size() == 1) {
            throw new IllegalStateException("Cannot remove point when there is only point remaining!");
        }

        this.points.remove(index);
        if (index == lastPoint) {
            lastPoint++;
        }
    }

    public synchronized List<Vector3> getPoints() {
        return new ArrayList<>(points);
    }

    public synchronized int size() {
        return this.points.size();
    }

    public synchronized Vector3 current() {
        return points.get(lastPoint);
    }

    public synchronized void next() {
        if (reverseLoopback) {
            if (isReversing) {
                if (lastPoint == 0) {
                    lastPoint++;
                    isReversing = false;
                } else {
                    lastPoint--;
                }
            } else {
                if (lastPoint + 1 >= points.size()) {
                    lastPoint--;
                    isReversing = true;
                } else {
                    lastPoint++;
                }
            }
        } else {
            if (lastPoint + 1 >= points.size()) {
                lastPoint = 0;
            } else {
                lastPoint++;
            }
        }
    }

    public synchronized void reset() {
        lastPoint = 0;
        isReversing = false;
    }

    @NotNull
    public PathMaker getPathMaker(RavinPlugin plugin) {
        if(pathMaker == null) {
            pathMaker = new PathMaker(plugin, this);
        }
        return pathMaker;
    }

    public synchronized void savePathMaker() {
        if(pathMaker != null) {
            points.clear();
            reset();

            this.pathMaker.getPoints().forEach(v -> addPoint(v.x, v.y, v.z));
            this.pathMaker = null;
        }
    }

}
