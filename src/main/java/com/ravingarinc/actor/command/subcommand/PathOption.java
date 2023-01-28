package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.command.CommandOption;
import com.ravingarinc.actor.npc.selector.ActorSelector;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.PathFactory;
import com.ravingarinc.actor.pathing.PathingAgent;
import com.ravingarinc.actor.pathing.type.Path;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PathOption extends CommandOption {
    public PathOption(final CommandOption parent, final ActorSelector selector) {
        super("path", parent, "actors.path", "", 2, (sender, args) -> false);

        addOption("create", 3, (sender, args) -> {
            if (sender instanceof Player player) {
                final Actor<?> actor = selector.getSelection(player);
                if (actor == null) {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You have no actor selected!");
                    if (!selector.isSelecting(player)) {
                        ChatUtil.send(player, new ComponentBuilder(ChatColor.GRAY + "Use ")
                                .append("/actor selector on")
                                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/actor selector on"))
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_GRAY + "<click to autofill command>")))
                                .append(" and then left-click an actor to select it!")
                                .event((ClickEvent) null)
                                .event((HoverEvent) null)
                                .create());
                    }
                } else {
                    final PathingAgent agent = actor.getPathingAgent();
                    final Path path = PathFactory.build(args[3], agent);
                    if (path == null) {
                        sender.sendMessage(ChatUtil.PREFIX + "Unknown path type called " + args[3]);
                        return true;
                    }
                    //selector.setPathSelection(player, Selector.Mode.PATH); // TODO FIX UP THIS
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.AQUA + "Entered Path Creation Mode" + ChatColor.GRAY + " | When ");
                }
            } else {
                sender.sendMessage(ChatColor.DARK_AQUA + "Actors | " + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        }).buildTabCompletions((sender, args) -> {
            if (args.length == 2) {
                return PathFactory.getTypes();
            }
            return new ArrayList<>();
        });
        addOption("update", 3, (sender, args) -> {
            if (sender instanceof Player player) {
                final Actor<?> actor = selector.getSelection(player);
                if (actor == null) {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You have no actor selected!");
                    if (!selector.isSelecting(player)) {
                        ChatUtil.send(player, new ComponentBuilder(ChatColor.GRAY + "Use ")
                                .append("/actor selector on")
                                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/actor selector on"))
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_GRAY + "<click to autofill command>")))
                                .append(" and then left-click an actor to select it!")
                                .event((ClickEvent) null)
                                .event((HoverEvent) null)
                                .create());
                    }
                } else {
                    final PathingAgent agent = actor.getPathingAgent();
                    if (agent.getAmountOfPaths() == 0) {
                        sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You cannot update a path as this actor does not have any!");
                        return true;
                    }
                    int i = -1;
                    try {
                        i = Integer.parseInt(args[3]);
                        if (i <= 0 || i > agent.getAmountOfPaths()) {
                            throw new NumberFormatException("Array out of bound!");
                        }
                    } catch (final NumberFormatException e) {
                        sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "That is not a valid path number!");
                        return true;
                    }


                    return true;
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        }).buildTabCompletions((sender, args) -> {
            if (sender instanceof Player player && args.length == 3) {
                final Actor<?> actor = selector.getSelection(player);
                if (actor != null) {
                    final int size = actor.getPathingAgent().getAmountOfPaths();
                    final List<String> list = new ArrayList<>();
                    for (int i = 1; i < size + 1; i++) {
                        list.add("" + i);
                    }
                    return list;
                }
            }
            return new ArrayList<>();
        });

        addHelpOption(ChatColor.DARK_AQUA, ChatColor.AQUA);
    }
}
