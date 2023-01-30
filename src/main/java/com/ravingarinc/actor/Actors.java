package com.ravingarinc.actor;

import com.ravingarinc.actor.command.ActorsCommand;
import com.ravingarinc.actor.npc.ActorListener;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.selector.SelectorManager;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.skin.SkinClient;
import com.ravingarinc.actor.storage.ConfigManager;
import com.ravingarinc.actor.storage.sql.ActorDatabase;
import com.ravingarinc.actor.storage.sql.SkinDatabase;

public final class Actors extends RavinPlugin {
    @Override
    public void loadModules() {

        // add managers
        addModule(ConfigManager.class);
        addModule(ActorManager.class);
        addModule(ActorListener.class);
        addModule(PathingManager.class);

        //load databases
        addModule(SkinClient.class); // this must be after everything since the runner must be cancelled first!
        addModule(SkinDatabase.class);
        addModule(ActorDatabase.class);

        addModule(SelectorManager.class); // this must be after everything so it cancels any tasks
    }

    @Override
    public void loadCommands() {
        new ActorsCommand(this).register(this);
    }
}
