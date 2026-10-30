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

import java.util.*;
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
    /** -Dflatmc.trace=true prints the rollout values of every decision to stderr, for diagnosing the AI. */
    private static final boolean TRACE = Boolean.getBoolean("flatmc.trace");
    // Maximum strength (desktop) settings, see Config.maximized().
    static final int MAX_CANDIDATES_DESKTOP = 8;
    static final int MAX_ROLLOUTS_DESKTOP = 8;
    static final int MAX_EXTRA_ROUNDS_DESKTOP = 6;
    static final int MAX_PLAYOUT_DESKTOP = 10;
    static final int MAX_COMBO_PLAYOUT_DESKTOP = 20;
    static final int MAX_TARGET_VARIANTS_DESKTOP = 3;
    static final int MAX_DECISION_MS_DESKTOP = 1500;
    static final int MAX_TURN_MS_DESKTOP = 6000;
    static final int MAX_EXTRA_CANDIDATES_DESKTOP = 5;
    /** Share of a rollout's value that comes from the position after the opponent's next turn (desktop). */
    static final int LOOKAHEAD_PERCENT_DESKTOP = 50;
    // Phone and tablet settings, see Config.mobileLite().
    static final int MAX_CANDIDATES_MOBILE = 5;
    static final int MIN_ROLLOUTS_MOBILE = 3;
    static final int MOBILE_DECISION_MS = 600;
    static final int MOBILE_TURN_MS = 2500;
    static final int LOOKAHEAD_PERCENT_MOBILE = 35;
    /** On a phone the (expensive) look at the opponent's turn is only done for decisions with at most this many lines. */
    static final int MOBILE_LOOKAHEAD_MAX_CANDIDATES = 3;

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
    /**
     * When a playout must give up: twice the decision's budget, the same as the hard deadline. The soft and hard deadlines are only checked
     * between rollouts, and a rollout is a series of heuristic decisions of its own, so one slow position could
     * otherwise take several times the budget (5 s were seen with the 2.5 s desktop budget).
     */
    private static volatile long rolloutDeadlineNanos = Long.MAX_VALUE;
    private static final int ROLLOUT_DEADLINE_FACTOR = 2;
    /** Decisions slower than this are written down in the statistics, with where the time went. */
    private static final long SLOW_DECISION_NANOS = TimeUnit.MILLISECONDS.toNanos(2500);

    /** A playout ran past its decision's limit; the candidate is left out of the comparison. */
    private static final class RolloutTooSlow extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RolloutTooSlow() {
            super("rollout too slow", null, false, false);
        }
    }

    private static void checkRolloutDeadline() {
        if (System.nanoTime() > rolloutDeadlineNanos) {
            throw new RolloutTooSlow();
        }
    }

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
    public static final class Config implements Cloneable {
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
        /**
         * Time the first rollout and cut the candidate list to what the budget can pay for (at least two samples each),
         * so a slow device keeps a real, if smaller, comparison instead of one sample per line and no pairs.
         */
        public boolean scaleToDevice = true;
        /** Drop lines that are clearly behind after every round, so the budget goes to the lines still in contention. */
        public boolean pruning = true;
        /** Value held-up cards by what they do (counter, removal, trick, flash creature) instead of mana cost alone. */
        public boolean categoryHold = true;
        /** Long hands get longer playouts even when they do not look like a combo hand. */
        public boolean adaptivePlayout = true;
        /** Other targets tried in addition to the one the heuristics chose, for plays with a single target (0 = off). */
        public int targetVariants = 2;
        /** Scale (percent) of the strategic evaluation terms added to the position score, 0 = off. */
        public int strategicWeightPercent = 100;
        /** Add legal card draw / selection plays the heuristics did not approve, and keep the candidates varied. */
        public boolean discovery = true;
        /** How many such plays may be added to the comparison. */
        public int extraCandidates = 3;
        /** Discovery also adds creatures, permanents, token makers, counters and ramp the heuristics turned down. */
        public boolean broadDiscovery = false;
        /**
         * Share (percent, 0 = off) of a main phase rollout's value that comes from the position after the
         * opponent's next turn, which the opponent's heuristic AI plays out in the copy (its plays, its attack
         * against the AI's blockers). Catches crack-backs and removal that the end-of-turn estimate only guesses.
         */
        public int opponentLookaheadPercent = 0;
        /**
         * When above 0, the opponent's turn is only played out for decisions with at most this many candidates
         * (every line of such a decision gets it, so the lines stay comparable); bigger decisions are scored at the end
         * of the AI's own turn. 0 = always. This is what keeps the extra turn affordable on a phone.
         */
        public int lookaheadMaxCandidates = 0;

        /** This configuration for a decision with this many candidates: the lookahead is dropped when it is too many. */
        Config forDecision(final int candidates) {
            if (opponentLookaheadPercent <= 0 || lookaheadMaxCandidates <= 0 || candidates <= lookaheadMaxCandidates) {
                return this;
            }
            try {
                final Config copy = (Config) clone();
                copy.opponentLookaheadPercent = 0;
                return copy;
            } catch (final CloneNotSupportedException e) {
                return this;
            }
        }
        /** What the deck is for; tilts the valuation a little (see {@link DeckPlan}). */
        public DeckPlan plan = DeckPlan.NONE;
        /** Compare the lands that can be played instead of taking the heuristic's land. */
        public boolean landChoice = true;
        /** Also decide on the opponent's turn: responses on the stack and instant-speed plays at their end step. */
        public boolean reactive = true;

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
            cfg.strategicWeightPercent = intProp(ai, AiProps.FLAT_MC_STRATEGIC_WEIGHT, 0, 300);
            cfg.discovery = ai.getBoolProperty(AiProps.FLAT_MC_DISCOVERY);
            cfg.extraCandidates = intProp(ai, AiProps.FLAT_MC_EXTRA_CANDIDATES, 0, 8);
            cfg.broadDiscovery = ai.getBoolProperty(AiProps.FLAT_MC_BROAD_DISCOVERY);
            cfg.opponentLookaheadPercent = intProp(ai, AiProps.FLAT_MC_OPPONENT_LOOKAHEAD, 0, 100);
            cfg.plan = ai.deckPlan(player);
            cfg.riskWeightPercent = cfg.riskWeightPercent * cfg.plan.riskPercent / 100;
            cfg.landChoice = ai.getBoolProperty(AiProps.FLAT_MC_LAND_CHOICE);
            cfg.reactive = ai.getBoolProperty(AiProps.FLAT_MC_REACTIVE);
            cfg.controlFactor = controlFactor(player, cfg.categoryHold);
            return cfg;
        }

        /**
         * Raises this configuration to the maximum strength used on desktops: more lines, more samples per line,
         * longer playouts and several times the time budget. Values already above the maximum (a profile that asks for
         * more) are kept, and the profile's judgement settings (bonus, weights, majority) are left alone, so only the
         * amount of search changes. The hard limits stay in force: the AI timeout still caps every decision and the
         * circuit breaker still switches Monte Carlo off if it errors or cannot keep up.
         */
        public Config maximized() {
            maxCandidates = Math.max(maxCandidates, MAX_CANDIDATES_DESKTOP);
            rolloutsPerCandidate = Math.max(rolloutsPerCandidate, MAX_ROLLOUTS_DESKTOP);
            extraRounds = Math.max(extraRounds, MAX_EXTRA_ROUNDS_DESKTOP);
            maxPlayoutPlays = Math.max(maxPlayoutPlays, MAX_PLAYOUT_DESKTOP);
            comboPlayoutPlays = Math.max(comboPlayoutPlays, MAX_COMBO_PLAYOUT_DESKTOP);
            targetVariants = Math.max(targetVariants, MAX_TARGET_VARIANTS_DESKTOP);
            budgetNanos = Math.max(budgetNanos, TimeUnit.MILLISECONDS.toNanos(MAX_DECISION_MS_DESKTOP));
            turnBudgetNanos = Math.max(turnBudgetNanos, TimeUnit.MILLISECONDS.toNanos(MAX_TURN_MS_DESKTOP));
            pruning = true;
            categoryHold = true;
            adaptivePlayout = true;
            considerHolding = true;
            extraCandidates = Math.max(extraCandidates, MAX_EXTRA_CANDIDATES_DESKTOP);
            broadDiscovery = true;
            // Look at the opponent's next turn too (their plays and their attack into the AI's blockers): this is what
            // turns the end-of-turn guess about crack-backs and removal into something the rollouts actually saw.
            opponentLookaheadPercent = Math.max(opponentLookaheadPercent, LOOKAHEAD_PERCENT_DESKTOP);
            return this;
        }

        /**
         * The strength used on phones and tablets: the profile's search size is kept (the number of samples already
         * shrinks with the speed of the device), but the two things that matter most against a player who is not a
         * beginner are added at a price a phone can pay: attack sets are compared (at most
         * {@link FlatMonteCarloPicker#MAX_ATTACK_CANDIDATES}, once per turn), and the opponent's next turn is
         * played out for the best few lines only. The time limits are raised a little, never above what a phone can
         * spend without the game feeling stuck.
         */
        public Config mobileLite() {
            maxCandidates = Math.max(maxCandidates, MAX_CANDIDATES_MOBILE);
            rolloutsPerCandidate = Math.max(rolloutsPerCandidate, MIN_ROLLOUTS_MOBILE);
            budgetNanos = Math.max(budgetNanos, TimeUnit.MILLISECONDS.toNanos(MOBILE_DECISION_MS));
            turnBudgetNanos = Math.max(turnBudgetNanos, TimeUnit.MILLISECONDS.toNanos(MOBILE_TURN_MS));
            pruning = true;
            categoryHold = true;
            adaptivePlayout = true;
            opponentLookaheadPercent = Math.max(opponentLookaheadPercent, LOOKAHEAD_PERCENT_MOBILE);
            lookaheadMaxCandidates = MOBILE_LOOKAHEAD_MAX_CANDIDATES;
            return this;
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
        /** For {@link Mode#ATTACK}: the attack set rollouts chose (empty = no attack), else null. */
        public Map<Card, GameEntity> attackers;

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
            return new Result(heuristicChoice, heuristicChoice == null, false, 0, 0, 0, reason);
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
        /** A play the heuristics did not approve, added by discovery: it cannot be revalidated by them. */
        final boolean extra;
        /** For an attack decision: the attackers and what each attacks; empty means no attack. Null for plays. */
        final Map<Card, GameEntity> attackers;

        Candidate(final Map<Card, GameEntity> attackers) {
            this.sa = null;
            this.target = null;
            this.extra = false;
            this.attackers = attackers;
        }

        Candidate(final SpellAbility sa) {
            this(sa, null, false);
        }

        Candidate(final SpellAbility sa, final GameObject target) {
            this(sa, target, false);
        }

        Candidate(final SpellAbility sa, final GameObject target, final boolean extra) {
            this.sa = sa;
            this.target = target;
            this.extra = extra;
            this.attackers = null;
        }

        boolean isHold() {
            return sa == null && attackers == null;
        }

        /** Name for statistics: the card of the play, or HOLD. */
        String label() {
            if (attackers != null) {
                return "ATTACK" + attackers.keySet();
            }
            if (isHold()) {
                return "HOLD";
            }
            final Card host = sa.getHostCard();
            final String name = host == null ? String.valueOf(sa) : host.getName();
            return target == null ? name : name + " -> " + target;
        }

        @Override
        public String toString() {
            if (attackers != null) {
                return "ATTACK" + attackers.keySet();
            }
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
        return choose(player, approved, Collections.emptyList(), cfg, revalidate);
    }

    /**
     * As {@link #choose(Player, List, Config, Predicate)}, with plays the heuristics did <em>not</em> approve but that
     * are legal and strategically relevant (card draw, card selection, ...). They are compared like the others, but
     * need no heuristic approval to be chosen. {@code approved} may be empty when there are extras.
     */
    public static Result choose(final Player player, final List<SpellAbility> approved, final List<SpellAbility> extras,
                                final Config cfg, final Predicate<SpellAbility> revalidate) {
        return choose(player, approved, extras, cfg, revalidate, Mode.MAIN);
    }

    /** Which kind of decision is being made, which decides how far a rollout plays on from the candidate. */
    public enum Mode {
        /** The AI's own main phase: play on, fight the combat, score at the second main phase. */
        MAIN,
        /** Priority with something on the stack or in combat on the opponent's turn: respond, let it resolve, score at their end of turn. */
        RESPOND,
        /** The opponent's end step, empty stack: act now or pass, then play the AI's whole next turn and score at its second main phase. */
        END_STEP,
        /** Declaring attackers: compare attack sets, play the combat out (the opponent blocks) and score at the second main phase. */
        ATTACK
    }

    public static Result choose(final Player player, final List<SpellAbility> approved, final List<SpellAbility> extras,
                                final Config cfg, final Predicate<SpellAbility> revalidate, final Mode mode) {
        final long start = System.nanoTime();
        final SpellAbility heuristicChoice = approved.isEmpty() ? null : approved.get(0);
        try {
            final Result result = chooseImpl(player, approved, extras, cfg, revalidate, start, mode);
            if (DEBUG) {
                System.err.println(player + " " + result);
            }
            return result;
        } catch (final AITimeoutTracker.Expired e) {
            return new Result(heuristicChoice, heuristicChoice == null, false, 0, 0, System.nanoTime() - start, "deadline");
        } catch (final RuntimeException e) {
            // Rollouts only ever run on copies of the game, so whatever went wrong in there must not take the real
            // game down: play what the heuristics wanted to play anyway.
            reportFailure(e);
            return new Result(heuristicChoice, heuristicChoice == null, false, 0, 0, System.nanoTime() - start, "error");
        } finally {
            rolloutDeadlineNanos = Long.MAX_VALUE;
        }
    }

    private static Result chooseImpl(final Player player, final List<SpellAbility> approved, final List<SpellAbility> extras,
                                     final Config cfg, final Predicate<SpellAbility> revalidate, final long start,
                                     final Mode mode) {
        return chooseImpl(player, approved, extras, cfg, revalidate, start, mode, null);
    }

    private static Result chooseImpl(final Player player, final List<SpellAbility> approved, final List<SpellAbility> extras,
                                     final Config cfg, final Predicate<SpellAbility> revalidate, final long start,
                                     final Mode mode, final List<Candidate> prebuilt) {
        final SpellAbility heuristicChoice = approved.isEmpty() ? null : approved.get(0);
        // When the heuristics approved nothing, "what they would do" is just passing: a discovered play then needs a
        // smaller margin than a play that has to beat the heuristics' own favourite.
        final int bonus0 = approved.isEmpty() && prebuilt == null ? cfg.heuristicBonus / 4 : cfg.heuristicBonus;
        final List<Candidate> cands = prebuilt != null ? prebuilt : buildCandidates(player, approved, extras, cfg, revalidate, mode);
        final long buildNanos = System.nanoTime() - start;
        if (cands.size() < 2) {
            return Result.keepHeuristic(heuristicChoice, "single-candidate");
        }

        final long budget = Math.min(cfg.budgetNanos, AITimeoutTracker.remainingNanos() / 2);
        if (budget <= 0) {
            return Result.keepHeuristic(heuristicChoice, "no-time");
        }
        final long softDeadline = start + budget;
        final long hardDeadline = start + HARD_DEADLINE_FACTOR * budget;
        rolloutDeadlineNanos = start + ROLLOUT_DEADLINE_FACTOR * budget;
        final long baseSeed = MyRandom.getRandom().nextLong();

        final int playoutPlays = playoutLength(player, cfg);
        final int n = cands.size();
        // One setting for every line of this decision, so that the lines are compared like with like.
        final Config rcfg = cfg.forDecision(n);
        // With pruning, lines that are clearly behind stop costing time, and the lines still in the race may take
        // some extra rounds beyond the base number. Without it this is exactly the original round-robin.
        final int maxRounds = cfg.rolloutsPerCandidate + (cfg.pruning ? Math.max(0, cfg.extraRounds) : 0);
        final long[] sum = new long[n];
        final int[] count = new int[n];
        final boolean[] invalid = new boolean[n];
        final boolean[] pruned = new boolean[n];
        final long[][] vals = new long[n][maxRounds];
        int limitN = n; // how many lines this device can afford to compare, found out by timing the first rollout
        int evaluated = 0;
        int rollouts = 0;
        int prunedCount = 0;
        long maxRolloutNanos = 0;
        boolean tooSlow = false;

        rounds:
        for (int r = 0; r < maxRounds; r++) {
            final long seed = baseSeed + r * SEED_STEP;
            final int active = r == 0 ? limitN : evaluated;
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
                final boolean stillWanted = cand.isHold() || cand.extra || revalidate == null || revalidate.test(cand.sa);
                final boolean aimed = stillWanted && (cand.target == null || applyTarget(cand.sa, cand.target));
                final Long value = !aimed ? null : rollout(player, cand, seed, rcfg, playoutPlays, mode);
                maxRolloutNanos = Math.max(maxRolloutNanos, System.nanoTime() - t0);
                rollouts++;
                if (rollouts == 1 && value != null && cfg.scaleToDevice) {
                    limitN = Math.min(limitN, affordableLines(softDeadline - System.nanoTime(), maxRolloutNanos,
                            cfg.rolloutsPerCandidate, n));
                }
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
                prunedCount += pruneBehind(vals, count, invalid, pruned, evaluated, bonus0);
            }
        }

        final long elapsed = System.nanoTime() - start;
        if (elapsed > SLOW_DECISION_NANOS) {
            FlatMonteCarloStats.noteSlow(mode + " " + (elapsed / 1_000_000) + "ms: candidates=" + n + " evaluated=" + evaluated
                    + " rollouts=" + rollouts + " build=" + (buildNanos / 1_000_000) + "ms longestRollout="
                    + (maxRolloutNanos / 1_000_000) + "ms budget=" + (budget / 1_000_000) + "ms");
        }
        if (evaluated < 2) {
            return new Result(heuristicChoice, heuristicChoice == null, false, evaluated, rollouts, elapsed, tooSlow ? "too-slow" : "no-time", prunedCount);
        }
        if (invalid[0] || count[0] == 0) {
            // We cannot even simulate what the heuristics want to do, so there is nothing to compare against.
            return new Result(heuristicChoice, heuristicChoice == null, false, evaluated, rollouts, elapsed, "heuristic-unsimulatable", prunedCount);
        }

        if (TRACE) {
            for (int c = 0; c < evaluated; c++) {
                System.err.println("\t[flatmc " + mode + "] " + cands.get(c) + ": n=" + count[c] + " mean="
                        + (count[c] == 0 ? "-" : String.valueOf(sum[c] / count[c])) + (invalid[c] ? " INVALID" : "")
                        + (pruned[c] ? " pruned" : "") + " values=" + Arrays.toString(Arrays.copyOf(vals[c], count[c])));
            }
        }
        // Only lines still in the race can win: a pruned line was behind when it stopped, and its few samples must not
        // be compared with the many samples of the survivors.
        int best = 0;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < evaluated; c++) {
            if (invalid[c] || pruned[c] || count[c] == 0) {
                continue;
            }
            final double value = (double) sum[c] / count[c] + (c == 0 ? bonus0 : 0);
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
            int losses = 0;
            for (int i = 0; i < pairs; i++) {
                if (vals[best][i] > vals[0][i] + bonus0 / 4) {
                    wins++;
                } else if (vals[best][i] + bonus0 / 4 < vals[0][i]) {
                    losses++;
                }
            }
            // A line that is never worse in any paired round and clearly better on average (a cantrip: often only a
            // small gain, when it draws a land, but it costs nothing) does not have to win a majority outright.
            final boolean neverWorse = pairs >= 3 && losses == 0 && wins >= 1
                    && (double) sum[best] / count[best] > (double) sum[0] / count[0] + bonus0;
            if (pairs >= 2 && !neverWorse && wins * 100 <= pairs * cfg.overrideMajorityPercent) {
                best = 0;
            } else if (pairs < 2 && (double) sum[best] / count[best] < (double) sum[0] / count[0] + 2.0 * bonus0) {
                // One sample per line is only noise: leave the heuristic choice unless the gap is large.
                best = 0;
            }
        }

        final Candidate chosen = cands.get(best);
        final String reason = chosen.isHold() ? "hold" : best == 0 ? "kept-heuristic"
                : chosen.target != null ? "target-override" : "override";
        if (TRACE) {
            System.err.println("\t[flatmc " + mode + "] => " + reason + " " + chosen + " (heuristic: " + cands.get(0) + ", turn "
                    + player.getGame().getPhaseHandler().getTurn() + " " + player.getGame().getPhaseHandler().getPhase() + ")");
        }
        final Result decided = new Result(chosen.sa, chosen.isHold(), true, evaluated, rollouts, elapsed, reason, prunedCount, chosen.target);
        decided.attackers = chosen.attackers;
        return decided;
    }

    /**
     * How many lines fit in the time that is left when one rollout took {@code rolloutNanos}: every line should get
     * at least {@code rounds} samples (two at the least, or nothing can be compared), and the first line has had its
     * first already. Never fewer than two lines, never more than {@code n}.
     */
    static int affordableLines(final long leftNanos, final long rolloutNanos, final int rounds, final int n) {
        final long perRollout = Math.max(1, rolloutNanos);
        final long fit = Math.max(0, leftNanos) / perRollout + 1;
        final long lines = fit / Math.max(2, rounds);
        return (int) Math.max(2, Math.min(n, lines));
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
        if (isComboLike(player) || cfg.plan.longPlayouts) {
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
                                                   final List<SpellAbility> extras, final Config cfg,
                                                   final Predicate<SpellAbility> revalidate, final Mode mode) {
        final List<SpellAbility> approved = approvedPlays.isEmpty() ? approvedPlays : dedupePlays(approvedPlays);
        final List<Candidate> cands = new ArrayList<>();
        // Index 0 is always what the heuristics would do: their favourite play, or passing when they approved nothing.
        cands.add(approved.isEmpty() ? new Candidate((SpellAbility) null) : new Candidate(approved.get(0)));
        // On the opponent's turn passing is always an option, and the plays are compared against it; on the AI's own
        // turn holding is only worth comparing when there is an instant to keep the mana up for.
        final boolean passOption = mode != Mode.MAIN || cfg.considerHolding && !cfg.holdForbidden && isHoldPossible(player);
        if (!approved.isEmpty() && passOption) {
            cands.add(new Candidate((SpellAbility) null));
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
        // Plays the heuristics did not approve, found by discovery: they come last and are limited in number.
        int extrasLeft = cfg.extraCandidates;
        final Set<String> seen = new HashSet<>();
        for (final Candidate c : cands) {
            if (!c.isHold()) {
                seen.add(playKey(c.sa));
            }
        }
        for (final SpellAbility sa : extras) {
            if (extrasLeft <= 0) {
                break;
            }
            if (!seen.add(playKey(sa))) {
                continue;
            }
            GameObject target = null;
            final SpellAbility targeting = singleTargetingAbility(sa);
            if (targeting != null && targeting.getApi() == ApiType.Counter) {
                // aimed at the spell on the stack by the caller, which no GameEntity target can express
                if (targeting.getTargets().isEmpty()) {
                    continue;
                }
            } else if (targeting != null) {
                target = hostileTarget(player, sa);
                if (target == null) {
                    continue; // a targeted play with nothing sensible to aim at is not worth a rollout
                }
            }
            cands.add(new Candidate(sa, target, true));
            extrasLeft--;
        }
        FlatMonteCarloStats.noteExtraCandidates(cfg.extraCandidates - extrasLeft);
        return cands;
    }

    private static String playKey(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        return (host == null ? "" : host.getName()) + "|" + sa.getDescription();
    }

    /** What a play is for, for keeping the candidates varied and for deciding what discovery may add. */
    enum PlayKind { STRATEGIC, INTERACTION, CREATURE, OTHER }

    static PlayKind playKind(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (host != null && sa.isSpell() && host.isCreature()) {
            return PlayKind.CREATURE;
        }
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            if (api == null) {
                continue;
            }
            switch (api) {
                case Draw:
                case Dig:
                case Scry:
                case Surveil:
                case Investigate:
                case GainLife:
                    return PlayKind.STRATEGIC;
                case Counter:
                case Destroy:
                case DestroyAll:
                case DamageAll:
                case DealDamage:
                case Fight:
                case Tap:
                case TapAll:
                    return PlayKind.INTERACTION;
                case ChangeZone:
                    if (a.usesTargeting()) {
                        return PlayKind.INTERACTION; // bounce, exile
                    }
                    break;
                default:
                    break;
            }
        }
        return PlayKind.OTHER;
    }

    /**
     * Whether discovery may add this play although the heuristics did not approve it: card draw, card selection, life
     * gain and clues without targets, and bounce, removal and tapping of an opposing permanent. Plays with an X in
     * the cost, lands and mana abilities never are.
     */
    public static boolean isDiscoverable(final SpellAbility sa) {
        return isDiscoverable(sa, false);
    }

    /**
     * With {@code broad}, also the plays that develop the position without a target: creature and other permanent
     * spells, token makers, counters, library searches and animation. Rollouts decide whether they beat the plan the
     * heuristics had; nothing here approves them.
     */
    public static boolean isDiscoverable(final SpellAbility sa, final boolean broad) {
        if (sa == null || sa.isLandAbility() || sa.isManaAbility() || sa.getHostCard() == null) {
            return false;
        }
        if (sa.getPayCosts() != null && sa.getPayCosts().hasXInAnyCostPart()) {
            return false;
        }
        final PlayKind kind = playKind(sa);
        if (kind == PlayKind.STRATEGIC) {
            return singleTargetingAbility(sa) == null && !hasTargeting(sa);
        }
        if (kind == PlayKind.INTERACTION) {
            final SpellAbility ts = singleTargetingAbility(sa);
            if (ts == null) {
                return false;
            }
            final ApiType api = ts.getApi();
            return api == ApiType.ChangeZone || api == ApiType.Destroy || api == ApiType.Tap;
        }
        return broad && isDevelopment(sa);
    }

    private static boolean isDevelopment(final SpellAbility sa) {
        if (hasTargeting(sa) || sa.getPayCosts() == null) {
            return false;
        }
        final Card host = sa.getHostCard();
        if (sa.isSpell() && host.isPermanent() && !host.isLand()) {
            return true; // creatures, artifacts, enchantments, planeswalkers
        }
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            if (api == ApiType.Token || api == ApiType.PutCounter || api == ApiType.PutCounterAll
                    || api == ApiType.Animate || api == ApiType.ChangeZone) {
                return true;
            }
        }
        return false;
    }

    /** Removal, bounce, tapping and counterspells: the plays that are worth an instant in the middle of combat. */
    public static boolean isInteraction(final SpellAbility sa) {
        return sa != null && (sa.getApi() == ApiType.Counter || playKind(sa) == PlayKind.INTERACTION);
    }

    private static boolean hasTargeting(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.usesTargeting()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps the first (the heuristics' choice) and then fills up to {@code cap} plays taking turns between the kinds
     * of play, so that a hand full of creature spells cannot push out the card draw or the removal spell the heuristics
     * ranked a little lower. Within a kind the heuristic order is kept.
     */
    public static List<SpellAbility> diversify(final List<SpellAbility> approved, final int cap) {
        if (approved.size() <= cap) {
            return approved;
        }
        final List<SpellAbility> out = new ArrayList<>();
        out.add(approved.get(0));
        final List<List<SpellAbility>> buckets = new ArrayList<>();
        for (int i = 0; i < PlayKind.values().length; i++) {
            buckets.add(new ArrayList<>());
        }
        for (int i = 1; i < approved.size(); i++) {
            buckets.get(playKind(approved.get(i)).ordinal()).add(approved.get(i));
        }
        // strategic and interaction plays first: they are the ones that a creature-first ordering buries
        final int[] order = { PlayKind.STRATEGIC.ordinal(), PlayKind.INTERACTION.ordinal(), PlayKind.CREATURE.ordinal(),
                PlayKind.OTHER.ordinal() };
        boolean added = true;
        while (out.size() < cap && added) {
            added = false;
            for (final int k : order) {
                if (out.size() < cap && !buckets.get(k).isEmpty()) {
                    out.add(buckets.get(k).remove(0));
                    added = true;
                }
            }
        }
        return out;
    }

    /**
     * The best opposing target for a play that has a single target and is meant to be used against the opponent
     * (bounce, removal, tap), or null if it is not such a play or nothing legal is there. Targets are ranked like the
     * target variants: opposing permanents by their value.
     */
    public static GameObject hostileTarget(final Player player, final SpellAbility sa) {
        final SpellAbility ts = singleTargetingAbility(sa);
        if (ts == null || ts.isDividedAsYouChoose()) {
            return null;
        }
        final TargetRestrictions tr = ts.getTargetRestrictions();
        if (tr == null || tr.getMaxTargets(sa.getHostCard(), ts) != 1) {
            return null;
        }
        final ApiType api = ts.getApi();
        if (api != ApiType.ChangeZone && api != ApiType.Destroy && api != ApiType.Tap && api != ApiType.DealDamage) {
            return null;
        }
        sa.setActivatingPlayer(player);
        GameEntity best = null;
        int bestRank = Integer.MIN_VALUE;
        for (final GameEntity e : tr.getAllCandidates(ts)) {
            if (!(e instanceof Card) || !Boolean.FALSE.equals(isOwnSide(player, e))) {
                continue;
            }
            final int rank = targetRank(player, e);
            if (rank > bestRank) {
                bestRank = rank;
                best = e;
            }
        }
        return best != null && applyTarget(sa, best) ? best : null;
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
            // never offer the legend the AI already has working as a target to bring back: the legend rule ends it
            if (e != current && wantsOwn == isOwnSide(player, e) && !ComboGuard.wouldBreakLegendRule(player, ts, e)) {
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
                                final int playoutPlays, final Mode mode) {
        TOTAL_ROLLOUTS.incrementAndGet();
        final Random savedRandom = MyRandom.getRandom();
        try {
            // MyRandom is global state; this is safe because the game thread is waiting for this AI decision.
            MyRandom.setRandom(new Random(seed));

            final GameSimulator sim = GameSimulator.forRollout(player.getGame(), player, mode == Mode.RESPOND);
            final Game simGame = sim.getSimulatedGameState();
            final Player simAi = sim.getSimulatedAiPlayer();
            simGame.AI_TIMEOUT = Math.min(simGame.AI_TIMEOUT, ROLLOUT_AI_TIMEOUT_SECONDS);
            HiddenInfoShuffler.shuffle(simGame, sim.getGameCopier(), player, new Random(seed ^ HIDDEN_INFO_SALT));

            final Long value;
            switch (mode) {
                case RESPOND:
                    value = rolloutRespond(sim, simGame, simAi, cand, cfg, playoutPlays);
                    break;
                case END_STEP:
                    value = rolloutEndStep(sim, simGame, simAi, cand, cfg, playoutPlays);
                    break;
                case ATTACK:
                    value = rolloutAttack(sim, simGame, simAi, cand, cfg, playoutPlays);
                    break;
                default:
                    value = rolloutMain(sim, simGame, simAi, cand, cfg, playoutPlays);
                    break;
            }
            return value;
        } catch (final AITimeoutTracker.Expired e) {
            throw e;
        } catch (final RolloutTooSlow e) {
            lastRolloutProblem = "slow";
            return null;
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

    private static Long rolloutMain(final GameSimulator sim, final Game simGame, final Player simAi, final Candidate cand,
                                    final Config cfg, final int playoutPlays) {
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
        final Long own = valueAtSecondMain(simGame, simAi, cand, cfg, true);
        if (cfg.opponentLookaheadPercent <= 0 || own == null || own == WIN_VALUE || own == LOSS_VALUE) {
            return own;
        }
        return withOpponentTurn(simGame, simAi, cand, cfg, playoutPlays, own);
    }

    // ------------------------------------------------------------------------------------------------ attacks

    /** Attack sets compared at most (the heuristics' own attack included). */
    static final int MAX_ATTACK_CANDIDATES = 6;

    /**
     * Compares attack sets for the declaration in progress: what the heuristics chose, not attacking, attacking with
     * everything that can, and the heuristic attack with one attacker fewer or more. Each set is played out in a copy
     * of the game (the opponent's heuristic AI blocks) up to the second main phase.
     *
     * @return the verdict; {@link Result#attackers} is the set to attack with when rollouts decided, otherwise null
     */
    public static Result chooseAttack(final Player player, final forge.game.combat.Combat combat, final Config cfg) {
        final long start = System.nanoTime();
        try {
            final List<Candidate> cands = buildAttackCandidates(player, combat);
            if (cands.size() < 2) {
                return Result.keepHeuristic(null, "single-candidate");
            }
            return chooseImpl(player, new ArrayList<>(), new ArrayList<>(), cfg, null, start, Mode.ATTACK, cands);
        } catch (final AITimeoutTracker.Expired e) {
            return new Result(null, false, false, 0, 0, System.nanoTime() - start, "deadline");
        } catch (final RuntimeException e) {
            reportFailure(e);
            return new Result(null, false, false, 0, 0, System.nanoTime() - start, "error");
        } finally {
            rolloutDeadlineNanos = Long.MAX_VALUE;
        }
    }

    private static List<Candidate> buildAttackCandidates(final Player player, final forge.game.combat.Combat combat) {
        final Map<Card, GameEntity> heuristic = new LinkedHashMap<>();
        final Map<GameEntity, Integer> perDefender = new HashMap<>();
        for (final Card c : combat.getAttackers()) {
            final GameEntity d = combat.getDefenderByAttacker(c);
            if (d != null) {
                heuristic.put(c, d);
                perDefender.merge(d, 1, Integer::sum);
            }
        }
        GameEntity main = null;
        int best = 0;
        for (final Map.Entry<GameEntity, Integer> e : perDefender.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                main = e.getKey();
            }
        }
        if (main == null) {
            main = player.getWeakestOpponent();
        }
        final List<Candidate> cands = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        addAttackCandidate(cands, seen, heuristic); // index 0: what the heuristics would do
        if (main == null) {
            return cands;
        }
        final List<Card> legal = new ArrayList<>();
        for (final Card c : player.getCreaturesInPlay()) {
            if (forge.game.combat.CombatUtil.canAttack(c, main)) {
                legal.add(c);
            }
        }
        if (legal.isEmpty()) {
            return cands;
        }
        legal.sort((a, b) -> Integer.compare(ComputerUtilCard.evaluateCreature(b), ComputerUtilCard.evaluateCreature(a)));
        addAttackCandidate(cands, seen, new LinkedHashMap<>()); // not attacking
        final Map<Card, GameEntity> everything = new LinkedHashMap<>(heuristic);
        for (final Card c : legal) {
            everything.putIfAbsent(c, main);
        }
        addAttackCandidate(cands, seen, everything);
        // one attacker fewer: the weakest ones first (they are the likeliest to have been sent in for nothing)
        final List<Card> attacking = new ArrayList<>(heuristic.keySet());
        attacking.sort((a, b) -> Integer.compare(ComputerUtilCard.evaluateCreature(a), ComputerUtilCard.evaluateCreature(b)));
        if (attacking.size() >= 2) {
            for (int i = 0; i < attacking.size() && i < 2; i++) {
                final Map<Card, GameEntity> fewer = new LinkedHashMap<>(heuristic);
                fewer.remove(attacking.get(i));
                addAttackCandidate(cands, seen, fewer);
            }
        }
        // one attacker more: the strongest that stayed home
        int added = 0;
        for (final Card c : legal) {
            if (added >= 2) {
                break;
            }
            if (!heuristic.containsKey(c)) {
                final Map<Card, GameEntity> more = new LinkedHashMap<>(heuristic);
                more.put(c, main);
                addAttackCandidate(cands, seen, more);
                added++;
            }
        }
        while (cands.size() > MAX_ATTACK_CANDIDATES) {
            cands.remove(cands.size() - 1);
        }
        return cands;
    }

    private static void addAttackCandidate(final List<Candidate> cands, final Set<String> seen, final Map<Card, GameEntity> set) {
        final List<String> ids = new ArrayList<>();
        for (final Map.Entry<Card, GameEntity> e : set.entrySet()) {
            ids.add(e.getKey().getId() + ">" + e.getValue().getId());
        }
        Collections.sort(ids);
        if (seen.add(String.join(",", ids))) {
            cands.add(new Candidate(set));
        }
    }

    /** One sample of one attack set: declare it in the copy, let the opponent block, play on to the second main phase. */
    private static Long rolloutAttack(final GameSimulator sim, final Game simGame, final Player simAi, final Candidate cand,
                                      final Config cfg, final int playoutPlays) {
        final PhaseHandler ph = simGame.getPhaseHandler();
        final forge.game.combat.Combat simCombat = ph.getCombat();
        if (simCombat == null || !ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)
                || !(simAi.getController() instanceof PlayerControllerAi)) {
            lastRolloutProblem = "no-combat";
            return null;
        }
        final Map<Card, GameEntity> mapped = new LinkedHashMap<>();
        for (final Map.Entry<Card, GameEntity> e : cand.attackers.entrySet()) {
            final Card c = sim.getGameCopier().find(e.getKey());
            final GameEntity d = sim.getGameCopier().find(e.getValue());
            if (c == null || d == null) {
                lastRolloutProblem = "attack-map";
                return null;
            }
            mapped.put(c, d);
        }
        simCombat.clearAttackers();
        ((PlayerControllerAi) simAi.getController()).getAi().setAttackOverride(mapped);
        ph.devDeclareAttackers();
        if (!simGame.isGameOver()) {
            ph.devAdvanceToPhase(PhaseType.MAIN2, () -> GameSimulator.resolveStack(simGame, simAi.getWeakestOpponent()));
            if (!simGame.isGameOver()) {
                playOut(simGame, simAi, playoutPlays);
            }
        }
        return valueAtSecondMain(simGame, simAi, cand, cfg, true);
    }

    /** What a line that loses the game during the opponent's turn is worth, relative to how it looked before. */
    private static final int LOOKAHEAD_LOSS_PENALTY = 1500;

    /**
     * Plays the opponent's next turn in the copy (their heuristic AI casts and attacks, the AI blocks) and blends the
     * position at the end of it with the one at the end of the AI's own turn. Candidates share the seed, so the same
     * opponent draw and hand hit every line: only what the line changes shows up in the difference.
     */
    private static Long withOpponentTurn(final Game simGame, final Player simAi, final Candidate cand, final Config cfg,
                                         final int playoutPlays, final long own) {
        final Player simOpp = simAi.getWeakestOpponent();
        if (simOpp == null || simGame.isGameOver()) {
            return own;
        }
        final PhaseHandler ph = simGame.getPhaseHandler();
        if (!ph.devAdvanceThroughTurnEnd(PhaseType.MAIN1, () -> GameSimulator.resolveStack(simGame, simOpp))
                && !simGame.isGameOver()) {
            return own; // not simulatable (reversed turn order): the end-of-turn estimate stands
        }
        long later;
        if (simGame.isGameOver() || !simAi.isInGame()) {
            later = simAi.hasWon() ? own + LOOKAHEAD_LOSS_PENALTY : own - LOOKAHEAD_LOSS_PENALTY;
        } else {
            if (ph.getPlayerTurn() == simOpp) {
                playOut(simGame, simOpp, playoutPlays);
                if (!simGame.isGameOver() && ph.getPhase().isBefore(PhaseType.MAIN2)) {
                    ph.devAdvanceToPhase(PhaseType.MAIN2, () -> GameSimulator.resolveStack(simGame, simAi));
                    playOut(simGame, simOpp, playoutPlays);
                }
            }
            if (simGame.isGameOver() || !simAi.isInGame()) {
                later = simAi.hasWon() ? own + LOOKAHEAD_LOSS_PENALTY : own - LOOKAHEAD_LOSS_PENALTY;
            } else {
                final Long v = valueAtSecondMain(simGame, simAi, cand, cfg, false);
                later = v == null ? own : v;
            }
        }
        final long blended = own + (later - own) * cfg.opponentLookaheadPercent / 100;
        return Math.max(LOSS_VALUE + 1, Math.min(WIN_VALUE - 1, blended));
    }

    /**
     * The value of the position at the second main phase (or wherever the AI's turn ended up): the board score plus
     * the terms for open mana, the crack-back and the strategic ones. A finished game is a win or a loss.
     */
    private static Long valueAtSecondMain(final Game simGame, final Player simAi, final Candidate cand, final Config cfg,
                                          final boolean holdAndRisk) {
        if (simGame.isGameOver() || !simAi.isInGame()) {
            return simAi.hasWon() ? WIN_VALUE : LOSS_VALUE;
        }
        // At the second main phase the evaluator needs no further game copy for combat.
        final Score score = new GameStateEvaluator().getScoreForGameState(simGame, simAi);
        // Ordinary positions stay strictly between the terminal values, so only a real win equals WIN_VALUE.
        long value = (long) score.value + StrategicEvaluator.adjust(simAi, cfg);
        if (TRACE) {
            final Player o = simAi.getWeakestOpponent();
            System.err.println("\t\t[flatmc value] " + (cand == null ? "?" : cand) + " turn=" + simGame.getPhaseHandler().getTurn() + " "
                    + simGame.getPhaseHandler().getPhase() + " board=" + score.value + " strategic=" + StrategicEvaluator.adjust(simAi, cfg)
                    + " myLife=" + simAi.getLife() + " oppLife=" + (o == null ? "-" : String.valueOf(o.getLife()))
                    + " oppBF=" + (o == null ? "-" : o.getCardsIn(ZoneType.Battlefield).toString()) + " oppHand=" + (o == null ? 0 : o.getCardsIn(ZoneType.Hand).size())
                    + " myHand=" + simAi.getCardsIn(ZoneType.Hand));
        }
        if (holdAndRisk) {
            value += holdUpBonus(simAi, cfg) + riskAdjustment(simAi, cfg);
        }
        if (cand != null && cand.sa != null) {
            value += StrategicEvaluator.selectionBonus(cand.sa, cfg);
        }
        return Math.max(LOSS_VALUE + 1, Math.min(WIN_VALUE - 1, value));
    }

    /**
     * A decision with priority on the opponent's turn (a spell on the stack, or their attack): play the candidate on
     * top of what is there, let the AI follow up if its own heuristics want to (a counterspell found by a cantrip),
     * then let the rest resolve and play the turn on to its end step.
     */
    private static Long rolloutRespond(final GameSimulator sim, final Game simGame, final Player simAi,
                                       final Candidate cand, final Config cfg, final int playoutPlays) {
        final Player simOpp = simAi.getWeakestOpponent();
        if (!cand.isHold()) {
            if (!sim.applySpellAbility(cand.sa, false)) {
                lastRolloutProblem = "apply";
                return null;
            }
            GameSimulator.resolveOwnTop(simGame, simAi, simOpp);
            playOutResponding(simGame, simAi, simOpp, Math.min(RESPONSE_FOLLOW_UPS, playoutPlays));
        }
        // Mana and instants still open for the rest of the turn are worth something, but the turn is nearly over.
        final long held = simGame.isGameOver() ? 0 : holdUpBonus(simAi, cfg) / 2;
        if (!simGame.isGameOver()) {
            GameSimulator.resolveStack(simGame, simOpp);
        }
        final PhaseHandler ph = simGame.getPhaseHandler();
        if (!simGame.isGameOver() && ph.getPhase().isBefore(PhaseType.END_OF_TURN)) {
            ph.devAdvanceToPhase(PhaseType.END_OF_TURN, () -> GameSimulator.resolveStack(simGame, simOpp));
        }
        final Long base = valueAtSecondMain(simGame, simAi, cand, cfg, false);
        return base == null || base == WIN_VALUE || base == LOSS_VALUE ? base
                : Math.max(LOSS_VALUE + 1, Math.min(WIN_VALUE - 1, base + held));
    }

    /**
     * The opponent's end step: act now or pass, then play on into the AI's own next turn (untap, draw, main phase
     * with what it has, combat) and score at its second main phase. This is what makes drawing a card at the end of
     * the opponent's turn worth more than the same mana wasted, while a play that taps mana the AI needed on its own
     * turn shows up as a worse turn.
     */
    private static Long rolloutEndStep(final GameSimulator sim, final Game simGame, final Player simAi,
                                       final Candidate cand, final Config cfg, final int playoutPlays) {
        final Player simOpp = simAi.getWeakestOpponent();
        if (!cand.isHold() && !sim.applySpellAbility(cand.sa)) {
            lastRolloutProblem = "apply";
            return null;
        }
        final PhaseHandler ph = simGame.getPhaseHandler();
        if (!simGame.isGameOver()
                && !ph.devAdvanceThroughTurnEnd(PhaseType.MAIN1, () -> GameSimulator.resolveStack(simGame, simOpp))
                && !simGame.isGameOver()) {
            lastRolloutProblem = "advance";
            return null;
        }
        if (!simGame.isGameOver() && ph.getPlayerTurn() == simAi) {
            playOut(simGame, simAi, playoutPlays);
            if (!simGame.isGameOver() && ph.getPhase().isBefore(PhaseType.MAIN2)) {
                ph.devAdvanceToPhase(PhaseType.MAIN2, () -> GameSimulator.resolveStack(simGame, simOpp));
                playOut(simGame, simAi, playoutPlays);
            }
        }
        return valueAtSecondMain(simGame, simAi, cand, cfg, true);
    }

    /** Follow-up plays the AI may make after its response while the opponent's spell is still on the stack. */
    private static final int RESPONSE_FOLLOW_UPS = 3;

    private static void playOutResponding(final Game simGame, final Player simAi, final Player simOpp, final int maxPlays) {
        if (!(simAi.getController() instanceof PlayerControllerAi)) {
            return;
        }
        final PlayerControllerAi pc = (PlayerControllerAi) simAi.getController();
        for (int i = 0; i < maxPlays && !simGame.isGameOver() && !simGame.getStack().isEmpty(); i++) {
            AITimeoutTracker.check();
            checkRolloutDeadline();
            final List<SpellAbility> next = pc.chooseSpellAbilityToPlay();
            if (next == null || next.isEmpty()) {
                return;
            }
            pc.playChosenSpellAbility(next.get(0));
            GameSimulator.resolveOwnTop(simGame, simAi, simOpp);
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
            checkRolloutDeadline();
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
            final int danger = ComputerUtil.predictNextCombatsDanger(simAi, simAi.getOpponents());
            if (danger == 2) {
                adj -= RISK_SERIOUS_DANGER;
            } else if (danger == 1) {
                adj -= RISK_DANGER;
            }
            final Player opp = simAi.getWeakestOpponent();
            if (opp != null && ComputerUtil.predictNextCombatsRemainingLife(opp, false, false, 0, null,
                    Collections.singletonList(simAi)) == Integer.MIN_VALUE) {
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
        final StringBuilder where = new StringBuilder();
        for (int i = 0; i < Math.min(3, trace.length); i++) {
            where.append(i == 0 ? " @ " : " < ").append(trace[i].getClassName().replaceAll(".*\\.", "")).append('.')
                    .append(trace[i].getMethodName()).append(':').append(trace[i].getLineNumber());
        }
        FlatMonteCarloStats.noteFailure(String.valueOf(e) + where);
        if (TOTAL_FAILURES.incrementAndGet() <= 5) {
            System.err.println("Flat Monte Carlo rollout failed, using the heuristic choice instead: " + e);
            for (int i = 0; i < Math.min(8, trace.length); i++) {
                System.err.println("\tat " + trace[i]);
            }
        }
    }
}
