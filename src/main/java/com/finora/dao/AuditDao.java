package com.finora.dao;

import com.finora.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuditDao {
    private final Database database;
    public AuditDao(Database database) { this.database = database; }

    public void record(Long actorId, String event, String details) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO audit_logs(actor_user_id,event,details) VALUES(?,?,?)")) {
            if (actorId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setLong(1, actorId);
            ps.setString(2, event); ps.setString(3, details); ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> latest(int limit) throws SQLException {
        return search(null, null, null, null, limit);
    }

    public List<Map<String, Object>> search(String query, String event, String from, String to, int limit) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT a.event,a.details,a.occurred_at,COALESCE(u.email,'System') actor_email " +
                "FROM audit_logs a LEFT JOIN users u ON u.id=a.actor_user_id WHERE 1=1");
        if (query != null && !query.isBlank()) sql.append(" AND (a.event LIKE ? COLLATE NOCASE OR a.details LIKE ? COLLATE NOCASE OR u.email LIKE ? COLLATE NOCASE)");
        if (event != null && !event.isBlank()) sql.append(" AND a.event=?");
        if (from != null && !from.isBlank()) sql.append(" AND date(a.occurred_at)>=date(?)");
        if (to != null && !to.isBlank()) sql.append(" AND date(a.occurred_at)<=date(?)");
        sql.append(" ORDER BY a.id DESC LIMIT ?");
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (query != null && !query.isBlank()) { String pattern = "%" + query.trim() + "%"; ps.setString(i++, pattern); ps.setString(i++, pattern); ps.setString(i++, pattern); }
            if (event != null && !event.isBlank()) ps.setString(i++, event);
            if (from != null && !from.isBlank()) ps.setString(i++, from);
            if (to != null && !to.isBlank()) ps.setString(i++, to);
            ps.setInt(i, Math.max(1, Math.min(limit, 5000)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(Map.of("event", rs.getString("event"), "details", rs.getString("details"),
                        "occurredAt", rs.getString("occurred_at"), "actorEmail", rs.getString("actor_email")));
            }
        }
        return rows;
    }

    public int countEventInLastDays(String event, int days) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM audit_logs WHERE event=? AND occurred_at>=datetime('now',?)")) {
            ps.setString(1, event); ps.setString(2, "-" + Math.max(1, days) + " days");
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public int countAllInLastDays(int days) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM audit_logs WHERE occurred_at>=datetime('now',?)")) {
            ps.setString(1, "-" + Math.max(1, days) + " days");
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }
}
