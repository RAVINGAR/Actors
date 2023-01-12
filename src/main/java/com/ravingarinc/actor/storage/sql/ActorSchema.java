package com.ravingarinc.actor.storage.sql;

@SuppressWarnings("PMD.FieldNamingConventions")
public class ActorSchema {
    public static final String ACTORS = "players";

    public static final String UUID = "uuid";
    public static final String EXAMPLE_2 = "value";
    public static final String createTable = "CREATE TABLE IF NOT EXISTS " + ACTORS + " (" +
            UUID + " TEXT PRIMARY KEY," +
            EXAMPLE_2 + " REAL NOT NULL)";
}
