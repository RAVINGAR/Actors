package com.ravingarinc.actor.npc.selector;

import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.playback.PlaybackBuilder;
import com.ravingarinc.actor.playback.path.PathMaker;
import com.ravingarinc.api.I;
import com.ravingarinc.api.module.ModuleListener;
import com.ravingarinc.api.module.ModuleLoadException;
import com.ravingarinc.api.module.RavinPlugin;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SelectorManager extends ModuleListener {
    private final Map<UUID, Selector> playerSelections;
    private ActorManager actorManager;

    /**
     * This selector manager is mainly treated as Sync Only
     *
     * @param plugin
     */
    public SelectorManager(final RavinPlugin plugin) {
        super(SelectorManager.class, plugin, ActorManager.class);
        this.playerSelections = new HashMap<>();
        // todo when we add asynchronous ActorRemove events, make sure that a players selector has the given actor removed
    }

    @Override
    public void load() throws ModuleLoadException {
        this.actorManager = plugin.getModule(ActorManager.class);
        super.load();
    }

    @Override
    public void cancel() {
        super.cancel();
        this.playerSelections.values().forEach(selector -> {
            try {
                selector.unselect(false);
            } catch (final SelectionFailException e) {
                I.log(Level.WARNING, "Encountered exception removing selections..", e);
            }
        });
        this.playerSelections.clear();
    }

    /**
     * Toggles a players interactions for selecting.
     *
     * @param player The player
     * @return true if player is now selecting, false if not
     */
    public boolean toggleSelecting(final Player player) {
        final UUID uuid = player.getUniqueId();
        if (playerSelections.containsKey(uuid)) {
            removeSelection(playerSelections.remove(uuid), false);
            return false;
        } else {
            playerSelections.put(uuid, new Selector(player));
            return true;
        }
    }

    @Nullable
    public Selectable getSelection(final Player player) {
        final Selector selector = playerSelections.get(player.getUniqueId());
        if (selector == null) {
            return null;
        }
        return selector.getSelection();
    }

    public void removeActorSelection(final Actor<?> actor) {
        playerSelections.values().forEach(selector -> {
            if (selector.getSelection() instanceof Actor<?> found && found.equals(actor)) {
                removeSelection(selector, false);
            }
            if (selector.getSelection() instanceof PlaybackBuilder builder && builder.getOwningAgent().getActor().equals(actor)) {
                removeSelection(selector, false);
            }
        });
    }

    public boolean isSelecting(final Player player) {
        return playerSelections.containsKey(player.getUniqueId());
    }

    public void removeSelection(final Selector selector, final boolean resumeLastSelection) {
        final Selectable object = selector.getSelection();
        if (object != null) {
            try {
                selector.unselect(resumeLastSelection);
                selector.getPlayer().sendMessage(ChatUtil.PREFIX + "Your current selection has been removed.");
                if (resumeLastSelection) {
                    selector.getPlayer().sendMessage(ChatUtil.PREFIX + "Your previous selection has been resumed.");
                }
            } catch (final SelectionFailException e) {
                selector.getPlayer().sendMessage(ChatUtil.PREFIX + e.getMessage());
            }
        }
    }

    public void removeSelection(final Player player, final boolean resumeLastSelection) {
        final Selector selector = playerSelections.get(player.getUniqueId());
        if (selector == null) {
            return;
        }
        removeSelection(selector, resumeLastSelection);
    }

    public void trySelect(final Player player, final Selectable selection, final String messageType) {
        final Selector selector = playerSelections.get(player.getUniqueId());
        if (selector != null) {
            this.trySelect(selector, selection, messageType);
        }
    }

    /**
     * Try to select a new object. This will only work if no object is already selected OR if the currently selected object
     * is the same type as the newly selected object!
     */
    public void trySelect(final Selector selector, final Selectable selection, final String messageType) {
        final Player player = selector.getPlayer();
        if (selector.getSelection() == null) {
            try {
                selector.select(selection);
                player.sendMessage(ChatUtil.PREFIX + "You are now selecting " + messageType);
            } catch (final SelectionFailException exception) {
                player.sendMessage(ChatUtil.PREFIX + exception.getMessage());
            }
        } else {
            final Selectable selected = selector.getSelection();
            if (selected.getClass().isAssignableFrom(selection.getClass())) {
                try {
                    selector.unselect(false);
                    selector.select(selection);
                    player.sendMessage(ChatUtil.PREFIX + "You are now selecting " + messageType);
                } catch (final SelectionFailException exception) {
                    player.sendMessage(ChatUtil.PREFIX + exception.getMessage());
                }
            } else {
                player.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You cannot select that object at this time! Please unselect your current object.");
            }
        }
    }

    //This priority must be lower than the EntityDamageEvent listener in ActorListener
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageEvent(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && isSelecting(player)) {
            final Entity entity = event.getEntity();
            final Actor<?> actor = actorManager.getActor(entity.getEntityId());
            if (actor != null) {
                event.setCancelled(true);
                trySelect(playerSelections.get(player.getUniqueId()), actor, "Actor with UUID " + actor.getUUID());
                return;
            }

            /* TODO Might not need this, see if this fires
            if(entity instanceof ArmorStand && entity.hasMetadata(Path.PathMaker.PATH_INDEX)) {
                int index = entity.getMetadata(Path.PathMaker.PATH_INDEX).get(0).asInt();

            }*/
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEvent(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }

        final Player player = event.getPlayer();
        if (player.isSneaking() && getSelection(player) instanceof PathMaker path) {
            final Block block = event.getClickedBlock();
            final int x = block.getX();
            final int y = block.getY() + 1;
            final int z = block.getZ();
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                final int i = path.getPointIndex(x, y, z);
                if (i == -1) {
                    final int lastSelection = path.getSelection();
                    if (lastSelection == -1) {
                        path.addPoint(x, y, z);
                        player.sendMessage(ChatColor.AQUA + "Added point at '" + x + ", " + y + ", " + z + "' for index " + (path.size()));
                        event.setCancelled(true);
                    } else {
                        path.setPoint(lastSelection, x, y, z);
                        player.sendMessage(ChatColor.AQUA + "Set point to '" + x + ", " + y + ", " + z + "' for index " + (lastSelection + 1));
                        path.setSelection(-1);
                        event.setCancelled(true);
                    }
                } else {
                    player.sendMessage(ChatColor.AQUA + "Selected point for index " + (i + 1));
                    path.setSelection(i);
                    event.setCancelled(true);
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                int i = path.getSelection();
                if (i == -1) {
                    i = path.getPointIndex(x, y, z);
                    if (i != -1) {
                        path.removePoint(i);
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.AQUA + "Removed point at '" + x + ", " + y + ", " + z + "' for index " + (i + 1));
                    }
                } else {
                    path.setSelection(-1);
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.AQUA + "Cancelled selection for point at index " + (i + 1));
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(final PlayerTeleportEvent event) {
        if (isSelecting(event.getPlayer())) {
            if (!event.getTo().getWorld().equals(event.getFrom().getWorld())) {
                removeSelection(event.getPlayer(), false);
            }
        }
    }
}
