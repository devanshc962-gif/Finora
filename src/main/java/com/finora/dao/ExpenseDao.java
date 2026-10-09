package com.finora.dao;

import com.finora.config.Database;
import com.finora.model.Expense;
import com.finora.security.FinanceCipher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseDao {
    private final Database database;
    private final FinanceCipher cipher;
    public ExpenseDao(Database database, FinanceCipher cipher) { this.database = database; this.cipher = cipher; }

    public List<Expense> findByUser(long userId, String category, String from, String to) throws Exception {
        List<Expense> expenses = new ArrayList<>();
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT id,encrypted_payload FROM expenses WHERE user_id=? ORDER BY id DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] fields = cipher.decryptFields(rs.getString("encrypted_payload"), 4);
                    String spentOn = fields[3];
                    if (category != null && !category.isBlank() && !category.equals(fields[1])) continue;
                    if (from != null && spentOn.compareTo(from) < 0) continue;
                    if (to != null && spentOn.compareTo(to) > 0) continue;
                    expenses.add(new Expense(rs.getLong("id"), fields[0], fields[1], Long.parseLong(fields[2]), spentOn));
                }
            }
        }
        expenses.sort((a, b) -> { int date = b.getSpentOn().compareTo(a.getSpentOn()); return date != 0 ? date : Long.compare(b.getId(), a.getId()); });
        return expenses;
    }

    public void create(long userId, String description, String category, long amount, String spentOn) throws SQLException {
        try (Connection c = database.getConnection()) { create(c, userId, description, category, amount, spentOn); }
    }

    public void create(Connection c, long userId, String description, String category, long amount, String spentOn) throws SQLException {
        String payload = cipher.encryptFields(description, category, Long.toString(amount), spentOn);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO expenses(user_id,description,category,amount_cents,spent_on,encrypted_payload) VALUES(?,?,?,?,?,?)")) {
            ps.setLong(1, userId); ps.setString(2, "[encrypted]"); ps.setString(3, "[encrypted]"); ps.setLong(4, 1);
            ps.setString(5, "1970-01-01"); ps.setString(6, payload); ps.executeUpdate();
        }
    }

    public boolean update(long id, long userId, String description, String category, long amount, String spentOn) throws SQLException {
        String payload = cipher.encryptFields(description, category, Long.toString(amount), spentOn);
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE expenses SET description='[encrypted]',category='[encrypted]',amount_cents=1,spent_on='1970-01-01',encrypted_payload=? WHERE id=? AND user_id=?")) {
            ps.setString(1, payload); ps.setLong(2, id); ps.setLong(3, userId); return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(long id, long userId) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM expenses WHERE id=? AND user_id=?")) {
            ps.setLong(1, id); ps.setLong(2, userId); return ps.executeUpdate() == 1;
        }
    }

    public long totalForUser(long userId) throws Exception {
        return findByUser(userId, null, null, null).stream().mapToLong(Expense::getAmountCents).sum();
    }

    public long totalForMonth(long userId, String monthStart, String nextMonthStart) throws Exception {
        return findByUser(userId, null, monthStart, null).stream().filter(e -> e.getSpentOn().compareTo(nextMonthStart) < 0)
                .mapToLong(Expense::getAmountCents).sum();
    }

    public List<Map<String, Object>> categoryTotals(long userId, String monthStart, String nextMonthStart) throws Exception {
        Map<String, Long> totals = new HashMap<>();
        for (Expense expense : findByUser(userId, null, monthStart, null)) {
            if (expense.getSpentOn().compareTo(nextMonthStart) < 0) totals.merge(expense.getCategory(), expense.getAmountCents(), Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        totals.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> result.add(Map.of("category", entry.getKey(), "amountCents", entry.getValue())));
        return result;
    }
}
