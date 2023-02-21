package com.ravingarinc.api.concurrent.key;

import org.bukkit.scheduler.BukkitRunnable;

public abstract class KeyedRunnable extends BukkitRunnable implements Keyed<String> {
    private final String identifier;

    public KeyedRunnable(final String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getKey() {
        return identifier;
    }
}
