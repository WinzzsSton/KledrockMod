package ru.kledrock.mod.database;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        
        File dbFile = new File(dataFolder, "moderation.db");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS punishments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "player_name VARCHAR(16)," +
                        "type VARCHAR(16)," +
                        "reason TEXT," +
                        "issued_by VARCHAR(16)," +
                        "time BIGINT," +
                        "active BOOLEAN DEFAULT 1" +
                        ");");
                
                stmt.execute("CREATE TABLE IF NOT EXISTS appeals (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "punishment_id INTEGER," +
                        "discord_user_id VARCHAR(32)," +
                        "text TEXT," +
                        "status VARCHAR(16) DEFAULT 'PENDING'," +
                        "created_at BIGINT" +
                        ");");
            }
            plugin.getLogger().info("База данных SQLite подключена.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подключения к БД: " + e.getMessage());
        }
    }

    public void addPunishment(String playerName, String type, String reason, String issuer, long durationMillis) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO punishments (player_name, type, reason, issued_by, time) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, playerName);
            ps.setString(2, type);
            ps.setString(3, reason);
            ps.setString(4, issuer);
            ps.setLong(5, System.currentTimeMillis() + durationMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка записи наказания: " + e.getMessage());
        }
    }

    public void addAppeal(int punishmentId, String discordUserId, String text) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO appeals (punishment_id, discord_user_id, text, created_at) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, punishmentId);
            ps.setString(2, discordUserId);
            ps.setString(3, text);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка записи апелляции: " + e.getMessage());
        }
    }

    public int getActiveWarns(String playerName) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM punishments WHERE player_name = ? AND type = 'WARN' AND active = 1")) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isMuted(String playerName) {
        return isPunished(playerName, "MUTE");
    }

    public boolean isBanned(String playerName) {
        return isPunished(playerName, "BAN");
    }

    private boolean isPunished(String playerName, String type) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT time FROM punishments WHERE player_name = ? AND type = ? AND active = 1 ORDER BY time DESC LIMIT 1")) {
            ps.setString(1, playerName);
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long expireTime = rs.getLong("time");
                if (expireTime == 0 || expireTime > System.currentTimeMillis()) {
                    return true;
                } else {
                    removePunishment(playerName, type);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void removePunishment(String playerName, String type) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE punishments SET active = 0 WHERE player_name = ? AND type = ? AND active = 1")) {
            ps.setString(1, playerName);
            ps.setString(2, type);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}