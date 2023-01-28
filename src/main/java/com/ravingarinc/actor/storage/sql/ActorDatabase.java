package com.ravingarinc.actor.storage.sql;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.async.AsyncHandler;
import com.ravingarinc.actor.api.async.AsynchronousException;
import com.ravingarinc.actor.api.async.Sync;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.api.util.Vector3;
import com.ravingarinc.actor.command.Argument;
import com.ravingarinc.actor.command.Registry;
import com.ravingarinc.actor.npc.ActorFactory;
import com.ravingarinc.actor.npc.ActorManager;
import com.ravingarinc.actor.npc.type.Actor;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static com.ravingarinc.actor.storage.sql.SQLSchema.Actors;

public class ActorDatabase extends Database {

    private final Map<String, List<UnloadedActor>> unloadedActors;

    private ActorManager actorManager;

    public ActorDatabase(final RavinPlugin plugin) {
        super(Actors.ACTORS, Actors.createTable, ActorDatabase.class, plugin, ActorManager.class);
        unloadedActors = new HashMap<>();
    }

    @Override
    public void load() throws ModuleLoadException {
        super.load();
        actorManager = plugin.getModule(ActorManager.class);
        loadAllActors();
    }

    @Override
    public void cancel() {
        unloadedActors.clear();
        saveAllActors();
        super.cancel();
    }

    @Sync.AsyncOnly
    public void loadAllActors() {
        queue(() -> query(Actors.selectAll, (result) -> {
            try {
                while (result.next()) {
                    try {
                        loadActor(result);
                    } catch(AsynchronousException e) {
                        I.log(Level.SEVERE, "Encountered issue querying database!", e);
                    }
                }
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue querying database!", e);
            }
            if (unloadedActors.size() > 0) {
                I.log(Level.WARNING, "Some actors could not be loaded. If you have recently changed world name and wish to migrate these unloaded actors, please type /actors migrate world %s <new-world-name> and then reload the plugin!");
            }
            return null;
        }));
    }

    @Sync.AsyncOnly
    private void loadActor(final ResultSet result) throws SQLException, AsynchronousException {
        final UUID uuid = UUID.fromString(result.getString(1));
        final String type = result.getString(2);
        final int x = result.getInt(3);
        final int y = result.getInt(4);
        final int z = result.getInt(5);
        final String worldName = result.getString(6);
        final String arguments = result.getString(7);

        final World world = AsyncHandler.executeBlockingSyncComputation(() -> Bukkit.getWorld(worldName), 2000);
        if (world == null) {
            I.log(Level.WARNING, "Actor '%s' - Cannot load as world named '%s' does not exist.", uuid.toString(), worldName);
            addUnloadedActor(uuid, type, x, y, z, worldName, arguments);
        } else {
            if (ActorFactory.getTypes().contains(type)) {
                Argument[] args;
                try {
                    args = Registry.parseArguments(Registry.ACTOR_ARGS, 0, arguments.split(" "), null);
                } catch (final Argument.InvalidArgumentException exception) {
                    I.log(Level.WARNING, "Actor '%s' - Could not parse stored arguments!", exception);
                    args = new Argument[0];
                }
                actorManager.createActor(type, uuid, new Vector3(x, y, z, 0, 0, world), args);
            } else {
                I.log(Level.WARNING, "Actor '%s' - Cannot load as actor type '%s' is unknown!", type);
                addUnloadedActor(uuid, type, x, y, z, worldName, arguments);
            }
        }
    }

    private void addUnloadedActor(final UUID uuid, final String type, final int x, final int y, final int z, final String worldName, final String arguments) {
        final List<UnloadedActor> actors = unloadedActors.computeIfAbsent(worldName, name -> new ArrayList<>());
        actors.add(new UnloadedActor(uuid, type, x, y, z, worldName, arguments));
    }

    private void migrateUnloadedActors(final String oldWorld, final String newWorld) {
        // todo
    }

    private void saveAllActors() {
        actorManager.getActors().forEach(actor -> queue(() -> saveActor(actor)));
    }

    public void saveActor(final Actor<?> actor) {
        final Boolean exists = query(Actors.select, (statement) -> {
            try {
                statement.setString(1, actor.getUUID().toString());
            } catch (final SQLException exception) {
                I.log(Level.SEVERE, "Encountered issue preparing statement!", exception);
            }
        }, (result) -> {
            try {
                return result.next();
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue saving actor!", e);
                return false;
            }
        });
        if (exists == null || !exists) {
            // if null or doesnt exist
            insertActor(actor);
        } else {
            updateActor(actor);
        }
    }

    private void insertActor(final Actor<?> actor) {
        prepareStatement(Actors.insert, (statement) -> {
            try {
                statement.setString(1, actor.getUUID().toString());
                statement.setString(2, actor.getType().getKey());
                final Vector3 location = actor.getSpawnLocation();
                statement.setDouble(3, location.getX());
                statement.setDouble(4, location.getY());
                statement.setDouble(5, location.getZ());
                statement.setString(6, location.getWorldName());
                final StringBuilder builder = new StringBuilder();
                final Iterator<String> iterator = actor.getAppliedArguments().iterator();
                while (iterator.hasNext()) {
                    builder.append(iterator.next());
                    if (iterator.hasNext()) {
                        builder.append(" ");
                    }
                }
                statement.setString(7, builder.toString());
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue saving actor!", e);
            }
        });
    }

    private void updateActor(final Actor<?> actor) {
        prepareStatement(Actors.update, (statement) -> {
            try {
                statement.setString(1, actor.getType().getKey());
                final Vector3 location = actor.getSpawnLocation();
                statement.setDouble(2, location.getX());
                statement.setDouble(3, location.getY());
                statement.setDouble(4, location.getZ());
                statement.setString(5, location.getWorldName());
                final StringBuilder builder = new StringBuilder();
                final Iterator<String> iterator = actor.getAppliedArguments().iterator();
                while (iterator.hasNext()) {
                    builder.append(iterator.next());
                    if (iterator.hasNext()) {
                        builder.append(" ");
                    }
                }
                statement.setString(6, builder.toString());
                statement.setString(7, actor.getUUID().toString());
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue saving actor!", e);
            }
        });
    }

    /**
     * Represents an actor that was present in the database, however it's origin world could not be found!
     */
    private record UnloadedActor(UUID uuid, String type, int x, int y, int z, String world, String args) {
    }
}
