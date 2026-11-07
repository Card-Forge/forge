package forge.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * A cap on how often the AI activates the same ability in one turn when the card that has it keeps coming back as a new
 * object (Felidar Guardian blinking Saheeli Rai, so that every {@code -2} is a first activation of a fresh planeswalker).
 * {@link SpellAbility#getActivationsThisTurn()} starts again at zero each time, so the AI went on until the game ran out
 * of memory. The turn, the player, the card name and the text of the ability make the key.
 */
public final class RunawayLoop {
    /** Enough hasty copies to win with: a loop that has not won after this many is not going to. */
    public static final int LIMIT = 40;

    private static final Map<Game, Map<String, Integer>> COUNTS = new WeakHashMap<>();

    private RunawayLoop() {
    }

    private static String key(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        return game.getPhaseHandler().getTurn() + "|" + ai.getId() + "|" + sa.getHostCard().getName() + "|"
                + sa.getDescription();
    }

    /** Notes that the AI is activating the ability. */
    public static void note(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || sa.getHostCard() == null || !sa.isActivatedAbility()) {
            return;
        }
        synchronized (COUNTS) {
            final Map<String, Integer> turn = COUNTS.computeIfAbsent(ai.getGame(), g -> new HashMap<>());
            if (turn.size() > 500) {
                turn.clear(); // keys of past turns
            }
            turn.merge(key(ai, sa), 1, Integer::sum);
        }
    }

    /** Whether the AI has activated the ability this turn as often as it is going to. */
    public static boolean exceeded(final SpellAbility sa) {
        final Player ai = sa == null ? null : sa.getActivatingPlayer();
        if (ai == null || sa.getHostCard() == null || !sa.isActivatedAbility()) {
            return false;
        }
        synchronized (COUNTS) {
            final Map<String, Integer> turn = COUNTS.get(ai.getGame());
            return turn != null && turn.getOrDefault(key(ai, sa), 0) >= LIMIT;
        }
    }
}
