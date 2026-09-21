package forge.gamemodes.net;

/**
 * POJO representing a room listed in the public lobby.
 */
public class LobbyRoom {
    private final String id;
    private final String name;
    private final String format;
    private final String gameVersion;
    private final boolean hasPassword;
    private final int playerCount;
    private final int maxPlayers;
    private final String relayHost;
    private final int relayPort;
    private final boolean useRelay;

    public LobbyRoom(String id, String name, String format,
                     String gameVersion, boolean hasPassword, int playerCount, int maxPlayers,
                     String relayHost, int relayPort, boolean useRelay) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.gameVersion = gameVersion;
        this.hasPassword = hasPassword;
        this.playerCount = playerCount;
        this.maxPlayers = maxPlayers;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.useRelay = useRelay;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getFormat()      { return format; }
    public String getGameVersion() { return gameVersion; }
    public boolean isHasPassword() { return hasPassword; }
    public int getPlayerCount()    { return playerCount; }
    public int getMaxPlayers()     { return maxPlayers; }
    public String getRelayHost()   { return relayHost; }
    public int getRelayPort()      { return relayPort; }
    public boolean isUseRelay()    { return useRelay; }

    /**
     * Returns the address to connect to.
     * If a relay is available, returns relay_host:relay_port.
     * Returns null if no relay is configured.
     */
    public String getConnectAddress() {
        if (useRelay && relayHost != null && !relayHost.isEmpty() && relayPort > 0) {
            return relayHost + ":" + relayPort;
        }
        return null;
    }

    public static LobbyRoom fromJson(String json) {
        try {
            String id = extractString(json, "id");
            String name = extractString(json, "name");
            String format = extractString(json, "format");
            String gameVersion = extractString(json, "game_version");
            boolean hasPassword = extractBool(json, "has_password");
            int playerCount = extractInt(json, "player_count");
            int maxPlayers = extractInt(json, "max_players");
            String relayHost = extractString(json, "relay_host");
            int relayPort = extractInt(json, "relay_port");
            boolean useRelay = extractBool(json, "use_relay");

            return new LobbyRoom(id, name, format, gameVersion,
                    hasPassword, playerCount, maxPlayers, relayHost, relayPort, useRelay);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractString(String json, String key) {
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

    private static boolean extractBool(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return false;
        idx = json.indexOf(":", idx + search.length());
        if (idx < 0) return false;
        String after = json.substring(idx + 1).trim();
        return after.startsWith("true");
    }

    @Override
    public String toString() {
        return name + " (" + format + ") - " + playerCount + "/" + maxPlayers + (hasPassword ? " [LOCKED]" : "");
    }
}
