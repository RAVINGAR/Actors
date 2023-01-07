package com.ravingarinc.actor.command;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.command.subcommand.NPCOption;
import com.ravingarinc.actor.command.subcommand.SkinOption;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.SkinClient;

public class ParentCommand extends BaseCommand {

    public ParentCommand(final RavinPlugin plugin) {
        super("actors");

        // The arguments count basically after the initial identifier
        // aka /actors reload will be considered 2 arguments, args[0] will return reload, meanwhile [1] will be whatever
        // the user is currently typing.
        addOption("reload", 2, (sender, args) -> {
            plugin.reload();
            return true;
        });

        addOption("npc", new NPCOption(this, plugin.getModule(ActorManager.class)));

        addOption("skin", new SkinOption(this, plugin.getModule(SkinClient.class)));
    }
}
