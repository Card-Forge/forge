package forge.ai.simulation;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Unit tests for the Monte Carlo refinements: successive elimination (pruning), the deck-agnostic classification and
 * valuation of held-up cards, the playout length, the statistics collector and the AI profiles. Most of it is plain
 * arithmetic on arrays and needs no game; the rest uses a handful of cards from the card database.
 */
public class FlatMonteCarloEnhancementTest extends AITest {
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

    // ------------------------------------------------------------------ pruning (pure arithmetic)

    private static int prune(final long[][] vals, final boolean[] pruned, final int bonus) {
        final int n = vals.length;
        final int[] count = new int[n];
        for (int i = 0; i < n; i++) {
            count[i] = vals[i].length;
        }
        return FlatMonteCarloPicker.pruneBehind(vals, count, new boolean[n], pruned, n, bonus);
    }

    @Test
    public void testClearlyWorseLinesArePrunedAndCloseOnesStay() {
        final long[][] vals = {
                { 100, 110, 90, 100 },   // the heuristic line: mean 100, +80 bonus = 180
                { 400, 410, 390, 400 },  // the leader
                { 395, 405, 385, 395 },  // 5 points behind the leader on every sample: still in the race
        };
        final boolean[] pruned = new boolean[3];
        final int dropped = prune(vals, pruned, 80);
        AssertJUnit.assertEquals(1, dropped);
        AssertJUnit.assertTrue("220 points behind on every sample must be dropped", pruned[0]);
        AssertJUnit.assertFalse("the leader is never dropped", pruned[1]);
        AssertJUnit.assertFalse("a line 5 points behind is too close to call", pruned[2]);
    }

    @Test
    public void testNoisyLinesAreNotPrunedOnEqualMeans() {
        // same mean, wildly different samples: nothing can be concluded
        final long[][] vals = { { 0, 1000 }, { 500, 500 } };
        final boolean[] pruned = new boolean[2];
        AssertJUnit.assertEquals(0, prune(vals, pruned, 0));
        AssertJUnit.assertFalse(pruned[0]);
        AssertJUnit.assertFalse(pruned[1]);
    }

    @Test
    public void testHighVarianceGapIsNotPrunedButTheSameGapWithLowVarianceIs() {
        // Both challengers are 100 points behind on average. With the paired differences spread widely (+/-300) the
        // lead may be luck; with identical differences it is not.
        final long[][] noisy = { { 500, 800, 200 }, { 400, 100, 300 } }; // diffs: 100, 700, -100 -> mean 233, big spread
        final boolean[] prunedNoisy = new boolean[2];
        prune(noisy, prunedNoisy, 0);
        AssertJUnit.assertFalse("a lead that may be luck must not prune", prunedNoisy[1]);

        final long[][] steady = { { 500, 500, 500 }, { 400, 400, 400 } };
        final boolean[] prunedSteady = new boolean[2];
        prune(steady, prunedSteady, 0);
        AssertJUnit.assertTrue("a steady 100 point lead may prune", prunedSteady[1]);
    }

    @Test
    public void testHeuristicBonusProtectsTheHeuristicLine() {
        // challenger is 50 points better on every sample
        final long[][] vals = { { 100, 100, 100 }, { 150, 150, 150 } };

        // without a bonus the heuristic line is clearly behind
        final boolean[] noBonus = new boolean[2];
        prune(vals, noBonus, 0);
        AssertJUnit.assertTrue(noBonus[0]);

        // with the bonus of 80 the heuristic line (180) leads, so the challenger is the one behind (by 30 > 25)
        final boolean[] withBonus = new boolean[2];
        prune(vals, withBonus, 80);
        AssertJUnit.assertFalse("the leader must survive", withBonus[0]);
        AssertJUnit.assertTrue(withBonus[1]);

        // a bonus that leaves them within the minimum gap keeps both
        final boolean[] close = new boolean[2];
        prune(vals, close, 60); // 160 vs 150: 10 points
        AssertJUnit.assertFalse(close[0]);
        AssertJUnit.assertFalse(close[1]);
    }

