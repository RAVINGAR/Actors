package com.ravingarinc.actor.storage.sql;

@SuppressWarnings("PMD.FieldNamingConventions")
public class SQLSchema {

    public static class Skins {
        public static final String SKINS = "skins";

        public static final String createTable = "todo";
    }

    public static class Actors {
        public static final String ACTORS = "actors";
        public static final String selectAll = "SELECT * FROM " + ACTORS;
        public static final String UUID = "uuid";
        public static final String TYPE = "type";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String Z = "z";
        public static final String WORLD = "world";
        public static final String ARGUMENTS = "arguments";
        public static final String createTable = "CREATE TABLE IF NOT EXISTS " + ACTORS + " (" +
                UUID + " TEXT PRIMARY KEY," +
                TYPE + " TEXT NOT NULL," +
                X + " REAL NOT NULL," +
                Y + " REAL NOT NULL," +
                Z + " REAL NOT NULL," +
                WORLD + " TEXT NOT NULL," +
                ARGUMENTS + " TEXT NOT NULL) WITHOUT ROWID;";
        public static final String select = "SELECT *" +
                " FROM " + ACTORS +
                " WHERE " + Actors.UUID + " = ?";

        public static final String insert = "INSERT INTO " + ACTORS + "(" +
                UUID + "," +
                TYPE + "," +
                X + "," +
                Y + "," +
                Z + "," +
                WORLD + "," +
                ARGUMENTS + ") VALUES(?,?,?,?,?,?,?)";

        public static final String update = "UPDATE " + ACTORS + " SET " +
                TYPE + " = ? , " +
                X + " = ? , " +
                Y + " = ? , " +
                Z + " = ? , " +
                WORLD + " = ? , " +
                ARGUMENTS + " = ? " +
                "WHERE " + UUID + " = ?";
    }
}
