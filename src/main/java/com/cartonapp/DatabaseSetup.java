package com.cartonapp;

import com.cartonapp.config.DatabaseConfig;
import com.cartonapp.config.DatabaseConnectionFactory;
import com.cartonapp.dao.SchemaInitializerDao;
import com.cartonapp.dao.SeedDataDao;

/** Explicit local setup command; not run by the Swing application. */
public final class DatabaseSetup {
    private DatabaseSetup() { }
    public static void main(String[] args) throws Exception {
        DatabaseConnectionFactory connections = new DatabaseConnectionFactory(DatabaseConfig.loadDefault());
        new SchemaInitializerDao(connections).initialize();
        new SeedDataDao(connections).seed();
        System.out.println("Database schema and seed data are ready.");
    }
}
