package com.finora.model;

public class User {
    private final long id;
    private final String name;
    private final String email;
    private final String role;
    private final boolean active;
    private final String createdAt;

    public User(long id, String name, String email, String role, boolean active, String createdAt) {
        this.id = id; this.name = name; this.email = email; this.role = role; this.active = active; this.createdAt = createdAt;
    }
    public long getId() { return id; }
    public String getName() { return name; }
    public String getInitial() { return name == null || name.isEmpty() ? "F" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT); }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public String getCreatedAt() { return createdAt; }
}
