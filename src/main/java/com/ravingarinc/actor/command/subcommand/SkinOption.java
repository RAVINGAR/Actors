package com.ravingarinc.actor.command.subcommand;

import com.ravingarinc.actor.command.CommandOption;

public class SkinOption extends CommandOption {
    public SkinOption(final CommandOption parent) {
        super(parent, 2, (player, args) -> false);

    }
}
