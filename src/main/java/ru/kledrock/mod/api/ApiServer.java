package ru.kledrock.mod.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;
import ru.kledrock.mod.KledrockMod;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiServer {
    private final KledrockMod plugin;
    private final Gson gson = new Gson();
    private HttpServer server;

    public ApiServer(KledrockMod plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int port = plugin.getConfig().getInt("api.port", 8080);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/ping", this::handlePing);
            server.createContext("/api/stats", this::handleStats);
            server.createContext("/api/appeal", this::handleAppeal);
            server.createContext("/api/appeal/action", this::handleAppealAction);
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("HTTP API запущен на порту " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось запустить HTTP API: " + e.getMessage());
        }
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String providedKey = exchange.getRequestHeaders().getFirst("X-Kledrock-Key");
        return providedKey != null && providedKey.equals(plugin.getConfig().getString("api.secret-key"));
    }

    private void handlePing(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) { sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }
        sendResponse(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) { sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        Map<String, Object> stats = new HashMap<>();
        stats.put("tps", Bukkit.getTPS()[0]);
        stats.put("online", Bukkit.getOnlinePlayers().size());
        stats.put("deathsToday", plugin.getDatabaseManager().getTodayStat("deaths"));
        stats.put("joinsToday", plugin.getDatabaseManager().getTodayStat("joins"));

        sendResponse(exchange, 200, gson.toJson(stats));
    }

    private void handleAppeal(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) { sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendResponse(exchange, 405, "{}"); return; }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<?, ?> map = gson.fromJson(body, Map.class);
            
            int punishmentId = Integer.parseInt(map.get("punishmentId").toString());
            String discordUserId = map.get("discordUserId").toString();
            String text = map.get("text").toString();

            int appealId = plugin.getDatabaseManager().addAppeal(punishmentId, discordUserId, text);
            
            if (appealId != -1) {
                sendResponse(exchange, 200, "{\"status\":\"success\",\"appealId\":" + appealId + "}");
            } else {
                sendResponse(exchange, 500, "{}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 400, "{}");
        }
    }

    private void handleAppealAction(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) { sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendResponse(exchange, 405, "{}"); return; }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<?, ?> map = gson.fromJson(body, Map.class);
            
            int appealId = Integer.parseInt(map.get("appealId").toString());
            String action = map.get("action").toString();

            boolean success = plugin.getDatabaseManager().processAppeal(appealId, action);
            if (success) sendResponse(exchange, 200, "{\"status\":\"success\"}");
            else sendResponse(exchange, 400, "{}");
        } catch (Exception e) {
            sendResponse(exchange, 500, "{}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    public void stop() { if (server != null) server.stop(0); }
}