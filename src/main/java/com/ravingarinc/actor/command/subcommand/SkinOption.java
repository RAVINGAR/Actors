package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.command.ChatComponents;
import com.ravingarinc.actor.command.CommandOption;
import com.ravingarinc.actor.npc.skin.SkinClient;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SkinOption extends CommandOption {
    public SkinOption(final CommandOption parent, final SkinClient client) {
        super(parent, 2, (player, args) -> false);

        addOption("upload", 3, (sender, args) -> {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Incorrect usage - /actors skin upload <file> <name>");
                return true;
            }
            final String fileName = args[2];
            boolean valid = false;
            for (final String extension : client.getValidExtensions()) {
                if (fileName.endsWith(extension)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                sender.sendMessage(ChatColor.RED + "The file '" + fileName + "' cannot be used to upload skins. The file must end with .png, .jpg or .jpeg!");
                return true;
            }

            final String name = args.length > 3 ? args[3] : fileName.substring(0, fileName.indexOf("."));
            if (client.getSkin(name) == null) {
                try {
                    client.uploadSkin(sender, new File(client.getSkinFolder(), fileName), name);
                    sender.sendMessage(ChatColor.GRAY + "The file " + fileName + " is being uploaded as  id '" + args[3] + "'");
                } catch (final FileNotFoundException e) {
                    sender.sendMessage(ChatColor.RED + "Could not find image file called '" + fileName + '!');
                }
            } else {
                sender.sendMessage(ChatColor.RED + "A skin called '%s'" + name + "' already exists! " +
                        "If you wish to update this skin with a new texture, please use /actors skin update <file> <name>");
            }
            return true;
        }).buildTabCompletions((sender, args) -> {
            final List<String> list = new ArrayList<>();
            if (args.length == 3) {
                return client.getValidFileNames();
            } else if (args.length == 4) {
                list.add("<name>");
                return list;
            } else {
                return list;
            }
        });

        addOption("update", 3, (sender, args) -> {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Incorrect usage - /actors skin update <file> <name>");
                return true;
            }
            final String fileName = args[2];
            boolean valid = false;
            for (final String extension : client.getValidExtensions()) {
                if (fileName.endsWith(extension)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                sender.sendMessage(ChatColor.RED + "The file '" + fileName + "' cannot be used to upload skins. The file must end with .png, .jpg or .jpeg!");
                return true;
            }
            try {
                client.uploadSkin(sender, new File(client.getSkinFolder(), fileName), args[3]);
                sender.sendMessage(ChatColor.GRAY + "The file " + fileName + " is being uploaded to id '" + args[3] + "'");
            } catch (final FileNotFoundException e) {
                sender.sendMessage(ChatColor.RED + "Could not find image file called '" + fileName + '!');
            }
            return true;
        }).buildTabCompletions((sender, args) -> {
            if (args.length == 3) {
                return client.getValidFileNames();
            } else if (args.length == 4) {
                return client.getSkins().stream().toList();
            } else {
                return new ArrayList<>();
            }
        });

        addOption("save", 3, (sender, args) -> {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Incorrect usage - /actors skin save <url> <name>");
                return true;
            }
            client.saveSkin(sender, args[2], args[3]);
            sender.sendMessage(ChatColor.GRAY + "The url " + args[2] + " is being saved to id '" + args[3] + "'");
            return true;
        });

        addOption("list", 2, (sender, args) -> {
            sender.sendMessage(ChatComponents.TITLE);
            sender.sendMessage(ChatColor.DARK_GRAY + "Currently Loaded -->");
            client.getSkins().forEach(skin -> sender.sendMessage(ChatColor.GRAY + "- " + skin));
            return true;
        }).buildTabCompletions((sender, args) -> new ArrayList<>());
    }
}
