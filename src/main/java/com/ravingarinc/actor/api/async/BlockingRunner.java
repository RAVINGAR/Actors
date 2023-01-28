package com.ravingarinc.actor.api.async;

import com.ravingarinc.actor.api.util.I;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingRunner<T extends Future<?> & Runnable> extends BukkitRunnable {
    private final BlockingQueue<T> queue;

    private final AtomicBoolean cancelled;

    public BlockingRunner(final BlockingQueue<T> queue) {
        this.queue = queue;
        this.cancelled = new AtomicBoolean(false);
    }

    public <V extends T> void queue(final V task) {
        if(!this.cancelled.getAcquire()) {
            this.queue.add(task);
        }
    }

    public Collection<T> getRemaining() {
        return new HashSet<>(this.queue);
    }

    public void queueAll(final Collection<T> collection) {
        if(!this.cancelled.getAcquire()) {
            this.queue.addAll(collection);
        }
    }

    @Override
    public void run() {
        while (!isCancelled() && !cancelled.getAcquire()) {
            try {
                T item = queue.take();
                if(!item.isCancelled()) {
                    item.run();
                }
            } catch (final InterruptedException e) {
                I.logIfDebug(() -> "BlockingRunner task was interrupted! This may be expected!", e);
            }
        }
    }

    /**
     * It is expected that after {@link #cancel(boolean)} is called, that this is queued as the next task.
     * @return The cancel runnable
     */
    public synchronized Runnable getCancelTask() {
        return () -> {
            cancelled.setRelease(true);
            if(!isCancelled()) {
                super.cancel();
            }
        };
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        this.cancel(false);
    }

    public synchronized void cancel(boolean mayInterruptIfRunning) throws IllegalStateException {
        super.cancel();
        getRemaining().forEach(task -> task.cancel(mayInterruptIfRunning));
    }
}
