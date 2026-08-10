package me.Gabix.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.Gabix.tracker.ModPlayerTracker;
import net.minecraft.client.MinecraftClient;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ModServerClient {

    private static final String SERVER_BASE_URL = "https://mvp-backend-bods.onrender.com";
    private static final String HEARTBEAT_URL = SERVER_BASE_URL + "/api/heartbeat";
    private static final String LOGOUT_URL = SERVER_BASE_URL + "/api/logout";
    private static final String USERS_URL = SERVER_BASE_URL + "/api/users";

    /**
     * Heartbeat küldése aszinkron módon a Render szervernek
     */
    public static void sendHeartbeatAsync(UUID uuid) {
        new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null || client.getSession() == null) return;

                String username = client.getSession().getUsername();
                String serverIp = getCurrentServerIp(client);

                URL url = new URL(HEARTBEAT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000); // Render cold start miatt 8s timeout
                conn.setReadTimeout(8000);

                JsonObject body = new JsonObject();
                body.addProperty("uuid", uuid.toString());
                body.addProperty("username", username);
                body.addProperty("serverIp", serverIp);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject responseJson = JsonParser.parseReader(reader).getAsJsonObject();

                        // 🔴 TÁVOLI TILTÁS / TÖNKRE TÉTEL ELLENŐRZÉSE 🔴
                        if (responseJson.has("allowed") && !responseJson.get("allowed").getAsBoolean()) {

                            String reason = responseJson.has("reason")
                                    ? responseJson.get("reason").getAsString()
                                    : "A mod használata le lett tiltva ehhez a fiókhoz.";

                            System.err.println("[MOD TILTÁS] " + reason);

                            // Azonnal kilőjük a Minecraft folyamatot
                            client.execute(() -> {
                                System.exit(0);
                            });
                        }
                    }
                }

            } catch (Exception e) {
                // Hálózati hiba esetén nem zárja be a játékot
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Kijelentkezés küldése aszinkron módon
     */
    public static void sendLogoutAsync(UUID uuid) {
        new Thread(() -> {
            try {
                URL url = new URL(LOGOUT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);

                JsonObject body = new JsonObject();
                body.addProperty("uuid", uuid.toString());

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                conn.getResponseCode();
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Az aktív felhasználók listájának lekérése a szerverről
     */
    public static void fetchUsersAsync() {
        new Thread(() -> {
            try {
                URL url = new URL(USERS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonArray usersArray = JsonParser.parseReader(reader).getAsJsonArray();
                        ModPlayerTracker.clear();
                        for (JsonElement element : usersArray) {
                            JsonObject userObj = element.getAsJsonObject();
                            if (userObj.has("uuid")) {
                                try {
                                    UUID fetchedUuid = UUID.fromString(userObj.get("uuid").getAsString());
                                    ModPlayerTracker.registerUser(fetchedUuid);
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Aktuális szerver IP lekérése Fabric alatt
     */
    private static String getCurrentServerIp(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null) {
            return client.getCurrentServerEntry().address;
        } else if (client.isIntegratedServerRunning()) {
            return "Singleplayer (Világ)";
        } else {
            return "In game main menu";
        }
    }
}