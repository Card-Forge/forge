package forge.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.ai.simulation.GameCopier;
import forge.ai.simulation.GameSimulator;
import forge.ai.simulation.HiddenInfoShuffler;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.localinstance.properties.ForgePreferences.FPref;

/**
 * Scenario tests for the "flat Monte Carlo + heuristics" AI: that the setting defaults to on, that the rollouts
 * never touch the real game, that they stay inside their time budget, and that whole games (a control deck against a
 * creature deck, a burn finish) play out without errors with it switched on and off.
 */
public class FlatMonteCarloScenarioTest extends AITest {

    private static final int LONG_TIMEOUT_MS = 180_000;

    private Game createMonteCarloGame(boolean enabled) {
        Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = enabled;
        game.AI_TIMEOUT = 10;
        return game;
    }

    private static AiController brainOf(Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    /** What the heuristics approve of, with targets set up by the heuristics, like AiController does it. */
    private static List<SpellAbility> approvedPlays(Player p) {
        AiController ai = brainOf(p);
        List<SpellAbility> approved = new ArrayList<>();
        for (SpellAbility sa : new SpellAbilityPicker(p).getCandidateSpellsAndAbilities()) {
            if (ai.canPlaySa(sa) == AiPlayDecision.WillPlay) {
                approved.add(sa);
            }
        }
        return approved;
    }

    /**
     * The plays to compare in a test of the picker itself. Normally that is what the heuristics approve of, but they
     * may rightly prefer to wait (for example, draw spells are held until the second main phase). The picker must cope
     * with any list of playable plays, so fall back to everything that is playable, targets set up where possible.
     */
    private static List<SpellAbility> playsToCompare(Player p) {
        List<SpellAbility> approved = approvedPlays(p);
        if (!approved.isEmpty()) {
            return approved;
        }
        List<SpellAbility> playable = new SpellAbilityPicker(p).getCandidateSpellsAndAbilities();
        for (SpellAbility sa : playable) {
            brainOf(p).canPlaySa(sa);
        }
        return playable;
    }

    private static void fillLibrariesWith(Player p, FlatMonteCarloScenarioTest t, String... names) {
        for (int i = 0; i < 12; i++) {
            for (String name : names) {
                t.addCardToZone(name, p, ZoneType.Library);
            }
        }
    }

    // ------------------------------------------------------------------ the setting

    @Test
    public void testSettingDefaultsToTrue() {
        AssertJUnit.assertEquals("true", FPref.MATCH_AI_FLAT_MONTE_CARLO.getDefault());
    }

    @Test
    public void testProfileDefaultsAreSaneAndParse() {
        // every tuning knob must have a default that parses, so that profiles that do not mention them keep working
        for (AiProps prop : new AiProps[] { AiProps.FLAT_MC_MAX_CANDIDATES, AiProps.FLAT_MC_ROLLOUTS_PER_CANDIDATE,
                AiProps.FLAT_MC_MAX_PLAYOUT_PLAYS, AiProps.FLAT_MC_DECISION_BUDGET_MS, AiProps.FLAT_MC_TURN_BUDGET_MS,
                AiProps.FLAT_MC_HEURISTIC_BONUS, AiProps.FLAT_MC_HOLD_MANA_WEIGHT }) {
            Integer.parseInt(prop.getDefault());
        }
        Game game = createMonteCarloGame(true);
        Player p = game.getPlayers().get(1);
        FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(p), p);
        AssertJUnit.assertTrue(cfg.maxCandidates >= 2);
        AssertJUnit.assertTrue("the default must take at least a few samples per line", cfg.rolloutsPerCandidate >= 3);
        AssertJUnit.assertTrue(cfg.maxCandidates >= 4);
        // phone friendly: one decision well under a second, a whole turn only a little over a second
        AssertJUnit.assertTrue(cfg.budgetNanos <= TimeUnit.MILLISECONDS.toNanos(500));
        AssertJUnit.assertTrue(cfg.turnBudgetNanos <= TimeUnit.SECONDS.toNanos(2));
    }

