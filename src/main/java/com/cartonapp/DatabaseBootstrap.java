package com.cartonapp;

import com.cartonapp.config.DatabaseConfig;
import com.cartonapp.config.DatabaseConnectionFactory;
import com.cartonapp.dao.SchemaInitializerDao;

/**
 * Prepares the database when the program starts: creates any missing tables and columns. It is
 * safe to run on every start (everything is "IF NOT EXISTS") and never adds demo data, so a new
 * customer installation starts with an empty, ready database.
 */
public final class DatabaseBootstrap {
    private DatabaseBootstrap() { }

    /** Throws when the settings file is missing/invalid or PostgreSQL cannot be reached. */
    public static void ensureReady() throws Exception {
        DatabaseConnectionFactory connections = new DatabaseConnectionFactory(DatabaseConfig.loadDefault());
        new SchemaInitializerDao(connections).initialize();
    }
}
