package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.command.CommandOption;
import com.ravingarinc.actor.npc.SkinClient;
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
            for (final String extension : client.getValidExtensions()) {
                if (!fileName.endsWith(extension)) {
                    sender.sendMessage(ChatColor.RED + "The file '" + fileName + "' cannot be used to upload skins. The file must end with .png, .jpg or .jpeg!");
                    return true;
                }
            }
            final File file = new File(client.getSkinFolder(), fileName);
            final String name = args.length > 3 ? args[3] : fileName.substring(0, fileName.indexOf("."));
            try {
                client.uploadSkin(sender, file, name);
            } catch (final FileNotFoundException e) {
                sender.sendMessage(ChatColor.RED + "Could not find image file called '" + fileName + '!');
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
    }
}
