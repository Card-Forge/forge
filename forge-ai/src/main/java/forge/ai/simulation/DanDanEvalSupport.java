package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Two-player DanDan helpers for {@link GameStateEvaluator}: shared library top and who draws it next.
 * <p>
 * Manual verification: enable {@link GameStateEvaluator#setDebugging(boolean)} and inspect {@code DanDan shared top}
 * lines during a 2-player DanDan simulation.
 * </p>
 */
final class DanDanEvalSupport {

    private DanDanEvalSupport() {
    }

    static boolean applies(final Game game) {
        return game.getRules().isDanDan() && game.getPlayers().size() == 2;
    }

    /**
     * Index 0 of the shared library (player 0 hosts the shared zone in DanDan).
     */
    static Card sharedLibraryTop(final Game game) {
        final CardCollectionView lib = game.getPlayers().get(0).getCardsIn(ZoneType.Library);
        return lib.isEmpty() ? null : lib.getFirst();
    }

    /**
     * Which player will take the next draw step that pulls from the shared library.
     * Uses {@link PhaseType#MAIN1}: phases before main (Untap, Upkeep, Draw) still belong to the
     * active player's upcoming draw this turn.
     */
    static Player nextLibraryDrawRecipient(final Game game) {
        final PhaseHandler ph = game.getPhaseHandler();
        if (ph.getPhase().isBefore(PhaseType.MAIN1)) {
            return ph.getPlayerTurn();
        }
        return ph.getNextTurn();
    }
}
