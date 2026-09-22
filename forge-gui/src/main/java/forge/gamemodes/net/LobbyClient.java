package forge.gamemodes.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for the Forge lobby server.
 * Manages room registration, heartbeat, room listing, and password verification.
 */
public class LobbyClient {

    private final String serverUrl;
    private final HttpClient httpClient;
    private volatile String currentRoomId;
    private volatile String currentSecret;
    private volatile String relayAddress;
    private ScheduledExecutorService heartbeatScheduler;
    private ScheduledFuture<?> heartbeatFuture;
    private volatile int currentPlayerCount = 0;
    private volatile String currentFormat = "Constructed";
    private volatile RelayTunnel relayTunnel;

    public LobbyClient(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Register a room with the lobby server.
     * The server generates the room ID and secret. Both are stored for later use.
     * Returns the registered Room on success, null on failure.
     */
    public LobbyRoom registerRoom(String name, String format, int port, String gameVersion,
                                   boolean hasPassword, String password, int maxPlayers) {
        try {
            String json = buildRoomJson(name, format, port, gameVersion, hasPassword, password, maxPlayers);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/rooms"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                // Parse server-generated ID and secret from response
                currentRoomId = extractString(response.body(), "id");
                currentSecret = extractString(response.body(), "secret");
                currentPlayerCount = 1;
                startHeartbeat();
                return LobbyRoom.fromJson(response.body());
            }
        } catch (Exception e) {
            System.err.println("Lobby registration failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Unregister the current room and stop heartbeat.
     */
    public void unregisterRoom() {
        stopHeartbeat();
        if (relayTunnel != null) {
            relayTunnel.stop();
            relayTunnel = null;
        }
        if (currentRoomId != null && currentSecret != null) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/rooms/" + currentRoomId))
                        .header("X-Room-Token", currentSecret)
                        .DELETE()
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                // Best effort cleanup
            }
            currentRoomId = null;
            currentSecret = null;
            relayAddress = null;
        }
    }

    /**
     * Update the player count and format sent in heartbeats.
     */
    public void updatePlayerCount(int count) {
        this.currentPlayerCount = count;
    }

    /**
     * Update the format sent in heartbeats. Sends an immediate heartbeat on a
     * background thread only when the format actually changed, since callers
     * invoke this on every lobby state change.
     */
    public void updateFormat(String format) {
        if (format == null || format.equals(currentFormat)) {
            return;
        }
        this.currentFormat = format;
        new Thread(this::sendHeartbeatNow, "LobbyHeartbeatNow").start();
    }

    /**
     * Send a single heartbeat immediately (in addition to the scheduled ones).
     */
    private void sendHeartbeatNow() {
        if (currentRoomId == null) return;
        try {
            String payload = String.format("{\"id\":\"%s\",\"player_count\":%d,\"format\":\"%s\"}",
                    currentRoomId, currentPlayerCount, escapeJson(currentFormat));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/heartbeat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    /**
     * Fetch all active rooms from the lobby server.
     * Returns empty list on failure.
     */
    public List<LobbyRoom> fetchRooms() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/rooms"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseRoomsJson(response.body());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch rooms: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRelayAddress() {
        return relayAddress;
    }

    /**
     * Request a relay port from the lobby server for the current room.
     * Requires that registerRoom() was called successfully first.
     * Starts the reverse tunnel to the VPS so joiners can connect through it.
     * Returns the relay port on success, -1 on failure.
     */
    public int requestRelay(int gamePort) {
        if (currentRoomId == null || currentSecret == null) {
            System.err.println("Cannot request relay: no room registered");
            return -1;
        }
        try {
            String json = String.format("{\"room_id\":\"%s\"}", escapeJson(currentRoomId));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/relay"))
                    .header("Content-Type", "application/json")
                    .header("X-Room-Token", currentSecret)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                String relayHost = extractString(response.body(), "relay_host");
                int relayPort = extractInt(response.body(), "relay_port");
                this.relayAddress = relayHost + ":" + relayPort;

                // Start reverse tunnel: host connects outbound to VPS
                relayTunnel = new RelayTunnel(relayHost, relayPort, gamePort);
                new Thread(() -> {
                    try {
                        relayTunnel.start();
                    } catch (Exception e) {
                        System.err.println("RelayTunnel failed to start: " + e.getMessage());
                    }
                }, "RelayTunnelStarter").start();

                return relayPort;
            } else {
                System.err.println("Relay request failed with status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Relay request failed: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Verify a password for a private room.
     * Returns true if the password is correct or the room has no password.
     */
    public boolean verifyPassword(String roomId, String password) {
        try {
            String json = String.format("{\"password\":\"%s\"}", escapeJson(password));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/rooms/" + roomId + "/verify"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Password verification failed: " + e.getMessage());
            return false;
        }
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LobbyHeartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (currentRoomId == null) return;
            try {
                String payload = String.format("{\"id\":\"%s\",\"player_count\":%d,\"format\":\"%s\"}",
                        currentRoomId, currentPlayerCount, escapeJson(currentFormat));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serverUrl + "/heartbeat"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
    }

    private static String buildRoomJson(String name, String format, int port,
                                         String gameVersion, boolean hasPassword, String password, int maxPlayers) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(escapeJson(name)).append("\",");
        sb.append("\"format\":\"").append(escapeJson(format)).append("\",");
        sb.append("\"port\":").append(port).append(",");
        sb.append("\"game_version\":\"").append(escapeJson(gameVersion)).append("\",");
        sb.append("\"has_password\":").append(hasPassword).append(",");
        sb.append("\"max_players\":").append(maxPlayers);
        if (hasPassword && password != null && !password.isEmpty()) {
            sb.append(",\"password\":\"").append(escapeJson(password)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static List<LobbyRoom> parseRoomsJson(String json) {
        List<LobbyRoom> result = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[")) return result;

        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{' && depth == 0) {
                start = i;
                depth = 1;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    String obj = json.substring(start, i + 1);
                    LobbyRoom room = LobbyRoom.fromJson(obj);
                    if (room != null) {
                        result.add(room);
                    }
                    start = -1;
                }
            }
        }
        return result;
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        idx = json.indexOf(":", idx + search.length());
        if (idx < 0) return "";
        int start = json.indexOf("\"", idx + 1);
        if (start < 0) return "";
        int end = json.indexOf("\"", start + 1);
        if (end < 0) return "";
        return json.substring(start + 1, end);
    }

    private static int extractInt(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        idx = json.indexOf(":", idx + search.length());
        if (idx < 0) return 0;
        int start = idx + 1;
        while (start < json.length() && !Character.isDigit(json.charAt(start)) && json.charAt(start) != '-') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
