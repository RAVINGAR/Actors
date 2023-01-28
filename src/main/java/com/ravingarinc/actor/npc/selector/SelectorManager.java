package com.ravingarinc.actor.npc.selector;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.ModuleListener;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.type.PathMaker;
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
            }
            catch(SelectionFailException e) {
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
        Selector selector = playerSelections.get(player.getUniqueId());
        if(selector == null) {
            return null;
        }
        return selector.getSelection();
    }

    public boolean isSelecting(final Player player) {
        return playerSelections.containsKey(player.getUniqueId());
    }

    public void removeSelection(final Selector selector, boolean resumeLastSelection) {
        Selectable object = selector.getSelection();
        if(object != null) {
            try {
                selector.unselect(resumeLastSelection);
                selector.getPlayer().sendMessage(ChatUtil.PREFIX + "Your current selection has been removed.");
                if(resumeLastSelection) {
                    selector.getPlayer().sendMessage(ChatUtil.PREFIX + "Your previous selection has been resumed.");
                }
            }
            catch(SelectionFailException e) {
                selector.getPlayer().sendMessage(ChatUtil.PREFIX + e.getMessage());
            }
        }
    }

    public void removeSelection(final Player player, boolean resumeLastSelection) {
        Selector selector = playerSelections.get(player.getUniqueId());
        if(selector == null) {
            return;
        }
        removeSelection(selector, resumeLastSelection);
    }

    public void trySelect(final Player player, final Selectable selection, final String messageType) {
        Selector selector = playerSelections.get(player.getUniqueId());
        if(selector != null) {
            this.trySelect(selector, selection, messageType);
        }
    }

    /**
     * Try to select a new object. This will only work if no object is already selected OR if the currently selected object
     * is the same type as the newly selected object!
     */
    public void trySelect(final Selector selector, final Selectable selection, final String messageType) {
        Player player = selector.getPlayer();
        if(selector.getSelection() == null) {
            try {
                selector.select(selection);
                player.sendMessage(ChatUtil.PREFIX + "You are now selecting " + messageType);
            }
            catch(SelectionFailException exception) {
                player.sendMessage(ChatUtil.PREFIX + exception.getMessage());
            }
        }
        else {
            Selectable selected = selector.getSelection();
            if(selected.getClass().isAssignableFrom(selection.getClass())) {
                try {
                    selector.unselect(false);
                    selector.select(selection);
                    player.sendMessage(ChatUtil.PREFIX + "You are now selecting " + messageType);
                }
                catch(SelectionFailException exception) {
                    player.sendMessage(ChatUtil.PREFIX + exception.getMessage());
                }
            }
            else {
                player.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You cannot select that object at this time! Please unselect your current object.");
            }
        }
    }

    //This priority must be lower than the EntityDamageEvent listener in ActorListener
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
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
    public void onInteractEvent(PlayerInteractEvent event) {
        if(event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        if(player.isSneaking() && getSelection(player) instanceof PathMaker path) {
            Block block = event.getClickedBlock();
            int x = block.getX();
            int y = block.getY() + 1;
            int z = block.getZ();
            if(event.getAction() == Action.LEFT_CLICK_BLOCK) {
                 int i = path.getPointIndex(x, y, z);
                 if(i == -1) {
                     int lastSelection = path.getSelection();
                     if(lastSelection == -1) {
                         path.addPoint(x, y, z);
                         player.sendMessage(ChatColor.AQUA + "Added point at '" + x + ", " + y + ", " + z + "' for index " + (path.size()));
                     }
                     else {
                         path.setPoint(lastSelection, x, y, z);
                         player.sendMessage(ChatColor.AQUA + "Set point to '" + x + ", " + y + ", " + z + "' for index " + (lastSelection+1));
                         path.setSelection(-1);
                     }
                 }
                 else {
                     player.sendMessage(ChatColor.AQUA + "Selected point for index " + (i + 1));
                     path.setSelection(i);
                 }
            }
            else if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                int i = path.getSelection();
                if(i == -1) {
                    i = path.getPointIndex(x, y, z);
                    if(i != -1) {
                        path.removePoint(i);
                        player.sendMessage(ChatColor.AQUA + "Removed point at '" + x + ", " + y + ", " + z + "' for index " + (i+1));
                    }
                }
                else {
                    path.setSelection(-1);
                    player.sendMessage(ChatColor.AQUA + "Cancelled selection for point at index " + (i+1));
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerTeleportEvent event) {
        if(isSelecting(event.getPlayer())) {
            if(!event.getTo().getWorld().equals(event.getFrom().getWorld())) {
                removeSelection(event.getPlayer(), false);
            }
        }
    }
}
