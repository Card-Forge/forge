package forge.ai.simulation;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * A record of every play the AI's guards turn down, for finding out why it did not do something (switch on with
 * {@code -Dflatmc.veto=true}; off, every hook is a single boolean test).
 * <p>
 * A line is written to the standard error stream the first time a (turn, phase, card, reason) is seen:
 * {@code [veto] T5 MAIN2 AI Alice | Cunning Wish | spell | reason=idle-chain | at=guard}. The reason is the guard's own
 * word ("no-valid-targets", "poor-sacrifice", "duplicate-engine", ...), the site says which decision turned it down
 * (<i>guard</i>: a play of the rollouts, <i>discovery</i>: an ability that was never offered to them, <i>assembly</i>: a
 * piece of a catalogued combo). {@link #summary()} counts the reasons by card for a whole game or test run. This is a
 * development tool, not part of normal play.
 */
public final class VetoLog {
    private static volatile boolean enabled = Boolean.getBoolean("flatmc.veto");

    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();

    private VetoLog() { }

    public static boolean isEnabled() {
        return enabled;
    }

    /** For tests (and a debugger): the property sets the starting value. */
    public static void setEnabled(final boolean on) {
        enabled = on;
    }

    /** Records a refused play; a no-op unless enabled, or when there is no reason. */
    public static void refused(final Player ai, final SpellAbility sa, final String reason, final String site) {
        if (!enabled || sa == null || reason == null) {
            return;
        }
        try {
            final Card host = sa.getHostCard();
            final String card = host == null ? "?" : host.getName();
            COUNTS.merge(card + " | " + reason, 1, Integer::sum);
            String when = "";
            if (ai != null && ai.getGame() != null) {
                final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
                when = "T" + ph.getTurn() + " " + ph.getPhase() + " ";
            }
            final String line = "[veto] " + when + (ai == null ? "" : ai.getName()) + " | " + card + " | "
                    + (sa.isSpell() ? "spell" : sa.isLandAbility() ? "land" : "ability") + " | reason=" + reason + " | at=" + site;
            if (SEEN.add(line)) {
                System.err.println(line);
            }
        } catch (final RuntimeException e) {
            // a log must never break a turn
        }
    }

    /** The count of each refusal, "card | reason" per line, for the end of a game or a test. */
    public static String summary() {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, Integer> e : new TreeMap<>(COUNTS).entrySet()) {
            sb.append(e.getValue()).append("x ").append(e.getKey()).append('\n');
        }
        return sb.toString();
    }

    /** Forgets everything (between tests). */
    public static void reset() {
        SEEN.clear();
        COUNTS.clear();
    }
}
