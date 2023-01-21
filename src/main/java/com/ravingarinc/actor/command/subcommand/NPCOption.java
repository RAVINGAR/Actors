package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.command.CommandOption;
import com.ravingarinc.actor.command.Registry;
import com.ravingarinc.actor.npc.ActorFactory;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.ActorSelector;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.LivingActor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import com.ravingarinc.actor.skin.ActorSkin;
import com.ravingarinc.actor.skin.SkinClient;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NPCOption extends CommandOption {
    private final ActorManager manager;

    private final SkinClient client;

    private final ActorSelector selector;

    public NPCOption(final CommandOption parent, final RavinPlugin plugin) {
        super(parent, 2, (sender, args) -> false);
        this.manager = plugin.getModule(ActorManager.class);
        this.client = plugin.getModule(SkinClient.class);
        this.selector = plugin.getModule(ActorSelector.class);
        registerArguments();
        registerOptions();
    }

    private void registerArguments() {
        Registry.registerArgument(Registry.ACTOR_ARGS, "--name", 1, (sender, object, args) -> {
            if (object instanceof Actor<?> actor) {
                actor.updateName(args[0]);
                return args[0];
            }
            return null;
        });
        Registry.registerArgument(Registry.ACTOR_ARGS, "--skin", 1, () -> client.getSkinNames().stream().toList(), (sender, object, args) -> {
            if (object instanceof PlayerActor playerActor) {
                final String argument = args[0];
                ActorSkin skin;

                try {
                    skin = client.getSkin(UUID.fromString(argument));
                } catch (final IllegalArgumentException ignored) {
                    skin = client.getSkin(argument);
                }
                if (skin == null) {
                    if (sender != null) {
                        sender.sendMessage(ChatColor.RED + "Could not find skin called '" + argument + "'!");
                    }
                    // todo should this be executed on the sync thread?

                } else {
                    client.unlinkActorAll(playerActor);
                    skin.linkActor(playerActor);
                    return skin.getUUID().toString();
                }
            }
            return null;
        });

        Registry.registerArgument(Registry.ACTOR_ARGS, "--invuln", 1,
                () -> Arrays.stream((new String[]{"true", "false"})).toList(),
                (sender, object, args) -> {
                    if (object instanceof LivingActor livingActor) {
                        if (args[0].equalsIgnoreCase("true")) {
                            livingActor.setInvuln(true);
                        } else if (args[0].equalsIgnoreCase("false")) {
                            livingActor.setInvuln(false);
                        }
                    }
                    return null;
                });
    }

    private void registerOptions() {
        addOption("create", 3, (sender, args) -> {
            if (sender instanceof Player player) {
                final String argType = args[2].toLowerCase().replace("-", "_");
                if (!ActorFactory.getTypes().contains(argType)) {
                    sender.sendMessage(ChatColor.RED + "Unknown actor type called " + args[2] + "!");
                    return true;
                }
                try {
                    final Argument[] arguments = Registry.parseArguments(Registry.ACTOR_ARGS, 3, args, sender);
                    final Vector3 location = new Vector3(player.getLocation());
                    AsyncHandler.runAsynchronously(() -> manager.createNewActor(argType, location, arguments));
                    sender.sendMessage(ChatColor.DARK_AQUA + "Created a new NPC with the given arguments!");
                } catch (final Argument.InvalidArgumentException exception) {
                    sender.sendMessage(ChatColor.RED + exception.getMessage());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can execute this command!");
            }
            return true;
        }).buildTabCompletions((player, args) -> {
            if (args.length == 3) {
                return ActorFactory.getTypes();
            } else if (args[args.length - 2].startsWith("--")) {
                final Argument argument = Registry.getArgumentTypes(Registry.ACTOR_ARGS).get(args[args.length - 2]);
                if (argument == null) {
                    return null;
                }
                List<String> list = argument.getTabCompletions();
                if (list == null) {
                    list = new ArrayList<>();
                    list.add("<" + args[args.length - 2].substring(2) + ">");
                }
                return list;
            } else {
                return Registry.getArgumentTypes(Registry.ACTOR_ARGS).keySet().stream().toList();
            }
        });

        addOption("update", 3, (sender, args) -> {
            if (sender instanceof Player player) {
                final Actor<?> actor = selector.getSelection(player);
                if (actor == null) {
                    sender.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You have no actor selected!");
                    if (selector.isSelecting(player)) {
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
                    try {
                        final Argument[] arguments = Registry.parseArguments(Registry.ACTOR_ARGS, 2, args, sender);
                        actor.applyArguments(arguments);
                        actor.update(manager);
                        sender.sendMessage(ChatUtil.PREFIX + ChatColor.AQUA + "Successfully applied update! The actor now has the following arguments;");
                        actor.getAppliedArguments().forEach(arg -> {
                            sender.sendMessage(ChatColor.GRAY + arg);
                        });
                    } catch (final Argument.InvalidArgumentException e) {
                        sender.sendMessage(ChatColor.RED + e.getMessage());
                    }
                }
            } else {
                sender.sendMessage(ChatColor.DARK_AQUA + "Actors | " + ChatColor.RED + "This command can only be used by a player!");
            }
            return true;
        }).buildTabCompletions((player, args) -> {
            if (args[args.length - 2].startsWith("--")) {
                final Argument argument = Registry.getArgumentTypes(Registry.ACTOR_ARGS).get(args[args.length - 2]);
                if (argument == null) {
                    return null;
                }
                List<String> list = argument.getTabCompletions();
                if (list == null) {
                    list = new ArrayList<>();
                    list.add("<" + args[args.length - 2].substring(2) + ">");
                }
                return list;
            } else {
                return Registry.getArgumentTypes(Registry.ACTOR_ARGS).keySet().stream().toList();
            }
        });
    }
}
