package com.ravingarinc.actor;

import com.ravingarinc.actor.command.ActorsCommand;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.ActorPacketInterceptor;
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
        addModule(SkinClient.class);
        addModule(ActorPacketInterceptor.class);

        //load databases
        addModule(SkinDatabase.class);
        addModule(ActorDatabase.class);
        //addModule(SQLHandler.class); // comment out if not needed
        // add listeners

    }

    @Override
    public void loadCommands() {
        new ActorsCommand(this).register(this);
    }
}
