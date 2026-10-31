package forge.ai.simulation;

import java.util.*;

import forge.game.Game;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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

    private static final class Node implements Comparable<Node> {
        final GameSimulator sim;
        final List<Step> steps;
        final int score;
        final Set<Integer> gyCast; // ids of graveyard cards already cast in this line

        Node(final GameSimulator sim, final List<Step> steps, final int score, final Set<Integer> gyCast) {
            this.sim = sim;
            this.steps = steps;
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
    /** What the last search that found no line saw: the start, the best state and why it stopped. */
    private static String lastDiagnostic = "";

    private Line line;
    private int index;
    private int turnKey = -1;
    private int solves;
    /** What the last call did, for tests and traces. */
    public String lastNote = "";

    /**
     * The next play of the winning line, or null when there is none and the caller should decide as it always did.
     * {@code combos} is the plan of the AI; this does nothing for a deck that is not a tutor storm deck.
     */
    public SpellAbility next(final Player ai, final ComboPlan combos) {
        final SpellAbility sa = nextImpl(ai, combos);
        if (Boolean.getBoolean("storm.trace") && lastNote != null && !lastNote.isEmpty()) {
            System.out.println("STORM-SOLVER " + lastNote);
        }
        return sa;
    }

    private SpellAbility nextImpl(final Player ai, final ComboPlan combos) {
        lastNote = "";
        if (ai == null || combos == null || !combos.has(ComboPlan.Kind.TUTOR_STORM)) {
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
            line = null; // the game went another way: find a line from here
        }
        if (solves >= MAX_SOLVES_PER_TURN) {
            lastNote = "solve limit";
            return null;
        }
        solves++;
        line = solve(ai, combos, DEFAULT_MAX_NODES, DEFAULT_BUDGET_NANOS);
        index = 0;
        if (line == null) {
            lastNote = "no winning line: " + lastDiagnostic;
            return null;
        }
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

    /** What the last search that found no line saw (see {@link #solve}). */
    public static String lastDiagnostic() {
        return lastDiagnostic;
    }

    /** Searches for a line that wins this turn; null when none was found within the limits. */
    public static Line solve(final Player ai, final ComboPlan combos, final int maxNodes, final long budgetNanos) {
        final long start = System.nanoTime();
        final Game game = ai.getGame();
        final GameSimulator root = new GameSimulator(game, ai);
        HiddenInfoShuffler.shuffle(root.getSimulatedGameState(), root.getGameCopier(), ai, new Random(game.getPhaseHandler().getTurn()));
        setStorm(root.getSimulatedGameState(), root.getSimulatedAiPlayer(), ComboPlan.stormCount(ai));

        final PriorityQueue<Node> open = new PriorityQueue<>();
        final Set<String> seen = new HashSet<>();
        open.add(new Node(root, new ArrayList<Step>(), combos.solverScore(root.getSimulatedAiPlayer()), new HashSet<Integer>()));
        seen.add(stateKey(root.getSimulatedAiPlayer()));
        int nodes = 0;
        Node best = open.peek();
        final String startState = describe(root.getSimulatedAiPlayer());
        final StringBuilder trail = new StringBuilder();
        trail.append(" | picker: ").append(new SpellAbilityPicker(ai).explainCandidates());
        while (!open.isEmpty() && nodes < maxNodes && System.nanoTime() - start < budgetNanos) {
            final Node node = open.poll();
            nodes++;
            if (node.score > best.score) {
                best = node;
            }
            final Player simAi = node.sim.getSimulatedAiPlayer();
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
                if (System.nanoTime() - start >= budgetNanos) {
                    break;
                }
                final int stormBefore = ComboPlan.stormCount(simAi);
                final GameSimulator child = new GameSimulator(node.sim.getSimulatedGameState(), simAi);
                setStorm(child.getSimulatedGameState(), child.getSimulatedAiPlayer(), stormBefore);
                if (!child.applySpellAbility(sa)) {
                    if (trail.length() < 3000) {
                        trail.append(" | could not play ").append(new Step(sa));
                    }
                    continue;
                }
                final Player childAi = child.getSimulatedAiPlayer();
                final Game childGame = child.getSimulatedGameState();
                if (childGame.isGameOver()) {
                    continue;
                }
                final Set<Integer> gy = new HashSet<>(node.gyCast);
                if (fromGy) {
                    gy.add(host.getId());
                }
                if (!seen.add(stateKey(childAi) + new TreeSet<>(gy))) {
                    continue;
                }
                final List<Step> steps = new ArrayList<>(node.steps);
                steps.add(new Step(sa));
                if (trail.length() < 3000) {
                    trail.append(" | ").append(steps.get(steps.size() - 1).host).append(" -> {").append(describe(childAi)).append('}');
                }
                open.add(new Node(child, steps, combos.solverScore(childAi) + steps.size(), gy));
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
                            || System.nanoTime() - start >= budgetNanos) {
                        continue;
                    }
                    final GameSimulator child = tutorWithDiscardMana(node.sim, sa, discardMana);
                    if (child == null) {
                        continue;
                    }
                    final Player childAi = child.getSimulatedAiPlayer();
                    if (child.getSimulatedGameState().isGameOver() || !seen.add(stateKey(childAi) + new TreeSet<>(node.gyCast))) {
                        continue;
                    }
                    final List<Step> steps = new ArrayList<>(node.steps);
                    steps.add(new Step(sa));
                    steps.add(new Step(discardMana, true));
                    if (trail.length() < 3000) {
                        trail.append(" | ").append(host.getName()).append("+").append(discardMana.getHostCard().getName())
                                .append(" -> {").append(describe(childAi)).append('}');
                    }
                    open.add(new Node(child, steps, combos.solverScore(childAi) + steps.size(), new HashSet<>(node.gyCast)));
                }
            }
        }
        lastDiagnostic = "start {" + startState + "} searched " + nodes + " states in " + (System.nanoTime() - start) / 1_000_000
                + " ms (" + (open.isEmpty() ? "all explored" : nodes >= maxNodes ? "state limit" : "time limit") + "); best score "
                + best.score + " after " + best.steps + " {" + describe(best.sim.getSimulatedAiPlayer()) + "}" + trail;
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
        return child;
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
