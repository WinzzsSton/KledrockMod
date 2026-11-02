package ru.kledrock.mod.database;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

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
                stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                        "date VARCHAR(16) PRIMARY KEY," +
                        "deaths INTEGER DEFAULT 0," +
                        "joins INTEGER DEFAULT 0" +
                        ");");
            }
            plugin.getLogger().info("База данных SQLite подключена.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подключения к БД: " + e.getMessage());
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    public void addStat(String column) {
        String today = getTodayDate();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_stats (date) VALUES (?) ON CONFLICT(date) DO NOTHING")) {
            ps.setString(1, today);
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET " + column + " = " + column + " + 1 WHERE date = ?")) {
            ps.setString(1, today);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTodayStat(String column) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT " + column + " FROM player_stats WHERE date = ?")) {
            ps.setString(1, getTodayDate());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
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

    public int addAppeal(int punishmentId, String discordUserId, String text) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO appeals (punishment_id, discord_user_id, text, created_at) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, punishmentId);
            ps.setString(2, discordUserId);
            ps.setString(3, text);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка записи апелляции: " + e.getMessage());
        }
        return -1;
    }

    public boolean processAppeal(int appealId, String action) {
        try {
            int punishmentId = -1;
            try (PreparedStatement ps = connection.prepareStatement("SELECT punishment_id FROM appeals WHERE id = ? AND status = 'PENDING'")) {
                ps.setInt(1, appealId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) punishmentId = rs.getInt("punishment_id");
            }

            if (punishmentId == -1) return false;

            if (action.equals("ACCEPT")) {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE punishments SET active = 0 WHERE id = ?")) {
                    ps.setInt(1, punishmentId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = connection.prepareStatement("UPDATE appeals SET status = ? WHERE id = ?")) {
                ps.setString(1, action);
                ps.setInt(2, appealId);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            return false;
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

    public boolean isMuted(String playerName) { return isPunished(playerName, "MUTE"); }
    public boolean isBanned(String playerName) { return isPunished(playerName, "BAN"); }

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