    @Test
    public void testPruningNeedsTwoSamplesAndIgnoresInvalidAndPrunedLines() {
        final boolean[] pruned = new boolean[2];
        // one sample each: no spread to judge by
        AssertJUnit.assertEquals(0, FlatMonteCarloPicker.pruneBehind(new long[][] { { 0 }, { 1000 } }, new int[] { 1, 1 },
                new boolean[2], pruned, 2, 0));

        // an invalid line is neither leader nor victim
        final long[][] vals = { { 0, 0, 0 }, { 1000, 1000, 1000 }, { 100, 100, 100 } };
        final boolean[] invalid = { false, true, false };
        final boolean[] prunedAgain = new boolean[3];
        final int dropped = FlatMonteCarloPicker.pruneBehind(vals, new int[] { 3, 3, 3 }, invalid, prunedAgain, 3, 0);
        AssertJUnit.assertEquals("the leader among the valid lines is index 2, so only line 0 is behind", 1, dropped);
        AssertJUnit.assertFalse(prunedAgain[1]);
        AssertJUnit.assertFalse(prunedAgain[2]);

        // pruning twice does not count a line twice
        AssertJUnit.assertEquals(0, FlatMonteCarloPicker.pruneBehind(vals, new int[] { 3, 3, 3 }, invalid, prunedAgain, 3, 0));
    }

    @Test
    public void testPruningNeverLeavesFewerThanOneLine() {
        // many lines, all but one far behind
        final long[][] vals = new long[6][];
        for (int i = 0; i < 6; i++) {
            vals[i] = new long[] { i * 10, i * 10, i * 10, i * 10 };
        }
        final boolean[] pruned = new boolean[6];
        prune(vals, pruned, 0);
        int survivors = 0;
        for (final boolean p : pruned) {
            if (!p) {
                survivors++;
            }
        }
        AssertJUnit.assertTrue("at least the leader must survive", survivors >= 1);
        AssertJUnit.assertFalse("line 5 leads", pruned[5]);
        AssertJUnit.assertTrue("line 0 is 50 points behind", pruned[0]);
        AssertJUnit.assertFalse("line 4 is only 10 points behind: within the minimum gap", pruned[4]);
    }

