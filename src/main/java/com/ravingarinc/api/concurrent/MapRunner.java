package com.ravingarinc.api.concurrent;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class MapRunner<T extends Future<?> & Runnable> extends BukkitRunnable {
    private final Map<UUID, Queue<T>> executables;

    public MapRunner() {
        executables = new ConcurrentHashMap<>();
    }

    public void add(final UUID uuid, final T runnable) {
        final Queue<T> list = executables.computeIfAbsent(uuid, u -> new ConcurrentLinkedQueue<>());
        list.add(runnable);
    }

    public boolean has(final UUID uuid) {
        final Queue<T> queue = executables.get(uuid);
        if (queue == null) {
            return false;
        }
        return !queue.isEmpty();
    }

    public void cancelFor(final UUID uuid) {
        final Queue<T> queue = executables.get(uuid);
        if (queue == null) {
            return;
        }
        queue.forEach(q -> q.cancel(false));
    }

    @Override
    public void run() {
        executables.values().forEach(queue -> {
            final T runnable = queue.poll();
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        executables.values().forEach(queue -> queue.forEach(q -> q.cancel(false)));
        executables.clear();
    }
}
