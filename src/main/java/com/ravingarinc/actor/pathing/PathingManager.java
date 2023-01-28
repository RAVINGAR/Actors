package com.ravingarinc.actor.pathing;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.BlockingRunner;
import com.ravingarinc.actor.api.async.DelayedFutureTask;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.Actor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

public class PathingManager extends Module {
    private final Map<UUID, PathingAgent> agents;
    private BlockingRunner<DelayedFutureTask> asyncRunner;

    public PathingManager(final RavinPlugin plugin) {
        super(PathingManager.class, plugin, ActorManager.class);
        this.agents = new ConcurrentHashMap<>();
    }

    @Override
    protected void load() throws ModuleLoadException {
        // the general idea is that two runners exist at the same time and are synchronised through a delayed queue.
        // the async runner will send packets every tick, meanwhile the sync runner will actually teleport the entity
        // itself on the sync thread every lets say 20 ticks.
        asyncRunner = new BlockingRunner<>(new DelayQueue<>());
        asyncRunner.runTaskAsynchronously(plugin);
    }

    public PathingAgent getAgent(Actor<?> actor) {
        return agents.computeIfAbsent(actor.getUUID(), uuid -> new PathingAgent(actor, this));
    }

    public void queue() {

    }

    @Override
    public void cancel() {
        DelayedFutureTask delayFuture = new DelayedFutureTask(asyncRunner.getCancelTask(), 0);
        asyncRunner.queue(delayFuture);
        AsyncHandler.waitForFuture(delayFuture);

        agents.clear();
    }
}
