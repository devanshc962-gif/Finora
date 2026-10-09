package com.finora.security;

import com.finora.config.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Encrypts existing finance rows on startup and scrubs the legacy plaintext columns. */
public class FinanceEncryptionMigrator {
    private final Database database;
    private final FinanceCipher cipher;
    public FinanceEncryptionMigrator(Database database, FinanceCipher cipher) { this.database = database; this.cipher = cipher; }

    public void migrateAndRekey() throws Exception {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                migrateExpenses(connection);
                migrateBudgets(connection);
                migrateGoals(connection);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void migrateExpenses(Connection connection) throws Exception {
        List<Row> rows = rows(connection, "SELECT id,description,category,amount_cents,spent_on,encrypted_payload FROM expenses");
        for (Row row : rows) {
            String payload = row.payload;
            if (payload == null) payload = cipher.encryptFields(row.fields[0], row.fields[1], row.fields[2], row.fields[3]);
            else if (isOldKey(payload)) payload = cipher.encryptFields(cipher.decryptFields(payload, 4));
            scrub(connection, "expenses", row.id, payload, "description='[encrypted]',category='[encrypted]',amount_cents=1,spent_on='1970-01-01'");
        }
    }
    private void migrateBudgets(Connection connection) throws Exception {
        List<Row> rows = rows(connection, "SELECT id,category,limit_cents,starts_on,ends_on,encrypted_payload FROM budgets");
        for (Row row : rows) {
            String payload = row.payload;
            if (payload == null) payload = cipher.encryptFields(row.fields[0], row.fields[1], row.fields[2], row.fields[3]);
            else if (isOldKey(payload)) payload = cipher.encryptFields(cipher.decryptFields(payload, 4));
            scrub(connection, "budgets", row.id, payload, "category='[encrypted]',limit_cents=1,starts_on='1970-01-01',ends_on='1970-01-01'");
        }
    }
    private void migrateGoals(Connection connection) throws Exception {
        List<Row> rows = rows(connection, "SELECT id,title,target_cents,saved_cents,deadline,encrypted_payload FROM goals");
        for (Row row : rows) {
            String payload = row.payload;
            if (payload == null) payload = cipher.encryptFields(row.fields[0], row.fields[1], row.fields[2], row.fields[3]);
            else if (isOldKey(payload)) payload = cipher.encryptFields(cipher.decryptFields(payload, 4));
            scrub(connection, "goals", row.id, payload, "title='[encrypted]',target_cents=1,saved_cents=0,deadline='1970-01-01'");
        }
    }

    private boolean isOldKey(String payload) {
        String active = cipher.getActiveKeyId();
        return payload != null && payload.startsWith("v1.") && !payload.startsWith("v1." + active + ".");
    }

    private static List<Row> rows(Connection connection, String sql) throws Exception {
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String[] fields = new String[4];
                for (int i = 0; i < fields.length; i++) fields[i] = rs.getString(i + 2);
                rows.add(new Row(rs.getLong(1), fields, rs.getString(6)));
            }
        }
        return rows;
    }

    private static void scrub(Connection connection, String table, long id, String payload, String assignments) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE " + table + " SET " + assignments + ",encrypted_payload=? WHERE id=?")) {
            ps.setString(1, payload); ps.setLong(2, id); ps.executeUpdate();
        }
    }

    private record Row(long id, String[] fields, String payload) { }
}
