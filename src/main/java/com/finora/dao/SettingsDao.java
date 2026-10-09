package com.finora.dao;

import com.finora.config.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsDao {
    private final Database database;
    public SettingsDao(Database database) { this.database = database; }
    public boolean registrationEnabled() throws SQLException {
        return getBoolean("registration_enabled", true);
    }
    public void setRegistrationEnabled(boolean enabled) throws SQLException {
        setBoolean("registration_enabled", enabled);
    }
    public boolean getBoolean(String key, boolean defaultValue) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT setting_value FROM app_settings WHERE setting_key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Boolean.parseBoolean(rs.getString(1)) : defaultValue; }
        }
    }
    public void setBoolean(String key, boolean enabled) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO app_settings(setting_key,setting_value) VALUES(?,?) " +
                        "ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value")) {
            ps.setString(1, key); ps.setString(2, Boolean.toString(enabled)); ps.executeUpdate();
        }
    }
}
