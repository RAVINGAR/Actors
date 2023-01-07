package com.ravingarinc.actor;

import com.comphenix.protocol.ProtocolLibrary;
import com.ravingarinc.actor.command.ParentCommand;
import com.ravingarinc.actor.command.TestCommand;
import com.ravingarinc.actor.file.ConfigManager;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.ActorPacketInterceptor;

public final class Actors extends RavinPlugin {
    @Override
    public void loadModules() {

        // add managers
        addModule(ConfigManager.class);
        addModule(ActorManager.class);
        addModule(ActorPacketInterceptor.class);
        //addModule(SQLHandler.class); // comment out if not needed
        // add listeners

    }

    @Override
    public void loadCommands() {
        new ParentCommand(this).register(this);
        new TestCommand(ProtocolLibrary.getProtocolManager());
    }
}
