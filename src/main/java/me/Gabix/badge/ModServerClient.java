package me.Gabix.badge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ModServerClient {
    private static final String BASE_URL = "https://mvp-backend-bods.onrender.com/api";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static String getPlayerName(MinecraftClient client) {
        if (client.player != null) {
            return client.player.getName().getString();
        }
        return client.getSession() != null ? client.getSession().getUsername() : "Unknown";
    }

    private static String getCurrentServerIp(MinecraftClient client) {
        if (client.isIntegratedServerRunning()) {
            if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
                String levelName = client.getServer().getSaveProperties().getLevelName();
                return "Singleplayer (" + levelName + ")";
            }
            return "Singleplayer";
        }
        ServerInfo serverEntry = client.getCurrentServerEntry();
        if (serverEntry != null && serverEntry.address != null && !serverEntry.address.isEmpty()) {
            return serverEntry.address;
        }
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null && networkHandler.getConnection() != null) {
            String address = networkHandler.getConnection().getAddress().toString();
            if (address.contains("/")) {
                String host = address.split("/")[0];
                return !host.isEmpty() ? host : address.split("/")[1];
            }
            return address;
        }
        return "In game main menu";
    }

    public static void sendHeartbeatAsync(UUID uuid) {
        if (uuid == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        String username = getPlayerName(client);
        String serverIp = getCurrentServerIp(client);

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("uuid", uuid.toString());
        jsonBody.addProperty("username", username);
        jsonBody.addProperty("serverIp", serverIp);

        postAsync(BASE_URL + "/heartbeat", jsonBody.toString(), 5000);
    }

    public static void sendLogoutAsync(UUID uuid) {
        if (uuid == null) return;
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("uuid", uuid.toString());
        postAsync(BASE_URL + "/logout", jsonBody.toString(), 3000);
    }

    private static void postAsync(String url, String body, int timeoutMs) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> {
                    System.err.println("[badge] POST hiba (" + url + "): " + ex.getMessage());
                    return null;
                });
    }

    public static void fetchUsersAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenAccept(response -> {
                    if (response.statusCode() != 200) return;
                    try {
                        JsonObject obj = GSON.fromJson(response.body(), JsonObject.class);
                        if (obj == null || !obj.has("users")) return;

                        JsonArray arr = obj.getAsJsonArray("users");
                        Set<UUID> fetchedUuids = new HashSet<>();
                        for (JsonElement el : arr) {
                            try {
                                fetchedUuids.add(UUID.fromString(el.getAsString()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        ModPlayerTracker.updateUsers(fetchedUuids);
                    } catch (Exception ignored) {}
                })
                .exceptionally(ex -> {
                    System.err.println("[badge] fetch hiba: " + ex.getMessage());
                    return null;
                });
    }
}