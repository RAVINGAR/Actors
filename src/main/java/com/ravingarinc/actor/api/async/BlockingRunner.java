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

    public Collection<Runnable> getRemaining() {
        return new HashSet<>(this.queue);
    }

    @Override
    public void run() {
        if (!isCancelled()) {
            try {
                queue.take().run();
            } catch (final InterruptedException e) {
                I.log(Level.SEVERE, "BlockingRunner's queue run() was interrupted!", e);
            }
            run();
        } else {
            I.log(Level.WARNING, "DEBUG -> BlockingRunner was cancelled but still runs run()!");
        }
    }
}
