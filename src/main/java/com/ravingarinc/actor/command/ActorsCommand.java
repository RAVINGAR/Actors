package com.ravingarinc.actor.command;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.command.subcommand.NPCOption;
import com.ravingarinc.actor.command.subcommand.PathOption;
import com.ravingarinc.actor.command.subcommand.SkinOption;
import com.ravingarinc.actor.npc.selector.ActorSelector;
import com.ravingarinc.actor.skin.SkinClient;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ActorsCommand extends BaseCommand {

    private final ActorSelector selector;

    public ActorsCommand(final RavinPlugin plugin) {
        super("actor", null);

        selector = plugin.getModule(ActorSelector.class);

        // The arguments count basically after the initial identifier
        // aka /actors reload will be considered 2 arguments, args[0] will return reload, meanwhile [1] will be whatever
        // the user is currently typing.
        addOption("reload", "actors.reload", "", 1, (sender, args) -> {
            plugin.reload();
            sender.sendMessage(ChatUtil.PREFIX + "Plugin has been reloaded!");
            return true;
        });

        addOption("selector", "actors.selector", "", 1, (sender, args) -> {
            if (sender instanceof Player player) {
                if (selector.toggleSelecting(player)) {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.DARK_AQUA + "Selector - ON " + ChatColor.GRAY + "| You can now LEFT-CLICK any Actors to select them!");
                } else {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.DARK_AQUA + "Selector - OFF " + ChatColor.GRAY + "| Actor Selection on LEFT-CLICK is now disabled.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        }).addOption("on", 2, (sender, args) -> {
            if (sender instanceof Player player) {
                if (selector.isSelecting(player)) {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.DARK_AQUA + "Selector - ON " + ChatColor.AQUA + "| Actor Selection is already enabled!");
                } else {
                    selector.toggleSelecting(player);
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.DARK_AQUA + "Selector - ON " + ChatColor.AQUA + "| You can now LEFT-CLICK any Actors to select them!");
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        }).getParent().addOption("off", 2, (sender, args) -> {
            if (sender instanceof Player player) {
                if (selector.isSelecting(player)) {
                    selector.toggleSelecting(player);
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.DARK_AQUA + "Selector - OFF " + ChatColor.GRAY + "| Actor Selection on LEFT-CLICK is now disabled.");
                } else {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.DARK_AQUA + "Selector - OFF " + ChatColor.GRAY + "| Actor Selection is already disabled!");
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        });

        addOption("npc", new NPCOption(this, plugin));

        addOption("skin", new SkinOption(this, plugin.getModule(SkinClient.class)));

        addOption("paths", new PathOption(this, plugin.getModule(ActorSelector.class)));

        addHelpOption(ChatColor.DARK_AQUA, ChatColor.AQUA);
    }
}
