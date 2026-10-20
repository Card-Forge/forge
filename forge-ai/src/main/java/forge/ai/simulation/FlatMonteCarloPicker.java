package forge.ai.simulation;

import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.AITimeoutTracker;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Flat Monte Carlo refinement on top of the normal heuristic AI.
 * <p>
 * The heuristic AI walks its sorted list of spell abilities and plays the <em>first</em> one it approves of. This
 * class receives the first few approved plays (best heuristic choice first) and compares them, plus an optional
 * "hold" option (pass with mana up), by sampling:
 * <ul>
 *   <li>every sample copies the game and re-deals the cards the AI cannot see ({@link HiddenInfoShuffler}), so the
 *       samples really differ and the AI does not peek at the opponent's hand;</li>
 *   <li>the candidate is played, then the heuristic AI itself continues the turn (the playout policy), combat is
 *       run, and the position at the second main phase is scored with {@link GameStateEvaluator};</li>
 *   <li>all candidates use the same seeds (common random numbers) and are sampled round-robin, so a time-limited
 *       run still compares like with like.</li>
 * </ul>
 * The heuristic choice gets a bonus ({@link Config#heuristicBonus}), so rollouts only override it for a clear
 * gain, and every problem (time, errors, a line that cannot be simulated) falls back to the heuristic choice.
 * No threads are used and cost is bounded by {@link Config#budgetNanos}, which is what keeps this usable on slow
 * devices: the number of samples simply shrinks with the speed of the device.
 * <p>
 * This is deliberately not a tree search (see {@link SpellAbilityPicker} for that): it only chooses between
 * plays the heuristics already approve of, and only looks as far as the end of the AI's own turn.
 */
public final class FlatMonteCarloPicker {
    /** Print one line per decision to stderr; start the JVM with -Dforge.flatmc.debug=true. */
    public static boolean DEBUG = Boolean.getBoolean("forge.flatmc.debug");

    /** Copies must never think for long: they only exist to be thrown away. */
    static final int ROLLOUT_AI_TIMEOUT_SECONDS = 2;
    /** Terminal values, sized well above any ordinary {@link GameStateEvaluator} score (which is in the low thousands). */
    static final long WIN_VALUE = 20000;
    static final long LOSS_VALUE = -20000;

    private static final long HIDDEN_INFO_SALT = 0x5DEECE66DL;
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;
    /** The second rollout (needed to compare anything) may take this many budgets in total. */
    private static final int HARD_DEADLINE_FACTOR = 2;

    private static final int HOLD_VALUE_PER_CMC = 14;
    private static final int HOLD_COUNTER_VALUE_PER_CMC = 22;
    private static final int HOLD_VALUE_CAP = 90;
    private static final int HOLD_TOTAL_CAP = 140;
    /** Decks that look like control decks value holding up instants more. */
    private static final double CONTROL_HOLD_FACTOR = 1.75;

    /** Penalties/bonuses for the position after the turn, in evaluator points (a 2/2 creature is worth ~150). */
    private static final int RISK_SERIOUS_DANGER = 700;
    private static final int RISK_DANGER = 260;
    private static final int PRESSURE_BONUS = 120;
    /** Decks full of cheap creatures and burn hold up less, they would rather spend the mana. */
    private static final double AGGRO_HOLD_FACTOR = 0.5;

    private static final AtomicInteger TOTAL_ROLLOUTS = new AtomicInteger();
    private static final AtomicInteger TOTAL_FAILURES = new AtomicInteger();

    /** How many rollouts have been started in this JVM; lets tests check that rollouts really happened. */
    public static int getTotalRollouts() {
        return TOTAL_ROLLOUTS.get();
    }

    /** How many rollouts (or whole decisions) ended in an unexpected exception in this JVM. Should stay at 0. */
    public static int getTotalFailures() {
        return TOTAL_FAILURES.get();
    }

    private FlatMonteCarloPicker() {
    }

    /** Tunable parameters, normally read from the AI profile with {@link #fromProfile}. */
    public static final class Config {
        /** How many of the heuristic-approved plays are compared (the hold option comes on top). */
        public int maxCandidates = 5;
        /** Samples per candidate, at most; fewer when the time budget runs out. */
        public int rolloutsPerCandidate = 4;
        /** Plays the heuristic AI may make per main phase while a rollout continues the turn. */
        public int maxPlayoutPlays = 6;
        /** The same for a hand that looks like a combo hand (long chains of cheap spells need a longer playout). */
        public int comboPlayoutPlays = 14;
        /** Soft limit on the time of one decision. */
        public long budgetNanos = TimeUnit.MILLISECONDS.toNanos(450);
        /** Soft limit on the time of all decisions in one turn. */
        public long turnBudgetNanos = TimeUnit.MILLISECONDS.toNanos(2000);
        /** Score added to the heuristic's first choice, i.e. how clearly rollouts must prefer another line. */
        public int heuristicBonus = 80;
        /** Whether "pass and keep mana up for instants" is compared with the plays. */
        public boolean considerHolding = true;
        /** The hold option is dropped for this decision, e.g. because the AI already held up last turn. */
        public boolean holdForbidden = false;
        /** Percentage of the value of held-up instants that is counted, 0 turns the valuation off. */
        public int holdManaWeightPercent = 100;
        /** Extra multiplier for the held-up value, above 1 for decks that look like control decks. */
        public double controlFactor = 1.0;
        /** Percentage of the penalty for ending the turn open to a lethal/dangerous crack-back, 0 turns it off. */
        public int riskWeightPercent = 100;
        /** Rounds in which another line has to beat the heuristic line, as a share of rounds (percent, 50 = more than half). */
        public int overrideMajorityPercent = 50;

        public static Config fromProfile(final AiController ai, final Player player) {
            final Config cfg = new Config();
            cfg.maxCandidates = intProp(ai, AiProps.FLAT_MC_MAX_CANDIDATES, 2, 8);
            cfg.rolloutsPerCandidate = intProp(ai, AiProps.FLAT_MC_ROLLOUTS_PER_CANDIDATE, 1, 10);
            cfg.maxPlayoutPlays = intProp(ai, AiProps.FLAT_MC_MAX_PLAYOUT_PLAYS, 0, 20);
            cfg.comboPlayoutPlays = intProp(ai, AiProps.FLAT_MC_COMBO_PLAYOUT_PLAYS, 0, 30);
            cfg.budgetNanos = TimeUnit.MILLISECONDS.toNanos(intProp(ai, AiProps.FLAT_MC_DECISION_BUDGET_MS, 0, 10000));
            cfg.turnBudgetNanos = TimeUnit.MILLISECONDS.toNanos(intProp(ai, AiProps.FLAT_MC_TURN_BUDGET_MS, 0, 60000));
            cfg.heuristicBonus = intProp(ai, AiProps.FLAT_MC_HEURISTIC_BONUS, 0, 100000);
            cfg.considerHolding = ai.getBoolProperty(AiProps.FLAT_MC_CONSIDER_HOLDING);
            cfg.holdManaWeightPercent = intProp(ai, AiProps.FLAT_MC_HOLD_MANA_WEIGHT, 0, 1000);
            cfg.riskWeightPercent = intProp(ai, AiProps.FLAT_MC_RISK_WEIGHT, 0, 1000);
            cfg.overrideMajorityPercent = intProp(ai, AiProps.FLAT_MC_OVERRIDE_MAJORITY, 0, 99);
            cfg.controlFactor = controlFactor(player);
            return cfg;
        }

        private static int intProp(final AiController ai, final AiProps prop, final int min, final int max) {
            int value;
            try {
                value = ai.getIntProperty(prop);
            } catch (final NumberFormatException e) {
                value = Integer.parseInt(prop.getDefault()); // a typo in a profile must not break the AI
            }
            return Math.max(min, Math.min(max, value));
        }
    }

    /** What {@link #choose} decided, with enough detail for tests and debugging. */
    public static final class Result {
        /** The chosen play, or null when {@link #hold} is set. */
        public final SpellAbility choice;
        /** Pass priority and keep the mana instead of playing any of the candidates. */
        public final boolean hold;
        /** True if rollouts made the decision, false if this is just the heuristic choice. */
        public final boolean decidedByRollouts;
        public final int candidatesEvaluated;
        public final int rollouts;
        public final long elapsedNanos;
        /** Short machine readable explanation, e.g. "override", "kept-heuristic", "hold", "too-slow", "error". */
        public final String reason;

        Result(final SpellAbility choice, final boolean hold, final boolean decidedByRollouts,
               final int candidatesEvaluated, final int rollouts, final long elapsedNanos, final String reason) {
            this.choice = choice;
            this.hold = hold;
            this.decidedByRollouts = decidedByRollouts;
            this.candidatesEvaluated = candidatesEvaluated;
            this.rollouts = rollouts;
            this.elapsedNanos = elapsedNanos;
            this.reason = reason;
        }

        /** A result that simply keeps the heuristic choice without having sampled anything. */
        public static Result keepHeuristic(final SpellAbility heuristicChoice, final String reason) {
            return new Result(heuristicChoice, false, false, 0, 0, 0, reason);
        }

        @Override
        public String toString() {
            return "FlatMC[" + reason + ", " + (hold ? "HOLD" : String.valueOf(choice))
                    + ", candidates=" + candidatesEvaluated + ", rollouts=" + rollouts
                    + ", ms=" + TimeUnit.NANOSECONDS.toMillis(elapsedNanos) + "]";
        }
    }

    private static final class Candidate {
        /** The play, or null for the hold option. */
        final SpellAbility sa;

        Candidate(final SpellAbility sa) {
            this.sa = sa;
        }

        boolean isHold() {
            return sa == null;
        }

        @Override
        public String toString() {
            return isHold() ? "HOLD" : String.valueOf(sa);
        }
    }

    /**
     * Picks between plays the heuristic AI approved of.
     *
     * @param player   the AI player, in the real game
     * @param approved plays the heuristics would make, the one they prefer first; must not be empty
     * @param revalidate asked right before a play is sampled, to have the heuristics set up its targets again
     *                   (evaluating one play can disturb the state of another); may be null
     */
    public static Result choose(final Player player, final List<SpellAbility> approved, final Config cfg,
                                final Predicate<SpellAbility> revalidate) {
        final long start = System.nanoTime();
        final SpellAbility heuristicChoice = approved.get(0);
        try {
            final Result result = chooseImpl(player, approved, cfg, revalidate, start);
            if (DEBUG) {
                System.err.println(player + " " + result);
            }
            return result;
        } catch (final AITimeoutTracker.Expired e) {
            return new Result(heuristicChoice, false, false, 0, 0, System.nanoTime() - start, "deadline");
        } catch (final RuntimeException e) {
            // Rollouts only ever run on copies of the game, so whatever went wrong in there must not take the real
            // game down: play what the heuristics wanted to play anyway.
            reportFailure(e);
            return new Result(heuristicChoice, false, false, 0, 0, System.nanoTime() - start, "error");
        }
    }

    private static Result chooseImpl(final Player player, final List<SpellAbility> approved, final Config cfg,
                                     final Predicate<SpellAbility> revalidate, final long start) {
        final SpellAbility heuristicChoice = approved.get(0);
        final List<Candidate> cands = buildCandidates(player, approved, cfg);
        if (cands.size() < 2) {
            return Result.keepHeuristic(heuristicChoice, "single-candidate");
        }

        final long budget = Math.min(cfg.budgetNanos, AITimeoutTracker.remainingNanos() / 2);
        if (budget <= 0) {
            return Result.keepHeuristic(heuristicChoice, "no-time");
        }
        final long softDeadline = start + budget;
        final long hardDeadline = start + HARD_DEADLINE_FACTOR * budget;
        final long baseSeed = MyRandom.getRandom().nextLong();

        final int playoutPlays = isComboLike(player) ? Math.max(cfg.maxPlayoutPlays, cfg.comboPlayoutPlays) : cfg.maxPlayoutPlays;
        final int n = cands.size();
        final long[] sum = new long[n];
        final int[] count = new int[n];
        final boolean[] invalid = new boolean[n];
        final long[][] vals = new long[n][cfg.rolloutsPerCandidate];
        int evaluated = 0;
        int rollouts = 0;
        long maxRolloutNanos = 0;
        boolean tooSlow = false;

        rounds:
        for (int r = 0; r < cfg.rolloutsPerCandidate; r++) {
            final long seed = baseSeed + r * SEED_STEP;
            final int active = r == 0 ? n : evaluated;
            // Only start another round if all of it fits, so that every candidate gets the same samples.
            if (r > 0 && System.nanoTime() + maxRolloutNanos * active > (r == 1 ? hardDeadline : softDeadline)) {
                break;
            }
            for (int c = 0; c < active; c++) {
                if (invalid[c]) {
                    continue;
                }
                AITimeoutTracker.check();
                if (rollouts > 0) {
                    // The second candidate is what makes a comparison possible at all, so it may use the hard limit.
                    final long limit = r == 0 && c == 1 || r == 1 ? hardDeadline : softDeadline;
                    if (System.nanoTime() + maxRolloutNanos > limit) {
                        tooSlow = r == 0 && c < 2;
                        break rounds;
                    }
                }
                final long t0 = System.nanoTime();
                final Candidate cand = cands.get(c);
                final Long value = !cand.isHold() && revalidate != null && !revalidate.test(cand.sa)
                        ? null : rollout(player, cand, seed, cfg, playoutPlays);
                maxRolloutNanos = Math.max(maxRolloutNanos, System.nanoTime() - t0);
                rollouts++;
                if (value == null) {
                    invalid[c] = true;
                } else {
                    sum[c] += value;
                    vals[c][count[c]] = value;
                    count[c]++;
                    if (value == WIN_VALUE && !cand.isHold()) {
                        // This line wins the game outright: nothing needs comparing, and waiting only wastes time.
                        return new Result(cand.sa, false, true, Math.max(evaluated, c + 1), rollouts,
                                System.nanoTime() - start, "wins");
                    }
                }
                if (r == 0) {
                    evaluated = c + 1;
                }
            }
        }

        final long elapsed = System.nanoTime() - start;
        if (evaluated < 2) {
            return new Result(heuristicChoice, false, false, evaluated, rollouts, elapsed, tooSlow ? "too-slow" : "no-time");
        }
        if (invalid[0] || count[0] == 0) {
            // We cannot even simulate what the heuristics want to do, so there is nothing to compare against.
            return new Result(heuristicChoice, false, false, evaluated, rollouts, elapsed, "heuristic-unsimulatable");
        }

        int best = 0;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < evaluated; c++) {
            if (invalid[c] || count[c] == 0) {
                continue;
            }
            final double value = (double) sum[c] / count[c] + (c == 0 ? cfg.heuristicBonus : 0);
            if (value > bestValue) {
                bestValue = value;
                best = c;
            }
        }

        if (best != 0 && count[best] > 0 && count[0] > 0) {
            // Overriding the heuristics needs more than a higher mean: the line must also win a majority of the
            // paired rounds (same seed = same hidden cards), or one lucky sample could talk the AI out of a fine play.
            final int rounds = Math.min(count[0], count[best]);
            int wins = 0;
            for (int i = 0; i < rounds; i++) {
                if (vals[best][i] > vals[0][i] + cfg.heuristicBonus / 4) {
                    wins++;
                }
            }
            if (rounds >= 2 && wins * 100 <= rounds * cfg.overrideMajorityPercent) {
                best = 0;
            } else if (rounds < 2 && (double) sum[best] / count[best] < (double) sum[0] / count[0] + 2.0 * cfg.heuristicBonus) {
                // One sample per line is only noise: leave the heuristic choice unless the gap is large.
                best = 0;
            }
        }

        final Candidate chosen = cands.get(best);
        final String reason = chosen.isHold() ? "hold" : best == 0 ? "kept-heuristic" : "override";
        return new Result(chosen.sa, chosen.isHold(), true, evaluated, rollouts, elapsed, reason);
    }

    /**
     * Drops plays that are the same as an earlier one: same card name, same ability text and same targets, like two
     * copies of a permanent with an activated ability. (Identical cards in hand are already merged by the heuristics.)
     * Playing either gives the same result, so sampling both only wastes the time budget. The order is kept.
     */
    public static List<SpellAbility> dedupePlays(final List<SpellAbility> plays) {
        final List<SpellAbility> unique = new ArrayList<>(plays.size());
        final Set<String> seen = new HashSet<>();
        for (final SpellAbility sa : plays) {
            final Card host = sa.getHostCard();
            final String key = (host == null ? "" : host.getName()) + "|" + sa.getDescription() + "|"
                    + (sa.usesTargeting() ? String.valueOf(sa.getTargets()) : "");
            if (seen.add(key)) {
                unique.add(sa);
            }
        }
        return unique;
    }

    private static List<Candidate> buildCandidates(final Player player, final List<SpellAbility> approvedPlays,
                                                   final Config cfg) {
        final List<SpellAbility> approved = dedupePlays(approvedPlays);
        final List<Candidate> cands = new ArrayList<>();
        cands.add(new Candidate(approved.get(0))); // index 0 is always the heuristic choice
        if (cfg.considerHolding && !cfg.holdForbidden && isHoldPossible(player)) {
            cands.add(new Candidate(null));
        }
        final int plays = Math.min(approved.size(), cfg.maxCandidates);
        for (int i = 1; i < plays; i++) {
            cands.add(new Candidate(approved.get(i)));
        }
        return cands;
    }

    /** Holding is only an option if there is something to hold mana up for, and it will not force a discard. */
    public static boolean isHoldPossible(final Player player) {
        final List<Card> hand = new ArrayList<>(player.getCardsIn(ZoneType.Hand));
        if (!player.isUnlimitedHandSize() && hand.size() > player.getMaxHandSize()) {
            return false;
        }
        final int mana = ComputerUtilMana.getAvailableManaEstimate(player, true);
        for (final Card c : hand) {
            if (isInstantSpeedSpell(c) && c.getCMC() <= mana) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInstantSpeedSpell(final Card c) {
        return !c.isLand() && (c.isInstant() || c.hasKeyword(Keyword.FLASH));
    }

    private static boolean isCounterspell(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Counter) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the AI's hand looks like it wants a long chain of plays this turn: a Storm spell, several mana
     * rituals, or a fistful of nearly free spells. Those lines only pay off at the end of a long sequence, so the
     * playout has to be allowed to run longer to see the payoff (it costs more time, which the budget still caps).
     */
    public static boolean isComboLike(final Player player) {
        int rituals = 0;
        int cheap = 0;
        for (final Card c : player.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                continue;
            }
            if (c.hasKeyword(Keyword.STORM)) {
                return true;
            }
            if (c.getCMC() <= 1) {
                cheap++;
                for (final SpellAbility sa : c.getSpellAbilities()) {
                    if (sa.getApi() == ApiType.Mana) {
                        rituals++;
                        break;
                    }
                }
            }
        }
        return rituals >= 2 || cheap >= 4;
    }

    /** 1.0, or {@link #CONTROL_HOLD_FACTOR} for a deck with several counterspells or many reactive spells. */
    static double controlFactor(final Player player) {
        int spells = 0;
        int reactive = 0;
        int counters = 0;
        for (final Card c : player.getAllCards()) {
            if (c.isLand() || c.isToken() || c.isImmutable()) {
                continue;
            }
            spells++;
            final boolean counter = isCounterspell(c);
            if (counter) {
                counters++;
            }
            if (counter || isInstantSpeedSpell(c)) {
                reactive++;
            }
        }
        if (spells == 0) {
            return 1.0;
        }
        if (counters >= 3 || reactive * 100 / spells >= 30) {
            return CONTROL_HOLD_FACTOR;
        }
        return reactive * 100 / spells <= 5 ? AGGRO_HOLD_FACTOR : 1.0;
    }

    /**
     * One sample of one candidate.
     *
     * @return the value of the position reached, or null if this candidate cannot be simulated
     */
    private static Long rollout(final Player player, final Candidate cand, final long seed, final Config cfg,
                                final int playoutPlays) {
        TOTAL_ROLLOUTS.incrementAndGet();
        final Random savedRandom = MyRandom.getRandom();
        try {
            // MyRandom is global state; this is safe because the game thread is waiting for this AI decision.
            MyRandom.setRandom(new Random(seed));

            final GameSimulator sim = new GameSimulator(player.getGame(), player);
            final Game simGame = sim.getSimulatedGameState();
            final Player simAi = sim.getSimulatedAiPlayer();
            simGame.AI_TIMEOUT = Math.min(simGame.AI_TIMEOUT, ROLLOUT_AI_TIMEOUT_SECONDS);
            HiddenInfoShuffler.shuffle(simGame, sim.getGameCopier(), player, new Random(seed ^ HIDDEN_INFO_SALT));

            if (!cand.isHold()) {
                if (!sim.applySpellAbility(cand.sa)) {
                    return null;
                }
                playOut(simGame, simAi, playoutPlays);
            }

            final PhaseHandler ph = simGame.getPhaseHandler();
            if (!simGame.isGameOver() && ph.getPhase().isBefore(PhaseType.MAIN2)) {
                // Run the combat of this turn, so that plays before and after it are told apart.
                ph.devAdvanceToPhase(PhaseType.MAIN2,
                        () -> GameSimulator.resolveStack(simGame, simAi.getWeakestOpponent()));
                if (!cand.isHold()) {
                    playOut(simGame, simAi, playoutPlays);
                }
            }

            if (simGame.isGameOver() || !simAi.isInGame()) {
                return simAi.hasWon() ? WIN_VALUE : LOSS_VALUE;
            }
            // At the second main phase the evaluator needs no further game copy for combat.
            final Score score = new GameStateEvaluator().getScoreForGameState(simGame, simAi);
            // Ordinary positions stay strictly between the terminal values, so only a real win equals WIN_VALUE.
            final long value = (long) score.value + holdUpBonus(simAi, cfg) + riskAdjustment(simAi, cfg);
            return Math.max(LOSS_VALUE + 1, Math.min(WIN_VALUE - 1, value));
        } catch (final AITimeoutTracker.Expired e) {
            throw e;
        } catch (final RuntimeException e) {
            reportFailure(e);
            return null;
        } catch (final StackOverflowError | OutOfMemoryError e) {
            // A runaway copy must not kill the real game either (Android has small heaps).
            reportFailure(new RuntimeException(e));
            return null;
        } finally {
            MyRandom.setRandom(savedRandom);
        }
    }

    /** Lets the heuristic AI keep playing in the copied game, like it would for real. */
    private static void playOut(final Game simGame, final Player simAi, final int maxPlays) {
        if (!(simAi.getController() instanceof PlayerControllerAi)) {
            return;
        }
        final PlayerControllerAi pc = (PlayerControllerAi) simAi.getController();
        for (int i = 0; i < maxPlays && !simGame.isGameOver(); i++) {
            AITimeoutTracker.check();
            final List<SpellAbility> next = pc.chooseSpellAbilityToPlay();
            if (next == null || next.isEmpty()) {
                return;
            }
            pc.playChosenSpellAbility(next.get(0));
            GameSimulator.resolveStack(simGame, simAi.getWeakestOpponent());
        }
    }

    /**
     * What the mana and instants still unused at the end of the AI's turn are worth. The evaluator only looks at
     * what is on the table, which would always favour tapping out; this makes holding up a counterspell or a
     * removal spell comparable to the small plays that would use the mana instead. Only the best single card counts
     * (there is only so much mana) and it is capped well below the value of a real creature.
     */
    static int holdUpBonus(final Player simAi, final Config cfg) {
        if (cfg.holdManaWeightPercent <= 0) {
            return 0;
        }
        int mana = ComputerUtilMana.getAvailableManaEstimate(simAi, true);
        if (mana <= 0) {
            return 0;
        }
        final Player opponent = simAi.getWeakestOpponent();
        final int opponentCards = opponent == null ? 1 : opponent.getCardsIn(ZoneType.Hand).size();

        // Worth of each instant-speed card that can be cast, best first...
        final List<int[]> options = new ArrayList<>(); // {value, cmc}
        for (final Card c : simAi.getCardsIn(ZoneType.Hand)) {
            if (!isInstantSpeedSpell(c) || c.getCMC() > mana) {
                continue;
            }
            final boolean counter = isCounterspell(c);
            if (counter && opponentCards == 0) {
                continue; // nothing to counter, and nothing will be cast into it
            }
            final int perCmc = counter ? HOLD_COUNTER_VALUE_PER_CMC : HOLD_VALUE_PER_CMC;
            int value = perCmc * Math.max(1, c.getCMC());
            if (opponentCards == 0) {
                value /= 2; // can still be used on what is already on the table
            }
            options.add(new int[] { value, Math.max(1, c.getCMC()) });
        }
        options.sort((a, b) -> Integer.compare(b[0], a[0]));

        // ...and as many as the open mana pays for, because two counterspells held with four lands are worth more
        // than one. The first card is capped on its own, the extra ones share a second, smaller cap.
        int total = 0;
        boolean first = true;
        for (final int[] option : options) {
            if (option[1] > mana) {
                continue;
            }
            mana -= option[1];
            if (first) {
                total += Math.min(option[0], HOLD_VALUE_CAP);
                first = false;
            } else {
                total += Math.min(option[0], HOLD_VALUE_CAP) / 2;
            }
        }
        total = Math.min(total, HOLD_TOTAL_CAP);
        return (int) Math.round(total * (cfg.holdManaWeightPercent / 100.0) * cfg.controlFactor);
    }

    /**
     * Looks one combat ahead from the position at the end of the AI's turn: ending it open to a crack-back that
     * threatens the AI's life is penalised, leaving the opponent in the same spot is rewarded. This is what stops
     * the rollouts from tapping out for a flashy play and dying to the counter-attack, which the plain board
     * evaluator cannot see.
     */
    public static int riskAdjustment(final Player simAi, final Config cfg) {
        if (cfg.riskWeightPercent <= 0) {
            return 0;
        }
        int adj = 0;
        try {
            if (ComputerUtil.predictNextCombatsRemainingLife(simAi, true, false, 0, null) == Integer.MIN_VALUE) {
                adj -= RISK_SERIOUS_DANGER;
            } else if (ComputerUtil.predictNextCombatsRemainingLife(simAi, false, false, 0, null) == Integer.MIN_VALUE) {
                adj -= RISK_DANGER;
            }
            final Player opp = simAi.getWeakestOpponent();
            if (opp != null && ComputerUtil.predictNextCombatsRemainingLife(opp, false, false, 0, null,
                    java.util.Collections.singletonList(simAi)) == Integer.MIN_VALUE) {
                adj += PRESSURE_BONUS;
            }
        } catch (final RuntimeException e) {
            return 0; // the prediction is a refinement only
        }
        return (int) Math.round(adj * (cfg.riskWeightPercent / 100.0));
    }

    private static void reportFailure(final RuntimeException e) {
        // A few lines are plenty to notice a systematic problem without flooding the log of a long game.
        if (TOTAL_FAILURES.incrementAndGet() <= 5) {
            System.err.println("Flat Monte Carlo rollout failed, using the heuristic choice instead: " + e);
        }
    }
}
