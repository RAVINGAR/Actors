package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.command.CommandOption;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.skin.ActorSkin;
import com.ravingarinc.actor.npc.skin.SkinClient;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.npc.type.PlayerActor;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NPCOption extends CommandOption {
    private final ActorManager manager;

    private final SkinClient client;

    private final List<String> entityTypes;

    public NPCOption(final CommandOption parent, final RavinPlugin plugin) {
        super(parent, 2, (sender, args) -> false);
        this.manager = plugin.getModule(ActorManager.class);
        this.client = plugin.getModule(SkinClient.class);
        this.entityTypes = new ArrayList<>();
        for (final EntityType t : EntityType.values()) {
            entityTypes.add(t.name().toLowerCase());
        }
        registerArguments();
        registerOptions();
    }

    private void registerArguments() {
        registerArgument("--name", 1, (object, args) -> {
            if (object instanceof Actor<?> actor) {
                actor.updateName(args[0]);
            }
        });
        registerArgument("--skin", 1, () -> client.getSkins().stream().toList(), (object, args) -> {
            if (object instanceof PlayerActor playerActor) {
                final ActorSkin skin = client.getSkin(args[0].toLowerCase());
                if (skin != null) {
                    skin.linkActor(playerActor);
                }
            }
        });
    }

    private void registerOptions() {
        addOption("create", 3, (sender, args) -> {
            if (sender instanceof Player player) {
                final EntityType type;
                try {
                    type = EntityType.valueOf(args[2].toUpperCase().replace("-", "_"));
                } catch (final IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Unknown entity type called " + args[2] + "!");
                    return true;
                }
                try {
                    manager.createActor(type, player.getLocation(), parseArguments(3, args));
                } catch (final Argument.InvalidArgumentException exception) {
                    sender.sendMessage(ChatColor.RED + exception.getMessage());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can execute this command!");
            }
            return true;
        }).buildTabCompletions((player, args) -> {
            if (args.length == 3) {
                return entityTypes;
            } else if (args[args.length - 2].startsWith("--")) {
                final Argument argument = getArgumentTypes().get(args[args.length - 2]);
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
                return getArgumentTypes().keySet().stream().toList();
            }
        });

        addOption("update", 3, (sender, args) -> {
            // todo handle things then the last thing you do is call

            //manager.processActorUpdate(actor);
            return true;
        });
    }
}