    // ------------------------------------------------------------------ building blocks

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testHiddenInfoShufflerKeepsKnownCardsAndZoneSizes() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        addCardToZone("Shock", ai, ZoneType.Hand);
        addCardToZone("Mountain", ai, ZoneType.Hand);
        String[] oppHand = { "Island", "Counterspell", "Divination" };
        for (String n : oppHand) {
            addCardToZone(n, opp, ZoneType.Hand);
        }
        for (int i = 0; i < 6; i++) {
            addCardToZone("Runeclaw Bear", opp, ZoneType.Library);
            addCardToZone("Mountain", ai, ZoneType.Library);
        }

        GameSimulator sim = new GameSimulator(game, ai);
        Game copy = sim.getSimulatedGameState();
        Player copyAi = sim.getSimulatedAiPlayer();
        Player copyOpp = copy.getPlayers().get(0);

        Set<Integer> aiHandBefore = idsIn(copyAi, ZoneType.Hand);
        int oppHandSize = copyOpp.getZone(ZoneType.Hand).size();
        int oppLibrarySize = copyOpp.getZone(ZoneType.Library).size();
        int aiLibrarySize = copyAi.getZone(ZoneType.Library).size();

        boolean oppHandChangedAtLeastOnce = false;
        Set<Integer> oppHandBefore = idsIn(copyOpp, ZoneType.Hand);
        for (long seed = 1; seed <= 30; seed++) {
            HiddenInfoShuffler.shuffle(copy, sim.getGameCopier(), ai, new java.util.Random(seed));
            AssertJUnit.assertEquals("the AI's own hand is known to it and must stay as it is", aiHandBefore,
                    idsIn(copyAi, ZoneType.Hand));
            AssertJUnit.assertEquals(oppHandSize, copyOpp.getZone(ZoneType.Hand).size());
            AssertJUnit.assertEquals(oppLibrarySize, copyOpp.getZone(ZoneType.Library).size());
            AssertJUnit.assertEquals(aiLibrarySize, copyAi.getZone(ZoneType.Library).size());
            if (!oppHandBefore.equals(idsIn(copyOpp, ZoneType.Hand))) {
                oppHandChangedAtLeastOnce = true;
            }
        }
        AssertJUnit.assertTrue("the opponent's hidden hand must be re-dealt, or rollouts would peek at it",
                oppHandChangedAtLeastOnce);
    }

    private static Set<Integer> idsIn(Player p, ZoneType zone) {
        Set<Integer> ids = new HashSet<>();
        for (Card c : p.getZone(zone).getCards()) {
            ids.add(c.getId());
        }
        return ids;
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRolloutsNeverChangeTheRealGame() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        addCards("Island", 3, ai);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Divination", ai, ZoneType.Hand);
        addCardToZone("Air Elemental", ai, ZoneType.Hand);
        addCardToZone("Merfolk of the Pearl Trident", ai, ZoneType.Hand); // castable for sure
        addCard("Runeclaw Bear", opp);
        fillLibrariesWith(ai, this, "Island");
        fillLibrariesWith(opp, this, "Mountain");
        addCardToZone("Shock", opp, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        int failuresBefore = FlatMonteCarloPicker.getTotalFailures();
        String before = gameStateToString(game);
        int aiLife = ai.getLife();
        int oppLife = opp.getLife();

        List<SpellAbility> approved = playsToCompare(ai);
        AssertJUnit.assertFalse("there should be something playable to compare", approved.isEmpty());

        FlatMonteCarloPicker.Result result = FlatMonteCarloPicker.choose(ai, approved,
                FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai), sa -> brainOf(ai).canPlaySa(sa) == AiPlayDecision.WillPlay);

        AssertJUnit.assertNotNull(result);
        AssertJUnit.assertFalse("rollouts must not fail on an ordinary position: " + result, "error".equals(result.reason));
        AssertJUnit.assertEquals("no rollout may fail with an exception", failuresBefore, FlatMonteCarloPicker.getTotalFailures());
        AssertJUnit.assertTrue("the rollouts must really have run", result.rollouts > 0);
        AssertJUnit.assertEquals("the real game must not have been touched", before, gameStateToString(game));
        AssertJUnit.assertEquals(aiLife, ai.getLife());
        AssertJUnit.assertEquals(oppLife, opp.getLife());
        AssertJUnit.assertFalse(game.isGameOver());
        if (!result.hold) {
            AssertJUnit.assertTrue("a chosen play must be one of the approved plays", approved.contains(result.choice));
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testNoTimeLeftKeepsTheHeuristicChoice() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        game.getPlayers().get(0).setTeam(1);
        ai.setTeam(0);

        addCards("Mountain", 2, ai);
        addCardToZone("Shock", ai, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        addCard("Runeclaw Bear", game.getPlayers().get(0));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        List<SpellAbility> approved = playsToCompare(ai);
        AssertJUnit.assertFalse("there should be something playable to compare", approved.isEmpty());

        FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        cfg.budgetNanos = 1; // no time at all
        long start = System.nanoTime();
        FlatMonteCarloPicker.Result result = FlatMonteCarloPicker.choose(ai, approved, cfg, null);
        long elapsed = System.nanoTime() - start;

        AssertJUnit.assertFalse(result.hold);
        AssertJUnit.assertSame("without time to compare anything, the heuristic choice must stand", approved.get(0), result.choice);
        AssertJUnit.assertTrue("an out-of-time decision must return quickly, took " + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms",
                elapsed < TimeUnit.SECONDS.toNanos(5));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testDecisionStaysInsideItsBudget() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        addCards("Island", 4, ai);
        addCards("Mountain", 2, ai);
        for (String n : new String[] { "Counterspell", "Divination", "Air Elemental", "Shock", "Runeclaw Bear" }) {
            addCardToZone(n, ai, ZoneType.Hand);
        }
        addCards("Runeclaw Bear", 2, opp);
        fillLibrariesWith(ai, this, "Island", "Mountain");
        fillLibrariesWith(opp, this, "Mountain", "Runeclaw Bear");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        List<SpellAbility> approved = playsToCompare(ai);
        AssertJUnit.assertFalse("there should be something playable to compare", approved.isEmpty());

        FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        FlatMonteCarloPicker.Result result = FlatMonteCarloPicker.choose(ai, approved, cfg, sa -> brainOf(ai).canPlaySa(sa) == AiPlayDecision.WillPlay);

        // The budget is soft (a rollout that has started is finished, and the second sample may use twice the
        // budget), so allow generous slack for a slow CI machine; the point is that it is bounded, not unbounded.
        long limit = 6 * cfg.budgetNanos + TimeUnit.SECONDS.toNanos(3);
        AssertJUnit.assertTrue("decision took " + TimeUnit.NANOSECONDS.toMillis(result.elapsedNanos) + " ms: " + result,
                result.elapsedNanos < limit);
        AssertJUnit.assertFalse(String.valueOf(result), "error".equals(result.reason));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testIdenticalPlaysAreSampledOnlyOnce() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);

        Card first = addCard("Prodigal Sorcerer", ai);
        Card second = addCard("Prodigal Sorcerer", ai);
        List<SpellAbility> plays = new ArrayList<>();
        plays.add(first.getSpellAbilities().get(first.getSpellAbilities().size() - 1));
        plays.add(second.getSpellAbilities().get(second.getSpellAbilities().size() - 1));
        AssertJUnit.assertEquals(2, plays.size());

        List<SpellAbility> unique = FlatMonteCarloPicker.dedupePlays(plays);
        AssertJUnit.assertEquals("the same ability of two identical cards is one candidate", 1, unique.size());
        AssertJUnit.assertSame("the first one is kept, so the heuristic order is unchanged", plays.get(0), unique.get(0));

        // different cards must never be merged
        Card bear = addCard("Runeclaw Bear", ai);
        List<SpellAbility> mixed = new ArrayList<>(plays);
        mixed.add(bear.getSpellAbilities().get(0));
        AssertJUnit.assertEquals(2, FlatMonteCarloPicker.dedupePlays(mixed).size());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testLethalLineEndsTheSearchEarly() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        addCard("Mountain", ai);
        addCardToZone("Shock", ai, ZoneType.Hand);
        opp.setLife(2, null); // no creatures on the other side: the face is the only target
        fillLibrariesWith(ai, this, "Mountain");
        fillLibrariesWith(opp, this, "Mountain");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        List<SpellAbility> approved = new ArrayList<>(playsToCompare(ai));
        AssertJUnit.assertFalse("there should be something playable to compare", approved.isEmpty());
        // the picker gets the burn spell as the heuristic's first choice, whatever order the plays were found in
        for (int i = 0; i < approved.size(); i++) {
            if (approved.get(i).getHostCard() != null && "Shock".equals(approved.get(i).getHostCard().getName())) {
                approved.add(0, approved.remove(i));
                break;
            }
        }

        FlatMonteCarloPicker.Result result = FlatMonteCarloPicker.choose(ai, approved,
                FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai), sa -> brainOf(ai).canPlaySa(sa) == AiPlayDecision.WillPlay);

        AssertJUnit.assertFalse("must not hold back when the game can be won now: " + result, result.hold);
        AssertJUnit.assertEquals(String.valueOf(result), "wins", result.reason);
        AssertJUnit.assertSame(approved.get(0), result.choice);
        AssertJUnit.assertEquals("a win needs no further samples: " + result, 1, result.rollouts);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testComboHandsGetLongerPlayouts() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);

        // a control hand is not a combo hand
        for (String n : new String[] { "Counterspell", "Divination", "Air Elemental" }) {
            addCardToZone(n, ai, ZoneType.Hand);
        }
        AssertJUnit.assertFalse(FlatMonteCarloPicker.isComboLike(ai));

        // several mana rituals are
        for (int i = 0; i < 3; i++) {
            addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        }
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isComboLike(ai));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testStormCardMakesAHandComboLike() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        addCardToZone("Grapeshot", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isComboLike(ai));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testComboHandPlaysOutWithoutErrors() {
        int failuresBefore = FlatMonteCarloPicker.getTotalFailures();
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        addCards("Swamp", 3, ai);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        }
        addCardToZone("Grapeshot", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        fillLibrariesWith(ai, this, "Swamp");
        fillLibrariesWith(opp, this, "Mountain");
        addCard("Runeclaw Bear", opp);

        for (int i = 0; i < 4 && !game.isGameOver(); i++) {
            playUntilNextTurn(game);
        }

        AssertJUnit.assertEquals("no rollout may fail with an exception", failuresBefore, FlatMonteCarloPicker.getTotalFailures());
    }

    // ------------------------------------------------------------------ whole games

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testBurnFinishWinsTheGame() {
        // The rollouts must see that burning the opponent out ends the game, whatever else they compare it with.
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        addCard("Mountain", ai);
        addCardToZone("Shock", ai, ZoneType.Hand);
        addCard("Runeclaw Bear", opp);
        opp.setLife(2, null);
        fillLibrariesWith(ai, this, "Mountain");
        fillLibrariesWith(opp, this, "Mountain");

        playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue("Shock to the face for the last 2 life should end the game", game.isGameOver());
        AssertJUnit.assertTrue(ai.hasWon());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testControlDeckAgainstCreatureDeckPlaysWithoutErrors() {
        playMatchupForTurns(true, 12);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testSameMatchupStillWorksWithTheSettingOff() {
        playMatchupForTurns(false, 12);
    }

    private void playMatchupForTurns(boolean monteCarlo, int turns) {
        int failuresBefore = FlatMonteCarloPicker.getTotalFailures();
        int rolloutsBefore = FlatMonteCarloPicker.getTotalRollouts();
        Game game = createMonteCarloGame(monteCarlo);
        Player control = game.getPlayers().get(1); // takes the first turn, see AITest#initAndCreateGame
        Player beatdown = game.getPlayers().get(0);
        control.setTeam(0);
        beatdown.setTeam(1);

        // control: blue with counters, card draw and a finisher
        addCards("Island", 2, control);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Island", control, ZoneType.Library);
            addCardToZone("Counterspell", control, ZoneType.Library);
            addCardToZone("Divination", control, ZoneType.Library);
            addCardToZone("Air Elemental", control, ZoneType.Library);
        }
        for (String n : new String[] { "Island", "Counterspell", "Divination" }) {
            addCardToZone(n, control, ZoneType.Hand);
        }
        // beatdown: red creatures and burn
        addCards("Mountain", 2, beatdown);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", beatdown, ZoneType.Library);
            addCardToZone("Raging Goblin", beatdown, ZoneType.Library);
            addCardToZone("Shock", beatdown, ZoneType.Library);
        }
        for (String n : new String[] { "Mountain", "Raging Goblin", "Shock" }) {
            addCardToZone(n, beatdown, ZoneType.Hand);
        }

        int startedTurns = 0;
        for (int i = 0; i < turns && !game.isGameOver(); i++) {
            playUntilNextTurn(game);
            startedTurns++;
        }

        AssertJUnit.assertTrue("the game must keep advancing", startedTurns >= 1);
        AssertJUnit.assertTrue("turn counter must have moved on", game.getPhaseHandler().getTurn() >= 2 || game.isGameOver());
        AssertJUnit.assertEquals("no rollout may fail with an exception during a normal game", failuresBefore,
                FlatMonteCarloPicker.getTotalFailures());
        if (!monteCarlo) {
            AssertJUnit.assertEquals("with the setting off, no rollouts may be run", rolloutsBefore,
                    FlatMonteCarloPicker.getTotalRollouts());
            AssertJUnit.assertNull(brainOf(control).getLastFlatMonteCarloResult());
            AssertJUnit.assertNull(brainOf(beatdown).getLastFlatMonteCarloResult());
        } else {
            AssertJUnit.assertTrue("with the setting on, the AI must actually have used rollouts in this matchup",
                    FlatMonteCarloPicker.getTotalRollouts() > rolloutsBefore);
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testSimulationModesAreNotCombinedWithRollouts() {
        // Players that already use the (full or hybrid) simulation have their own lookahead: rollouts stay out of it.
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        brainOf(ai).setUseSimulation(AIOption.USE_FULL_SIMULATION);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        addCards("Mountain", 2, ai);
        addCardToZone("Shock", ai, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        fillLibrariesWith(ai, this, "Mountain");
        fillLibrariesWith(game.getPlayers().get(0), this, "Mountain");

        playUntilNextTurn(game);

        AssertJUnit.assertNull(brainOf(ai).getLastFlatMonteCarloResult());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testCopiedGamesNeverRunRollouts() {
        // A copy made for a rollout must not start rollouts of its own (that would recurse without end).
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        game.getPlayers().get(0).setTeam(1);
        ai.setTeam(0);
        addCard("Mountain", ai);

        GameSimulator sim = new GameSimulator(game, ai);
        AssertJUnit.assertFalse(sim.getSimulatedGameState().getAIFlatMonteCarlo());
        GameCopier copier = sim.getGameCopier();
        AssertJUnit.assertNotNull(copier.getCopiedGame());
    }

    // ------------------------------------------------------------------ round 3: risk, safety nets, profiles

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRiskAdjustmentPenalisesAnOpenCrackBack() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
        ai.setLife(3, null);
        addCards("Runeclaw Bear", 3, opp); // 6 power against 3 life and no blockers
        addCard("Island", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        FlatMonteCarloPicker.Config cfg = new FlatMonteCarloPicker.Config();
        cfg.riskWeightPercent = 100;
        int risky = FlatMonteCarloPicker.riskAdjustment(ai, cfg);
        AssertJUnit.assertTrue("being open to lethal must cost points, got " + risky, risky < 0);

        cfg.riskWeightPercent = 0;
        AssertJUnit.assertEquals("weight 0 switches the risk term off", 0, FlatMonteCarloPicker.riskAdjustment(ai, cfg));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRiskAdjustmentIsZeroWhenNothingThreatens() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        addCard("Island", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(0, FlatMonteCarloPicker.riskAdjustment(ai, new FlatMonteCarloPicker.Config()));
    }

    @Test
    public void testCircuitBreakerSwitchesMonteCarloOffAfterRepeatedErrors() {
        Game game = createMonteCarloGame(true);
        AiController brain = brainOf(game.getPlayers().get(1));
        AssertJUnit.assertFalse(brain.isFlatMonteCarloTripped());
        brain.noteFlatMcOutcome("error");
        brain.noteFlatMcOutcome("error");
        AssertJUnit.assertFalse("two errors are tolerated", brain.isFlatMonteCarloTripped());
        brain.noteFlatMcOutcome("error");
        AssertJUnit.assertTrue("the third error trips it", brain.isFlatMonteCarloTripped());
    }

    @Test
    public void testCircuitBreakerTripsOnlyOnConsecutiveSlowDecisions() {
        Game game = createMonteCarloGame(true);
        AiController brain = brainOf(game.getPlayers().get(1));
        for (int i = 0; i < 3; i++) {
            brain.noteFlatMcOutcome("too-slow");
        }
        brain.noteFlatMcOutcome("override"); // a decision that worked resets the streak
        for (int i = 0; i < 3; i++) {
            brain.noteFlatMcOutcome("no-time");
        }
        AssertJUnit.assertFalse(brain.isFlatMonteCarloTripped());
        brain.noteFlatMcOutcome("no-time");
        AssertJUnit.assertTrue(brain.isFlatMonteCarloTripped());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testPerPlayerOverrideBeatsTheGameSetting() {
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        brainOf(ai).setFlatMonteCarloOverride(false);
        addCards("Island", 3, ai);
        addCardToZone("Divination", ai, ZoneType.Hand);
        addCardToZone("Merfolk of the Pearl Trident", ai, ZoneType.Hand);
        fillLibrariesWith(ai, this, "Island");
        fillLibrariesWith(game.getPlayers().get(0), this, "Mountain");
        int before = FlatMonteCarloPicker.getTotalRollouts();
        playUntilNextTurn(game);
        AssertJUnit.assertEquals("an AI with the override off must not run rollouts", before, FlatMonteCarloPicker.getTotalRollouts());
    }

    @Test
    public void testHarderProfilesAreValidAndStrongerThanDefault() throws Exception {
        java.io.File dir = new java.io.File("../forge-gui/res/ai");
        if (!dir.isDirectory()) {
            return; // run from an unexpected directory: nothing to check
        }
        java.util.Map<String, java.util.Properties> profiles = new java.util.HashMap<>();
        for (String name : new String[] { "Default", "Hard", "Hardest" }) {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(new java.io.File(dir, name + ".ai"))) {
                props.load(in);
            }
            profiles.put(name, props);
            for (Object key : props.keySet()) {
                try {
                    AiProps.valueOf(key.toString());
                } catch (IllegalArgumentException e) {
                    AssertJUnit.fail(name + ".ai contains an unknown key " + key);
                }
            }
            for (AiProps p : AiProps.values()) {
                if (p.name().startsWith("FLAT_MC_") && !"true".equals(props.getProperty(p.name())) && !"false".equals(props.getProperty(p.name()))) {
                    Integer.parseInt(props.getProperty(p.name(), p.getDefault()).trim());
                }
            }
        }
        for (String key : new String[] { "FLAT_MC_ROLLOUTS_PER_CANDIDATE", "FLAT_MC_DECISION_BUDGET_MS", "FLAT_MC_MAX_CANDIDATES" }) {
            int def = Integer.parseInt(profiles.get("Default").getProperty(key));
            int hard = Integer.parseInt(profiles.get("Hard").getProperty(key));
            int hardest = Integer.parseInt(profiles.get("Hardest").getProperty(key));
            AssertJUnit.assertTrue(key + ": Hard must search more than Default", hard > def);
            AssertJUnit.assertTrue(key + ": Hardest must search more than Hard", hardest > hard);
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testMajorityRuleKeepsTheHeuristicWhenNoLineClearlyWins() {
        // Two identical-value lines: nothing may override the heuristic's first choice.
        Game game = createMonteCarloGame(true);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        addCards("Mountain", 2, ai);
        addCardToZone("Raging Goblin", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        fillLibrariesWith(ai, this, "Mountain");
        fillLibrariesWith(game.getPlayers().get(0), this, "Mountain");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        List<SpellAbility> plays = playsToCompare(ai);
        if (plays.size() < 2) {
            return;
        }
        FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        cfg.heuristicBonus = 100000; // the heuristic line can only be beaten by a huge margin
        FlatMonteCarloPicker.Result r = FlatMonteCarloPicker.choose(ai, plays, cfg, null);
        AssertJUnit.assertFalse("a huge bonus must never be overridden: " + r, "override".equals(r.reason));
    }

    /**
     * Strength benchmark, skipped unless run with -Dforge.mcBenchmark=GAMES (e.g. 20): Monte Carlo against the pure
     * heuristics in the same matchup, sides swapped every game. It only reports and asserts that nothing failed;
     * judging strength needs a large number of games.
     */
    @Test(timeOut = 3_600_000)
    public void testBenchmarkMonteCarloVersusHeuristics() {
        int games = Integer.getInteger("forge.mcBenchmark", 0);
        if (games <= 0) {
            return;
        }
        int mcWins = 0;
        int heuWins = 0;
        int failuresBefore = FlatMonteCarloPicker.getTotalFailures();
        for (int g = 0; g < games; g++) {
            Game game = createMonteCarloGame(false);
            game.AI_TIMEOUT = 5;
            Player control = game.getPlayers().get(1);
            Player beatdown = game.getPlayers().get(0);
            control.setTeam(0);
            beatdown.setTeam(1);
            boolean mcIsControl = g % 2 == 0;
            brainOf(control).setFlatMonteCarloOverride(mcIsControl);
            brainOf(beatdown).setFlatMonteCarloOverride(!mcIsControl);
            addCards("Island", 2, control);
            addCards("Mountain", 2, beatdown);
            for (int i = 0; i < 12; i++) {
                for (String n : new String[] { "Island", "Counterspell", "Divination", "Air Elemental", "Cancel" }) {
                    addCardToZone(n, control, ZoneType.Library);
                }
                for (String n : new String[] { "Mountain", "Raging Goblin", "Shock", "Lightning Bolt" }) {
                    addCardToZone(n, beatdown, ZoneType.Library);
                }
            }
            for (int turns = 0; turns < 40 && !game.isGameOver(); turns++) {
                playUntilNextTurn(game);
            }
            if (game.isGameOver()) {
                boolean controlWon = control.getOutcome() != null && control.getOutcome().hasWon();
                boolean beatdownWon = beatdown.getOutcome() != null && beatdown.getOutcome().hasWon();
                if (controlWon || beatdownWon) {
                    if (controlWon == mcIsControl) {
                        mcWins++;
                    } else {
                        heuWins++;
                    }
                }
            }
        }
        System.out.println("Flat MC benchmark: Monte Carlo " + mcWins + " - heuristics " + heuWins + " of " + games + " games");
        AssertJUnit.assertEquals("no rollout may fail", failuresBefore, FlatMonteCarloPicker.getTotalFailures());
    }
}
