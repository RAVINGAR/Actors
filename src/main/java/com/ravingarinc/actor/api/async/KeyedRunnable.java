package com.ravingarinc.actor.api.async;

import org.bukkit.scheduler.BukkitRunnable;

public abstract class KeyedRunnable extends BukkitRunnable implements ConcurrentKeyedQueue.Keyed {
    private final String identifier;

    public KeyedRunnable(final String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getKey() {
        return identifier;
    }
}
