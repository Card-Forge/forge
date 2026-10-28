package forge.gamemodes.replay;

import forge.game.Game;
import forge.game.GameType;
import forge.game.player.PlayerView;
import forge.gamemodes.net.server.HostingServer;
import forge.gui.control.GameEventForwarder;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.trackable.TrackableCollection;
import forge.util.IHasForgeLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Records one local game to a replay file. {@code HostedMatch} calls {@link #begin} once the players are set up and
 * {@link #finish} when the game is over; everything in between is driven by the game's own events.
 *
 * <p>Methods run on the game thread, as the delta-sync machinery behind them requires. A session never throws into
 * the match: when anything goes wrong, recording stops and the game carries on.
 */
public final class ReplaySession implements IHasForgeLog {
    private final Game game;
    private final List<PlayerControllerHuman> observed;
    private final ReplayRecorder recorder;
    private final RecordingGuiGame gui;
    private final GameEventForwarder forwarder;

    private ReplaySession(final Game game0, final List<PlayerControllerHuman> humans, final ReplayRecorder recorder0) {
        game = game0;
        observed = new ArrayList<>(humans);
        recorder = recorder0;
        gui = new RecordingGuiGame(recorder0);
        forwarder = new GameEventForwarder(gui);
    }

    /**
     * Starts recording {@code game} from the point of view of its local players, or returns null when replays are
     * off or not wanted for this kind of game (no human playing, or Adventure which runs its own match flow).
     */
    public static ReplaySession begin(final Game game, final List<PlayerControllerHuman> humans) {
        try {
            final boolean wanted = FModel.getPreferences() != null
                    && FModel.getPreferences().getPrefBoolean(FPref.UI_RECORD_REPLAYS);
            final GameType type = game.getRules().getGameType();
            if (!wanted || humans.isEmpty() || type == GameType.Adventure || type == GameType.AdventureEvent) {
                return null;
            }
            // Local games only: one person at this device, nobody connected over the network. A replay is shown from
            // its players' side, so recording several humans would put everyone's hidden cards in one file.
            if (humans.size() > 1 || HostingServer.isHosting()
                    || humans.get(0).getGui() instanceof forge.gamemodes.net.ProtocolGuiGame) {
                return null;
            }
            final ReplayRecorder recorder = new ReplayRecorder(ReplayStore.newFile(), title(game));
            final ReplaySession session = new ReplaySession(game, humans, recorder);
            session.attach();
            return session;
        } catch (final IOException | RuntimeException e) {
            netLog.error(e, "Could not start recording a replay");
            return null;
        }
    }

    private static String title(final Game game) {
        final StringBuilder sb = new StringBuilder();
        for (final forge.game.player.Player p : game.getPlayers()) {
            if (sb.length() > 0) {
                sb.append(" vs ");
            }
            sb.append(p.getName());
        }
        final int played = game.getMatch().getOutcomes().size();
        if (played > 0) {
            sb.append(" (game ").append(played + 1).append(')');
        }
        return sb.toString();
    }

    /** Wires the hidden GUI in the way {@code HostedMatch} wires a remote client's. */
    private void attach() {
        gui.setGameView(game.getView());
        gui.setForwarder(forwarder);
        game.subscribeToEvents(forwarder);
        // Events must be flushed on the game thread before it blocks for any human's input.
        final List<PlayerView> mine = new ArrayList<>();
        for (final PlayerControllerHuman hc : observed) {
            hc.getInputQueue().addObserver(forwarder);
            mine.add(hc.getLocalPlayerView());
        }
        gui.openView(new TrackableCollection<>(mine));
    }

    /** The game is over: write what is left and close the file. */
    public void finish() {
        try {
            // the end-of-game screen comes from the recorded GameFinished event, as on a netplay client
            forwarder.flush();
        } catch (final RuntimeException e) {
            netLog.error(e, "Could not finish the replay cleanly");
        } finally {
            detach();
        }
    }

    /** The game ended abnormally: keep what was recorded, without an ending. */
    public void abort() {
        detach();
    }

    private void detach() {
        for (final PlayerControllerHuman hc : observed) {
            hc.getInputQueue().deleteObserver(forwarder);
        }
        gui.shutdownForwarder();
        recorder.close();
    }
}
