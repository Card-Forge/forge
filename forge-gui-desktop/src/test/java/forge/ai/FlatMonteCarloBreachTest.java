package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Underworld Breach with Brain Freeze, step by step, to find out how far the AI gets before a Breach combo is built:
 * 1. the escape ability is offered for a card in the graveyard, 2. it can be played and paid, 3. the AI casts it in a
 * turn. Each failure says which step is missing, so that the next change goes where it is needed.
 */
public class FlatMonteCarloBreachTest extends AITest {
    private Game game;
    private Player ai;
    private Card freeze;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        addCards("Mountain", 2, ai);
        addCards("Island", 3, ai);
        addCard("Underworld Breach", ai);
        freeze = addCardToZone("Brain Freeze", ai, ZoneType.Graveyard);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Dark Ritual", ai, ZoneType.Graveyard);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
    }

    private SpellAbility escape() {
        final List<SpellAbility> abilities = freeze.getAllPossibleAbilities(ai, true);
        for (final SpellAbility sa : abilities) {
            if (sa.isSpell() && sa.isEscape()) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        return null;
    }

    @Test
    public void step1TheEscapeOfTheCardInTheGraveyardIsOffered() {
        setUp();
        AssertJUnit.assertNotNull("Brain Freeze has no escape ability in the graveyard: the static of Underworld Breach does"
                + " not reach getAllPossibleAbilities, so the AI never sees the play\n" + gameStateToString(game), escape());
    }

    @Test
    public void step2TheEscapeCanBePaid() {
        setUp();
        final SpellAbility sa = escape();
        AssertJUnit.assertNotNull("step 1 failed first", sa);
        AssertJUnit.assertTrue("the escape is offered but cannot be paid (mana {1}{U}, three other cards to exile)\n"
                + gameStateToString(game), sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false));
    }

    /**
     * A lone Brain Freeze for three cards is no play the AI wants (the first run of this test showed it passes), so the
     * opponent has three cards left in the library: the escape kills, and nothing else is to be done with the mana.
     */
    @Test(timeOut = 120000)
    public void step3TheAiCastsTheEscapeInItsTurn() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        while (opp.getCardsIn(ZoneType.Library).size() > 3) {
            opp.getZone(ZoneType.Library).remove(opp.getCardsIn(ZoneType.Library).get(0));
        }
        while (opp.getCardsIn(ZoneType.Library).size() < 3) {
            addCardToZone("Island", opp, ZoneType.Library);
        }
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        for (int i = 0; i < 600 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        AssertJUnit.assertTrue("the AI never cast Brain Freeze from the graveyard although it empties the library of the"
                + " opponent: either the escape is not offered to the rollouts (FlatMonteCarloPicker.isDiscoverable only"
                + " takes some plays) or the rollouts do not see the win (the opponent drawing from an empty library)\n"
                + gameStateToString(game), opp.getCardsIn(ZoneType.Library).isEmpty() || game.isGameOver());
    }
}
