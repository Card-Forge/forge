package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.ai.simulation.FlatMonteCarloStats;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Tests for "other targets as candidates": for a play with a single target, the Monte Carlo AI also samples aiming it
 * somewhere else than where the heuristics aimed it (the face instead of a creature, the next best creature), on the
 * same side only, so that a burn spell is never tried on the AI's own creature.
 */
public class FlatMonteCarloTargetTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 180_000;

    private static AiController brainOf(final Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    private Game newGame() {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        return game;
    }

    private SpellAbility spellOf(final Card c) {
        return c.getSpellAbilities().get(0);
    }

    // ------------------------------------------------------------------ which targets are offered

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testAlternativesStayOnTheSideTheHeuristicsAimedAt() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        final Card mine = addCard("Hill Giant", ai);
        final Card bears = addCard("Grizzly Bears", opp);
        final Card goblin = addCard("Raging Goblin", opp);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = spellOf(bolt);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.applyTarget(sa, bears));

        final List<GameObject> others = FlatMonteCarloPicker.alternativeTargets(ai, sa, 4);
        AssertJUnit.assertFalse("the AI must never be offered its own creature or itself: " + others, others.contains(mine));
        AssertJUnit.assertFalse(others.contains(ai));
        AssertJUnit.assertFalse("the heuristic target is not offered again", others.contains(bears));
        AssertJUnit.assertTrue("the opponent's face is an option for burn: " + others, others.contains(opp));
        AssertJUnit.assertTrue("so is the other creature: " + others, others.contains(goblin));
        AssertJUnit.assertSame("the face comes first", opp, others.get(0));
        // the heuristics' own choice is still what the play is aimed at
        AssertJUnit.assertTrue(sa.getTargets().contains(bears));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testAlternativesForABeneficialSpellAreOwnPermanents() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Forest", 2, ai);
        final Card giant = addCard("Hill Giant", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card theirs = addCard("Grizzly Bears", opp);
        final Card growth = addCardToZone("Giant Growth", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = spellOf(growth);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.applyTarget(sa, giant));
        final List<GameObject> others = FlatMonteCarloPicker.alternativeTargets(ai, sa, 4);
        AssertJUnit.assertTrue(others.toString(), others.contains(bears));
        AssertJUnit.assertFalse("a pump spell is not tried on the opponent's creature", others.contains(theirs));
        AssertJUnit.assertFalse(others.contains(opp));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testNothingIsOfferedForPlaysWithoutASingleTarget() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Plains", 4, ai);
        addCard("Grizzly Bears", opp);
        final Card creature = addCardToZone("Hill Giant", ai, ZoneType.Hand);
        final Card wrath = addCardToZone("Wrath of God", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(FlatMonteCarloPicker.alternativeTargets(ai, spellOf(creature), 4).isEmpty());
        AssertJUnit.assertTrue(FlatMonteCarloPicker.alternativeTargets(ai, spellOf(wrath), 4).isEmpty());
        AssertJUnit.assertFalse("nothing to aim at", FlatMonteCarloPicker.applyTarget(spellOf(wrath), opp));
        // nothing set up by the heuristics yet: no starting point, so no alternatives either
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.alternativeTargets(ai, spellOf(bolt), 4).isEmpty());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testIllegalTargetsAreRefusedAndLeaveTheOldTargetAlone() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        final Card bears = addCard("Grizzly Bears", opp);
        final Card protectedCreature = addCard("Black Knight", opp); // protection from white, not red
        final Card shrouded = addCard("Sylvan Caryatid", opp); // hexproof
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = spellOf(bolt);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.applyTarget(sa, bears));
        AssertJUnit.assertFalse("hexproof creatures cannot be targeted", FlatMonteCarloPicker.applyTarget(sa, shrouded));
        AssertJUnit.assertTrue("the old target is untouched", sa.getTargets().contains(bears));
        AssertJUnit.assertEquals(1, sa.getTargets().size());
        AssertJUnit.assertTrue(FlatMonteCarloPicker.applyTarget(sa, protectedCreature));
        AssertJUnit.assertTrue(sa.getTargets().contains(protectedCreature));
        AssertJUnit.assertEquals("a new target replaces the old one", 1, sa.getTargets().size());
        AssertJUnit.assertFalse("not a legal target at all: a card in hand", FlatMonteCarloPicker.applyTarget(sa, bolt));

        final List<GameObject> others = FlatMonteCarloPicker.alternativeTargets(ai, sa, 4);
        AssertJUnit.assertFalse("never offers what cannot be targeted: " + others, others.contains(shrouded));
    }

    // ------------------------------------------------------------------ what rollouts make of them

    /** Opponent at 3 life with a creature on the table: the heuristics' creature target is wrong, the face wins. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRolloutsFindTheLethalFaceBurnAmongTheTargets() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        final Card bears = addCard("Grizzly Bears", opp);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        opp.setLife(3, null);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Mountain", opp, ZoneType.Library);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = spellOf(bolt);
        sa.setActivatingPlayer(ai);
        final List<SpellAbility> plays = new ArrayList<>();
        plays.add(sa);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        cfg.considerHolding = false;
        cfg.targetVariants = 2;
        // a stand in for the heuristics that aim at the creature every time
        final FlatMonteCarloPicker.Result result = FlatMonteCarloPicker.choose(ai, plays, cfg,
                p -> FlatMonteCarloPicker.applyTarget(p, bears));

        AssertJUnit.assertFalse(String.valueOf(result), result.hold);
        AssertJUnit.assertSame(sa, result.choice);
        AssertJUnit.assertSame("the face, not the creature: " + result, opp, result.target);
        AssertJUnit.assertEquals(String.valueOf(result), "wins", result.reason);
    }

    /** With the switch off the same position must be decided exactly as before: one candidate, the heuristic target. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testVariantsCanBeSwitchedOff() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        final Card bears = addCard("Grizzly Bears", opp);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        opp.setLife(3, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = spellOf(bolt);
        sa.setActivatingPlayer(ai);
        final List<SpellAbility> plays = new ArrayList<>();
        plays.add(sa);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        cfg.considerHolding = false;
        cfg.targetVariants = 0;
        FlatMonteCarloStats.reset();
        final FlatMonteCarloPicker.Result result = FlatMonteCarloPicker.choose(ai, plays, cfg,
                p -> FlatMonteCarloPicker.applyTarget(p, bears));
        AssertJUnit.assertEquals(String.valueOf(result), "single-candidate", result.reason);
        AssertJUnit.assertNull(result.target);
        AssertJUnit.assertEquals(0, FlatMonteCarloStats.getTargetVariantCandidates());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRolloutsNeverLeaveTheRealGameAimedAtAnotherTarget() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Mountain", 3, ai);
        final Card bears = addCard("Grizzly Bears", opp);
        addCard("Raging Goblin", opp);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card shock = addCardToZone("Shock", ai, ZoneType.Hand);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Mountain", opp, ZoneType.Library);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final String before = gameStateToString(game) + ai.getLife() + "/" + opp.getLife();
        final List<SpellAbility> plays = new ArrayList<>();
        for (final Card c : new Card[] { bolt, shock }) {
            plays.add(spellOf(c));
            spellOf(c).setActivatingPlayer(ai);
        }
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        cfg.targetVariants = 3;
        FlatMonteCarloPicker.choose(ai, plays, cfg, p -> FlatMonteCarloPicker.applyTarget(p, bears));
        AssertJUnit.assertEquals("sampling targets must not touch the real game", before,
                gameStateToString(game) + ai.getLife() + "/" + opp.getLife());
        AssertJUnit.assertTrue(FlatMonteCarloStats.getTargetVariantCandidates() > 0);
    }

    // ------------------------------------------------------------------ whole games

    /** Burn on both sides is all about where the spells go: a whole game with variants on must stay healthy. */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testBurnMirrorPlaysWithoutErrorsWithTargetVariants() {
        final Game game = newGame();
        final Player a = game.getPlayers().get(0);
        final Player b = game.getPlayers().get(1);
        for (final Player p : new Player[] { a, b }) {
            addCards("Mountain", 3, p);
            addCard("Hill Giant", p); // something to aim at on both sides: face or creature is a real choice
            for (int i = 0; i < 10; i++) {
                for (final String n : new String[] { "Mountain", "Lightning Bolt", "Shock", "Raging Goblin", "Hill Giant" }) {
                    addCardToZone(n, p, i < 2 ? ZoneType.Hand : ZoneType.Library);
                }
            }
        }
        FlatMonteCarloStats.reset();
        final int failuresBefore = FlatMonteCarloPicker.getTotalFailures();
        for (int turns = 0; turns < 12 && !game.isGameOver(); turns++) {
            playUntilNextTurn(game);
        }
        System.out.println(FlatMonteCarloStats.summary());
        AssertJUnit.assertEquals("no rollout may fail", failuresBefore, FlatMonteCarloPicker.getTotalFailures());
        AssertJUnit.assertEquals(0, FlatMonteCarloStats.getFailureCount());
        AssertJUnit.assertTrue("burn decks have targets to compare", FlatMonteCarloStats.getTargetVariantCandidates() > 0);
    }
}