    // ------------------------------------------------------------------ classifying held-up cards

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testHoldKindIsDecidedByWhatTheCardDoesNotByItsName() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);

        final Map<String, FlatMonteCarloPicker.HoldKind> expected = new HashMap<>();
        expected.put("Counterspell", FlatMonteCarloPicker.HoldKind.COUNTER);
        expected.put("Remove Soul", FlatMonteCarloPicker.HoldKind.COUNTER);
        expected.put("Lightning Bolt", FlatMonteCarloPicker.HoldKind.REMOVAL);   // damage
        expected.put("Doom Blade", FlatMonteCarloPicker.HoldKind.REMOVAL);       // destroy
        expected.put("Disfigure", FlatMonteCarloPicker.HoldKind.REMOVAL);        // -2/-2 kills like removal
        expected.put("Giant Growth", FlatMonteCarloPicker.HoldKind.TRICK);       // pump
        expected.put("Ambush Viper", FlatMonteCarloPicker.HoldKind.FLASH_CREATURE);
        expected.put("Brineborn Cutthroat", FlatMonteCarloPicker.HoldKind.FLASH_CREATURE);
        expected.put("Brainstorm", FlatMonteCarloPicker.HoldKind.OTHER);         // draws, nothing to aim at
        expected.put("Opt", FlatMonteCarloPicker.HoldKind.OTHER);

        for (final Map.Entry<String, FlatMonteCarloPicker.HoldKind> e : expected.entrySet()) {
            final Card c = addCardToZone(e.getKey(), ai, ZoneType.Hand);
            AssertJUnit.assertEquals("classification of " + e.getKey(), e.getValue(), FlatMonteCarloPicker.holdKind(c));
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testHeldRemovalIsWorthMoreWhenThereIsSomethingToUseItOn() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);

        final FlatMonteCarloPicker.Config categories = new FlatMonteCarloPicker.Config();
        categories.categoryHold = true;
        final int noTarget = FlatMonteCarloPicker.holdUpBonus(ai, categories);

        addCard("Runeclaw Bear", opp);
        final int withTarget = FlatMonteCarloPicker.holdUpBonus(ai, categories);

        AssertJUnit.assertTrue("holding a burn spell has a value even without a creature to aim at: " + noTarget, noTarget > 0);
        AssertJUnit.assertTrue("an enemy creature to aim at must raise it: " + noTarget + " -> " + withTarget, withTarget > noTarget);

        final FlatMonteCarloPicker.Config legacy = new FlatMonteCarloPicker.Config();
        legacy.categoryHold = false;
        AssertJUnit.assertTrue("the original valuation must keep working", FlatMonteCarloPicker.holdUpBonus(ai, legacy) > 0);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testCounterspellIsOnlyWorthHoldingWhenTheOpponentHasCards() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 2, ai);
        addCardToZone("Counterspell", ai, ZoneType.Hand);

        final FlatMonteCarloPicker.Config cfg = new FlatMonteCarloPicker.Config();
        AssertJUnit.assertEquals("nothing to counter: no value", 0, FlatMonteCarloPicker.holdUpBonus(ai, cfg));

        addCardToZone("Runeclaw Bear", opp, ZoneType.Hand);
        AssertJUnit.assertTrue("the opponent can cast something: worth holding", FlatMonteCarloPicker.holdUpBonus(ai, cfg) > 0);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testHeldTrickNeedsACreatureToCast() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        addCards("Forest", 1, ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);

        final FlatMonteCarloPicker.Config cfg = new FlatMonteCarloPicker.Config();
        final int alone = FlatMonteCarloPicker.holdUpBonus(ai, cfg);
        addCard("Runeclaw Bear", ai);
        final int withCreature = FlatMonteCarloPicker.holdUpBonus(ai, cfg);
        AssertJUnit.assertTrue("a pump spell is worth more with a creature to pump: " + alone + " -> " + withCreature,
                withCreature > alone);
    }

    // ------------------------------------------------------------------ recognising the kind of deck

    private void putInLibrary(final Player p, final String name, final int count) {
        for (int i = 0; i < count; i++) {
            addCardToZone(name, p, ZoneType.Library);
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testBurnDeckIsNotMistakenForAControlDeck() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        putInLibrary(ai, "Mountain", 16);
        putInLibrary(ai, "Lightning Bolt", 4);
        putInLibrary(ai, "Shock", 4);
        putInLibrary(ai, "Goblin Guide", 4);
        putInLibrary(ai, "Raging Goblin", 4);
        putInLibrary(ai, "Hill Giant", 4);

        AssertJUnit.assertEquals("a burn deck wants to spend its instants, not hold them", 1.0,
                FlatMonteCarloPicker.controlFactor(ai), 0.0001);
        AssertJUnit.assertEquals("the original rule took it for a control deck (the flaw this fixes)", 1.75,
                FlatMonteCarloPicker.controlFactor(ai, false), 0.0001);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testCounterspellDeckIsControl() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        putInLibrary(ai, "Island", 16);
        putInLibrary(ai, "Counterspell", 4);
        putInLibrary(ai, "Mana Leak", 4);
        putInLibrary(ai, "Air Elemental", 4);
        AssertJUnit.assertEquals(1.75, FlatMonteCarloPicker.controlFactor(ai), 0.0001);
        AssertJUnit.assertEquals(1.75, FlatMonteCarloPicker.controlFactor(ai, false), 0.0001);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRemovalHeavyDeckWithFewCreaturesIsControl() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        putInLibrary(ai, "Swamp", 16);
        putInLibrary(ai, "Terror", 5);
        putInLibrary(ai, "Doom Blade", 5);
        putInLibrary(ai, "Disfigure", 5);
        putInLibrary(ai, "Air Elemental", 5);
        AssertJUnit.assertEquals(1.75, FlatMonteCarloPicker.controlFactor(ai), 0.0001);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testCreatureOnlyDeckHoldsLessAndAnEmptyDeckIsNeutral() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        AssertJUnit.assertEquals("no cards: neutral", 1.0, FlatMonteCarloPicker.controlFactor(ai), 0.0001);
        putInLibrary(ai, "Mountain", 16);
        putInLibrary(ai, "Raging Goblin", 8);
        putInLibrary(ai, "Goblin Guide", 8);
        putInLibrary(ai, "Hill Giant", 8);
        AssertJUnit.assertEquals(0.5, FlatMonteCarloPicker.controlFactor(ai), 0.0001);
    }

    // ------------------------------------------------------------------ playout length

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testAdaptivePlayoutGrowsWithTheHandAndCanBeSwitchedOff() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final FlatMonteCarloPicker.Config cfg = new FlatMonteCarloPicker.Config();
        cfg.maxPlayoutPlays = 6;
        cfg.comboPlayoutPlays = 14;

        addCardToZone("Mountain", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        cfg.adaptivePlayout = true;
        AssertJUnit.assertEquals("a small hand needs no more than the normal limit", 6, FlatMonteCarloPicker.playoutLength(ai, cfg));

        for (int i = 0; i < 6; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        }
        // 8 spells and no combo: 8 + 2 plays
        AssertJUnit.assertFalse(FlatMonteCarloPicker.isComboLike(ai));
        AssertJUnit.assertEquals(10, FlatMonteCarloPicker.playoutLength(ai, cfg));

        cfg.adaptivePlayout = false;
        AssertJUnit.assertEquals("switched off: the original limit", 6, FlatMonteCarloPicker.playoutLength(ai, cfg));

        for (int i = 0; i < 12; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        }
        cfg.adaptivePlayout = true;
        AssertJUnit.assertEquals("never above the combo limit", 14, FlatMonteCarloPicker.playoutLength(ai, cfg));
    }

    // ------------------------------------------------------------------ statistics

    private static FlatMonteCarloPicker.Result fakeResult(final String reason, final int rollouts, final long millis,
                                                          final boolean byRollouts, final int pruned) {
        return new FlatMonteCarloPicker.Result(null, false, byRollouts, 3, rollouts,
                millis * 1_000_000L, reason, pruned);
    }

    @Test
    public void testStatsCountDecisionsReasonsRolloutsAndPruning() {
        FlatMonteCarloStats.reset();
        FlatMonteCarloStats.record(fakeResult("override", 12, 100, true, 2));
        FlatMonteCarloStats.record(fakeResult("kept-heuristic", 8, 50, true, 0));
        FlatMonteCarloStats.record(fakeResult("kept-heuristic", 0, 1, false, 0));
        AssertJUnit.assertEquals(3, FlatMonteCarloStats.getDecisions());
        AssertJUnit.assertEquals(20, FlatMonteCarloStats.getRollouts());
        AssertJUnit.assertEquals(2, FlatMonteCarloStats.getPrunedLines());
        AssertJUnit.assertEquals(1, FlatMonteCarloStats.getReasonCount("override"));
        AssertJUnit.assertEquals(2, FlatMonteCarloStats.getReasonCount("kept-heuristic"));
        AssertJUnit.assertEquals(0, FlatMonteCarloStats.getReasonCount("hold"));
    }

    @Test
    public void testStatsPercentilesFollowTheDecisionTimes() {
        FlatMonteCarloStats.reset();
        AssertJUnit.assertEquals("no data: zero", 0.0, FlatMonteCarloStats.getPercentileMillis(95), 0.0);
        for (int ms = 1; ms <= 100; ms++) {
            FlatMonteCarloStats.record(fakeResult("kept-heuristic", 4, ms, true, 0));
        }
        AssertJUnit.assertEquals(50.0, FlatMonteCarloStats.getPercentileMillis(50), 0.5);
        AssertJUnit.assertEquals(95.0, FlatMonteCarloStats.getPercentileMillis(95), 0.5);
        AssertJUnit.assertEquals(100.0, FlatMonteCarloStats.getPercentileMillis(100), 0.5);
        AssertJUnit.assertTrue(FlatMonteCarloStats.getPercentileMillis(0) >= 1.0 - 0.001);
    }

    @Test
    public void testStatsTrackUnsimulatableCardsAndFailuresAndSummariseThem() {
        FlatMonteCarloStats.reset();
        FlatMonteCarloStats.noteUnsimulatable("Some Weird Card");
        FlatMonteCarloStats.noteUnsimulatable("Some Weird Card");
        FlatMonteCarloStats.noteUnsimulatable("Another Card");
        FlatMonteCarloStats.noteFailure("java.lang.IllegalStateException: boom");
        AssertJUnit.assertEquals(2, FlatMonteCarloStats.getUnsimulatableCardCount());
        AssertJUnit.assertEquals(1, FlatMonteCarloStats.getFailureCount());

        FlatMonteCarloStats.record(fakeResult("override", 4, 10, true, 1));
        final String summary = FlatMonteCarloStats.summary();
        AssertJUnit.assertTrue(summary, summary.startsWith("FLATMC-STATS decisions=1 "));
        AssertJUnit.assertTrue(summary, summary.contains("failures=1"));
        AssertJUnit.assertTrue(summary, summary.contains("unsimulatableCards=2"));
        AssertJUnit.assertTrue(summary, summary.contains("FLATMC-UNSIMULATABLE Some Weird Card=2"));
        AssertJUnit.assertTrue(summary, summary.contains("FLATMC-FAILURES java.lang.IllegalStateException: boom=1"));
        AssertJUnit.assertTrue(summary, summary.contains("FLATMC-REASONS override=1"));

        FlatMonteCarloStats.reset();
        AssertJUnit.assertEquals(0, FlatMonteCarloStats.getDecisions());
        AssertJUnit.assertEquals(0, FlatMonteCarloStats.getFailureCount());
    }

    @Test
    public void testStatsHandleAnEmptyRunWithoutDividingByZero() {
        FlatMonteCarloStats.reset();
        final String summary = FlatMonteCarloStats.summary();
        AssertJUnit.assertTrue(summary, summary.startsWith("FLATMC-STATS decisions=0 "));
        AssertJUnit.assertFalse(summary, summary.contains("NaN"));
        AssertJUnit.assertFalse(summary, summary.contains("Infinity"));
    }

    // ------------------------------------------------------------------ configuration

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testNewKnobsReadFromTheProfileAndDefaultToTheEnhancedBehaviour() {
        final Game game = newGame();
        final Player p = game.getPlayers().get(1);
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(p), p);
        AssertJUnit.assertTrue(cfg.pruning);
        AssertJUnit.assertTrue(cfg.categoryHold);
        AssertJUnit.assertTrue(cfg.adaptivePlayout);
        AssertJUnit.assertTrue("a few extra rounds for the lines still in the race", cfg.extraRounds >= 1);
        AssertJUnit.assertTrue(cfg.extraRounds <= 10);
    }

    @Test
    public void testProfilesDefineTheNewKnobsAndHarderMeansMoreExtraRounds() throws Exception {
        final File dir = new File("../forge-gui/res/ai");
        if (!dir.isDirectory()) {
            return; // run from an unexpected directory: nothing to check
        }
        final int[] extra = new int[3];
        final String[] names = { "Default", "Hard", "Hardest" };
        for (int i = 0; i < names.length; i++) {
            final Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(new File(dir, names[i] + ".ai"))) {
                props.load(in);
            }
            for (final AiProps prop : new AiProps[] { AiProps.FLAT_MC_PRUNING, AiProps.FLAT_MC_CATEGORY_HOLD,
                    AiProps.FLAT_MC_ADAPTIVE_PLAYOUT }) {
                final String value = props.getProperty(prop.name());
                AssertJUnit.assertTrue(names[i] + ".ai must set " + prop.name() + " to a boolean, was " + value,
                        "true".equals(value) || "false".equals(value));
            }
            extra[i] = Integer.parseInt(props.getProperty(AiProps.FLAT_MC_EXTRA_ROUNDS.name()).trim());
        }
        AssertJUnit.assertTrue("Hard gets at least as many extra rounds as Default", extra[1] >= extra[0]);
        AssertJUnit.assertTrue("Hardest gets at least as many extra rounds as Hard", extra[2] >= extra[1]);
    }
}
