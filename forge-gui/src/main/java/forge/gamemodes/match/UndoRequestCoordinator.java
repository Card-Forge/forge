package forge.gamemodes.match;

import java.util.ArrayList;
import java.util.List;

import forge.game.DrawOffer;
import forge.game.Game;
import forge.game.UndoHistory;
import forge.game.player.Player;
import forge.gamemodes.match.input.Input;
import forge.gamemodes.match.input.InputSyncronizedBase;
import forge.gui.FThreads;
import forge.player.PlayerControllerHuman;
import forge.util.Localizer;

/**
 * Takes a player's request to go back to one of their earlier decisions: they pick which, every other
 * human still in the game has to agree (a vote run by {@link DrawOfferCoordinator}), then the game is
 * told to go back (see {@link UndoHistory}).
 */
public final class UndoRequestCoordinator {
    private UndoRequestCoordinator() {}

    /** The player asked to undo (from the game menu). Background thread: picking the point waits on them. */
    public static void request(final PlayerControllerHuman requester) {
        final UndoHistory.Point target = choosePoint(requester, false);
        if (target != null) {
            DrawOfferCoordinator.offerUndo(requester.getGame(), requester.getPlayer(), target);
        }
    }

    /**
     * The player is about to lose: offer to go back instead. Game thread; the caller unwinds it.
     * @return whether going back was agreed
     */
    public static boolean requestBeforeLosing(final PlayerControllerHuman loser) {
        final UndoHistory.Point target = choosePoint(loser, true);
        final Game game = loser.getGame();
        if (target == null || !DrawOfferCoordinator.offerUndo(game, loser.getPlayer(), target)) {
            return false;
        }
        DrawOfferCoordinator.awaitSettled(game);
        return game.getUndoHistory().hasPendingUndo();
    }

    /** Has the requester pick which of their decisions to go back to; null if there is none or they pick none. */
    private static UndoHistory.Point choosePoint(final PlayerControllerHuman requester, final boolean beforeLosing) {
        final Game game = requester.getGame();
        final Player player = requester.getPlayer();
        final UndoHistory history = game.getUndoHistory();
        if (player == null || game.isGameOver() || !history.isEnabled()) {
            return null;
        }
        final Localizer localizer = Localizer.getInstance();
        final List<UndoHistory.Point> targets = history.getUndoTargets(player);
        if (targets.isEmpty()) {
            if (!beforeLosing) {
                requester.getGui().message(localizer.getMessage("lblUndoNothing"), localizer.getMessage("lblUndoLastDecision"));
            }
            return null;
        }
        if (beforeLosing && !requester.getGui().confirm(null, localizer.getMessage("lblUndoInsteadOfLosing"))) {
            return null;
        }
        final List<String> labels = new ArrayList<>();
        for (final UndoHistory.Point point : targets) {
            labels.add(localizer.getMessage("lblUndoPointFmt", labels.size() + 1, describe(point)));
        }
        final String chosen = requester.getGui().oneOrNone(localizer.getMessage("lblUndoChoosePoint"), labels);
        return chosen == null ? null : targets.get(labels.indexOf(chosen));
    }

    /** Everyone asked has agreed: tell the game to go back. */
    static void allowed(final Game game, final DrawOffer offer) {
        final Player requester = offer.getOfferer();
        if (game.getUndoHistory().requestUndo(requester, offer.getUndoTarget())) {
            releaseWaitingPrompts(game);
        } else if (requester.getController() instanceof PlayerControllerHuman pch) {
            // the game moved on while players were deciding, and that point is gone
            final Localizer localizer = Localizer.getInstance();
            FThreads.invokeInBackgroundThread(() -> pch.getGui().message(
                    localizer.getMessage("lblUndoTooLate"), localizer.getMessage("lblUndoLastDecision")));
        }
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
