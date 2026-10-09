package com.finora.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Owns SQLite connection setup and the first-run schema. */
public final class Database {
    private final String jdbcUrl;
    private final Path databasePath;

    public Database() throws Exception {
        String configuredPath = System.getenv().getOrDefault("FINORA_DB_PATH", "data/finora.db");
        databasePath = Path.of(configuredPath).toAbsolutePath();
        Files.createDirectories(databasePath.getParent());
        this.jdbcUrl = "jdbc:sqlite:" + databasePath;
        Class.forName("org.sqlite.JDBC");
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public Path getDatabasePath() { return databasePath; }

    private void initializeSchema() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                    "password_hash TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'USER' CHECK(role IN ('USER','ADMIN')), " +
                    "active INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "description TEXT NOT NULL, category TEXT NOT NULL, amount_cents INTEGER NOT NULL CHECK(amount_cents > 0), " +
                    "spent_on TEXT NOT NULL, encrypted_payload TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS budgets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "category TEXT NOT NULL, limit_cents INTEGER NOT NULL CHECK(limit_cents > 0), " +
                    "starts_on TEXT NOT NULL, ends_on TEXT NOT NULL, encrypted_payload TEXT, CHECK(ends_on >= starts_on))");
            statement.execute("CREATE TABLE IF NOT EXISTS goals (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "title TEXT NOT NULL, target_cents INTEGER NOT NULL CHECK(target_cents > 0), " +
                    "saved_cents INTEGER NOT NULL DEFAULT 0 CHECK(saved_cents >= 0), deadline TEXT NOT NULL, encrypted_payload TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS recurring_expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), encrypted_payload TEXT NOT NULL, " +
                    "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, actor_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL, " +
                    "event TEXT NOT NULL, details TEXT NOT NULL, occurred_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS app_settings (" +
                    "setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
            statement.execute("INSERT OR IGNORE INTO app_settings(setting_key, setting_value) VALUES ('registration_enabled','true')");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_expenses_owner_date ON expenses(user_id, spent_on)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_budgets_owner ON budgets(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_goals_owner ON goals(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_recurring_owner ON recurring_expenses(user_id, active)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(occurred_at)");
            ensureColumn(connection, "expenses", "encrypted_payload");
            ensureColumn(connection, "budgets", "encrypted_payload");
            ensureColumn(connection, "goals", "encrypted_payload");
        }
    }

    private static void ensureColumn(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) if (column.equals(columns.getString("name"))) return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " TEXT");
        }
    }
}
