package me.Gabix.badge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ModServerClient {
    private static final String BASE_URL = "https://mvp-backend-bods.onrender.com/api";
    private static final Gson GSON = new Gson();

    private static String getPlayerName(MinecraftClient client) {
        if (client.player != null) {
            return client.player.getName().getString();
        } else {
            return client.getSession() != null ? client.getSession().getUsername() : "Unknown";
        }
    }

    private static String getCurrentServerIp(MinecraftClient client) {
        if (client.isIntegratedServerRunning()) {
            if (client.getServer() != null && client.getServer().getSaveProperties() != null) {
                String levelName = client.getServer().getSaveProperties().getLevelName();
                return "Singleplayer (" + levelName + ")";
            } else {
                return "Singleplayer";
            }
        } else {
            ServerInfo serverEntry = client.getCurrentServerEntry();
            if (serverEntry != null && serverEntry.address != null && !serverEntry.address.isEmpty()) {
                return serverEntry.address;
            } else {
                ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
                if (networkHandler != null && networkHandler.getConnection() != null) {
                    String address = networkHandler.getConnection().getAddress().toString();
                    if (address.contains("/")) {
                        String host = address.split("/")[0];
                        return !host.isEmpty() ? host : address.split("/")[1];
                    } else {
                        return address;
                    }
                } else {
                    return "In game main menu";
                }
            }
        }
    }

    public static void sendHeartbeatAsync(UUID uuid) {
        if (uuid != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            String username = getPlayerName(client);
            String serverIp = getCurrentServerIp(client);
            CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URL(BASE_URL + "/heartbeat");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setDoOutput(true);

                    JsonObject jsonBody = new JsonObject();
                    jsonBody.addProperty("uuid", uuid.toString());
                    jsonBody.addProperty("username", username);
                    jsonBody.addProperty("serverIp", serverIp);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception ignored) {}
            });
        }
    }

    public static void sendLogoutAsync(UUID uuid) {
        if (uuid != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URL(BASE_URL + "/logout");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setDoOutput(true);

                    JsonObject jsonBody = new JsonObject();
                    jsonBody.addProperty("uuid", uuid.toString());

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception ignored) {}
            });
        }
    }

    public static void fetchUsersAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(BASE_URL + "/users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder content = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    JsonObject obj = GSON.fromJson(content.toString(), JsonObject.class);
                    if (obj.has("users")) {
                        JsonArray arr = obj.getAsJsonArray("users");
                        Set<UUID> fetchedUuids = new HashSet<>();

                        for (JsonElement el : arr) {
                            fetchedUuids.add(UUID.fromString(el.getAsString()));
                        }

                        ModPlayerTracker.updateUsers(fetchedUuids);
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        });
    }
}