package forge.gamemodes.replay;

import forge.game.event.GameEvent;
import forge.gamemodes.net.ProtocolGuiGame;
import forge.gamemodes.net.TrackableSerializer;
import forge.trackable.Tracker;

import java.util.List;

/**
 * A spectator that is never shown: it is fed like a remote client (state deltas and game events), and what it would
 * have sent over the wire goes into a replay file.
 */
final class RecordingGuiGame extends ProtocolGuiGame {
    private final ReplayRecorder recorder;

    RecordingGuiGame(final ReplayRecorder recorder0) {
        super(recorder0);
        recorder = recorder0;
    }

    @Override
    protected List<Object> encodeEvents(final List<GameEvent> events, final Tracker tracker) {
        return TrackableSerializer.wrapEvents(events, tracker);
    }

    @Override
    protected void onTrackerBound(final Tracker tracker, final int consumerId) {
        recorder.setCodecTracker(tracker, consumerId);
    }
}
