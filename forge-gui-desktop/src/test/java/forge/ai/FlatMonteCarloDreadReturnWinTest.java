package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.DreadReturnWin;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Oops! All Spells, the last step: the Balustrade Spy has milled the library and Thassa's Oracle is in the graveyard.
 * The AI must pay the flashback of Dread Return with three creatures and aim it at the Oracle. A failure prints the
 * board and says which condition of DreadReturnWin.sureCast was not met.
 */
public class FlatMonteCarloDreadReturnWinTest extends AITest {
    private Game game;
    private Player ai;

    private List<SpellAbility> setUp(final int library, final int creatures, final boolean oracleInYard) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int i = 0; i < library; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        for (int i = 0; i < creatures; i++) {
            addCard("Narcomoeba", ai);
        }
        if (oracleInYard) {
            addCardToZone("Thassa's Oracle", ai, ZoneType.Graveyard);
        }
        addCards("Swamp", 4, ai);
        final Card dreadReturn = addCardToZone("Dread Return", ai, ZoneType.Graveyard);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        return dreadReturn.getAllPossibleAbilities(ai, true);
    }

    @Test
    public void theFlashbackIsPaidWithThreeCreaturesForTheOracle() {
        final List<SpellAbility> candidates = setUp(0, 3, true);
        final SpellAbility sa = DreadReturnWin.sureCast(ai, candidates);
        AssertJUnit.assertNotNull("the flashback of Dread Return should be played (look at DreadReturnWin.sureCast: the"
                + " library is empty, the Oracle is in the graveyard, three creatures)\n" + gameStateToString(game), sa);
        AssertJUnit.assertEquals("Thassa's Oracle", sa.getTargets().getFirstTargetedCard().getName());
    }

    @Test
    public void withTwoCreaturesThereIsNothingToPayWith() {
        ai(0, 2, true);
    }

    private Player ai(final int library, final int creatures, final boolean oracle) {
        final List<SpellAbility> candidates = setUp(library, creatures, oracle);
        AssertJUnit.assertNull("the play needs " + (library > 2 ? "an empty library" : creatures < 3 ? "three creatures" : "the Oracle")
                + "\n" + gameStateToString(game), DreadReturnWin.sureCast(ai, candidates));
        return ai;
    }

    @Test
    public void notWhileTheLibraryIsFull() {
        ai(10, 3, true);
    }

    @Test
    public void notWithoutTheOracleInTheGraveyard() {
        ai(0, 3, false);
    }
}
