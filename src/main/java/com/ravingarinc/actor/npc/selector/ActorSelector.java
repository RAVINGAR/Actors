package com.ravingarinc.actor.npc.selector;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.ModuleListener;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.Actor;
import com.ravingarinc.actor.pathing.type.Path;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActorSelector extends ModuleListener {
    private final Map<UUID, Selector> playerSelections;
    private ActorManager actorManager;

    public ActorSelector(final RavinPlugin plugin) {
        super(ActorSelector.class, plugin, ActorManager.class);
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
            playerSelections.remove(uuid).unselect();
            return false;
        } else {
            playerSelections.put(uuid, new Selector(player));
            return true;
        }
    }

    /**
     * Sets the selection mode of the given player, throws exception is player is not selecting.
     */
    public void setPathSelection(final Player player, final Path path) {
        final Selector selector = playerSelections.get(player.getUniqueId());
        if (selector == null) {
            throw new IllegalStateException("Cannot set selection mode as player is not currently selecting!");
        }
        selector.setMode(Selector.Mode.PATH);
    }

    public void savePathSelection(final Player player) {
        final Selector selector = playerSelections.get(player.getUniqueId());
        if (selector == null) {
            throw new IllegalStateException("Cannot set selection mode as player is not currently selecting!");
        }
        //TODO SAVE OR SOMETIGN
    }

    @Nullable
    public Actor<?> getSelection(final Player player) {
        final Selector selector = playerSelections.get(player.getUniqueId());
        if (selector == null) {
            return null;
        }
        return selector.getSelection();
    }

    public boolean isSelecting(final Player player) {
        return playerSelections.containsKey(player.getUniqueId());
    }

    public void handleSelection(final Player player, final Actor<?> actor) {
        if (playerSelections.containsKey(player.getUniqueId())) {
            final Selector selector = playerSelections.get(player.getUniqueId());
            switch (selector.getMode()) {
                case ACTOR -> {
                    selector.select(actor);
                    player.sendMessage(ChatUtil.PREFIX + "You are now selecting Actor with UUID " + actor.getUUID());
                }
                case PATH ->
                        player.sendMessage(ChatUtil.PREFIX + ChatColor.RED + "You cannot select another actor whilst updating a path!");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteractEvent(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && isSelecting(player)) {
            final Entity entity = event.getEntity();
            final Actor<?> actor = actorManager.getActor(entity.getEntityId());
            if (actor != null) {
                event.setCancelled(true);
                handleSelection(player, actor);
            }
        }
    }
}
