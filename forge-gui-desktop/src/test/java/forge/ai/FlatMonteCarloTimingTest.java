package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * When the Flat Monte Carlo AI acts: nothing in its own upkeep or draw step, no flash permanent on its own turn, no
 * enters-the-battlefield exile with nothing to aim at (issues 12759, 12761, 12762), and Aether Vials that do not all
 * stop at the same number of counters (issue 12765), and a replicate that is not paid for again and again (issue 12760).
 */
public class FlatMonteCarloTimingTest extends AITest {
    private static final int LONG = 240_000;

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
    }

    private String nextPlay(final PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, ai);
        game.getAction().checkStateEffects(true);
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    @Test(timeOut = LONG)
    public void nothingIsCastInTheUpkeepOrTheDrawStep() {
        setUp();
        addCards("Mountain", 2, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card giant = addCardToZone("Hill Giant", opp, ZoneType.Battlefield);
        giant.setSickness(false);
        AssertJUnit.assertNull("upkeep", nextPlay(PhaseType.UPKEEP));
        AssertJUnit.assertNull("draw step", nextPlay(PhaseType.DRAW));
    }

    @Test(timeOut = LONG)
    public void aFlashCreatureIsNotCastInTheMainPhase() {
        setUp();
        addCards("Forest", 2, ai);
        addCardToZone("Ambush Viper", ai, ZoneType.Hand);
        AssertJUnit.assertNotSame("first main", "Ambush Viper", nextPlay(PhaseType.MAIN1));
        AssertJUnit.assertNotSame("second main", "Ambush Viper", nextPlay(PhaseType.MAIN2));
    }

    @Test(timeOut = LONG)
    public void anExileOnEnteringIsNotCastWithNothingToExile() {
        setUp();
        addCards("Plains", 6, ai);
        addCardToZone("Leyline Binding", ai, ZoneType.Hand);
        AssertJUnit.assertNotSame("first main", "Leyline Binding", nextPlay(PhaseType.MAIN1));
        AssertJUnit.assertNotSame("second main", "Leyline Binding", nextPlay(PhaseType.MAIN2));
    }

    @Test(timeOut = LONG)
    public void twoVialsAtTheSameNumberDoNotBothStop() {
        setUp();
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        for (int i = 0; i < 2; i++) {
            addCardToZone("Hill Giant", ai, ZoneType.Library);
        }
        final Card first = addCardToZone("Aether Vial", ai, ZoneType.Battlefield);
        final Card second = addCardToZone("Aether Vial", ai, ZoneType.Battlefield);
        first.addCounterInternal(CounterType.getType("CHARGE"), 2, ai, false, null, null);
        second.addCounterInternal(CounterType.getType("CHARGE"), 2, ai, false, null, null);
        final AiController brains = ((PlayerControllerAi) ai.getController()).getAi();
        final SpellAbility trigger = second.getTriggers().get(0).ensureAbility();
        trigger.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("the other Vial is already on 2: this one goes for 3", brains.doTrigger(trigger, false));
    }

    @Test(timeOut = LONG)
    public void aLoneVialStaysOnTheBestNumber() {
        setUp();
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        addCardToZone("Hill Giant", ai, ZoneType.Library);
        final Card vial = addCardToZone("Aether Vial", ai, ZoneType.Battlefield);
        vial.addCounterInternal(CounterType.getType("CHARGE"), 2, ai, false, null, null);
        final AiController brains = ((PlayerControllerAi) ai.getController()).getAi();
        final SpellAbility trigger = vial.getTriggers().get(0).ensureAbility();
        trigger.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("2 is where most of the creatures are", brains.doTrigger(trigger, false));
    }

    private SpellAbility onTheStack(final String name, final Player owner) {
        final Card spell = addCardToZone(name, owner, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(owner);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        return sa;
    }

    private int replicatesPaid() {
        final Card consign = addCardToZone("Consign to Memory", ai, ZoneType.Hand);
        final SpellAbility cast = consign.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        for (final KeywordInterface k : consign.getKeywords()) {
            if (k.getKeyword() == Keyword.REPLICATE) {
                return ai.getController().chooseNumberForKeywordCost(cast, new Cost("1", false), k, "", 10);
            }
        }
        throw new IllegalStateException("no replicate");
    }

    @Test(timeOut = LONG)
    public void replicateIsNotPaidWithOneTargetToCounter() {
        setUp();
        addCards("Island", 5, ai);
        onTheStack("Sol Ring", opp);
        AssertJUnit.assertEquals("one colorless spell: one counterspell", 0, replicatesPaid());
    }

    @Test(timeOut = LONG)
    public void replicateIsPaidOncePerOtherTarget() {
        setUp();
        addCards("Island", 5, ai);
        onTheStack("Sol Ring", opp);
        onTheStack("Mind Stone", opp);
        AssertJUnit.assertEquals("two colorless spells: the spell and one copy", 1, replicatesPaid());
    }

    @Test(timeOut = LONG)
    public void replicateIsPaidOncePerOtherTargetWithThreeSpells() {
        setUp();
        addCards("Island", 5, ai);
        onTheStack("Sol Ring", opp);
        onTheStack("Mind Stone", opp);
        onTheStack("Wayfarer's Bauble", opp);
        AssertJUnit.assertEquals("three colorless spells: the spell and two copies", 2, replicatesPaid());
    }

    @Test(timeOut = LONG)
    public void replicateDoesNotCountTheAisOwnSpells() {
        setUp();
        addCards("Island", 5, ai);
        onTheStack("Sol Ring", ai);
        onTheStack("Mind Stone", opp);
        AssertJUnit.assertEquals("only the opponent's spell is a target worth a copy", 0, replicatesPaid());
    }
}
