package com.ravingarinc.actor.playback;

import com.ravingarinc.actor.npc.selector.Selectable;
import com.ravingarinc.actor.npc.selector.SelectionFailException;
import com.ravingarinc.api.Sync;
import org.bukkit.entity.Player;

public abstract class PlaybackBuilder implements Selectable {
    @Override
    @Sync.SyncOnly
    public void onSelect(final Player selector) throws SelectionFailException {
        //todo move stuff from pathmaker to here
    }

    @Override
    @Sync.SyncOnly
    public void onUnselect(final Player selector) throws SelectionFailException {
        //todo move stuff from pathmaker to here!
    }

    public abstract PathingAgent getOwningAgent();

    public abstract void save();


}
