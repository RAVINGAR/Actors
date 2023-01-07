package com.ravingarinc.actor.api;

import com.ravingarinc.actor.RavinPlugin;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ModuleListener extends Module implements Listener {

    @SafeVarargs
    protected ModuleListener(final Class<? extends Module> identifier, final RavinPlugin plugin, final Class<? extends Module>... dependsOn) {
        super(identifier, plugin, dependsOn);
    }

    @Override
    public void cancel() {
        HandlerList.unregisterAll(this);
    }

    @Override
    protected void load() throws ModuleLoadException {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
