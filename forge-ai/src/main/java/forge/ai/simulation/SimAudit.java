package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fidelity audit of the game copies Monte Carlo reasons about (switch on with {@code -Dflatmc.audit=true}; off, every
 * hook is a single boolean test).
 * <p>
 * A rollout is only as good as the copied game it plays in. Two things are checked on the real games the AI plays:
 * <ul>
 * <li><b>copy equivalence</b>: every time the AI is asked for a play, a fresh copy of the game must look the same as
 * the game it was made from (players' life, zones, every permanent with its tapped state, power and toughness, damage,
 * counters and attachments);</li>
 * <li><b>play fidelity</b>: the play the AI then chooses is also applied to a copy and resolved there; at the AI's next
 * priority in the same phase (nothing else happened in between) the real game must look like that prediction.</li>
 * </ul>
 * Differences are counted by kind and the first few of each are kept, so that the sources of an unfaithful copy can be
 * found and fixed one by one. This is a development tool, not part of normal play.
 */
public final class SimAudit {
    public static final boolean ENABLED = Boolean.getBoolean("flatmc.audit");
    private static final int EXAMPLES_PER_KIND = 4;

    private static final Object LOCK = new Object();
    private static final Map<String, Integer> COUNTS = new TreeMap<>();
    private static final Map<String, List<String>> EXAMPLES = new LinkedHashMap<>();

    /** What the last real choice predicted, waiting to be compared at the AI's next priority. */
    private static final class Pending {
        final int gameId;
        final int turn;
        final PhaseType phase;
        final String play;
        final Map<String, String> predicted;

        Pending(final int gameId, final int turn, final PhaseType phase, final String play, final Map<String, String> predicted) {
            this.gameId = gameId;
            this.turn = turn;
            this.phase = phase;
            this.play = play;
            this.predicted = predicted;
        }
    }

    private static final Map<Long, Pending> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    private SimAudit() { }

    private static long key(final Game game, final Player p) {
        return ((long) game.getId() << 32) | (p.getId() & 0xFFFFFFFFL);
    }

    /** Called when the AI is asked for a play: compares a pending prediction with reality, and the game with a copy of it. */
    public static void checkpoint(final Game game, final Player ai) {
        try {
            final Map<String, String> actual = signature(game);
            final Pending pending = PENDING.remove(key(game, ai));
            if (pending != null) {
                final var ph = game.getPhaseHandler();
                if (pending.turn == ph.getTurn() && pending.phase == ph.getPhase() && game.getStack().isEmpty()) {
                    compare("play", pending.play, pending.predicted, actual);
                } else {
                    count("play-unverifiable");
                }
            }
            if (game.getStack().isEmpty()) {
                final GameSimulator sim = new GameSimulator(game, ai);
                compare("copy", "", actual, signature(sim.getSimulatedGameState()));
            }
        } catch (final RuntimeException e) {
            count("audit-error: " + e);
        }
    }

    /** Called with the play the AI chose (in the real game): predicts its result in a copy. */
    public static void afterChoice(final Game game, final Player ai, final List<SpellAbility> chosen) {
        PENDING.remove(key(game, ai));
        if (chosen == null || chosen.isEmpty() || !game.getStack().isEmpty()) {
            return;
        }
        final SpellAbility sa = chosen.get(0);
        if (sa.isLandAbility()) {
            return; // nothing to resolve
        }
        final String play = sa.getHostCard() + ": " + sa.getDescription();
        try {
            final GameSimulator sim = new GameSimulator(game, ai);
            if (!sim.applySpellAbility(sa)) {
                count("play-not-applicable");
                example("play-not-applicable", play);
                return;
            }
            final var ph = game.getPhaseHandler();
            PENDING.put(key(game, ai), new Pending(game.getId(), ph.getTurn(), ph.getPhase(), play, signature(sim.getSimulatedGameState())));
            count("play-predicted");
        } catch (final RuntimeException e) {
            count("play-error: " + e.getClass().getSimpleName());
            example("play-error: " + e.getClass().getSimpleName(), play + " " + e);
        }
    }

