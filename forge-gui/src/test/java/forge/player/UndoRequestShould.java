package forge.player;

import forge.game.UndoHistory;
import forge.gamemodes.match.DrawOfferCoordinator;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A request to undo a decision is put to the other human players through the draw-offer vote. These
 * pin what the vote leads to: the undo when they agree, nothing when they don't, and never a draw.
 */
public class UndoRequestShould {

    @Test
    public void goAheadOnceTheOtherHumanAccepts() {
        HumanControlledGame g = new HumanControlledGame();
        UndoHistory.Point decision = anEarlierDecisionOfTheHuman(g);

        DrawOfferCoordinator.offerUndo(g.game, g.human, decision);
        assertThat(g.game.getUndoHistory().hasPendingUndo()).isFalse();

        DrawOfferCoordinator.respond(g.game, g.opponent, true);

        assertThat(g.game.getUndoHistory().hasPendingUndo()).isTrue();
        assertThat(g.game.getDrawOffer()).isNull();
        assertThat(g.game.isGameOver()).isFalse();
    }

    @Test
    public void beDroppedWhenTheOtherHumanDeclines() {
        HumanControlledGame g = new HumanControlledGame();
        UndoHistory.Point decision = anEarlierDecisionOfTheHuman(g);

        DrawOfferCoordinator.offerUndo(g.game, g.human, decision);
        DrawOfferCoordinator.respond(g.game, g.opponent, false);

        assertThat(g.game.getUndoHistory().hasPendingUndo()).isFalse();
        assertThat(g.game.getDrawOffer()).isNull();
    }

    private static UndoHistory.Point anEarlierDecisionOfTheHuman(final HumanControlledGame g) {
        final UndoHistory history = g.game.getUndoHistory();
        history.setEnabled(true);
        final UndoHistory.Point decision = history.recordPriority(g.human);
        decision.setOutcome("passed");
        return decision;
    }
}
