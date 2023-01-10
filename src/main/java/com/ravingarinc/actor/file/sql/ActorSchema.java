package com.ravingarinc.actor.file.sql;

@SuppressWarnings("PMD.FieldNamingConventions")
public class ActorSchema {
    public static final String ACTORS = "players";

    public static final String EXAMPLE_1 = "key";
    public static final String EXAMPLE_2 = "value";
    public static final String createTable = "CREATE TABLE IF NOT EXISTS " + ACTORS + " (" +
            EXAMPLE_1 + " TEXT PRIMARY KEY," +
            EXAMPLE_2 + " REAL NOT NULL)";
}
