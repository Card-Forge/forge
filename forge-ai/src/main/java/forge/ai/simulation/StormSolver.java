package forge.ai.simulation;

import java.util.*;

import forge.game.Game;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.AITimeoutTracker;

/**
 * Finds a line that wins a tutor storm deck (Beseech the Mirror, Infernal Tutor, Gaea's Will, Tendrils of Agony) this
 * turn, instead of guessing: a best-first search over sequences of plays in copies of the game. A state is ranked by
 * {@link ComboPlan#stormProgress}; the storm card is only a move once it takes the last life of the opponent, so the
 * search never wastes it. The cards the AI cannot see are shuffled in the first copy, which makes the line a plan
 * that holds for one possible library; {@link #next} therefore checks every step against the real game and searches
 * again when the game went another way. Plays that are not found to win are left to Flat Monte Carlo.
 */
public final class StormSolver {
    /** One play of a line: the name of the card and the text of the ability, which is how it is found again. */
    public static final class Step {
        public final String host;
        public final String sa;
        /** Played in response to the spell the line cast before: Lion's Eye Diamond with Infernal Tutor on the stack. */
        public final boolean response;

        Step(final SpellAbility sa) {
            this(sa, false);
        }

        Step(final SpellAbility sa, final boolean response) {
            this.host = sa.getHostCard() == null ? "" : sa.getHostCard().getName();
            this.sa = sa.toString();
            this.response = response;
        }

        boolean matches(final SpellAbility other) {
            return other.getHostCard() != null && host.equals(other.getHostCard().getName()) && sa.equals(other.toString());
        }

        @Override
        public String toString() {
            return (response ? "(in response) " : "") + host + ": " + sa;
        }
    }

    /** A winning line, the number of states that were searched and the time it took. */
    public static final class Line {
        public final List<Step> steps;
        public final int nodes;
        public final long nanos;

        Line(final List<Step> steps, final int nodes, final long nanos) {
            this.steps = steps;
            this.nodes = nodes;
            this.nanos = nanos;
        }
    }

    /**
     * A state of the search. It remembers the plays that lead to it, not the game: only a few nodes hold their game
     * ({@link #remember}), the others are played again from their parent when they are needed ({@link #materialize}).
     * That keeps the memory at a handful of copies of the game instead of one per state.
     */
    private static final class Node implements Comparable<Node> {
        GameSimulator sim; // null when evicted
        final Node parent;
        final List<Step> steps;
        final int added; // how many plays of steps this node added to its parent's: 1, or 2 for the tutor and the Diamond
        final int score;
        final Set<Integer> gyCast; // ids of graveyard cards already cast in this line

        Node(final GameSimulator sim, final Node parent, final List<Step> steps, final int added, final int score, final Set<Integer> gyCast) {
            this.sim = sim;
            this.parent = parent;
            this.steps = steps;
            this.added = added;
            this.score = score;
            this.gyCast = gyCast;
        }

        @Override
        public int compareTo(final Node o) {
            return Integer.compare(o.score, score); // best first
        }
    }

    public static final int DEFAULT_MAX_NODES = 400;
    public static final long DEFAULT_BUDGET_NANOS = 4_000_000_000L;
    private static final int MAX_SOLVES_PER_TURN = 4;
    /** However many lines were found and left again, a turn is searched at most this often. */
    private static final int MAX_TOTAL_SOLVES = 16;
    /** The search stops (and the AI falls back to Flat Monte Carlo) when this share of the heap is in use. */
    private static final double MEMORY_LIMIT = 0.90;
    private static final boolean TRACE = Boolean.getBoolean("storm.trace");
    /** The plays that make the hand better when no kill was found (null: nothing worth doing). */
    static List<Step> lastSetup() {
        return lastSetup;
    }

    /** What the last search that found no line saw: the start, the best state and why it stopped. */
    private static String lastDiagnostic = "";
    /** The plays of the best state to end the turn in when no kill was found (see {@link ComboPlan#setupScore}), or null. */
    private static volatile List<Step> lastSetup;
    /** A setup play has to make the hand this much better, or the AI does what it always did. */
    private static final int SETUP_MIN_GAIN = 30;

