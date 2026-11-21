package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.ai.simulation.FlatMonteCarloStats;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Realistic scenarios for the flat Monte Carlo AI, aimed at showing that it copes with any kind of deck and not only
 * the control-against-creatures matchup the first tests used:
 * <ul>
 *   <li>whole games between recognisable archetypes (aggro, control, burn, midrange, tokens, tempo, removal), with
 *       real 40 card decks, a shuffled library and a seven card hand, played with Monte Carlo on for both sides;</li>
 *   <li>focused decisions on the picker itself: a lethal line among many, and the same position decided with the
 *       original sampling and with the refined sampling, which must both give valid answers inside their bounds.</li>
 * </ul>
 * What they check is that nothing breaks (no rollout throws, the real game is never touched, the game advances) and
 * that rollouts really happen. Whether the refinements make the AI <em>stronger</em> is measured with the benchmark in
 * tools/flatmc, because a handful of games cannot show that.
 */
public class FlatMonteCarloArchetypeTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 300_000;
    private static final int TURNS = 10;

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

    /**
     * Deals a deck to a player: the cards are shuffled with the given seed, the top seven become the hand and the rest
     * the library, and two of its lands are already in play to keep the test short.
     *
     * @param list alternating card names and counts, e.g. {@code "Mountain", 16, "Shock", 4}
     */
    private void deal(final Player p, final long seed, final String land, final Object... list) {
        final List<String> cards = new ArrayList<>();
        for (int i = 0; i < list.length; i += 2) {
            for (int n = 0; n < (Integer) list[i + 1]; n++) {
                cards.add((String) list[i]);
            }
        }
        Collections.shuffle(cards, new Random(seed));
        addCards(land, 2, p);
        for (int i = 0; i < cards.size(); i++) {
            addCardToZone(cards.get(i), p, i < 7 ? ZoneType.Hand : ZoneType.Library);
        }
    }

    /** Plays a matchup for a number of turns with Monte Carlo on for both sides and checks that nothing goes wrong. */
    private void playMatchup(final String label, final Runnable setup, final Game game) {
        setup.run();
        FlatMonteCarloStats.reset();
        final int pickerFailuresBefore = FlatMonteCarloPicker.getTotalFailures();
        final int rolloutsBefore = FlatMonteCarloPicker.getTotalRollouts();

        int turns = 0;
        for (int i = 0; i < TURNS && !game.isGameOver(); i++) {
            playUntilNextTurn(game);
            turns++;
        }

        System.out.println(label + ": " + turns + " turns, game over: " + game.isGameOver());
        System.out.println(FlatMonteCarloStats.summary());
        AssertJUnit.assertTrue(label + ": the game must advance", turns >= 1);
        AssertJUnit.assertEquals(label + ": no rollout may fail with an exception", pickerFailuresBefore,
                FlatMonteCarloPicker.getTotalFailures());
        AssertJUnit.assertEquals(label + ": the statistics must not record failures either", 0, FlatMonteCarloStats.getFailureCount());
        AssertJUnit.assertTrue(label + ": the AI must really have used rollouts",
                FlatMonteCarloPicker.getTotalRollouts() > rolloutsBefore);
        AssertJUnit.assertTrue(label + ": every decision with rollouts is counted", FlatMonteCarloStats.getDecisions() > 0);
    }

    // ------------------------------------------------------------------ whole games between archetypes

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRedAggroAgainstBlueControl() {
        final Game game = newGame();
        playMatchup("aggro vs control", () -> {
            deal(game.getPlayers().get(0), 11, "Mountain", "Mountain", 14, "Goblin Guide", 4, "Raging Goblin", 4,
                    "Lightning Bolt", 4, "Shock", 4, "Hill Giant", 4, "Lava Spike", 4);
            deal(game.getPlayers().get(1), 12, "Island", "Island", 15, "Counterspell", 4, "Mana Leak", 4, "Remand", 3,
                    "Divination", 4, "Air Elemental", 4, "Mulldrifter", 4);
        }, game);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testGreenMidrangeAgainstBurn() {
        final Game game = newGame();
        playMatchup("midrange vs burn", () -> {
            deal(game.getPlayers().get(0), 21, "Forest", "Forest", 15, "Grizzly Bears", 5, "Giant Spider", 5,
                    "Llanowar Elves", 4, "Giant Growth", 5, "Ambush Viper", 4);
            deal(game.getPlayers().get(1), 22, "Mountain", "Mountain", 14, "Lightning Bolt", 6, "Shock", 6,
                    "Lava Spike", 4, "Raging Goblin", 4, "Goblin Guide", 4);
        }, game);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testWhiteTokensAgainstWhiteSweepers() {
        final Game game = newGame();
        playMatchup("tokens vs sweepers", () -> {
            deal(game.getPlayers().get(0), 61, "Plains", "Plains", 16, "Savannah Lions", 8, "Raise the Alarm", 8,
                    "Healing Salve", 4, "Pacifism", 4);
            deal(game.getPlayers().get(1), 62, "Plains", "Plains", 17, "Wrath of God", 4, "Serra Angel", 5,
                    "Pacifism", 5, "Savannah Lions", 5, "Healing Salve", 4);
        }, game);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testBlueTempoAgainstBlackRemoval() {
        final Game game = newGame();
        playMatchup("tempo vs removal", () -> {
            deal(game.getPlayers().get(0), 41, "Island", "Island", 15, "Brineborn Cutthroat", 5, "Vendilion Clique", 3,
                    "Remand", 4, "Memory Lapse", 4, "Merfolk of the Pearl Trident", 5, "Opt", 4);
            deal(game.getPlayers().get(1), 42, "Swamp", "Swamp", 15, "Terror", 5, "Doom Blade", 5, "Disfigure", 5,
                    "Vampire Nighthawk", 5, "Gravedigger", 5);
        }, game);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testRampAgainstAggro() {
        final Game game = newGame();
        playMatchup("ramp vs aggro", () -> {
            deal(game.getPlayers().get(0), 51, "Forest", "Forest", 15, "Llanowar Elves", 4, "Elvish Mystic", 4,
                    "Giant Spider", 5, "Grizzly Bears", 4, "Giant Growth", 4, "Mind Stone", 4);
            deal(game.getPlayers().get(1), 52, "Mountain", "Mountain", 16, "Goblin Guide", 4, "Raging Goblin", 5,
                    "Hill Giant", 5, "Shock", 5, "Lightning Bolt", 5);
        }, game);
    }

    // ------------------------------------------------------------------ focused decisions

    /** What the heuristics approve of, with targets set up by the heuristics, like AiController does it. */
    private static List<SpellAbility> playsToCompare(final Player p) {
        final AiController ai = brainOf(p);
        final List<SpellAbility> candidates = new SpellAbilityPicker(p).getCandidateSpellsAndAbilities();
        final List<SpellAbility> approved = new ArrayList<>();
        for (final SpellAbility sa : candidates) {
            if (ai.canPlaySa(sa) == AiPlayDecision.WillPlay) {
                approved.add(sa);
            }
        }
        // The picker must cope with any list of playable plays, and the heuristics may rightly prefer to wait (or approve
        // only one of several): with fewer than two approved plays there would be nothing to compare, so use them all.
        return approved.size() < 2 ? candidates : approved;
    }

    private static void moveFirstWithHost(final List<SpellAbility> plays, final String cardName) {
        for (int i = 0; i < plays.size(); i++) {
            if (plays.get(i).getHostCard() != null && cardName.equals(plays.get(i).getHostCard().getName())) {
                plays.add(0, plays.remove(i));
                return;
            }
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testLethalBurnIsFoundAmongManyOtherPlays() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        for (final String n : new String[] { "Lightning Bolt", "Raging Goblin", "Shock" }) {
            addCardToZone(n, ai, ZoneType.Hand);
        }
        opp.setLife(3, null);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Mountain", opp, ZoneType.Library);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final List<SpellAbility> plays = new ArrayList<>(playsToCompare(ai));
        AssertJUnit.assertTrue("several plays to choose from, got " + plays, plays.size() >= 2);
        moveFirstWithHost(plays, "Lightning Bolt");
        final SpellAbility bolt = plays.get(0);
        AssertJUnit.assertEquals("Lightning Bolt", bolt.getHostCard().getName());

        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        final FlatMonteCarloPicker.Result result = FlatMonteCarloPicker.choose(ai, plays, cfg,
                sa -> brainOf(ai).canPlaySa(sa) == AiPlayDecision.WillPlay);

        AssertJUnit.assertFalse("never hold or pass with lethal on the table: " + result, result.hold);
        AssertJUnit.assertEquals(String.valueOf(result), "wins", result.reason);
        AssertJUnit.assertSame(bolt, result.choice);
        AssertJUnit.assertEquals("a win ends the search at once: " + result, 1, result.rollouts);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testOriginalAndRefinedSamplingBothDecideValidlyWithinTheirBounds() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCards("Island", 4, ai);
        addCards("Mountain", 2, ai);
        for (final String n : new String[] { "Counterspell", "Divination", "Air Elemental", "Shock", "Runeclaw Bear" }) {
            addCardToZone(n, ai, ZoneType.Hand);
        }
        addCards("Runeclaw Bear", 2, opp);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Mountain", opp, ZoneType.Library);
            addCardToZone("Runeclaw Bear", opp, ZoneType.Library);
        }
        addCardToZone("Shock", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final List<SpellAbility> plays = playsToCompare(ai);
        AssertJUnit.assertFalse("there should be something playable to compare", plays.isEmpty());
        final String before = gameStateToString(game);
        final int failuresBefore = FlatMonteCarloPicker.getTotalFailures();

        // the original behaviour: round robin, no pruning, no extra rounds, mana-cost-only hold values
        final FlatMonteCarloPicker.Config original = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        original.pruning = false;
        original.extraRounds = 0;
        original.categoryHold = false;
        original.adaptivePlayout = false;
        final FlatMonteCarloPicker.Result a = FlatMonteCarloPicker.choose(ai, plays, original,
                sa -> brainOf(ai).canPlaySa(sa) == AiPlayDecision.WillPlay);

        final FlatMonteCarloPicker.Config refined = FlatMonteCarloPicker.Config.fromProfile(brainOf(ai), ai);
        final FlatMonteCarloPicker.Result b = FlatMonteCarloPicker.choose(ai, plays, refined,
                sa -> brainOf(ai).canPlaySa(sa) == AiPlayDecision.WillPlay);

        for (final FlatMonteCarloPicker.Result r : new FlatMonteCarloPicker.Result[] { a, b }) {
            AssertJUnit.assertFalse(String.valueOf(r), "error".equals(r.reason));
            AssertJUnit.assertTrue("rollouts must have run: " + r, r.rollouts > 0);
            AssertJUnit.assertTrue("a chosen play must be one of the plays given: " + r, r.hold || plays.contains(r.choice));
            AssertJUnit.assertTrue("the leader is never pruned: " + r, r.pruned < Math.max(1, r.candidatesEvaluated));
        }
        AssertJUnit.assertEquals("the original sampling never prunes", 0, a.pruned);
        AssertJUnit.assertTrue("original: at most the base rounds per line: " + a,
                a.rollouts <= a.candidatesEvaluated * original.rolloutsPerCandidate);
        AssertJUnit.assertTrue("refined: at most the base plus extra rounds per line: " + b,
                b.rollouts <= b.candidatesEvaluated * (refined.rolloutsPerCandidate + refined.extraRounds));
        AssertJUnit.assertEquals("no rollout may fail", failuresBefore, FlatMonteCarloPicker.getTotalFailures());
        AssertJUnit.assertEquals("the real game must not have been touched", before, gameStateToString(game));
        AssertJUnit.assertFalse(game.isGameOver());

        // decisions must stay cheap enough for a phone: bounded by the budget with the usual slack for a slow machine
        final long limit = 6 * refined.budgetNanos + TimeUnit.SECONDS.toNanos(3);
        AssertJUnit.assertTrue("refined decision took " + TimeUnit.NANOSECONDS.toMillis(b.elapsedNanos) + " ms: " + b,
                b.elapsedNanos < limit);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testBurnDeckGivesItsBurnToTheFaceWhenItWins() {
        // A burn deck must not talk itself into holding Lightning Bolt when the opponent is on 3: this is the case the
        // control-deck hold bonus used to get wrong for decks with many instants.
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 16; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
        }
        for (final String n : new String[] { "Lightning Bolt", "Lightning Bolt", "Shock", "Shock", "Goblin Guide", "Hill Giant" }) {
            addCardToZone(n, ai, ZoneType.Library);
        }
        addCards("Mountain", 3, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        opp.setLife(3, null);
        for (int i = 0; i < 12; i++) {
            addCardToZone("Island", opp, ZoneType.Library);
        }
        playUntilPhase(game, PhaseType.END_OF_TURN);
        AssertJUnit.assertTrue("3 life against Bolt and Shock: the game should be over", game.isGameOver());
        AssertJUnit.assertTrue(ai.hasWon());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void testStatisticsDescribeARealGame() {
        final Game game = newGame();
        deal(game.getPlayers().get(0), 61, "Mountain", "Mountain", 14, "Goblin Guide", 4, "Raging Goblin", 4,
                "Lightning Bolt", 4, "Shock", 4, "Hill Giant", 4, "Lava Spike", 4);
        deal(game.getPlayers().get(1), 62, "Island", "Island", 15, "Counterspell", 4, "Mana Leak", 4, "Remand", 3,
                "Divination", 4, "Air Elemental", 4, "Mulldrifter", 4);
        FlatMonteCarloStats.reset();
        for (int i = 0; i < 6 && !game.isGameOver(); i++) {
            playUntilNextTurn(game);
        }
        final long decisions = FlatMonteCarloStats.getDecisions();
        AssertJUnit.assertTrue("decisions must have been recorded", decisions > 0);
        AssertJUnit.assertTrue(FlatMonteCarloStats.getRollouts() > 0);
        AssertJUnit.assertTrue("percentiles must be ordered", FlatMonteCarloStats.getPercentileMillis(50)
                <= FlatMonteCarloStats.getPercentileMillis(95));
        AssertJUnit.assertTrue(FlatMonteCarloStats.summary().startsWith("FLATMC-STATS decisions=" + decisions + " "));
    }
}
