package com.ravingarinc.actor.api.async;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class DelayedFutureTask extends FutureTask<Void> implements Delayed {
    private final long readyTime;

    public DelayedFutureTask(final Runnable runnable, final long delay) {
        super(runnable, null);
        this.readyTime = System.currentTimeMillis() + delay;
    }

    @Override
    public long getDelay(@NotNull final TimeUnit unit) {
        return unit.convert(this.readyTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull final Delayed other) {
        if (other instanceof DelayedFutureTask that) {
            return (int) (this.readyTime - that.readyTime);
        }
        throw new IllegalArgumentException("Cannot compare DelayedEvent to generic Delayed object!");
    }
}
