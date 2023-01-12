package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.command.CommandOption;
import com.ravingarinc.actor.command.Registry;
import com.ravingarinc.actor.npc.ActorFactory;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.skin.ActorSkin;
import com.ravingarinc.actor.npc.skin.SkinClient;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NPCOption extends CommandOption {
    private final ActorManager manager;

    private final SkinClient client;

    public NPCOption(final CommandOption parent, final RavinPlugin plugin) {
        super(parent, 2, (sender, args) -> false);
        this.manager = plugin.getModule(ActorManager.class);
        this.client = plugin.getModule(SkinClient.class);
        registerArguments();
        registerOptions();
    }

    private void registerArguments() {
        Registry.registerArgument(Registry.ACTOR_ARGS, "--name", 1, (object, args) -> {
            if (object instanceof Actor<?> actor) {
                actor.updateName(args[0]);
            }
        });
        Registry.registerArgument(Registry.ACTOR_ARGS, "--skin", 1, () -> client.getSkins().stream().toList(), (object, args) -> {
            if (object instanceof PlayerActor playerActor) {
                // todo make this --skin actually parse as a uuid when saved to the actor for the database!
                final ActorSkin skin = client.getSkin(args[0]);
                if (skin != null) {
                    skin.linkActor(playerActor);
                }
            }
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
                    final Argument[] arguments = Registry.parseArguments(Registry.ACTOR_ARGS, 3, args);
                    final Vector3 location = new Vector3(player.getLocation());
                    AsyncHandler.runAsynchronously(() -> manager.createActor(argType, location, arguments));
                    sender.sendMessage(ChatColor.GREEN + "Created a new NPC with the given arguments!");
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
            // todo handle things then the last thing you do is call

            //manager.processActorUpdate(actor);
            return true;
        });
    }
}
