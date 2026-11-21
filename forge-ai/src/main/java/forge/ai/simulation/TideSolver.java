package forge.ai.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Plays the High Tide turn out in a copy of the game before it is started: the loop of {@link LoopRunner} (every High
 * Tide, every land, the untaps, the finisher that the deck has: Stroke of Genius, Blue Sun's Zenith or Brain Freeze) is
 * run step by step on the copy, and when it ends with the opponent beaten (or their library empty) the AI knows it is
 * a line that wins, in one swoop, with the cards it holds. The estimate of the loop is a cheap and cautious guess;
 * this is the check that finds the turns the guess missed, and it is looked at once per state, not per question.
 */
public final class TideSolver {
    private static final int MAX_STEPS = 400;
    /** Orders of the hidden library tried, and how many of them have to win. */
    private static final int VARIANTS = 5;
    private static final int NEEDED = 2;
    private static final ThreadLocal<Boolean> RUNNING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<String, Boolean> KNOWN = new HashMap<>();
    /** What the last run saw, for tests and traces. */
    public static volatile String lastNote = "";

    private TideSolver() {
    }

    /** Whether the loop is being played out in a copy now (the loop then does not hold anything back for its estimate). */
    static boolean running() {
        return RUNNING.get();
    }

    private static String key(final Player ai) {
        final StringBuilder sb = new StringBuilder();
        sb.append(System.identityHashCode(ai.getGame())).append('|').append(ai.getGame().getPhaseHandler().getTurn()).append('|')
                .append(ai.getManaPool().totalMana());
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            sb.append('|').append(c.getName());
        }
        int untapped = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() && !c.isTapped()) {
                untapped++;
            }
        }
        return sb.append("|u").append(untapped).toString();
    }

    /** Whether playing the loop out wins the game this turn from the position as it is. */
    public static boolean wins(final Player ai) {
        if (ai == null || running()) {
            return false;
        }
        final String key = key(ai);
        synchronized (KNOWN) {
            final Boolean known = KNOWN.get(key);
            if (known != null) {
                return known;
            }
            if (KNOWN.size() > 64) {
                KNOWN.clear();
            }
        }
        boolean won = false;
        RUNNING.set(Boolean.TRUE);
        try {
            // the library is only a guess: a line that draws (Time Spiral) is played out in several orders of it, and is a
            // plan when enough of them end in a win; a line that draws nothing wins in all of them or none
            int wins = 0;
            final StringBuilder notes = new StringBuilder();
            for (int variant = 0; variant < VARIANTS; variant++) {
                final boolean variantWon = playOut(ai, variant);
                notes.append(notes.length() > 0 ? " | " : "").append("v").append(variant).append(": ").append(lastNote);
                if (variantWon) {
                    wins++;
                }
                if (wins >= NEEDED || wins + (VARIANTS - 1 - variant) < NEEDED) {
                    break;
                }
            }
            won = wins >= NEEDED;
            lastNote = wins + " of " + VARIANTS + " variants won: " + notes;
        } catch (final RuntimeException e) {
            lastNote = "solver error: " + e;
        } finally {
            RUNNING.set(Boolean.FALSE);
        }
        synchronized (KNOWN) {
            KNOWN.put(key, won);
        }
        if (VetoLog.isEnabled()) {
            // what the solver was looking at: the hand (the pieces it has or lacks), the lands, the High Tides in play
            final StringBuilder seen = new StringBuilder();
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (seen.length() > 0) {
                    seen.append(',');
                }
                seen.append(c.getName().length() > 14 ? c.getName().substring(0, 14) : c.getName());
            }
            VetoLog.note(ai, "tide-solver " + (won ? "win" : "no win") + " (" + lastNote + ") hand=[" + seen + "] lands="
                    + ai.getLandsInPlay().size() + " tides=" + LoopRunner.tideCount(ai));
        }
        return won;
    }

    private static boolean playOut(final Player ai, final int variant) {
        final Game game = ai.getGame();
        final GameSimulator sim = new GameSimulator(game, ai);
        HiddenInfoShuffler.shuffle(sim.getSimulatedGameState(), sim.getGameCopier(), ai,
                new java.util.Random(game.getPhaseHandler().getTurn() * 31L + 7919L * variant), c -> false);
        sim.releaseCopier();
        try {
            final Player simAi = sim.getSimulatedAiPlayer();
            final Game simGame = sim.getSimulatedGameState();
            final Player simOpp = simAi.getWeakestOpponent();
            if (simOpp == null) {
                return false;
            }
            for (int i = 0; i < MAX_STEPS; i++) {
                if (simGame.isGameOver() || simOpp.hasLost()
                        || simOpp.getCardsIn(ZoneType.Library).isEmpty()) {
                    lastNote = "wins after " + i + " plays";
                    return true;
                }
                final List<SpellAbility> moves = new java.util.ArrayList<>(StormSolver.movesOf(simAi));
                // the picker leaves mana abilities out, and the loop taps every land for mana
                for (final Card land : simAi.getCardsIn(ZoneType.Battlefield)) {
                    if (land.isLand() && !land.isTapped()) {
                        for (final SpellAbility ma : land.getManaAbilities()) {
                            ma.setActivatingPlayer(simAi);
                            if (ma.canPlay()) {
                                moves.add(ma);
                                break;
                            }
                        }
                    }
                }
                final SpellAbility step = LoopRunner.next(simAi, moves, s -> true);
                if (step == null) {
                    lastNote = "no win: the loop ended after " + i + " plays, hand=" + simAi.getCardsIn(ZoneType.Hand)
                            + " pool=" + simAi.getManaPool().totalMana();
                    return false;
                }
                if (!sim.applySpellAbility(step)) {
                    lastNote = "no win: " + step + " could not be played";
                    return false;
                }
            }
            lastNote = "no win: out of steps";
            return false;
        } finally {
            sim.releaseCopier();
        }
    }
}
