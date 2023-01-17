package com.ravingarinc.actor.api.async;

import com.ravingarinc.actor.api.util.I;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;

public class BlockingRunner<T extends Runnable> extends BukkitRunnable {
    private final BlockingQueue<T> queue;

    public BlockingRunner(final BlockingQueue<T> queue) {
        this.queue = queue;
    }

    public void queue(final T task) {
        this.queue.add(task);
    }

    public Collection<T> getRemaining() {
        return new HashSet<>(this.queue);
    }

    public void queueAll(final Collection<? extends T> collection) {
        this.queue.addAll(collection);
    }

    @Override
    public void run() {
        while (!isCancelled()) {
            try {
                queue.take().run();
            } catch (final InterruptedException e) {
                I.log(Level.SEVERE, "BlockingRunner's queue run() was interrupted!", e);
            }
        }
    }
}
