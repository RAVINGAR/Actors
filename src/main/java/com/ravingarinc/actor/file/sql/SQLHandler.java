package com.ravingarinc.actor.file.sql;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.Module;
import com.ravingarinc.actor.api.ModuleLoadException;
import com.ravingarinc.actor.api.util.I;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

@SuppressWarnings("PMD.FieldNamingConventions")
public class SQLHandler extends Module {
    private final String DATABASE_PATH;
    private final String URL;

    /**
     * The constructor for a Module, should only ever be called by {@link Module#initialise(RavinPlugin, Class)}.
     * Implementations of Managers should have one public constructor with a JavaPlugin object parameter.
     * The implementing constructor CANNOT call {@link RavinPlugin#getModule(Class)} otherwise potential issues
     * may occur. This must be done in {@link this#load()}.
     *
     * @param plugin The owning plugin
     */
    public SQLHandler(final RavinPlugin plugin) {
        super(SQLHandler.class, plugin);
        DATABASE_PATH = plugin.getDataFolder() + "/players.db";
        URL = "jdbc:sqlite:" + DATABASE_PATH;
    }

    @Override
    protected void load() throws ModuleLoadException {
        final File file = new File(DATABASE_PATH);
        if (!file.exists()) {
            try (Connection connection = DriverManager.getConnection(URL)) {
                I.log(Level.INFO, "Created new database with driver '%s'!", connection.getMetaData().getDriverName());
            } catch (final SQLException exception) {
                throw new ModuleLoadException(this, exception);
            }
            if (!execute(Schema.createTable)) {
                throw new ModuleLoadException(this, ModuleLoadException.Reason.SQL);
            }
        }
    }

    @Override
    public void cancel() {

    }

    /**
     * Prepare a statement for the database and consume it. This can be used for insert/update of data
     *
     * @param request  The request
     * @param consumer The consumer to apply to the statement
     */
    private void prepareStatement(final String request, final Consumer<PreparedStatement> consumer) {
        try (Connection connection = DriverManager.getConnection(URL);
             PreparedStatement statement = connection.prepareStatement(request)) {
            consumer.accept(statement);
            statement.executeUpdate();
        } catch (final SQLException exception) {
            I.log(Level.SEVERE, "Encountered issue inserting data into database!", exception);
        }
    }

    /**
     * Execute a query on the database.
     *
     * @param execution The query
     * @return TRUE if successful, FALSE is an exception occurred.
     */
    public boolean execute(final String execution) {
        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement()) {
            statement.execute(execution);
            return true;
        } catch (final SQLException e) {
            I.log(Level.SEVERE, "Encountered issue executing statement to database!", e);
            return false;
        }
    }

    public <T> Optional<T> query(final String query, final Consumer<PreparedStatement> consumer, final Function<ResultSet, T> cursor) {
        try (Connection connection = DriverManager.getConnection(URL);
             PreparedStatement statement = connection.prepareStatement(query)) {
            consumer.accept(statement);
            try (ResultSet result = statement.executeQuery()) {
                return Optional.ofNullable(cursor.apply(result));
            }
        } catch (final SQLException e) {
            I.log(Level.SEVERE, "Encountered issue querying statement to database!", e);
        }
        return Optional.empty();
    }
}
