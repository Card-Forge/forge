package forge.ai.ability;

import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.ComputerUtilMana;
import forge.ai.LobbyPlayerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The decision logic of {@link RollPlanarDiceAi}: when the AI wants to roll the planar die on a plane, with and without
 * an AIRollPlanarDieParams hint, and when it has the mana for a roll that costs extra.
 *
 * Every test runs under the Reckless profile (roll chance 100, hesitation 0, minimum turn 1), so no chance is involved
 * and the answers do not depend on the card or turn ids of the test game. A plane is stood in for by any card: the
 * logic only reads its AIRollPlanarDieParams SVar.
 *
 * mvn -pl forge-gui-desktop test -Dtest=RollPlanarDiceAiTest -DfailIfNoTests=false
 */
public class RollPlanarDiceAiTest extends AITest {
    private final RollPlanarDiceAi planarAi = new RollPlanarDiceAi();
    private Game game;
    private Player ai;

    /** A game with the AI (second player, as in AiReportScenarioTest) under the Reckless profile, in the given phase on its own turn. */
    private void start(final String phase) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Reckless");
        game.getPhaseHandler().devModeSet(PhaseType.valueOf(phase), ai);
    }

    /** The stand-in for the active plane; {@code hint} null leaves the card without the SVar. */
    private Card plane(final String hint) {
        final Card plane = addCardToZone("Grizzly Bears", ai, ZoneType.Exile);
        if (hint != null) {
            plane.setSVar("AIRollPlanarDieParams", hint);
        }
        return plane;
    }

    // planes without a hint

    @Test
    public void planeWithoutHintRollsInMain2ByProfile() {
        start("MAIN2");
        if (game.getPhaseHandler().getTurn() < 1) {
            throw new SkipException("the test game is at turn 0, the Reckless profile rolls from turn 1");
        }
        AssertJUnit.assertTrue(planarAi.willRollOnPlane(ai, plane(null)));
    }

    @Test
    public void planeWithoutHintWaitsForMain2() {
        start("MAIN1");
        AssertJUnit.assertFalse(planarAi.willRollOnPlane(ai, plane(null)));
    }

    // planes with a hint

    @Test
    public void hintWithoutModeNeverRolls() {
        start("MAIN2");
        AssertJUnit.assertFalse(planarAi.willRollOnPlane(ai, plane("MinTurn$ 0")));
    }

    @Test
    public void modeNeverNeverRolls() {
        start("MAIN2");
        AssertJUnit.assertFalse(planarAi.willRollOnPlane(ai, plane("Mode$ Never | MinTurn$ 0")));
    }

    @Test
    public void modeAlwaysRollsInMain2() {
        start("MAIN2");
        AssertJUnit.assertTrue(planarAi.willRollOnPlane(ai, plane("Mode$ Always | MinTurn$ 0")));
    }

    @Test
    public void modeAlwaysWaitsForMain2UnlessRollInMain1() {
        start("MAIN1");
        AssertJUnit.assertFalse(planarAi.willRollOnPlane(ai, plane("Mode$ Always | MinTurn$ 0")));
        AssertJUnit.assertTrue(planarAi.willRollOnPlane(ai, plane("Mode$ Always | MinTurn$ 0 | RollInMain1$ True")));
    }

    @Test
    public void minTurnInTheFutureHoldsTheRoll() {
        start("MAIN2");
        final int turn = game.getPhaseHandler().getTurn();
        AssertJUnit.assertFalse(planarAi.willRollOnPlane(ai, plane("Mode$ Always | MinTurn$ " + (turn + 1))));
    }

    @Test
    public void malformedParameterIsSkippedNotThrown() {
        start("MAIN2");
        // "garbage" has no $: it used to throw an ArrayIndexOutOfBoundsException, which made the AI pass its whole priority
        AssertJUnit.assertTrue(planarAi.willRollOnPlane(ai, plane("Mode$ Always | garbage | MinTurn$ 0")));
    }

    @Test
    public void cardsInHandLimitIsKept() {
        start("MAIN2");
        final String hint = "Mode$ Always | MinTurn$ 0 | CardsInHandLE$ 0";
        AssertJUnit.assertTrue(planarAi.willRollOnPlane(ai, plane(hint)));
        addCardToZone("Island", ai, ZoneType.Hand);
        AssertJUnit.assertFalse(planarAi.willRollOnPlane(ai, plane(hint)));
    }

    // the chance is decided once

    @Test
    public void stablePercentGivesTheSameAnswerEveryTime() {
        start("MAIN2");
        final Card plane = plane(null);
        AssertJUnit.assertFalse(RollPlanarDiceAi.stablePercent(ai, plane, 1, 0));
        AssertJUnit.assertTrue(RollPlanarDiceAi.stablePercent(ai, plane, 1, 100));
        final boolean first = RollPlanarDiceAi.stablePercent(ai, plane, 1, 50);
        for (int i = 0; i < 20; i++) {
            AssertJUnit.assertEquals(first, RollPlanarDiceAi.stablePercent(ai, plane, 1, 50));
        }
    }

    // the mana for a roll that costs extra

    @Test
    public void extraRollNeedsTheMana() {
        start("MAIN2");
        AssertJUnit.assertEquals(0, ComputerUtilMana.getAvailableManaEstimate(ai));
        AssertJUnit.assertFalse(planarAi.hasSpareManaForExtraRoll(ai, 1));
    }

    @Test
    public void extraRollWithNothingElseToSpendTheManaOn() {
        start("MAIN2");
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        AssertJUnit.assertTrue(planarAi.hasSpareManaForExtraRoll(ai, 1));
        addCardToZone("Island", ai, ZoneType.Hand); // a land in hand wants no mana
        AssertJUnit.assertTrue(planarAi.hasSpareManaForExtraRoll(ai, 1));
    }

    @Test
    public void extraRollIsNotWorthTheManaOfACastableSpell() {
        start("MAIN2");
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        AssertJUnit.assertFalse(planarAi.hasSpareManaForExtraRoll(ai, 1));
    }

    @Test
    public void extraRollIsFineWhenTheSpellInHandCannotBeCastAnyway() {
        start("MAIN2");
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Plains", ai, ZoneType.Battlefield);
        addCardToZone("Wurmcoil Engine", ai, ZoneType.Hand); // mana value 6
        AssertJUnit.assertTrue(planarAi.hasSpareManaForExtraRoll(ai, 1));
    }
}
