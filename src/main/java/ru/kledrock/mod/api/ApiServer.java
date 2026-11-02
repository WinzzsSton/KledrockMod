package ru.kledrock.mod.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import ru.kledrock.mod.KledrockMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ApiServer {
    private final KledrockMod plugin;
    private HttpServer server;

    public ApiServer(KledrockMod plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int port = plugin.getConfig().getInt("api.port", 8080);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/ping", this::handlePing);
            server.createContext("/api/appeal", this::handleAppeal);
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("HTTP API запущен на порту " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось запустить HTTP API: " + e.getMessage());
        }
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String providedKey = exchange.getRequestHeaders().getFirst("X-Kledrock-Key");
        String expectedKey = plugin.getConfig().getString("api.secret-key");
        return providedKey != null && providedKey.equals(expectedKey);
    }

    private void handlePing(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        sendResponse(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleAppeal(HttpExchange exchange) throws IOException {
        if (!isAuthorized(exchange)) {
            sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        try {
            String punishmentIdStr = extractJsonValue(body, "punishmentId");
            String discordUserId = extractJsonValue(body, "discordUserId");
            String text = extractJsonValue(body, "text");

            if (punishmentIdStr != null && discordUserId != null && text != null) {
                int punishmentId = Integer.parseInt(punishmentIdStr);
                plugin.getDatabaseManager().addAppeal(punishmentId, discordUserId, text);
                sendResponse(exchange, 200, "{\"status\":\"success\"}");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}