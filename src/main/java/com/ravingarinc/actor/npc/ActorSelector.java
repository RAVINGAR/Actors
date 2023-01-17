package com.ravingarinc.actor.npc;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.ModuleListener;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.npc.type.Actor;
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
    private final Map<UUID, UUID> playerSelections;
    private ActorManager actorManager;

    public ActorSelector(final RavinPlugin plugin) {
        super(ActorSelector.class, plugin, ActorManager.class);
        this.playerSelections = new HashMap<>();
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
            playerSelections.remove(uuid);
            return false;
        } else {
            playerSelections.put(uuid, null);
            return true;
        }
    }

    @Nullable
    public Actor<?> getSelection(final Player player) {
        if (isSelecting(player)) {
            final UUID uuid = playerSelections.get(player.getUniqueId());
            if (uuid == null) {
                return null;
            }
            return actorManager.getActor(uuid);
        }
        return null;
    }

    public boolean isSelecting(final Player player) {
        return playerSelections.containsKey(player.getUniqueId());
    }

    public void addSelection(final Player player, final Actor<?> actor) {
        playerSelections.put(player.getUniqueId(), actor.getUUID());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteractEvent(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && isSelecting(player)) {
            final Entity entity = event.getEntity();
            final Actor<?> actor = actorManager.getActor(entity.getEntityId());
            if (actor != null) {
                event.setCancelled(true);
                addSelection(player, actor);
            }
        }
    }
}
