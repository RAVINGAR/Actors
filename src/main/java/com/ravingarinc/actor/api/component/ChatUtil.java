package com.ravingarinc.actor.api.component;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatUtil {
    public static BaseComponent[] wrapCommandEvent(final String text, final ClickEvent.Action clickAction, final String clickValue, final String hoverValue) {
        return wrapCommandEvent(text, clickAction, clickValue, HoverEvent.Action.SHOW_TEXT, new Text(hoverValue));
    }

    public static BaseComponent[] wrapCommandEvent(final String text, final ClickEvent.Action clickAction, final String clickValue, final HoverEvent.Action hoverAction, final Content... hoverValue) {
        return new ComponentBuilder(text)
                .event(new ClickEvent(clickAction, clickValue))
                .event(new HoverEvent(hoverAction, hoverValue))
                .append("")
                .event((ClickEvent) null)
                .event((HoverEvent) null)
                .create();
    }

    public static void send(final CommandSender receiver, final BaseComponent... components) {
        if (components.length > 0) {
            if (receiver instanceof Player player) {
                final ComponentBuilder builder = new ComponentBuilder("");
                for (final BaseComponent component : components) {
                    builder.append(component);
                }
                player.spigot().sendMessage(builder.create());
            } else {
                for (final BaseComponent component : components) {
                    receiver.sendMessage(component.toLegacyText());
                }
            }
        }
    }
}
