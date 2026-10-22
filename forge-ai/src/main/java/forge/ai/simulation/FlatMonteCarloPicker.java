package forge.ai.simulation;

import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.ai.ComputerUtilCard;
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

    /** Why the last rollout that returned null did so, for the statistics (only read right after such a rollout). */
    private static volatile String lastRolloutProblem = "?";

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
        /** Rounds beyond {@link #rolloutsPerCandidate} that lines still in the race may get (needs {@link #pruning}). */
        public int extraRounds = 2;
        /** Drop lines that are clearly behind after every round, so the budget goes to the lines still in contention. */
        public boolean pruning = true;
        /** Value held-up cards by what they do (counter, removal, trick, flash creature) instead of mana cost alone. */
        public boolean categoryHold = true;
        /** Long hands get longer playouts even when they do not look like a combo hand. */
        public boolean adaptivePlayout = true;
        /** Other targets tried in addition to the one the heuristics chose, for plays with a single target (0 = off). */
        public int targetVariants = 2;

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
            cfg.extraRounds = intProp(ai, AiProps.FLAT_MC_EXTRA_ROUNDS, 0, 10);
            cfg.pruning = ai.getBoolProperty(AiProps.FLAT_MC_PRUNING);
            cfg.categoryHold = ai.getBoolProperty(AiProps.FLAT_MC_CATEGORY_HOLD);
            cfg.adaptivePlayout = ai.getBoolProperty(AiProps.FLAT_MC_ADAPTIVE_PLAYOUT);
            cfg.targetVariants = intProp(ai, AiProps.FLAT_MC_TARGET_VARIANTS, 0, 4);
            cfg.controlFactor = controlFactor(player, cfg.categoryHold);
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
        /** How many lines were dropped from the race early because they were clearly behind. */
        public final int pruned;
        /** When rollouts preferred other targets for {@link #choice} than the heuristics picked: the target to use, else null. */
        public final GameObject target;

        Result(final SpellAbility choice, final boolean hold, final boolean decidedByRollouts,
               final int candidatesEvaluated, final int rollouts, final long elapsedNanos, final String reason) {
            this(choice, hold, decidedByRollouts, candidatesEvaluated, rollouts, elapsedNanos, reason, 0);
        }

        Result(final SpellAbility choice, final boolean hold, final boolean decidedByRollouts,
               final int candidatesEvaluated, final int rollouts, final long elapsedNanos, final String reason,
               final int pruned) {
            this(choice, hold, decidedByRollouts, candidatesEvaluated, rollouts, elapsedNanos, reason, pruned, null);
        }

        Result(final SpellAbility choice, final boolean hold, final boolean decidedByRollouts,
               final int candidatesEvaluated, final int rollouts, final long elapsedNanos, final String reason,
               final int pruned, final GameObject target) {
            this.target = target;
            this.choice = choice;
            this.hold = hold;
            this.decidedByRollouts = decidedByRollouts;
            this.candidatesEvaluated = candidatesEvaluated;
            this.rollouts = rollouts;
            this.elapsedNanos = elapsedNanos;
            this.reason = reason;
            this.pruned = pruned;
        }

        /** A result that simply keeps the heuristic choice without having sampled anything. */
        public static Result keepHeuristic(final SpellAbility heuristicChoice, final String reason) {
            return new Result(heuristicChoice, false, false, 0, 0, 0, reason);
        }

        @Override
        public String toString() {
            return "FlatMC[" + reason + ", " + (hold ? "HOLD" : String.valueOf(choice))
                    + ", candidates=" + candidatesEvaluated + ", rollouts=" + rollouts
                    + (pruned > 0 ? ", pruned=" + pruned : "")
                    + ", ms=" + TimeUnit.NANOSECONDS.toMillis(elapsedNanos) + "]";
        }
    }

    private static final class Candidate {
        /** The play, or null for the hold option. */
        final SpellAbility sa;
        /** For a target variant: the target to use instead of the one the heuristics chose, else null. */
        final GameObject target;

        Candidate(final SpellAbility sa) {
            this(sa, null);
        }

        Candidate(final SpellAbility sa, final GameObject target) {
            this.sa = sa;
            this.target = target;
        }

        boolean isHold() {
            return sa == null;
        }

        /** Name for statistics: the card of the play, or HOLD. */
        String label() {
            if (isHold()) {
                return "HOLD";
            }
            final Card host = sa.getHostCard();
            final String name = host == null ? String.valueOf(sa) : host.getName();
            return target == null ? name : name + " -> " + target;
        }

        @Override
        public String toString() {
            return isHold() ? "HOLD" : target == null ? String.valueOf(sa) : sa + " -> " + target;
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
        final List<Candidate> cands = buildCandidates(player, approved, cfg, revalidate);
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

        final int playoutPlays = playoutLength(player, cfg);
        final int n = cands.size();
        // With pruning, lines that are clearly behind stop costing time, and the lines still in the race may take
        // some extra rounds beyond the base number. Without it this is exactly the original round-robin.
        final int maxRounds = cfg.rolloutsPerCandidate + (cfg.pruning ? Math.max(0, cfg.extraRounds) : 0);
        final long[] sum = new long[n];
        final int[] count = new int[n];
        final boolean[] invalid = new boolean[n];
        final boolean[] pruned = new boolean[n];
        final long[][] vals = new long[n][maxRounds];
        int evaluated = 0;
        int rollouts = 0;
        int prunedCount = 0;
        long maxRolloutNanos = 0;
        boolean tooSlow = false;

        rounds:
        for (int r = 0; r < maxRounds; r++) {
            final long seed = baseSeed + r * SEED_STEP;
            final int active = r == 0 ? n : evaluated;
            int racing = 0;
            for (int c = 0; c < active; c++) {
                if (!invalid[c] && !pruned[c]) {
                    racing++;
                }
            }
            if (r > 0) {
                if (racing < 2) {
                    break; // nothing left to compare
                }
                // Only start another round if all of it fits, so that every candidate gets the same samples.
                if (System.nanoTime() + maxRolloutNanos * racing > (r == 1 ? hardDeadline : softDeadline)) {
                    break;
                }
            }
            for (int c = 0; c < active; c++) {
                if (invalid[c] || pruned[c]) {
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
                final boolean stillWanted = cand.isHold() || revalidate == null || revalidate.test(cand.sa);
                final boolean aimed = stillWanted && (cand.target == null || applyTarget(cand.sa, cand.target));
                final Long value = !aimed ? null : rollout(player, cand, seed, cfg, playoutPlays);
                maxRolloutNanos = Math.max(maxRolloutNanos, System.nanoTime() - t0);
                rollouts++;
                if (value == null) {
                    invalid[c] = true;
                    FlatMonteCarloStats.noteUnsimulatable(cand.label() + (!stillWanted ? " [revalidate]" : !aimed ? " [target]" : " [" + lastRolloutProblem + "]"));
                } else {
                    sum[c] += value;
                    vals[c][count[c]] = value;
                    count[c]++;
                    if (value == WIN_VALUE && !cand.isHold()) {
                        // This line wins the game outright: nothing needs comparing, and waiting only wastes time.
                        return new Result(cand.sa, false, true, Math.max(evaluated, c + 1), rollouts,
                                System.nanoTime() - start, "wins", prunedCount, cand.target);
                    }
                }
                if (r == 0) {
                    evaluated = c + 1;
                }
            }
            if (cfg.pruning && r >= 1) {
                prunedCount += pruneBehind(vals, count, invalid, pruned, evaluated, cfg.heuristicBonus);
            }
        }

        final long elapsed = System.nanoTime() - start;
        if (evaluated < 2) {
            return new Result(heuristicChoice, false, false, evaluated, rollouts, elapsed, tooSlow ? "too-slow" : "no-time", prunedCount);
        }
        if (invalid[0] || count[0] == 0) {
            // We cannot even simulate what the heuristics want to do, so there is nothing to compare against.
            return new Result(heuristicChoice, false, false, evaluated, rollouts, elapsed, "heuristic-unsimulatable", prunedCount);
        }

        // Only lines still in the race can win: a pruned line was behind when it stopped, and its few samples must not
        // be compared with the many samples of the survivors.
        int best = 0;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < evaluated; c++) {
            if (invalid[c] || pruned[c] || count[c] == 0) {
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
            final int pairs = Math.min(count[0], count[best]);
            int wins = 0;
            for (int i = 0; i < pairs; i++) {
                if (vals[best][i] > vals[0][i] + cfg.heuristicBonus / 4) {
                    wins++;
                }
            }
            if (pairs >= 2 && wins * 100 <= pairs * cfg.overrideMajorityPercent) {
                best = 0;
            } else if (pairs < 2 && (double) sum[best] / count[best] < (double) sum[0] / count[0] + 2.0 * cfg.heuristicBonus) {
                // One sample per line is only noise: leave the heuristic choice unless the gap is large.
                best = 0;
            }
        }

        final Candidate chosen = cands.get(best);
        final String reason = chosen.isHold() ? "hold" : best == 0 ? "kept-heuristic"
                : chosen.target != null ? "target-override" : "override";
        return new Result(chosen.sa, chosen.isHold(), true, evaluated, rollouts, elapsed, reason, prunedCount, chosen.target);
    }

    /** Fewest paired samples before a line may be dropped; one sample has no spread to judge it by. */
    private static final int PRUNE_MIN_SAMPLES = 2;
    /** How many standard errors a line must be behind the leader (on paired samples) to be dropped for good. */
    private static final double PRUNE_Z = 1.5;
    /** Smallest lead, in evaluator points, that is worth dropping a line for. */
    private static final double PRUNE_MIN_GAP = 25;

    /**
     * Successive elimination: marks lines that are clearly behind the current leader as pruned, so the remaining
     * time goes to the lines that are still close. "Clearly behind" means the paired difference to the leader
     * (same seed, so the same hidden cards) is above {@link #PRUNE_MIN_GAP} even after subtracting
     * {@link #PRUNE_Z} standard errors, which keeps the chance of dropping the truly best line small. The leader
     * itself is never pruned, and the heuristic line (index 0) gets its bonus like it does in the final decision.
     *
     * @param vals  the sampled values, {@code vals[candidate][round]}, valid up to {@code count[candidate]}
     * @return how many lines were pruned by this call
     */
    static int pruneBehind(final long[][] vals, final int[] count, final boolean[] invalid, final boolean[] pruned,
                           final int candidates, final int heuristicBonus) {
        int leader = -1;
        double leaderMean = Double.NEGATIVE_INFINITY;
        int live = 0;
        for (int c = 0; c < candidates; c++) {
            if (invalid[c] || pruned[c] || count[c] < PRUNE_MIN_SAMPLES) {
                continue;
            }
            live++;
            long total = 0;
            for (int i = 0; i < count[c]; i++) {
                total += vals[c][i];
            }
            final double mean = (double) total / count[c] + (c == 0 ? heuristicBonus : 0);
            if (mean > leaderMean) {
                leaderMean = mean;
                leader = c;
            }
        }
        if (live < 2) {
            return 0;
        }
        int dropped = 0;
        for (int c = 0; c < candidates; c++) {
            if (c == leader || invalid[c] || pruned[c] || count[c] < PRUNE_MIN_SAMPLES) {
                continue;
            }
            final int pairs = Math.min(count[c], count[leader]);
            double diffSum = 0;
            double diffSquares = 0;
            for (int i = 0; i < pairs; i++) {
                final double d = (vals[leader][i] + (leader == 0 ? heuristicBonus : 0))
                        - (vals[c][i] + (c == 0 ? heuristicBonus : 0));
                diffSum += d;
                diffSquares += d * d;
            }
            final double mean = diffSum / pairs;
            final double variance = Math.max(0.0, (diffSquares - pairs * mean * mean) / (pairs - 1));
            final double stdErr = Math.sqrt(variance / pairs);
            if (mean - PRUNE_Z * stdErr > PRUNE_MIN_GAP) {
                pruned[c] = true;
                dropped++;
            }
        }
        return dropped;
    }

    /**
     * How many plays the heuristic AI may make while a rollout continues the turn. Hands that look like combo hands
     * get the long limit; with {@link Config#adaptivePlayout} a hand with many spells in it gets a limit that fits its
     * size too, so that a full grip is not cut short just because it does not look like a combo.
     */
    static int playoutLength(final Player player, final Config cfg) {
        if (isComboLike(player)) {
            return Math.max(cfg.maxPlayoutPlays, cfg.comboPlayoutPlays);
        }
        if (!cfg.adaptivePlayout) {
            return cfg.maxPlayoutPlays;
        }
        int spells = 0;
        for (final Card c : player.getCardsIn(ZoneType.Hand)) {
            if (!c.isLand()) {
                spells++;
            }
        }
        return Math.max(cfg.maxPlayoutPlays, Math.min(cfg.comboPlayoutPlays, spells + 2));
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
                                                   final Config cfg, final Predicate<SpellAbility> revalidate) {
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
        // Other targets for the plays that have one: the heuristics pick a target by rules of thumb, rollouts can tell
        // whether another one (the face instead of a creature, the second best creature) is better in this position.
        // They come last so that the indexes of the plays do not move, and their number is capped for the decision.
        int variantsLeft = cfg.targetVariants;
        for (int i = 0; i < plays && variantsLeft > 0; i++) {
            final SpellAbility sa = approved.get(i);
            if (revalidate != null && !revalidate.test(sa)) {
                continue; // the heuristics have set the target up again, or no longer want the play
            }
            for (final GameObject target : alternativeTargets(player, sa, variantsLeft)) {
                cands.add(new Candidate(sa, target));
                variantsLeft--;
            }
        }
        FlatMonteCarloStats.noteTargetVariants(cfg.targetVariants - variantsLeft);
        return cands;
    }

    /** The only ability of the play (itself or a sub ability) that has targets, or null if there is none or several. */
    static SpellAbility singleTargetingAbility(final SpellAbility sa) {
        SpellAbility found = null;
        for (SpellAbility ability = sa; ability != null; ability = ability.getSubAbility()) {
            if (ability.usesTargeting()) {
                if (found != null) {
                    return null;
                }
                found = ability;
            }
        }
        return found;
    }

    /**
     * Other legal targets for a play the heuristics have already set up with exactly one target, of the same kind:
     * if the heuristics aim at an opponent's permanent or at an opponent, only opposing targets are offered, and if they
     * aim at the AI's own, only its own (so a burn spell is never tried on the AI's own creature). Opposing players
     * come first, then permanents by their value, best first.
     */
    public static List<GameObject> alternativeTargets(final Player player, final SpellAbility sa, final int max) {
        final List<GameObject> result = new ArrayList<>();
        final SpellAbility ts = singleTargetingAbility(sa);
        if (ts == null || max <= 0 || ts.isDividedAsYouChoose() || ts.getTargets().size() != 1) {
            return result;
        }
        final TargetRestrictions tr = ts.getTargetRestrictions();
        if (tr == null || tr.getMaxTargets(sa.getHostCard(), ts) != 1) {
            return result;
        }
        final GameObject current = ts.getTargets().iterator().next();
        final Boolean wantsOwn = isOwnSide(player, current);
        if (wantsOwn == null) {
            return result;
        }
        sa.setActivatingPlayer(player);
        final List<GameEntity> candidates = new ArrayList<>(tr.getAllCandidates(ts));
        final List<GameEntity> usable = new ArrayList<>();
        for (final GameEntity e : candidates) {
            if (e != current && wantsOwn == isOwnSide(player, e)) {
                usable.add(e);
            }
        }
        usable.sort((a, b) -> Integer.compare(targetRank(player, b), targetRank(player, a)));
        for (int i = 0; i < usable.size() && result.size() < max; i++) {
            result.add(usable.get(i));
        }
        return result;
    }

    /** True for the AI itself and its permanents, false for opponents and theirs, null for anything else. */
    private static Boolean isOwnSide(final Player player, final GameObject o) {
        if (o instanceof Player) {
            return o == player || !((Player) o).isOpponentOf(player);
        }
        if (o instanceof Card) {
            final Player controller = ((Card) o).getController();
            return controller == player || controller != null && !controller.isOpponentOf(player);
        }
        return null;
    }

    private static int targetRank(final Player player, final GameEntity e) {
        if (e instanceof Player) {
            return 100000; // the face first: damage and drain are often best aimed there
        }
        final Card c = (Card) e;
        if (c.isCreature()) {
            return ComputerUtilCard.evaluateCreature(c);
        }
        return 150 + c.getCMC() * 10 + (c.isPlaneswalker() ? 100 : 0);
    }

    /**
     * Aims the play at the given target in place of what the heuristics chose.
     *
     * @return false (leaving the targets alone) if the play has no single target or this one is not legal for it
     */
    public static boolean applyTarget(final SpellAbility sa, final GameObject target) {
        final SpellAbility ts = singleTargetingAbility(sa);
        if (ts == null || !(target instanceof GameEntity) || !ts.canTarget((GameEntity) target)) {
            return false;
        }
        ts.resetTargets();
        return ts.getTargets().add(target);
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
        return controlFactor(player, true);
    }

    /**
     * @param refined false for the original rule, which takes every deck with 30% instants for a control deck. That
     *                is wrong for burn: Lightning Bolt and Shock are instants too, and a burn deck wants to spend them,
     *                not to sit on them. The refined rule needs the deck to be light on creatures as well.
     */
    static double controlFactor(final Player player, final boolean refined) {
        int spells = 0;
        int reactive = 0;
        int counters = 0;
        int creatures = 0;
        for (final Card c : player.getAllCards()) {
            if (c.isLand() || c.isToken() || c.isImmutable()) {
                continue;
            }
            spells++;
            if (c.isCreature()) {
                creatures++;
            }
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
        final int reactivePercent = reactive * 100 / spells;
        final boolean creatureLight = creatures * 100 / spells <= 40;
        if (counters >= 3 || reactivePercent >= 30 && (creatureLight || !refined)) {
            return CONTROL_HOLD_FACTOR;
        }
        return reactivePercent <= 5 ? AGGRO_HOLD_FACTOR : 1.0;
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
                    lastRolloutProblem = "apply";
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
            lastRolloutProblem = "error";
            return null;
        } catch (final StackOverflowError | OutOfMemoryError e) {
            // A runaway copy must not kill the real game either (Android has small heaps).
            reportFailure(new RuntimeException(e));
            lastRolloutProblem = "error";
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

    /** What a held-up instant-speed card is for. Decides how much it is worth to keep the mana open for it. */
    enum HoldKind { COUNTER, REMOVAL, TRICK, FLASH_CREATURE, OTHER }

    /** Per mana value, in evaluator points. Removal and tricks only matter when there is something to use them on. */
    private static final int HOLD_REMOVAL_PER_CMC = 16;
    private static final int HOLD_TRICK_PER_CMC = 10;
    private static final int HOLD_FLASH_CREATURE_PER_CMC = 12;
    private static final int HOLD_OTHER_PER_CMC = 7;

    /**
     * Classifies an instant-speed card by looking at its abilities, not its name, so that it works for any card of any
     * deck: counterspells, removal (destroy, damage, fight, edict, bounce or exile from the battlefield, shrink),
     * tricks (pump, protection), flash creatures, and everything else (draw spells, utility).
     */
    static HoldKind holdKind(final Card c) {
        boolean removal = false;
        boolean trick = false;
        for (final SpellAbility sa : c.getSpellAbilities()) {
            final ApiType api = sa.getApi();
            if (api == null) {
                continue;
            }
            switch (api) {
                case Counter:
                    return HoldKind.COUNTER;
                case Destroy:
                case DestroyAll:
                case DealDamage:
                case DamageAll:
                case Fight:
                case Sacrifice:
                case SacrificeAll:
                    removal = true;
                    break;
                case ChangeZone:
                case ChangeZoneAll:
                    if (sa.getParamOrDefault("Origin", "").contains("Battlefield")) {
                        removal = true; // bounce or exile
                    }
                    break;
                case Pump:
                case PumpAll:
                    if (sa.getParamOrDefault("NumAtt", "").startsWith("-")
                            || sa.getParamOrDefault("NumDef", "").startsWith("-")) {
                        removal = true; // a shrink effect kills like removal does
                    } else {
                        trick = true;
                    }
                    break;
                case Protection:
                case ProtectionAll:
                    trick = true;
                    break;
                default:
                    break;
            }
        }
        if (removal) {
            return HoldKind.REMOVAL;
        }
        if (trick) {
            return HoldKind.TRICK;
        }
        return c.isCreature() ? HoldKind.FLASH_CREATURE : HoldKind.OTHER;
    }

    /**
     * Worth of keeping mana open for one card in hand, 0 if it is of no use right now. With {@code categories} off this
     * is the original valuation (mana value only, counterspells a little higher).
     */
    private static int heldCardValue(final Card c, final Player simAi, final Player opponent, final int opponentCards,
                                     final boolean categories) {
        final int cmc = Math.max(1, c.getCMC());
        if (!categories) {
            final boolean counter = isCounterspell(c);
            if (counter && opponentCards == 0) {
                return 0; // nothing to counter, and nothing will be cast into it
            }
            final int value = (counter ? HOLD_COUNTER_VALUE_PER_CMC : HOLD_VALUE_PER_CMC) * cmc;
            return opponentCards == 0 ? value / 2 : value; // can still be used on what is already on the table
        }
        final boolean opponentHasCreatures = opponent != null && !opponent.getCreaturesInPlay().isEmpty();
        final boolean aiHasCreatures = !simAi.getCreaturesInPlay().isEmpty();
        switch (holdKind(c)) {
            case COUNTER:
                // nothing to counter when the opponent has no cards, and nothing will be cast into the open mana
                return opponentCards == 0 ? 0 : HOLD_COUNTER_VALUE_PER_CMC * cmc;
            case REMOVAL:
                // damage and exile also work on the face or the table later, so it is never worth nothing
                return HOLD_REMOVAL_PER_CMC * cmc / (opponentHasCreatures ? 1 : 2);
            case TRICK:
                return aiHasCreatures ? HOLD_TRICK_PER_CMC * cmc : HOLD_TRICK_PER_CMC * cmc / 3;
            case FLASH_CREATURE:
                return HOLD_FLASH_CREATURE_PER_CMC * cmc;
            default:
                return HOLD_OTHER_PER_CMC * cmc;
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
            final int value = heldCardValue(c, simAi, opponent, opponentCards, cfg.categoryHold);
            if (value <= 0) {
                continue;
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
        final StackTraceElement[] trace = e.getStackTrace();
        FlatMonteCarloStats.noteFailure(String.valueOf(e) + (trace.length > 0 ? " @ " + trace[0] : ""));
        if (TOTAL_FAILURES.incrementAndGet() <= 5) {
            System.err.println("Flat Monte Carlo rollout failed, using the heuristic choice instead: " + e);
            for (int i = 0; i < Math.min(8, trace.length); i++) {
                System.err.println("\tat " + trace[i]);
            }
        }
    }
}
