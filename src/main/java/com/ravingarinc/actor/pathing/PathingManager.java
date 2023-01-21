package com.ravingarinc.actor.pathing;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.BlockingRunner;
import com.ravingarinc.actor.api.async.DelayedFutureTask;
import com.ravingarinc.actor.npc.ActorManager;

import java.util.concurrent.DelayQueue;

public class PathingManager extends Module {
    private BlockingRunner<DelayedFutureTask> asyncRunner;
    private BlockingRunner<DelayedFutureTask> syncRunner;

    public PathingManager(final RavinPlugin plugin) {
        super(PathingManager.class, plugin, ActorManager.class);
    }

    @Override
    protected void load() throws ModuleLoadException {
        // the general idea is that two runners exist at the same time and are synchronised through a delayed queue.
        // the async runner will send packets every tick, meanwhile the sync runner will actually teleport the entity
        // itself on the sync thread every lets say 20 ticks.
        asyncRunner = new BlockingRunner<>(new DelayQueue<>());
        asyncRunner.runTaskAsynchronously(plugin);

        syncRunner = new BlockingRunner<>(new DelayQueue<>());
        syncRunner.runTask(plugin);
    }

    public void queue() {

    }

    @Override
    public void cancel() {
        asyncRunner.cancel();
        syncRunner.cancel();
    }
}
