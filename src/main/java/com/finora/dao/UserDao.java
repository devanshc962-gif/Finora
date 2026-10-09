package com.finora.dao;

import com.finora.config.Database;
import com.finora.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private final Database database;
    public UserDao(Database database) { this.database = database; }

    public record Credentials(User user, String passwordHash) { }

    public Optional<Credentials> findCredentials(String email) throws SQLException {
        String sql = "SELECT id,name,email,password_hash,role,active,created_at FROM users WHERE email=? COLLATE NOCASE";
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Credentials(userFrom(rs), rs.getString("password_hash")));
            }
        }
    }

    public Optional<User> findById(long id) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT id,name,email,role,active,created_at FROM users WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(userFrom(rs)) : Optional.empty(); }
        }
    }

    public boolean createUser(String name, String email, String hash) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(name,email,password_hash) VALUES(?,?,?)")) {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, hash);
            return ps.executeUpdate() == 1;
        }
    }

    public void createInitialAdmin(String name, String email, String hash) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO users(name,email,password_hash,role) VALUES(?,?,?,'ADMIN')")) {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, hash); ps.executeUpdate();
        }
    }

    public List<User> findAll() throws SQLException {
        return findAll(null, null, null);
    }

    public List<User> findAll(String search, String role, String status) throws SQLException {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id,name,email,role,active,created_at FROM users WHERE 1=1");
        if (search != null && !search.isBlank()) sql.append(" AND (name LIKE ? COLLATE NOCASE OR email LIKE ? COLLATE NOCASE)");
        if ("USER".equals(role) || "ADMIN".equals(role)) sql.append(" AND role=?");
        if ("active".equals(status)) sql.append(" AND active=1");
        if ("inactive".equals(status)) sql.append(" AND active=0");
        sql.append(" ORDER BY created_at DESC,id DESC LIMIT 500");
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (search != null && !search.isBlank()) { String pattern = "%" + search.trim() + "%"; ps.setString(i++, pattern); ps.setString(i++, pattern); }
            if ("USER".equals(role) || "ADMIN".equals(role)) ps.setString(i++, role);
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(userFrom(rs));
            }
        }
        return users;
    }

    public int countAll() throws SQLException { return count("SELECT COUNT(*) FROM users"); }
    public int countActive() throws SQLException { return count("SELECT COUNT(*) FROM users WHERE active=1"); }
    public int countInactive() throws SQLException { return count("SELECT COUNT(*) FROM users WHERE active=0"); }
    public int countAdmins() throws SQLException { return count("SELECT COUNT(*) FROM users WHERE role='ADMIN' AND active=1"); }

    private int count(String sql) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void createAdminManagedUser(String name, String email, String hash, String role) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)")) {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, hash); ps.setString(4, role); ps.executeUpdate();
        }
    }

    public boolean updateProfileAndAccess(long id, String name, String email, String role, boolean active) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET name=?,email=?,role=?,active=? WHERE id=?")) {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, role); ps.setInt(4, active ? 1 : 0); ps.setLong(5, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updatePassword(long id, String hash) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
            ps.setString(1, hash); ps.setLong(2, id); return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(long id) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, id); return ps.executeUpdate() == 1;
        }
    }

    public void updateRoleAndStatus(long id, String role, boolean active) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET role=?,active=? WHERE id=?")) {
            ps.setString(1, role); ps.setInt(2, active ? 1 : 0); ps.setLong(3, id); ps.executeUpdate();
        }
    }

    public int countActiveAdmins() throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE role='ADMIN' AND active=1"); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static User userFrom(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("name"), rs.getString("email"), rs.getString("role"),
                rs.getInt("active") == 1, rs.getString("created_at"));
    }
}
