package forge.gamemodes.net.server;

import forge.game.GameView;
import forge.game.event.GameEvent;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.ProtocolGuiGame;
import forge.gamemodes.net.TrackableSerializer;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.model.FModel;
import forge.trackable.Tracker;

import java.util.List;

/**
 * Server-side proxy for one remote client's GUI. One instance per connected remote player,
 * constructed at lobby time and reused across the match (and across reconnects, via
 * {@link #resetForReconnect}).
 *
 * <p>Adds the network transport to {@link ProtocolGuiGame}: events are wrapped for Java
 * serialization, the channel codec follows the current game's tracker, and bandwidth can
 * be logged.
 */
public class RemoteClientGuiGame extends ProtocolGuiGame {

    private final RemoteClient client;

    // Bandwidth tracking — both sides measured via serialize+compress for apples-to-apples comparison
    private long totalDeltaBytes = 0;
    private long totalFullStateBytes = 0;
    private int deltaPacketCount = 0;
    private final boolean logBandwidth = FModel.getNetPreferences().getPrefBoolean(ForgeNetPreferences.FNetPref.NET_BANDWIDTH_LOGGING);

    public RemoteClientGuiGame(final RemoteClient client) {
        super(client);
        this.client = client;
        client.setGui(this);
    }

    public RemoteClient getClient() {
        return client;
    }

    @Override
    public boolean isLibgdxPort() {
        return client.isLibgdx();
    }

    @Override
    protected List<Object> encodeEvents(final List<GameEvent> events, final Tracker tracker) {
        return TrackableSerializer.wrapEvents(events, tracker);
    }

    @Override
    protected void onTrackerBound(final Tracker tracker, final int consumerId) {
        client.setCodecTracker(tracker, consumerId);
    }

    @Override
    protected void onDeltaSent(final DeltaPacket delta, final List<GameEvent> events, final GameView gameView) {
        if (!logBandwidth) {
            return;
        }
        final Tracker tracker = gameView.getTracker();
        final int deltaSize = TrackableSerializer.measureSize(delta, tracker);
        final int stateOnlyFullSize = TrackableSerializer.measureSize(gameView, null);
        final int fullStateSize = stateOnlyFullSize + (events.isEmpty() ? 0 : TrackableSerializer.measureSize(events, tracker));

        totalDeltaBytes += deltaSize;
        totalFullStateBytes += fullStateSize;
        deltaPacketCount++;

        final int savings = fullStateSize > 0 ? (int)((1.0 - (double)deltaSize / fullStateSize) * 100) : 0;
        if (events.isEmpty()) {
            netLog.info("[DeltaSync] Packet #{}: Delta={} bytes, FullState={} bytes, Savings={}%",
                deltaPacketCount, deltaSize, fullStateSize, savings);
        } else {
            final int stateOnlyDeltaSize = TrackableSerializer.measureSize(delta.withoutEvents(), tracker);
            netLog.info("[DeltaSync] Packet #{}: Delta={} bytes, FullState={} bytes, Savings={}%, StateOnlyDelta={} bytes, StateOnlyFull={} bytes",
                deltaPacketCount, deltaSize, fullStateSize, savings, stateOnlyDeltaSize, stateOnlyFullSize);
        }
        netLog.info("[DeltaSync]   Cumulative: Delta={}, FullState={}, Savings={}%",
            totalDeltaBytes, totalFullStateBytes,
            totalFullStateBytes > 0 ? (int)((1.0 - (double)totalDeltaBytes / totalFullStateBytes) * 100) : 0);
    }

    @Override
    public String toString() {
        final GameView gv = getGameView();
        return String.format("RemoteClientGuiGame[client=%d, deltaSyncEnabled=%b, gameView=%s]",
                client.getIndex(), useDeltaSync,
                gv != null ? "GameView@" + Integer.toHexString(System.identityHashCode(gv)) : "null");
    }
}
