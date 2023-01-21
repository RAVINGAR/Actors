package com.ravingarinc.actor.pathing.type;

import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.pathing.PathingAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Path {
    private final PathingAgent agent;
    private final List<Vector3> points;

    private final AtomicInteger lastPoint;

    private final boolean reverseLoopback = true;

    private boolean isReversing = false;

    public Path(final PathingAgent agent) {
        this.agent = agent;
        this.points = new ArrayList<>();
        this.lastPoint = new AtomicInteger(0);
    }

    public void addPoint(final double x, final double y, final double z) {
        if (agent.isMoving()) {
            throw new IllegalStateException("Cannot add point whilst Actor is moving!");
        }
        this.addPoint(x, y, z, 0, 0);
    }

    public void addPoint(final double x, final double y, final double z, final float yaw, final float pitch) {
        if (agent.isMoving()) {
            throw new IllegalStateException("Cannot add point whilst Actor is moving!");
        }
        this.points.add(new Vector3(x, y, z, yaw, pitch, null));
    }

    public void removePoint(final int index) {
        if (agent.isMoving()) {
            throw new IllegalStateException("Cannot add point whilst Actor is moving!");
        }
        if (points.size() == 1) {
            throw new IllegalStateException("Cannot remove point when there is only point remaining!");
        }

        this.points.remove(index);
        if (index == getLastPoint()) {
            setLastPoint(index - 1);
        }
    }

    private int getLastPoint() {
        return lastPoint.getAcquire();
    }

    private void setLastPoint(final int i) {
        this.lastPoint.setRelease(i);
    }

    public int size() {
        return this.points.size();
    }

    public Vector3 current() {
        return points.get(getLastPoint());
    }

    public void next() {
        final int i = getLastPoint();
        if (reverseLoopback) {
            if (isReversing) {
                if (i == 0) {
                    setLastPoint(i + 1);
                    isReversing = false;
                } else {
                    setLastPoint(i - 1);
                }
            } else {
                if (i + 1 >= points.size()) {
                    setLastPoint(i - 1);
                    isReversing = true;
                } else {
                    setLastPoint(i + 1);
                }
            }
        } else {
            if (i + 1 >= points.size()) {
                setLastPoint(0);
            } else {
                setLastPoint(i + 1);
            }
        }
    }

    public void reset() {
        setLastPoint(0);
        isReversing = false;
    }
}
