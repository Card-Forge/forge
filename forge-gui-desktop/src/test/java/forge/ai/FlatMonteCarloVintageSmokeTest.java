package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The real lists of the Vintage decks that are not built around a combo (Red Prison, Goblins, Eldrazi, Scam, the Psychic
 * Frog and Initiative decks), played for one turn by the Flat MC AI with a hand drawn from the list: the turn must run
 * to its end without an exception and the AI must do something with it (play a land or a spell) when the hand has a land or a free spell. The deck, the hand and
 * the board are printed when it does not.
 */
public class FlatMonteCarloVintageSmokeTest extends AITest {
    private static final String[] DECKS = { "Red Prison", "Goblins", "Eldrazi", "Scam", "4c Psychic Frog",
        "Abzan Initiative", "Mono-White Initiative", "Other Shops" };

    @Test(timeOut = 600000)
    public void eachDeckPlaysATurn() {
        final StringBuilder failures = new StringBuilder();
        for (final String[] deck : FlatMonteCarloVintageMetaTest.NOT_COMBO_DECKS) {
            boolean wanted = false;
            for (final String name : DECKS) {
                wanted |= name.equals(deck[0]);
            }
            if (!wanted) {
                continue;
            }
            final Game game = initAndCreateGame();
            final Player ai = game.getPlayers().get(1);
            for (final String line : deck[1].split("\n")) {
                final int space = line.indexOf(' ');
                if (space > 0) {
                    for (int n = Integer.parseInt(line.substring(0, space)); n > 0; n--) {
                        addCardToZone(line.substring(space + 1).trim(), ai, ZoneType.Library);
                    }
                }
            }
            ai.shuffle(null);
            ai.drawCards(7);
            boolean landInHand = false; // a hand without a land (or a free spell) can do nothing: not a failure of the AI
            for (final forge.game.card.Card c : ai.getCardsIn(ZoneType.Hand)) {
                landInHand |= c.isLand() || c.getCMC() == 0;
            }
            game.AI_FLAT_MONTE_CARLO = true;
            game.AI_TIMEOUT = 2;
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
            game.getAction().checkStateEffects(true);
            try {
                for (int i = 0; i < 600 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                        && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
                    game.getPhaseHandler().mainLoopStep();
                }
            } catch (final RuntimeException e) {
                failures.append(deck[0]).append(" threw ").append(e).append('\n');
                continue;
            }
            if (landInHand && ai.getCardsIn(ZoneType.Battlefield).isEmpty() && ai.getCardsIn(ZoneType.Graveyard).isEmpty()
                    && ai.getCardsIn(ZoneType.Hand).size() >= 7) {
                failures.append(deck[0]).append(" did nothing with its turn (no land, no spell; look at what the guards"
                        + " refuse or at the discovered plays)\n").append(gameStateToString(game)).append('\n');
            }
        }
        AssertJUnit.assertEquals("decks that failed their turn:\n" + failures, 0, failures.length());
    }
}
