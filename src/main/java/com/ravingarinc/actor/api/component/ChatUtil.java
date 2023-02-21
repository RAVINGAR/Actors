package com.ravingarinc.actor.api.component;

import com.ravingarinc.api.command.BaseCommand;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatUtil {

    public static final String PREFIX = ChatColor.DARK_AQUA + "Actors" + ChatColor.GRAY + " | ";
    public static final String TITLE = ChatColor.GRAY + "------- " + ChatColor.BOLD + ChatColor.AQUA + "$1" + ChatColor.RESET + ChatColor.GRAY + " -------";
    private static final long TIMEOUT = 30000;
    private static final CallbackCommand callback = null;

    public static BaseComponent[] createCallback(final Player player, final String text, final String key, final String hoverValue, final Runnable onClick) {
        final BaseComponent[] components = createCallback(text, ClickEvent.Action.RUN_COMMAND, "/callback" + key, HoverEvent.Action.SHOW_TEXT, new Text(hoverValue));
        if (callback != null) {
            callback.addCallback(player.getUniqueId(), key, onClick);
        }
        return components;
    }

    public static BaseComponent[] createCallback(final String text, final ClickEvent.Action clickAction, final String clickValue, final HoverEvent.Action hoverAction, final Content... hoverValue) {
        return new ComponentBuilder(text)
                .event(new ClickEvent(clickAction, clickValue))
                .event(new HoverEvent(hoverAction, hoverValue))
                .append("")
                .event((ClickEvent) null)
                .event((HoverEvent) null)
                .create();
    }

    public static BaseComponent[] createText(final String text) {
        return new ComponentBuilder(text).create();
    }

    public static void send(final CommandSender receiver, final BaseComponent[]... components) {
        if (components.length > 0) {
            if (receiver instanceof Player player) {
                final ComponentBuilder builder = new ComponentBuilder("");
                for (final BaseComponent[] component : components) {
                    builder.append(component);
                }
                player.spigot().sendMessage(builder.create());
            } else {
                final StringBuilder builder = new StringBuilder();
                for (final BaseComponent[] component : components) {
                    for (final BaseComponent c : component) {
                        builder.append(c.toLegacyText());
                    }
                }
                receiver.sendMessage(builder.toString());
            }
        }
    }

    public static class CallbackCommand extends BaseCommand {
        private final Map<UUID, Map<String, Callback>> callbacks;

        public CallbackCommand() {
            super("callback", null, "Callbacks for command", 2, (sender, args) -> false);
            this.callbacks = new HashMap<>();
            setFunction((sender, args) -> {
                if (sender instanceof Player player) {
                    final Map<String, Callback> map = callbacks.get(player.getUniqueId());
                    if (map == null) {
                        return true;
                    }
                    final Callback call = map.remove(args[1]);
                    if (map.isEmpty()) {
                        callbacks.remove(player.getUniqueId());
                    }
                    if (call == null) {
                        return true;
                    }
                    if (System.currentTimeMillis() >= TIMEOUT + call.creationTime()) {
                        sender.sendMessage(ChatColor.RED + "That option has timed out! Please use the command again!");
                        return true;
                    }
                    call.runnable().run();
                }
                return true;
            });
        }

        protected void addCallback(final UUID uuid, final String key, final Runnable runnable) {
            final Map<String, Callback> map = callbacks.computeIfAbsent(uuid, (u) -> new HashMap<>());
            map.put(key, new Callback(runnable, System.currentTimeMillis()));
        }

        @Override
        public void register(final JavaPlugin plugin) {
            super.register(plugin);
        }
    }

    private record Callback(Runnable runnable, long creationTime) {
    }
}