    /** Set after an OutOfMemoryError: the AI plays without the solver for the rest of the game. */
    private boolean disabled;
    private Line line;
    private int index;
    private int turnKey = -1;
    private int solves;
    private int totalSolves;
    /** States of this turn the search found nothing in: the same state is not searched again (the AI asks many times). */
    private final Set<String> failed = new HashSet<>();
    /** What the last call did, for tests and traces. */
    public String lastNote = "";

    /**
     * The next play of the winning line, or null when there is none and the caller should decide as it always did.
     * {@code combos} is the plan of the AI; this does nothing for a deck that is not a tutor storm deck.
     */
    public SpellAbility next(final Player ai, final ComboPlan combos) {
        SpellAbility sa;
        try {
            sa = nextImpl(ai, combos);
        } catch (final AITimeoutTracker.Expired e) {
            throw e;
        } catch (final RuntimeException e) {
            // a failure in the search must not end the turn or switch Flat Monte Carlo off: play on without the solver
            disabled = true;
            line = null;
            lastNote = "solver error, solver off: " + e;
            if (TRACE) {
                e.printStackTrace(System.out);
            }
            sa = null;
        }
        if (Boolean.getBoolean("storm.trace") && lastNote != null && !lastNote.isEmpty()) {
            System.out.println("STORM-SOLVER " + lastNote);
        }
        return sa;
    }

    private SpellAbility nextImpl(final Player ai, final ComboPlan combos) {
        lastNote = "";
        if (ai == null || combos == null || disabled || !combos.has(ComboPlan.Kind.TUTOR_STORM)) {
            return null;
        }
        final Game game = ai.getGame();
        final forge.game.phase.PhaseHandler ph = game.getPhaseHandler();
        if (ph.getPlayerTurn() != ai || !ph.getPhase().isMain()) {
            return null;
        }
        if (!game.getStack().isEmpty()) {
            // only to play the step of the line that answers the spell the line just cast
            if (!expectsResponse(ai)) {
                return null;
            }
            final Step pending = line.steps.get(index);
            for (final SpellAbility sa : movesOf(ai)) {
                if (pending.matches(sa)) {
                    index++;
                    lastNote = "follow " + pending;
                    aimAt(ai, sa);
                    return sa;
                }
            }
            line = null; // it cannot be played: the line is over, a new one is looked for when the stack is empty
            lastNote = "response not playable";
            return null;
        }
        final int key = game.getPhaseHandler().getTurn() * 31 + System.identityHashCode(game);
        if (key != turnKey) {
            turnKey = key;
            line = null;
            index = 0;
            solves = 0;
            totalSolves = 0;
            failed.clear();
        }
        final List<SpellAbility> moves = movesOf(ai);
        if (line != null && index < line.steps.size()) {
            final Step step = line.steps.get(index);
            for (final SpellAbility sa : moves) {
                if (step.matches(sa)) {
                    index++;
                    lastNote = "follow " + step;
                    aimAt(ai, sa);
                    return sa;
                }
            }
            final StringBuilder seen = new StringBuilder();
            for (int i = 0; i < moves.size() && i < 8; i++) {
                seen.append(" | ").append(new Step(moves.get(i)));
            }
            line = null; // the game went another way: find a line from here
            lastNote = "line left at step " + index + ", expected " + step + "; plays were" + seen;
            if (TRACE) {
                System.out.println("STORM-SOLVER " + lastNote);
            }
        }
        // a line that is being played and went another way is searched again whatever happened before in this turn
        final String stateNow = stateKey(ai);
        if (failed.contains(stateNow)) {
            lastNote = "same state, no winning line";
            return null;
        }
        if (solves >= MAX_SOLVES_PER_TURN || totalSolves >= MAX_TOTAL_SOLVES) {
            lastNote = "solve limit";
            return null;
        }
        solves++;
        totalSolves++;
        try {
            line = solve(ai, combos, DEFAULT_MAX_NODES, budgetNanos());
        } catch (final OutOfMemoryError e) {
            line = null; // the search is local and garbage now; Flat Monte Carlo plays on
            disabled = true;
            lastNote = "out of memory: solver off";
            return null;
        }
        index = 0;
        if (line == null) {
            failed.add(stateNow);
            final List<Step> setup = lastSetup();
            if (setup != null && !setup.isEmpty()) {
                // no kill this turn: play what makes the next turn's kill likelier (the finisher or the engine found,
                // mana put in play), and keep the rest. The plays are followed like a line, and checked after each.
                line = new Line(setup, 0, 0L);
                index = 0;
                for (final SpellAbility sa : moves) {
                    if (setup.get(0).matches(sa)) {
                        index = 1;
                        lastNote = "setup " + setup;
                        aimAt(ai, sa);
                        return sa;
                    }
                }
                line = null;
            }
            lastNote = "no winning line: " + lastDiagnostic;
            return null;
        }
        solves = 0; // a line was found: what was searched in vain before does not count against playing it
        final Step step = line.steps.get(0);
        for (final SpellAbility sa : moves) {
            if (step.matches(sa)) {
                index = 1;
                lastNote = "start " + line.steps + " (" + line.nodes + " states, " + line.nanos / 1_000_000 + " ms)";
                aimAt(ai, sa);
                return sa;
            }
        }
        line = null;
        lastNote = "first step not playable";
        return null;
    }

