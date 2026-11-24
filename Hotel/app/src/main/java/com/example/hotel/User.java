package com.example.hotel;

import java.io.Serializable;

/**
 * Жүйе пайдаланушысы: әкімші (admin) немесе клиент (client).
 * Serializable: Intent арқылы бір Activity-ден екіншісіне беруге болады.
 */
public class User implements Serializable {

    public static final String ADMIN = "admin";
    public static final String CLIENT = "client";

    private final int id;
    private final String username;
    private final String role;
    private final String fullName;

    public User(int id, String username, String role, String fullName) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isAdmin() {
        return ADMIN.equals(role);
    }

    /** Экранда көрсетілетін рөл атауы. */
    public String roleTitle() {
        return isAdmin() ? "әкімші" : "клиент";
    }
}
