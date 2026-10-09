package com.finora.dao;

import com.finora.config.Database;
import com.finora.model.Goal;
import com.finora.security.FinanceCipher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GoalDao {
    private final Database database;
    private final FinanceCipher cipher;
    public GoalDao(Database database, FinanceCipher cipher) { this.database = database; this.cipher = cipher; }
    public List<Goal> findByUser(long userId) throws Exception {
        List<Goal> rows = new ArrayList<>();
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id,encrypted_payload FROM goals WHERE user_id=? ORDER BY id DESC")) {
            ps.setLong(1, userId); try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] fields = cipher.decryptFields(rs.getString("encrypted_payload"), 4);
                    rows.add(new Goal(rs.getLong("id"), fields[0], Long.parseLong(fields[1]), Long.parseLong(fields[2]), fields[3]));
                }
            }
        }
        return rows;
    }
    public void create(long userId, String title, long target, long saved, String deadline) throws SQLException {
        String payload = cipher.encryptFields(title, Long.toString(target), Long.toString(saved), deadline);
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO goals(user_id,title,target_cents,saved_cents,deadline,encrypted_payload) VALUES(?,?,?,?,?,?)")) {
            ps.setLong(1, userId); ps.setString(2, "[encrypted]"); ps.setLong(3, 1); ps.setLong(4, 0); ps.setString(5, "1970-01-01"); ps.setString(6, payload); ps.executeUpdate();
        }
    }
    public boolean update(long id, long userId, String title, long target, long saved, String deadline) throws SQLException {
        String payload = cipher.encryptFields(title, Long.toString(target), Long.toString(saved), deadline);
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE goals SET title='[encrypted]',target_cents=1,saved_cents=0,deadline='1970-01-01',encrypted_payload=? WHERE id=? AND user_id=?")) {
            ps.setString(1, payload); ps.setLong(2, id); ps.setLong(3, userId); return ps.executeUpdate() == 1;
        }
    }
    public boolean delete(long id, long userId) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM goals WHERE id=? AND user_id=?")) {
            ps.setLong(1, id); ps.setLong(2, userId); return ps.executeUpdate() == 1;
        }
    }
}
