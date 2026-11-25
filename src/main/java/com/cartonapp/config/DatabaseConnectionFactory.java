package com.cartonapp.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens short-lived JDBC connections. Callers must use try-with-resources. */
public final class DatabaseConnectionFactory {
    private final DatabaseConfig config;

    public DatabaseConnectionFactory(DatabaseConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        // 30 s: a hosted database (e.g. Neon) that has been idle may need a few seconds to wake up.
        DriverManager.setLoginTimeout(30);
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }
}
