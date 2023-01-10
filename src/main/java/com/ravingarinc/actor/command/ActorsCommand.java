package com.ravingarinc.actor.command;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.command.subcommand.NPCOption;
import com.ravingarinc.actor.command.subcommand.SkinOption;
import com.ravingarinc.actor.npc.skin.SkinClient;
import org.bukkit.ChatColor;

public class ActorsCommand extends BaseCommand {

    public ActorsCommand(final RavinPlugin plugin) {
        super("actors");

        // The arguments count basically after the initial identifier
        // aka /actors reload will be considered 2 arguments, args[0] will return reload, meanwhile [1] will be whatever
        // the user is currently typing.
        addOption("reload", 2, (sender, args) -> {
            plugin.reload();
            sender.sendMessage(ChatColor.GRAY + "Actors has been reloaded!");
            return true;
        });

        addOption("npc", new NPCOption(this, plugin));

        addOption("skin", new SkinOption(this, plugin.getModule(SkinClient.class)));
    }
}
