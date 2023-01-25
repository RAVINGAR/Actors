package com.ravingarinc.actor.storage.sql;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.util.I;
import com.ravingarinc.actor.skin.ActorSkin;
import com.ravingarinc.actor.skin.SkinClient;
import org.jetbrains.annotations.Async;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import static com.ravingarinc.actor.storage.sql.SQLSchema.Skins;

public class SkinDatabase extends Database {
    private SkinClient client;

    public SkinDatabase(final RavinPlugin plugin) {
        super(Skins.SKINS, Skins.createTable, SkinDatabase.class, plugin);
    }

    @Override
    public void load() throws ModuleLoadException {
        super.load();
        client = plugin.getModule(SkinClient.class);
        queue(this::loadAllSkins);
    }

    @Override
    public void cancel() {
        saveAllSkins();
        super.cancel();
    }

    public void loadAllSkins() {
        query(Skins.selectAll, (result) -> {
            try {
                while (result.next()) {
                    loadSkin(result);
                }
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue querying database!", e);
            }
            return null;
        });
    }

    public void loadSkin(final ResultSet result) throws SQLException {
        final UUID uuid = UUID.fromString(result.getString(1));
        final String name = result.getString(2);
        final String value = result.getString(3);
        final String signature = result.getString(4);

        client.createSkin(name, uuid, value, signature);
    }

    public void saveAllSkins() {
        client.getSkins().forEach(skin -> queue(() -> saveSkin(skin)));
    }

    @Async.Schedule
    public void saveSkin(final ActorSkin skin) {
        if (skin.getValue() == null || skin.getSignature() == null) {
            I.log(Level.WARNING, "Could not save skin '%s' as no texture has been loaded!", skin.getName());
            return;
        }
        final Boolean exists = query(Skins.select, (statement) -> {
            try {
                statement.setString(1, skin.getUUID().toString());
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
            insertSkin(skin);
        } else {
            updateSkin(skin);
        }
    }

    public void insertSkin(final ActorSkin skin) {
        prepareStatement(Skins.insert, (statement) -> {
            try {
                statement.setString(1, skin.getUUID().toString());
                statement.setString(2, skin.getName());
                statement.setString(3, skin.getValue());
                statement.setString(4, skin.getSignature());
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue saving skin!", e);
            }
        });
    }

    public void updateSkin(final ActorSkin skin) {
        prepareStatement(Skins.update, (statement) -> {
            try {
                statement.setString(1, skin.getName());
                statement.setString(2, skin.getValue());
                statement.setString(3, skin.getSignature());
                statement.setString(4, skin.getUUID().toString());
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue saving skin!", e);
            }
        });
    }
}