    /**
     * A play from the picker's list has no targets yet. The simulation never needed them, but in the real game a spell
     * that needs a target and has none is not cast at all (the Tendrils of Agony that ends the line was dropped that
     * way, with nothing in the output): aim it at the opponent when it can take a player.
     */
    private static void aimAt(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting() || sa.getTargetRestrictions() == null || !sa.getTargetRestrictions().canTgtPlayer()) {
            return;
        }
        final Player opp = ai.getWeakestOpponent();
        if (opp != null && sa.canTarget(opp)) {
            sa.resetTargets();
            sa.getTargets().add(opp);
        }
    }

    /** Whether the next step of the line answers the spell on the stack (and the spell is the AI's own). */
    public boolean expectsResponse(final Player ai) {
        if (line == null || index >= line.steps.size() || !line.steps.get(index).response) {
            return false;
        }
        final Game game = ai.getGame();
        return !game.getStack().isEmpty() && ai.equals(game.getStack().peekAbility().getActivatingPlayer());
    }

    /**
     * The plays of the AI: the spells and abilities the picker lists, and the mana abilities that discard the hand
     * (Lion's Eye Diamond). Those are no spells, so the picker leaves them out, but they are what the deck turns its
     * hand into mana with, and a line that needs one is a line.
     */
    static List<SpellAbility> movesOf(final Player ai) {
        final List<SpellAbility> moves = new ArrayList<>(new SpellAbilityPicker(ai).getCandidateSpellsAndAbilities());
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (final SpellAbility sa : c.getManaAbilities()) {
                if (sa.getPayCosts() == null || !sa.getPayCosts().hasSpecificCostType(CostDiscard.class)
                        || !sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                    continue;
                }
                sa.setActivatingPlayer(ai);
                if (sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false)) {
                    moves.add(sa);
                    break; // one per card
                }
            }
        }
        return moves;
    }

    /**
     * Each GameSimulator copy starts its "spells cast this turn" list empty, so the storm count was lost after every
     * play. This writes the count back into a copy (the entries are placeholders; only the number matters).
     */
    private static void setStorm(final Game g, final Player simAi, final int count) {
        Card any = null;
        for (final ZoneType z : new ZoneType[] { ZoneType.Hand, ZoneType.Battlefield, ZoneType.Library, ZoneType.Graveyard }) {
            if (!simAi.getCardsIn(z).isEmpty()) {
                any = simAi.getCardsIn(z).get(0);
                break;
            }
        }
        final List<SpellAbility> cast = g.getStack().getSpellsCastThisTurn();
        cast.clear();
        if (any == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            cast.add(new SpellAbility.EmptySa(ApiType.Draw, any));
        }
    }

    /**
     * Burning Wish and the other Wishes fetch from outside the game, which the copy does not have: the simulation
     * could not cast them, or took another card instead. The sorceries of the real sideboard are put at the bottom of
     * the copy's library, and the Wishes of the copy search the library, where the pick of the deck finds them.
     */
    private static void addWishTargets(final Player ai, final GameSimulator root) {
        final Player simAi = root.getSimulatedAiPlayer();
        final Game simGame = root.getSimulatedGameState();
        boolean wish = false;
        for (final Card c : simAi.getCardsIn(ZoneType.Hand)) {
            for (final SpellAbility sa : c.getSpellAbilities()) {
                if (sa.getParamOrDefault("Origin", "").contains("Sideboard")) {
                    sa.putParam("Origin", "Library");
                    wish = true;
                }
            }
        }
        if (!wish) {
            return;
        }
        for (final Card sb : ai.getCardsIn(ZoneType.Sideboard)) {
            if (sb.isSorcery() && sb.getPaperCard() != null) {
                simAi.getZone(ZoneType.Library).add(CardFactory.getCard(sb.getPaperCard(), simAi, simGame));
            }
        }
    }

    /** A setup only prepares the next turn: it must not spend the hand, a sacrifice mana source or the Talisman. */
    private static boolean safeSetup(final List<Step> steps) {
        for (final Step s : steps) {
            final boolean spends = s.response || s.host.equals("Lion's Eye Diamond") || s.host.equals("Lotus Petal")
                    || s.host.equals("Dark Ritual") || (s.host.equals("Wishclaw Talisman") && s.sa.contains("wish counter"));
            if (spends) {
                return false;
            }
        }
        return true;
    }

    /** What the last search that found no line saw (see {@link #solve}). */
    public static String lastDiagnostic() {
        return lastDiagnostic;
    }

    /** How many different orders of the unseen library are tried: Ad Nauseam, Infernal Tutor and the like draw or find cards at random. */
    private static final int SHUFFLES = 3;

    /**
     * Searches for a line that wins this turn; null when none was found within the limits. The library is only a guess,
     * so a few different shuffles are searched, and the first line that wins in one of them is the plan. It is checked
     * against the real game after every play ({@link #next}), and when the draws went another way a new line is looked for.
     * That is how a hand with Ad Nauseam plays: the cast is made when some order of the library wins, then it is re-solved.
     */
    public static Line solve(final Player ai, final ComboPlan combos, final int maxNodes, final long budgetNanos) {
        final long start = System.nanoTime();
        final int shuffles = shuffles();
        final long each = budgetNanos / shuffles;
        Line found = null;
        for (int i = 0; i < shuffles && found == null; i++) {
            final long left = budgetNanos - (System.nanoTime() - start);
            if (left <= 0) {
                break;
            }
            found = solveShuffle(ai, combos, maxNodes, Math.min(left, each), i);
        }
        return found == null ? null : new Line(found.steps, found.nodes, System.nanoTime() - start);
    }

    private static Line solveShuffle(final Player ai, final ComboPlan combos, final int maxNodes, final long budgetNanos, final int variant) {
        final long start = System.nanoTime();
        final Game game = ai.getGame();
        final GameSimulator root = new GameSimulator(game, ai);
        HiddenInfoShuffler.shuffle(root.getSimulatedGameState(), root.getGameCopier(), ai, new Random(game.getPhaseHandler().getTurn() + 7919L * variant),
                ComboPlan::isTutorStormPiece);
        setStorm(root.getSimulatedGameState(), root.getSimulatedAiPlayer(), ComboPlan.stormCount(ai));
        addWishTargets(ai, root);
        root.releaseCopier(); // the shuffle above was the last use of the link to the real game

        lastSetup = null;
        int setupBest = combos.setupScore(root.getSimulatedAiPlayer());
        final int setupStart = setupBest;
        List<Step> setupSteps = null;
        final int cap = cacheSize();
        final List<Node> live = new ArrayList<>(cap + 2); // the nodes that hold their game, oldest first
        final PriorityQueue<Node> open = new PriorityQueue<>();
        final Set<Long> seen = new HashSet<>();
        final Node first = new Node(root, null, new ArrayList<Step>(), 0, combos.solverScore(root.getSimulatedAiPlayer()), new HashSet<Integer>());
        open.add(first); // the root keeps its game for good: every replay starts from it at the latest
        seen.add(hash(stateKey(root.getSimulatedAiPlayer())));
        int nodes = 0;
        int bestScore = first.score;
        List<Step> bestSteps = first.steps;
        String bestState = describe(root.getSimulatedAiPlayer()); // only text is kept, never a simulation
        final String startState = bestState;
        final StringBuilder trail = new StringBuilder();
        if (TRACE) {
            trail.append(" | picker: ").append(new SpellAbilityPicker(ai).explainCandidates());
        }
        while (!open.isEmpty() && nodes < maxNodes && System.nanoTime() - start < budgetNanos && !lowMemory()) {
            final Node node = open.poll();
            final GameSimulator nodeSim = materialize(node, live, cap);
            if (nodeSim == null) {
                continue; // the plays that lead here cannot be played again
            }
            nodes++;
            if (node.score > bestScore) {
                bestScore = node.score;
                bestSteps = node.steps;
                bestState = describe(nodeSim.getSimulatedAiPlayer());
            }
            final Player simAi = nodeSim.getSimulatedAiPlayer();
            final List<SpellAbility> moves = movesOf(simAi);
            // the winning cast first: if it is a move at all, the line is complete
            for (final SpellAbility sa : moves) {
                final Card host = sa.getHostCard();
                if (host != null && ComboPlan.isStormFinisher(host) && ComboPlan.stormIsLethal(simAi, host)) {
                    final List<Step> steps = new ArrayList<>(node.steps);
                    steps.add(new Step(sa));
                    return new Line(Collections.unmodifiableList(steps), nodes, System.nanoTime() - start);
                }
            }
            for (final SpellAbility sa : moves) {
                final Card host = sa.getHostCard();
                if (sa.isLandAbility() || host == null || ComboPlan.isStormFinisher(host) || combos.wastesTutor(sa)) {
                    continue; // lands are the AI's own business, and the storm card is kept for the lethal cast
                }
                final boolean fromGy = host.isInZone(ZoneType.Graveyard);
                if (fromGy && node.gyCast.contains(host.getId())) {
                    continue; // Gaea's Will exiles it after the first cast
                }
                if (System.nanoTime() - start >= budgetNanos || lowMemory()) {
                    break;
                }
                final GameSimulator child = playOn(nodeSim, sa);
                if (child == null) {
                    if (TRACE && trail.length() < 3000) {
                        trail.append(" | could not play ").append(new Step(sa));
                    }
                    continue;
                }
                final Player childAi = child.getSimulatedAiPlayer();
                if (child.getSimulatedGameState().isGameOver()) {
                    continue;
                }
                final Set<Integer> gy = new HashSet<>(node.gyCast);
                if (fromGy) {
                    gy.add(host.getId());
                }
                if (!seen.add(hash(stateKey(childAi) + new TreeSet<>(gy)))) {
                    continue;
                }
                final List<Step> steps = new ArrayList<>(node.steps);
                steps.add(new Step(sa));
                final int setupNow = combos.setupScore(childAi);
                if (setupNow > setupBest && safeSetup(steps)) {
                    setupBest = setupNow;
                    setupSteps = steps;
                }
                if (TRACE && trail.length() < 3000) {
                    trail.append(" | ").append(steps.get(steps.size() - 1).host).append(" -> {").append(describe(childAi)).append('}');
                }
                final Node childNode = new Node(child, node, steps, 1, combos.solverScore(childAi) + steps.size(), gy);
                remember(live, childNode, cap);
                open.add(childNode);
            }
            // Infernal Tutor with the Diamond cracked in response: the hand is empty when the Tutor resolves (Hellbent)
            SpellAbility discardMana = null;
            for (final SpellAbility m : moves) {
                if (m.isManaAbility() && m.getHostCard() != null) {
                    discardMana = m;
                    break;
                }
            }
            if (discardMana != null) {
                for (final SpellAbility sa : moves) {
                    final Card host = sa.getHostCard();
                    if (host == null || !sa.isSpell() || !ComboPlan.isHellbentTutor(host)
                            || System.nanoTime() - start >= budgetNanos || lowMemory()) {
                        continue;
                    }
                    final GameSimulator child = tutorWithDiscardMana(nodeSim, sa, discardMana);
                    if (child == null) {
                        continue;
                    }
                    final Player childAi = child.getSimulatedAiPlayer();
                    if (child.getSimulatedGameState().isGameOver() || !seen.add(hash(stateKey(childAi) + new TreeSet<>(node.gyCast)))) {
                        continue;
                    }
                    final List<Step> steps = new ArrayList<>(node.steps);
                    steps.add(new Step(sa));
                    steps.add(new Step(discardMana, true));
                    final int setupNow = combos.setupScore(childAi);
                    if (setupNow > setupBest && safeSetup(steps)) {
                        setupBest = setupNow;
                        setupSteps = steps;
                    }
                    if (TRACE && trail.length() < 3000) {
                        trail.append(" | ").append(host.getName()).append("+").append(discardMana.getHostCard().getName())
                                .append(" -> {").append(describe(childAi)).append('}');
                    }
                    final Node childNode = new Node(child, node, steps, 2, combos.solverScore(childAi) + steps.size(), new HashSet<>(node.gyCast));
                    remember(live, childNode, cap);
                    open.add(childNode);
                }
            }
        }
        if (setupSteps != null && setupBest - setupStart >= SETUP_MIN_GAIN) {
            lastSetup = Collections.unmodifiableList(setupSteps);
        }
        lastDiagnostic = "start {" + startState + "} searched " + nodes + " states in " + (System.nanoTime() - start) / 1_000_000
                + " ms (" + (open.isEmpty() ? "all explored" : nodes >= maxNodes ? "state limit" : lowMemory() ? "memory limit" : "time limit")
                + ", games kept " + cap + ", heap " + heap() + "); best score " + bestScore + " after " + bestSteps + " {" + bestState + "}" + trail;
        return null;
    }

    /** The game after one play of {@code from}: a copy, with the storm count of the parent; null when it cannot be played. */
    private static GameSimulator playOn(final GameSimulator from, final SpellAbility sa) {
        final Player simAi = from.getSimulatedAiPlayer();
        final int stormBefore = ComboPlan.stormCount(simAi);
        final GameSimulator child = new GameSimulator(from.getSimulatedGameState(), simAi);
        setStorm(child.getSimulatedGameState(), child.getSimulatedAiPlayer(), stormBefore);
        if (!child.applySpellAbility(sa)) {
            return null;
        }
        child.releaseCopier(); // the play is applied: the child no longer needs its parent
        return child;
    }

    /** Lets the node keep its game; the node that has kept it longest lets go of its game when there are too many. */
    private static void remember(final List<Node> live, final Node n, final int cap) {
        live.remove(n);
        live.add(n);
        while (live.size() > cap) {
            final Node old = live.remove(0);
            if (old.parent != null) { // the root keeps its game: it has no parent to be played again from
                old.sim = null;
            }
        }
    }

    /**
     * The game of a node: the one it kept, or the plays that lead to it played again from the nearest ancestor that
     * kept its game. Null when a play cannot be repeated.
     */
    private static GameSimulator materialize(final Node n, final List<Node> live, final int cap) {
        if (n.sim != null) {
            remember(live, n, cap);
            return n.sim;
        }
        if (n.parent == null) {
            return null;
        }
        final GameSimulator parentSim = materialize(n.parent, live, cap);
        if (parentSim == null) {
            return null;
        }
        final List<SpellAbility> moves = movesOf(parentSim.getSimulatedAiPlayer());
        final int k = n.steps.size() - n.added;
        final SpellAbility sa = find(moves, n.steps.get(k));
        if (sa == null) {
            return null;
        }
        GameSimulator g;
        if (n.added == 1) {
            g = playOn(parentSim, sa);
        } else {
            final SpellAbility discardMana = find(moves, n.steps.get(k + 1));
            g = discardMana == null ? null : tutorWithDiscardMana(parentSim, sa, discardMana);
        }
        if (g == null) {
            return null;
        }
        n.sim = g;
        remember(live, n, cap);
        return g;
    }

    private static SpellAbility find(final List<SpellAbility> moves, final Step step) {
        for (final SpellAbility sa : moves) {
            if (step.matches(sa)) {
                return sa;
            }
        }
        return null;
    }

    /**
     * Casts the tutor, cracks the mana ability that discards the hand while it is on the stack, and lets the tutor
     * resolve. Null when any of it cannot be played.
     */
    private static GameSimulator tutorWithDiscardMana(final GameSimulator from, final SpellAbility tutor, final SpellAbility discardMana) {
        final Player simAi = from.getSimulatedAiPlayer();
        final int stormBefore = ComboPlan.stormCount(simAi);
        final GameSimulator child = new GameSimulator(from.getSimulatedGameState(), simAi);
        setStorm(child.getSimulatedGameState(), child.getSimulatedAiPlayer(), stormBefore);
        if (!child.applySpellAbility(tutor, false)) {
            return null;
        }
        final Player childAi = child.getSimulatedAiPlayer();
        final Card card = child.getGameCopier().find(discardMana.getHostCard());
        SpellAbility mine = null;
        if (card != null) {
            for (final SpellAbility m : card.getManaAbilities()) {
                if (m.getDescription().equals(discardMana.getDescription())) {
                    mine = m;
                    break;
                }
            }
        }
        if (mine == null) {
            return null;
        }
        mine.setActivatingPlayer(childAi);
        if (!ComputerUtil.handlePlayingSpellAbility(childAi, mine, null)) {
            return null;
        }
        GameSimulator.resolveStack(child.getSimulatedGameState(), childAi.getWeakestOpponent());
        child.releaseCopier(); // the lookup of the Diamond above was the last use of the link to the parent
        return child;
    }

    /** A phone or tablet gives the app a few hundred MB: search less there. */
    private static boolean constrained() {
        return Runtime.getRuntime().maxMemory() <= (768L << 20);
    }


    /** How many states keep their game while the search runs: the others are played again when needed. */
    private static int cacheSize() {
        return constrained() ? 4 : 10;
    }

    private static int shuffles() {
        return 1; // the pieces of the deck are at the bottom of the library: one order is the pessimistic one, more is luck
    }

    private static long budgetNanos() {
        return constrained() ? DEFAULT_BUDGET_NANOS / 2 : DEFAULT_BUDGET_NANOS;
    }

    /** 64 bit hash of a state description: the set of seen states then holds numbers, not long strings. */
    private static long hash(final String s) {
        long h = 1125899906842597L;
        for (int i = 0; i < s.length(); i++) {
            h = 31 * h + s.charAt(i);
        }
        return h;
    }



    /** When the collector was last asked for: the used heap is mostly garbage that is not collected yet. */
    private static long lastGc;

    /**
     * The heap is nearly full of live data. The used memory of a search is mostly garbage, so the first time it looks
     * full the collector runs (at most once a second) and the heap is measured again; only what survives counts.
     */
    private static boolean lowMemory() {
        final Runtime rt = Runtime.getRuntime();
        if (rt.totalMemory() - rt.freeMemory() <= rt.maxMemory() * MEMORY_LIMIT) {
            return false;
        }
        final long now = System.nanoTime();
        if (now - lastGc > 1_000_000_000L) {
            System.gc();
            lastGc = System.nanoTime();
        }
        return rt.totalMemory() - rt.freeMemory() > rt.maxMemory() * MEMORY_LIMIT;
    }

    private static String heap() {
        final Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory() >> 20) + "/" + (rt.maxMemory() >> 20) + " MB";
    }

    private static String describe(final Player ai) {
        final List<String> hand = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            hand.add(c.getName());
        }
        final List<String> field = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isLand() || !c.isBasicLand()) {
                field.add(c.getName());
            }
        }
        return "hand=" + hand + " field=" + field + " lands=" + ai.getLandsInPlay().size() + " storm="
                + ai.getGame().getStack().getSpellsCastThisTurn().size() + " oppLife=" + ai.getWeakestOpponent().getLife();
    }

    /** Two states with the same cards in the same zones, mana, storm count and life are the same state. */
    static String stateKey(final Player ai) {
        final StringBuilder sb = new StringBuilder();
        for (final ZoneType z : new ZoneType[] { ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile }) {
            final List<String> names = new ArrayList<>();
            for (final Card c : ai.getCardsIn(z)) {
                names.add(c.getName() + (c.isTapped() ? "'" : ""));
            }
            Collections.sort(names);
            sb.append(z).append(names).append(';');
        }
        sb.append(ai.getManaPool().toString()).append(';')
                .append(ai.getGame().getStack().getSpellsCastThisTurn().size()).append(';')
                .append(ai.getLife()).append(';').append(ai.getWeakestOpponent().getLife());
        return sb.toString();
    }
}
