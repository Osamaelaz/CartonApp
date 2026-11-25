package com.cartonapp.dao;

import com.cartonapp.config.DatabaseConnectionFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Key/value application settings (company details, password hash) in table {@code app_settings}. */
public final class SettingsDao {
    private final DatabaseConnectionFactory connections;

    public SettingsDao(DatabaseConnectionFactory connections) {
        this.connections = connections;
    }

    public Map<String, String> findAll() throws SQLException {
        SchemaUpgrades.ensure(connections);
        Map<String, String> values = new HashMap<>();
        try (Connection c = connections.open();
                PreparedStatement p = c.prepareStatement("SELECT setting_key,setting_value FROM app_settings");
                ResultSet r = p.executeQuery()) {
            while (r.next()) values.put(r.getString(1), r.getString(2));
        }
        return values;
    }

    /** Inserts or updates all given settings in one transaction. */
    public void save(Map<String, String> values) throws SQLException {
        SchemaUpgrades.ensure(connections);
        String sql = "INSERT INTO app_settings(setting_key,setting_value,updated_at) VALUES (?,?,CURRENT_TIMESTAMP) "
                + "ON CONFLICT (setting_key) DO UPDATE SET setting_value=EXCLUDED.setting_value,updated_at=CURRENT_TIMESTAMP";
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(sql)) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    p.setString(1, entry.getKey());
                    p.setString(2, entry.getValue());
                    p.addBatch();
                }
                p.executeBatch();
                c.commit();
            } catch (SQLException exception) {
                c.rollback();
                throw exception;
            }
        }
    }

    /** Checks that the database answers; used by the settings screen. */
    public String serverVersion() throws SQLException {
        try (Connection c = connections.open();
                PreparedStatement p = c.prepareStatement("SHOW server_version");
                ResultSet r = p.executeQuery()) {
            r.next();
            return r.getString(1);
        }
    }
}
