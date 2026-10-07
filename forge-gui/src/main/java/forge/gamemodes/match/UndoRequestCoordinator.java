package forge.gamemodes.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import forge.game.Game;
import forge.game.UndoHistory;
import forge.game.player.Player;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputSyncronizedBase;
import forge.player.PlayerControllerHuman;
import forge.util.Localizer;

/**
 * Takes a player's request to go back to one of their earlier decisions: they pick which, every other
 * human still in the game has to agree, then the game is told to go back (see {@link UndoHistory}).
 * <p>
 * Runs on a background thread, since it waits on players; the prompts it uses work the same for local
 * and network players. AI opponents are never asked.
 */
public final class UndoRequestCoordinator {
    private UndoRequestCoordinator() {}

    private static final Set<Game> inFlight = Collections.newSetFromMap(new WeakHashMap<>());

    /** The player asked to undo (from the game menu). Background thread. */
    public static void request(final PlayerControllerHuman requester) {
        if (agree(requester, false)) {
            releaseWaitingPrompts(requester.getGame());
        }
    }

    /**
     * The player is about to lose: offer to go back instead. Game thread; the caller unwinds it.
     * @return whether going back was agreed
     */
    public static boolean requestBeforeLosing(final PlayerControllerHuman loser) {
        return agree(loser, true);
    }

    /** Settles on a point with the requester and every other human, and marks it pending. */
    private static boolean agree(final PlayerControllerHuman requester, final boolean beforeLosing) {
        final Game game = requester.getGame();
        final Player player = requester.getPlayer();
        final UndoHistory history = game.getUndoHistory();
        if (player == null || game.isGameOver() || !history.isEnabled()) {
            return false;
        }
        synchronized (inFlight) {
            if (!inFlight.add(game)) {
                return false; // one request at a time
            }
        }
        try {
            return ask(requester, game, player, history, beforeLosing);
        } finally {
            synchronized (inFlight) {
                inFlight.remove(game);
            }
        }
    }

    private static boolean ask(final PlayerControllerHuman requester, final Game game, final Player player,
                               final UndoHistory history, final boolean beforeLosing) {
        final Localizer localizer = Localizer.getInstance();
        final String title = localizer.getMessage("lblUndoLastDecision");

        final List<UndoHistory.Point> targets = history.getUndoTargets(player);
        if (targets.isEmpty()) {
            if (!beforeLosing) {
                requester.getGui().message(localizer.getMessage("lblUndoNothing"), title);
            }
            return false;
        }
        if (beforeLosing && !requester.getGui().confirm(null, localizer.getMessage("lblUndoInsteadOfLosing"))) {
            return false;
        }
        final List<String> labels = new ArrayList<>();
        for (final UndoHistory.Point point : targets) {
            labels.add(localizer.getMessage("lblUndoPointFmt", labels.size() + 1, describe(point)));
        }
        final String chosen = requester.getGui().oneOrNone(localizer.getMessage("lblUndoChoosePoint"), labels);
        if (chosen == null) {
            return false;
        }
        final UndoHistory.Point target = targets.get(labels.indexOf(chosen));

        for (final Player other : game.getPlayers()) {
            if (other == player || !(other.getController() instanceof PlayerControllerHuman pch)) {
                continue;
            }
            final String question = localizer.getMessage("lblUndoApprove", other.getName(), player.getName(), describe(target));
            if (!pch.getGui().confirm(null, question)) {
                requester.getGui().message(localizer.getMessage("lblUndoDeclined", other.getName()), title);
                return false;
            }
        }

        if (!history.requestUndo(player, target)) {
            // the game moved on while players were deciding, and that point is gone
            requester.getGui().message(localizer.getMessage("lblUndoTooLate"), title);
            return false;
        }
        return true;
    }

    /** Whichever human the game is waiting on gives way; if none is, the next one asked does. */
    private static void releaseWaitingPrompts(final Game game) {
        for (final Player p : game.getRegisteredPlayers()) {
            if (p.getController() instanceof PlayerControllerHuman pch) {
                final Input input = pch.getInputQueue().getInput();
                if (input instanceof InputSyncronizedBase waiting && waiting.isUndoPoint()) {
                    waiting.stop();
                }
            }
        }
    }

    public static String describe(final UndoHistory.Point point) {
        final Localizer localizer = Localizer.getInstance();
        final String phase = point.getPhase() == null ? "" : point.getPhase().nameForUi;
        final String outcome = point.getOutcome() == null ? "" : point.getOutcome();
        return localizer.getMessage("lblUndoPointDescription", point.getTurn(), phase, outcome);
    }
}
