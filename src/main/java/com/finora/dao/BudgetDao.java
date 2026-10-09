package com.finora.dao;

import com.finora.config.Database;
import com.finora.model.Budget;
import com.finora.model.Expense;
import com.finora.security.FinanceCipher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BudgetDao {
    private final Database database;
    private final FinanceCipher cipher;
    public BudgetDao(Database database, FinanceCipher cipher) { this.database = database; this.cipher = cipher; }

    public List<Budget> findByUser(long userId) throws Exception {
        List<Budget> rows = new ArrayList<>();
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT id,encrypted_payload FROM budgets WHERE user_id=? ORDER BY id DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] fields = cipher.decryptFields(rs.getString("encrypted_payload"), 4);
                    long limit = Long.parseLong(fields[1]);
                    long spent = new ExpenseDao(database, cipher).findByUser(userId, fields[0], fields[2], fields[3]).stream().mapToLong(Expense::getAmountCents).sum();
                    rows.add(new Budget(rs.getLong("id"), fields[0], limit, fields[2], fields[3], spent));
                }
            }
        }
        return rows;
    }

    public void create(long userId, String category, long limit, String startsOn, String endsOn) throws SQLException {
        String payload = cipher.encryptFields(category, Long.toString(limit), startsOn, endsOn);
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO budgets(user_id,category,limit_cents,starts_on,ends_on,encrypted_payload) VALUES(?,?,?,?,?,?)")) {
            ps.setLong(1, userId); ps.setString(2, "[encrypted]"); ps.setLong(3, 1); ps.setString(4, "1970-01-01");
            ps.setString(5, "1970-01-01"); ps.setString(6, payload); ps.executeUpdate();
        }
    }

    public boolean update(long id, long userId, String category, long limit, String startsOn, String endsOn) throws SQLException {
        String payload = cipher.encryptFields(category, Long.toString(limit), startsOn, endsOn);
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE budgets SET category='[encrypted]',limit_cents=1,starts_on='1970-01-01',ends_on='1970-01-01',encrypted_payload=? WHERE id=? AND user_id=?")) {
            ps.setString(1, payload); ps.setLong(2, id); ps.setLong(3, userId); return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(long id, long userId) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM budgets WHERE id=? AND user_id=?")) {
            ps.setLong(1, id); ps.setLong(2, userId); return ps.executeUpdate() == 1;
        }
    }
}
