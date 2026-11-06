package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.AntiCombo;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * AntiCombo: the combo pieces and win conditions the opponents show (battlefield, stack, graveyard) are a threat the
 * rollouts try to remove, and a removal spell aims at a key card first. A failure prints what the AI sees and names the
 * role list (ai-card-roles.txt, anti_combo_key and anti_combo_piece) or the constant to adjust.
 */
public class FlatMonteCarloAntiComboTest extends AITest {
    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
    }

    @Test
    public void keyCardsAndPiecesHaveTheirWeightAndOtherCardsNone() {
        setUp();
        AssertJUnit.assertEquals(60, AntiCombo.weight(createCard("Time Vault", opp)));
        AssertJUnit.assertEquals(20, AntiCombo.weight(createCard("Dark Ritual", opp)));
        AssertJUnit.assertEquals(0, AntiCombo.weight(createCard("Grizzly Bears", opp)));
    }

    @Test
    public void aKeyCardOnTheBattlefieldIsAThreatAndLeavingItRemovesTheThreat() {
        setUp();
        AssertJUnit.assertEquals("nothing seen, nothing to fear", 0, AntiCombo.threat(ai));
        final Card vault = addCard("Time Vault", opp);
        AssertJUnit.assertEquals("a key on the battlefield counts in full: " + AntiCombo.describe(ai), 60, AntiCombo.threat(ai));
        AssertJUnit.assertEquals(-60, AntiCombo.term(ai));
        game.getAction().moveTo(ZoneType.Graveyard, vault, null, null);
        AssertJUnit.assertEquals("in the graveyard it counts for 40%: " + AntiCombo.describe(ai), 24, AntiCombo.threat(ai));
        game.getAction().exile(vault, null, null);
        AssertJUnit.assertEquals("exiled it is gone", 0, AntiCombo.threat(ai));
    }

    @Test
    public void twoKeysAreMoreThanTheSumAndTheThreatIsCapped() {
        setUp();
        addCard("Time Vault", opp);
        addCard("Doomsday", opp);
        AssertJUnit.assertEquals("two keys: 120 and the synergy bonus: " + AntiCombo.describe(ai), 180, AntiCombo.threat(ai));
        for (int i = 0; i < 6; i++) {
            addCard("Oath of Druids", opp);
        }
        AssertJUnit.assertEquals("never more than the cap", 300, AntiCombo.threat(ai));
    }

    @Test
    public void whatIsInTheHandOrTheLibraryOfTheOpponentIsNotSeen() {
        setUp();
        addCardToZone("Time Vault", opp, ZoneType.Hand);
        addCardToZone("Doomsday", opp, ZoneType.Library);
        AssertJUnit.assertEquals("a hidden zone must not be counted", 0, AntiCombo.threat(ai));
    }

    @Test
    public void theAisOwnKeyCardsAreNotATarget() {
        setUp();
        final Card mine = addCard("Time Vault", ai);
        final Card theirs = addCard("Time Vault", opp);
        AssertJUnit.assertEquals(0, AntiCombo.rank(ai, mine));
        AssertJUnit.assertTrue("an opponent's key card ranks above any creature", AntiCombo.rank(ai, theirs) > 5000);
        AssertJUnit.assertEquals("the AI's own key cards are no threat", 60, AntiCombo.threat(ai));
    }

    @Test
    public void theMostDangerousCardComesFirst() {
        setUp();
        addCard("Dark Ritual", opp);
        final Card oath = addCard("Oath of Druids", opp);
        AssertJUnit.assertEquals("the key before the piece: " + AntiCombo.describe(ai), oath, AntiCombo.threats(ai).get(0));
    }
}
