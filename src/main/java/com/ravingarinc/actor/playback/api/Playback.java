package com.ravingarinc.actor.playback.api;

import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.playback.PathingManager;
import org.bukkit.entity.Player;

public interface Playback {

    @Sync.SyncOnly
    void start(PathingManager manager);

    @Sync.SyncOnly
    void stop(PathingManager manager);

    @Sync.SyncOnly
    void reset(PathingManager manager);

    Vector3 location(Player viewer);
}
