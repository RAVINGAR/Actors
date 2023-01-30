package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.command.CommandOption;
import com.ravingarinc.actor.npc.selector.SelectorManager;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.PathFactory;
import com.ravingarinc.actor.pathing.PathingAgent;
import com.ravingarinc.actor.pathing.PathingManager;
import com.ravingarinc.actor.pathing.type.Path;
import com.ravingarinc.actor.pathing.type.PathMaker;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PathOption extends CommandOption {
    public PathOption(final CommandOption parent, final RavinPlugin plugin) {
        super("path", parent, "actors.path", "", 2, (sender, args) -> false);
        final SelectorManager selector = plugin.getModule(SelectorManager.class);
        final PathingManager manager = plugin.getModule(PathingManager.class);

        addOption("create", 3, (sender, args) -> {
            if (sender instanceof Player player) {
                final Object object = selector.getSelection(player);
                if (object instanceof Actor<?> actor) {
                    final PathingAgent agent = manager.getAgentOrCreate(actor);
                    final Path path = PathFactory.build(args[2], agent);
                    if (path == null) {
                        sender.sendMessage(ChatUtil.PREFIX + "Unknown path type called " + args[2]);
                        return true;
                    }
                    manager.stop(actor);
                    agent.addPath(path);
                    selector.removeSelection(player, false);
                    selector.trySelect(player, path.getPathMaker(manager), "Path");
                    sendPathHelp(sender);
                } else {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You have no actor selected!");
                    if (!selector.isSelecting(player)) {
                        suggestActorSelect(player);
                    }
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
                final Object object = selector.getSelection(player);
                if (object instanceof Actor<?> actor) {
                    final PathingAgent agent = manager.getAgentOrCreate(actor);
                    if (agent.getAmountOfPaths() == 0) {
                        sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You cannot update a path as this actor does not have any! Use /actor path create to make one!");
                        return true;
                    }
                    final int i;
                    try {
                        i = Integer.parseInt(args[2]);
                        if (i <= 0 || i > agent.getAmountOfPaths()) {
                            throw new NumberFormatException("Array index out of bounds!");
                        }
                    } catch (final NumberFormatException e) {
                        sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "That is not a valid path number!");
                        return true;
                    }
                    manager.reset(actor);
                    selector.removeSelection(player, false);
                    selector.trySelect(player, agent.getPath(i - 1).getPathMaker(manager), "Path #" + i);
                    sendPathHelp(player);
                } else {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You have no actor selected!");
                    if (!selector.isSelecting(player)) {
                        sendPathHelp(player);
                    }
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        }).buildTabCompletions((sender, args) -> {
            if (sender instanceof Player player && args.length == 3) {
                final Object object = selector.getSelection(player);
                if (object instanceof Actor<?> actor) {
                    final int size = manager.getAgentOrCreate(actor).getAmountOfPaths();
                    final List<String> list = new ArrayList<>();
                    for (int i = 1; i < size + 1; i++) {
                        list.add("" + i);
                    }
                    return list;
                }
            }
            return new ArrayList<>();
        });

        addOption("save", 2, (sender, args) -> {
            if (sender instanceof Player player) {
                if (selector.getSelection(player) instanceof PathMaker) {
                    player.sendMessage(ChatColor.AQUA + "The path has been saved and applied!");
                    selector.removeSelection(player, true);
                } else {
                    sender.sendMessage(ChatUtil.PREFIX + "You cannot save a path as you have none selected!");
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        });

        addOption("set", "Set an actor's active path.", 3, (sender, args) -> {
            if (sender instanceof Player player) {
                if (selector.getSelection(player) instanceof Actor<?> actor) {
                    final PathingAgent agent = manager.getAgentOrCreate(actor);
                    final int i;
                    try {
                        i = Integer.parseInt(args[2]);
                        if (i <= 0 || i > agent.getAmountOfPaths()) {
                            throw new NumberFormatException("Array index out of bounds!");
                        }
                    } catch (final NumberFormatException e) {
                        sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "That is not a valid path number!");
                        return true;
                    }
                    manager.select(actor, i - 1);
                    sender.sendMessage(ChatColor.AQUA + "Path was selected!");
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        }).buildTabCompletions((sender, args) -> {
            if (sender instanceof Player player && args.length == 3) {
                final Object object = selector.getSelection(player);
                if (object instanceof Actor<?> actor) {
                    final int size = manager.getAgentOrCreate(actor).getAmountOfPaths();
                    final List<String> list = new ArrayList<>();
                    for (int i = 1; i < size + 1; i++) {
                        list.add("" + i);
                    }
                    return list;
                }
            }
            return new ArrayList<>();
        });

        addOption("start", "Start moving an actor along a path.", 2, (sender, args) -> {
            if (sender instanceof Player player) {
                if (selector.getSelection(player) instanceof Actor<?> actor) {
                    sender.sendMessage(ChatColor.AQUA + "Actor's path was stopped!");
                    manager.start(actor);
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        });

        addOption("stop", "Stop moving an actor along a path", 2, (sender, args) -> {
            if (sender instanceof Player player) {
                if (selector.getSelection(player) instanceof Actor<?> actor) {
                    manager.stop(actor);
                    sender.sendMessage(ChatColor.AQUA + "Actor's path was stopped!");
                }
            } else {
                sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        });

        addHelpOption(ChatColor.DARK_AQUA, ChatColor.AQUA);
    }

    private void suggestActorSelect(final CommandSender sender) {
        ChatUtil.send(sender, new ComponentBuilder(ChatColor.GRAY + "Use ")
                .append(ChatColor.GRAY + "/actor selector on")
                .bold(true)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/actor selector on"))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.DARK_GRAY + "Use /actor selector on")))
                .append("")
                .event((ClickEvent) null)
                .event((HoverEvent) null)
                .append(ChatColor.GRAY + " and then left-click an actor to select it!")
                .create());
    }

    private void sendPathHelp(final CommandSender sender) {
        sender.sendMessage(ChatUtil.PREFIX + "Pathing Guide |");
        sender.sendMessage(ChatColor.AQUA + "ADD " + ChatColor.GRAY + "| Shift-Left-Click a block WITHOUT a point to create a new point");
        sender.sendMessage(ChatColor.AQUA + "REMOVE " + ChatColor.GRAY + "| Shift-Right-Click a block WITH a point to remove that point");
        sender.sendMessage(ChatColor.AQUA + "SET " + ChatColor.GRAY + "| Shift-Left-Click a block WITH a point to select that point. Then Shift-Left-Click at another location to move the selected point to that");
    }
}
