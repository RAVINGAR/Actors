package com.ravingarinc.actor.pathing.type;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.npc.selector.Selectable;
import com.ravingarinc.actor.npc.selector.SelectionFailException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.LinkedList;
import java.util.List;

public class PathMaker implements Selectable {

    public static final String PATH_INDEX = "path_maker_index";

    private Player player;
    private World world;

    private final Path path;
    private final List<ArmorStand> displayedSelectables;

    private final BukkitScheduler scheduler;

    private final RavinPlugin plugin;

    private int lastSelected = -1;

    private final List<Vector3> points;

    public PathMaker(RavinPlugin plugin, Path path) {
        this.player = null;
        this.displayedSelectables = new LinkedList<>();
        this.path = path;
        this.points = new LinkedList<>();
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();

        this.path.getPoints().forEach(p -> addPoint(p.x, p.y, p.z));
    }

    public void addPoint(double x, double y, double z) {
        points.add(new Vector3(x, y, z));
        addDisplayEffect(x, y, z);
    }

    public void setPoint(int index, double x, double y, double z) {
        points.remove(index);
        points.add(index, new Vector3(x, y, z));
        setDisplayEffect(index, x, y, z);
    }

    public void removePoint(int index) {
        points.remove(index);
        removeDisplayEffect(index);
    }

    public void setSelection(int index) {
        lastSelected = index;
    }

    public int getSelection() {
        return lastSelected;
    }

    public int size() {
        return points.size();
    }

    /**
     * Returns the index of a stored point if it exists in this path. Or -1 if it does not.
     */
    public int getPointIndex(double x, double y, double z) {
        for (int i = 0; i < points.size(); i++) {
            Vector3 vector = points.get(i);
            if (vector.x == x && vector.y == y && vector.z == z) {
                return i;
            }
        }
        return -1;
    }

    public List<Vector3> getPoints() {
        return points;
    }

    @Sync.SyncOnly
    private void addDisplayEffect(double x, double y, double z) {
        ArmorStand armorStand = world.spawn(new Location(world, x, y - 3, z), ArmorStand.class);
        armorStand.setMarker(true);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setCustomName(ChatColor.AQUA + "#" + points.size());
        armorStand.setCustomNameVisible(true);
        armorStand.setMetadata(PATH_INDEX, new FixedMetadataValue(plugin, points.size() - 1));
        armorStand.teleport(new Location(world, x + 0.5, y, z + 0.5));
        displayedSelectables.add(armorStand);
    }

    @Sync.SyncOnly
    private void setDisplayEffect(int index, double x, double y, double z) {
        ArmorStand armorStand = displayedSelectables.get(index);
        armorStand.teleport(new Location(world, x + 0.5, y, z + 0.5));
    }

    @Sync.SyncOnly
    private void removeDisplayEffect(int index) {
        displayedSelectables.remove(index).remove();
        for (int i = index; i < displayedSelectables.size(); i++) {
            displayedSelectables.get(index).setCustomName(ChatColor.AQUA + "#" + (index+1));
        }
    }

    @Override
    @Sync.SyncOnly
    public void onSelect(Player selector) throws SelectionFailException {
        if (player == null) {
            player = selector;
            world = player.getWorld();
        } else {
            throw new SelectionFailException("You cannot select this path as it is currently already selected by another player!");
        }
    }

    @Override
    @Sync.SyncOnly
    public void onUnselect(Player selector) throws SelectionFailException {
        if (player == null) {
            throw new SelectionFailException("You cannot unselect this path as it is already unselected!");
        } else {
            displayedSelectables.forEach(Entity::remove);
            displayedSelectables.clear();

            if (points.size() == 0) {
                selector.sendMessage(ChatUtil.PREFIX + "Path was removed from actor as no points were added!");
                path.getAgent().removePath(path);
            } else {
                scheduler.runTaskAsynchronously(plugin, path::savePathMaker);
            }
        }
    }
}
