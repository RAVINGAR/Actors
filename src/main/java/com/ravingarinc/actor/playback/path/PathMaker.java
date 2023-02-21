package com.ravingarinc.actor.playback.path;

import com.ravingarinc.actor.api.component.ChatUtil;
import com.ravingarinc.actor.npc.selector.SelectionFailException;
import com.ravingarinc.actor.playback.PathingAgent;
import com.ravingarinc.actor.playback.PathingManager;
import com.ravingarinc.actor.playback.PlaybackBuilder;
import com.ravingarinc.actor.playback.api.Movement;
import com.ravingarinc.api.Sync;
import com.ravingarinc.api.Vector3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PathMaker extends PlaybackBuilder {

    public static final String PATH_INDEX = "path_maker_index";
    private final Path path;
    private final List<ArmorStand> displayedSelectables;
    private final PathingManager manager;
    private final List<Vector3> points;
    private Player player;
    private World world;
    private int lastSelected = -1;

    private boolean wasChanged = false;

    public PathMaker(final PathingManager manager, final Path path) {
        this.player = null;
        this.displayedSelectables = new LinkedList<>();
        this.path = path;
        this.points = new LinkedList<>();
        this.manager = manager;
        for (final Movement movement : this.path.getFrames()) {
            final Vector3 initial = movement.point();
            points.add(new Vector3(initial.x, initial.y, initial.z));
        }
    }

    public void addPoint(final double x, final double y, final double z) {
        wasChanged = true;
        points.add(new Vector3(x + 0.5, y, z + 0.5, 0, 0, world));
        addDisplayEffect(x + 0.5, y, z + 0.5);
    }

    public void setPoint(final int index, final double x, final double y, final double z) {
        wasChanged = true;
        points.remove(index);
        points.add(index, new Vector3(x + 0.5, y, z + 0.5, 0, 0, world));
        setDisplayEffect(index, x + 0.5, y, z + 0.5);
    }

    public void removePoint(final int index) {
        wasChanged = true;
        points.remove(index);
        removeDisplayEffect(index);
    }

    public int getSelection() {
        return lastSelected;
    }

    public void setSelection(final int index) {
        lastSelected = index;
    }

    public int size() {
        return points.size();
    }

    public Path getPath() {
        return path;
    }

    /**
     * Returns the index of a stored point if it exists in this path. Or -1 if it does not.
     */
    public int getPointIndex(final double x, final double y, final double z) {
        for (int i = 0; i < points.size(); i++) {
            final Vector3 vector = points.get(i);
            if (vector.x == x + 0.5 && vector.y == y && vector.z == z + 0.5) {
                return i;
            }
        }
        return -1;
    }

    public List<Vector3> getPoints() {
        return points;
    }

    @Sync.SyncOnly
    private void addDisplayEffect(final double x, final double y, final double z) {
        final ArmorStand armorStand = world.spawn(new Location(world, x, y - 3, z), ArmorStand.class);
        armorStand.setMarker(true);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setCustomName(ChatColor.AQUA + "#" + (displayedSelectables.size() + 1));
        armorStand.setCustomNameVisible(true);
        armorStand.setMetadata(PATH_INDEX, new FixedMetadataValue(manager.getPlugin(), displayedSelectables.size()));
        armorStand.teleport(new Location(world, x, y, z));
        displayedSelectables.add(armorStand);
    }

    @Sync.SyncOnly
    private void setDisplayEffect(final int index, final double x, final double y, final double z) {
        final ArmorStand armorStand = displayedSelectables.get(index);
        armorStand.teleport(new Location(world, x, y, z));
    }

    @Sync.SyncOnly
    private void removeDisplayEffect(final int index) {
        displayedSelectables.remove(index).remove();
        for (int i = index; i < displayedSelectables.size(); i++) {
            displayedSelectables.get(index).setCustomName(ChatColor.AQUA + "#" + (index + 1));
        }
    }

    @Override
    @Sync.SyncOnly
    public void onSelect(final Player selector) throws SelectionFailException {
        if (player == null) {
            player = selector;
            world = player.getWorld();
            points.forEach(p -> addDisplayEffect(p.x, p.y, p.z));
        } else {
            throw new SelectionFailException("You cannot select this path as it is currently already selected by another player!");
        }
    }

    @Override
    @Sync.SyncOnly
    public void onUnselect(final Player selector) throws SelectionFailException {
        if (player == null) {
            throw new SelectionFailException("You cannot unselect this path as it is already unselected!");
        } else {
            displayedSelectables.forEach(Entity::remove);
            displayedSelectables.clear();

            if (!wasChanged) {
                selector.sendMessage(ChatUtil.PREFIX + "No changes was made to the path!");
                return;
            }

            if (points.size() < 2) {
                selector.sendMessage(ChatUtil.PREFIX + "Path was removed from actor as at least 2 points is required!");
                path.getAgent().removePath(path);
            } else {
                points.add(points.get(0).copy());
                manager.savePath(this);
            }
        }
    }

    @Override
    public PathingAgent getOwningAgent() {
        return path.getAgent();
    }

    @Override
    public void save() {
        final Iterator<Vector3> iterator = points.iterator();
        path.clearFrames();

        final int id = path.getAgent().getActor().getId();
        final Vector3 first = iterator.next();
        final Vector3 second = iterator.next();
        Vector3 previous = first;
        Vector3 terminal = second;
        do {
            final Vector3 next = iterator.next();
            path.addFrame(id, previous, terminal, next, 0.1f);
            previous = terminal;
            terminal = next;
        } while (iterator.hasNext());
        path.addFrame(id, previous, terminal, second, 0.1f);

        points.clear();
    }
}
