package forge.net;

/**
 * Enumeration of test modes for game testing infrastructure.
 *
 * Allows easy configuration and switching between different test scenarios:
 * - LOCAL: Non-network testing using LocalLobby (pure game engine, no network overhead)
 * - NETWORK_LOCAL: Network testing with all local AI players via ServerGameLobby
 * - NETWORK_REMOTE: Full network testing with HeadlessNetworkClient as remote player
 *
 * Part of the unified test configuration system for headless game testing.
 */
public enum GameTestMode {
    /**
     * Local (non-network) game testing.
     * Uses LocalLobby with all AI players local to the process.
     * No network stack involvement: no sockets, no serialization, no delta sync.
     * Best for: Game rules regression, AI behavior validation, performance baselines.
     */
    LOCAL("Local (non-network)", "Pure game engine, no network overhead"),

    /**
     * Network game testing with all local players.
     * Uses ServerGameLobby with FServerManager, but all AI players are local.
     * Network stack is active but no actual remote connections.
     * Best for: Server-side logic testing, network overhead measurement.
     */
    NETWORK_LOCAL("Network (local players)", "ServerGameLobby, all local AI players"),

    /**
     * Full network game testing with remote client.
     * Uses ServerGameLobby with HeadlessNetworkClient connecting via TCP.
     * Tests actual network protocol including delta sync.
     * Best for: Delta sync validation, reconnection testing, bandwidth measurement.
     */
    NETWORK_REMOTE("Network (remote client)", "ServerGameLobby + HeadlessNetworkClient");

    private final String displayName;
    private final String description;

    GameTestMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the human-readable display name for this mode.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of this mode.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this mode involves network testing.
     *
     * @return true if NETWORK_LOCAL or NETWORK_REMOTE, false if LOCAL
     */
    public boolean isNetworkMode() {
        return this != LOCAL;
    }

    /**
     * Check if this mode uses a remote client.
     *
     * @return true if NETWORK_REMOTE, false otherwise
     */
    public boolean usesRemoteClient() {
        return this == NETWORK_REMOTE;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name(), displayName);
    }
}