    // ---------------------------------------------------------------------------------------------- signature

    static Map<String, String> signature(final Game game) {
        final Map<String, String> sig = new LinkedHashMap<>();
        for (final Player p : game.getPlayers()) {
            final String who = "p" + p.getId();
            sig.put(who + ".life", String.valueOf(p.getLife()));
            sig.put(who + ".hand", names(p.getCardsIn(ZoneType.Hand), false));
            sig.put(who + ".library", String.valueOf(p.getCardsIn(ZoneType.Library).size()));
            sig.put(who + ".graveyard", names(p.getCardsIn(ZoneType.Graveyard), false));
            sig.put(who + ".exile", names(p.getCardsIn(ZoneType.Exile), false));
            sig.put(who + ".battlefield", names(p.getCardsIn(ZoneType.Battlefield), true));
        }
        return sig;
    }

    private static String names(final Iterable<Card> cards, final boolean detailed) {
        final List<String> list = new ArrayList<>();
        for (final Card c : cards) {
            list.add(detailed ? describe(c) : c.getName());
        }
        Collections.sort(list);
        return String.join(" ; ", list);
    }

    private static String describe(final Card c) {
        final StringBuilder sb = new StringBuilder(c.getName());
        if (c.isTapped()) {
            sb.append(" tapped");
        }
        if (c.isCreature()) {
            sb.append(' ').append(c.getNetPower()).append('/').append(c.getNetToughness());
            if (c.getDamage() > 0) {
                sb.append(" dmg").append(c.getDamage());
            }
            if (c.hasSickness()) {
                sb.append(" sick");
            }
        }
        final int counters = c.getCounters().size(); // a multiset: every counter counts
        if (counters > 0) {
            sb.append(" ctr").append(counters);
        }
        if (c.getAttachedTo() != null) {
            sb.append(" on:").append(c.getAttachedTo().getName());
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------- reporting

    private static void compare(final String kind, final String what, final Map<String, String> expected, final Map<String, String> actual) {
        count(kind + "-compared");
        boolean any = false;
        for (final Map.Entry<String, String> e : expected.entrySet()) {
            final String other = actual.get(e.getKey());
            if (!e.getValue().equals(other)) {
                any = true;
                final String field = e.getKey().substring(e.getKey().indexOf('.') + 1);
                final String name = kind + "-differs:" + field;
                count(name);
                example(name, (what.isEmpty() ? "" : what + " => ") + e.getKey() + ": [" + e.getValue() + "] vs [" + other + "]");
            }
        }
        if (any) {
            count(kind + "-compared-differing");
        }
    }

    private static void count(final String name) {
        synchronized (LOCK) {
            COUNTS.merge(name, 1, Integer::sum);
        }
    }

    private static void example(final String name, final String text) {
        synchronized (LOCK) {
            final List<String> list = EXAMPLES.computeIfAbsent(name, k -> new ArrayList<>());
            if (list.size() < EXAMPLES_PER_KIND) {
                list.add(text.length() > 400 ? text.substring(0, 400) : text);
            }
        }
    }

    /** Counts by kind, and the first examples of every kind of difference. */
    public static String summary() {
        synchronized (LOCK) {
            final StringBuilder sb = new StringBuilder("SIMAUDIT");
            for (final Map.Entry<String, Integer> e : COUNTS.entrySet()) {
                sb.append(' ').append(e.getKey()).append('=').append(e.getValue());
            }
            for (final Map.Entry<String, List<String>> e : EXAMPLES.entrySet()) {
                for (final String text : e.getValue()) {
                    sb.append("\nSIMAUDIT-EXAMPLE ").append(e.getKey()).append(": ").append(text);
                }
            }
            return sb.toString();
        }
    }

    public static void reset() {
        synchronized (LOCK) {
            COUNTS.clear();
            EXAMPLES.clear();
        }
        PENDING.clear();
    }
}
