package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Elves decks (Glimpse of Nature, Heritage Druid, Nettle Sentinel, Elvish Visionary, Craterhoof Behemoth).
 * <p>
 * Glimpse of Nature draws a card for every creature spell of the turn, so the turn is one long chain: Glimpse, then every
 * creature of the hand in the order that pays for the next (the cheap ones that draw or make mana first), each one
 * drawing the next, until Craterhoof Behemoth is in the hand and the creatures in play are enough for it to be lethal.
 * Nothing is cast before Glimpse, or the cards it would have drawn are lost; and Glimpse is held in the hand until a
 * copy of the game, played through with the library shuffled, shows the chain ending in a lethal Craterhoof.
 * (A Glimpse deck without Craterhoof has no chain to solve: Glimpse still goes first, ahead of the creatures.)
 * Craterhoof is never wasted on a board that cannot kill: the chain goes on, and each creature is one more point for
 * every attacker.
 */
public final class ElfLoop {
    private static final String GLIMPSE = "Glimpse of Nature";
    private static final String CRATERHOOF = "Craterhoof Behemoth";
    private static final int MAX_STEPS = 200;
    private static final int VARIANTS = 4;
    private static final int NEEDED = 2;
    private static final ThreadLocal<Boolean> RUNNING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<String, Boolean> KNOWN = new HashMap<>();
    /** What the last solver run saw, for tests and traces. */
    public static volatile String lastNote = "";

    private ElfLoop() {
    }

    static boolean running() {
        return RUNNING.get();
    }

    /** A deck with Glimpse of Nature among the cards the AI owns (Craterhoof Behemoth, Mirror Entity, Grapeshot, ... finish it). */
    public static boolean isElfDeck(final Player ai) {
        return owns(ai, GLIMPSE);
    }

    private static boolean owns(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Battlefield,
                ZoneType.Sideboard)) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean glimpseActive(final Player ai) {
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Command)) {
            if (c.getName().startsWith(GLIMPSE)) {
                return true;
            }
        }
        return false;
    }

    /** The damage of the creatures that can attack, with the blockers' toughness taken off (trample). */
    private static int damage(final Player ai, final Player opp, final int bonus, final boolean hoofToCome) {
        int dmg = 0;
        for (final Card c : ai.getCreaturesInPlay()) {
            if (c.isTapped() || c.isSick() && !c.hasKeyword(Keyword.HASTE)) {
                continue;
            }
            dmg += Math.max(0, c.getNetPower()) + bonus;
        }
        if (hoofToCome) {
            dmg += 5 + bonus; // Craterhoof itself, with haste
        }
        int blocked = 0;
        for (final Card b : opp.getCreaturesInPlay()) {
            if (!b.isTapped()) {
                blocked += Math.max(0, b.getNetToughness());
            }
        }
        return dmg - blocked;
    }

    /** Whether Craterhoof cast now would kill: every creature gets +X/+X for the X creatures there will be. */
    static boolean lethalWithHoof(final Player ai, final Player opp) {
        final int x = ai.getCreaturesInPlay().size() + 1;
        return damage(ai, opp, x, true) >= opp.getLife();
    }

    /** Whether the attack with the creatures now (Craterhoof in play, its trigger resolved) kills. */
    static boolean lethalNow(final Player ai, final Player opp) {
        return damage(ai, opp, 0, false) >= opp.getLife();
    }

    /** The creature spells of the hand, the cheap ones first (the ones that draw or make mana are cheap). */
    private static List<SpellAbility> creatureSpells(final Player ai, final List<SpellAbility> candidates) {
        final List<SpellAbility> out = new ArrayList<>();
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isSpell() && !sa.isLandAbility() && host.isCreature() && host.isInZone(ZoneType.Hand)
                    && ai.equals(host.getController()) && !host.getName().equals(CRATERHOOF)
                    && !(host.getType().isLegendary() && LoopRunner.inPlay(ai, host.getName()) != null)) {
                out.add(sa);
            }
        }
        out.sort(Comparator.comparingInt((SpellAbility s) -> s.getHostCard().getCMC())
                .thenComparing(s -> s.getHostCard().getName()));
        return out;
    }

    private static int creaturesInHand(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isCreature() && !c.getName().equals(CRATERHOOF)) {
                n++;
            }
        }
        return n;
    }

    /** Whether Glimpse of Nature can be paid for now, with the mana the AI has before any creature is cast. */
    private static boolean glimpseAffordable(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(GLIMPSE)) {
                final SpellAbility sa = c.getFirstSpellAbility();
                if (sa != null) {
                    sa.setActivatingPlayer(ai);
                    return ComputerUtilMana.canPayManaCost(sa, ai, 0, false);
                }
            }
        }
        return false;
    }

    /**
     * Glimpse of Nature first, win or not: every creature cast after it draws a card, every one cast before it does
     * not, so with two creatures in the hand it is card advantage even when the chain does not end in a Craterhoof.
     * (The chain that does is {@link #wins}; this is the rule for the turns where it does not.)
     */
    private static boolean glimpseFirst(final Player ai) {
        if (creaturesInHand(ai) < 2 || ai.getCardsIn(ZoneType.Library).size() <= 3 || !glimpseAffordable(ai)) {
            return false;
        }
        // Glimpse and the cheapest creature behind it: a Glimpse that leaves nothing to cast is a turn lost
        int cheapest = Integer.MAX_VALUE;
        int glimpse = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(GLIMPSE)) {
                glimpse = c.getCMC();
            } else if (c.isCreature() && !c.getName().equals(CRATERHOOF)) {
                cheapest = Math.min(cheapest, c.getCMC());
            }
        }
        return cheapest != Integer.MAX_VALUE
                && ComputerUtilMana.getAvailableManaEstimate(ai) >= glimpse + cheapest;
    }

    /**
     * Whether the creature spell is kept in the hand: Glimpse of Nature is in the hand, not cast yet, and the chain is
     * a win. Every creature cast before Glimpse would be a card it does not draw.
     */
    public static boolean held(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !sa.isSpell() || !host.isCreature() || !host.isInZone(ZoneType.Hand)
                || !ai.equals(host.getController()) || running() || glimpseActive(ai)
                || !LoopRunner.inHand(ai, GLIMPSE) || !isElfDeck(ai)) {
            return false;
        }
        return glimpseFirst(ai) || wins(ai);
    }

    /**
     * The next play of the chain, or null. Glimpse of Nature when the chain wins; then Craterhoof Behemoth as soon as it
     * is lethal, else the next creature of the hand.
     */
    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Player opp,
                             final Predicate<SpellAbility> approve) {
        if (!isElfDeck(ai)) {
            return null;
        }
        if (!glimpseActive(ai)) {
            final SpellAbility glimpse = LoopRunner.castFromHand(ai, candidates, GLIMPSE);
            if (glimpse != null && (running() || glimpseFirst(ai) || wins(ai))) {
                glimpse.setActivatingPlayer(ai);
                glimpse.resetTargets();
                return approve.test(glimpse) ? glimpse : null;
            }
            return null;
        }
        final SpellAbility hoof = LoopRunner.castFromHand(ai, candidates, CRATERHOOF);
        if (hoof != null && lethalWithHoof(ai, opp)) {
            hoof.setActivatingPlayer(ai);
            if (approve.test(hoof)) {
                return hoof;
            }
        }
        if (ai.getCardsIn(ZoneType.Library).size() < 2) {
            return null; // not the last cards of the library: the draws would lose the game
        }
        for (final SpellAbility sa : creatureSpells(ai, candidates)) {
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (approve.test(sa)) {
                return sa;
            }
        }
        return null;
    }

    /** Whether playing the chain out in a copy of the game ends in a lethal Craterhoof. */
    public static boolean wins(final Player ai) {
        if (ai == null || running() || !LoopRunner.inHand(ai, GLIMPSE) || !owns(ai, CRATERHOOF)) {
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
            int wins = 0;
            for (int variant = 0; variant < VARIANTS; variant++) {
                if (playOut(ai, variant)) {
                    wins++;
                }
                if (wins >= NEEDED || wins + (VARIANTS - 1 - variant) < NEEDED) {
                    break;
                }
            }
            won = wins >= NEEDED;
        } catch (final RuntimeException e) {
            lastNote = "solver error: " + e;
        } finally {
            RUNNING.set(Boolean.FALSE);
        }
        synchronized (KNOWN) {
            KNOWN.put(key, won);
        }
        if (VetoLog.isEnabled()) {
            final StringBuilder seen = new StringBuilder();
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (seen.length() > 0) {
                    seen.append(',');
                }
                seen.append(c.getName().length() > 14 ? c.getName().substring(0, 14) : c.getName());
            }
            VetoLog.note(ai, "elf-solver " + (won ? "win" : "no win") + " (" + lastNote + ") hand=[" + seen + "] creatures="
                    + ai.getCreaturesInPlay().size() + " mana=" + ComputerUtilMana.getAvailableManaEstimate(ai)
                    + " library=" + ai.getCardsIn(ZoneType.Library).size());
        }
        return won;
    }

    private static String key(final Player ai) {
        final StringBuilder sb = new StringBuilder();
        sb.append(System.identityHashCode(ai.getGame())).append('|').append(ai.getGame().getPhaseHandler().getTurn());
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            sb.append('|').append(c.getName());
        }
        int untapped = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isTapped()) {
                untapped++;
            }
            sb.append('/').append(c.getName());
        }
        return sb.append("|u").append(untapped).toString();
    }

    private static boolean playOut(final Player ai, final int variant) {
        final Game game = ai.getGame();
        final GameSimulator sim = new GameSimulator(game, ai);
        HiddenInfoShuffler.shuffle(sim.getSimulatedGameState(), sim.getGameCopier(), ai,
                new java.util.Random(game.getPhaseHandler().getTurn() * 37L + 6007L * variant), c -> false);
        sim.releaseCopier();
        try {
            final Player simAi = sim.getSimulatedAiPlayer();
            final Game simGame = sim.getSimulatedGameState();
            final Player simOpp = simAi.getWeakestOpponent();
            if (simOpp == null) {
                return false;
            }
            for (int i = 0; i < MAX_STEPS; i++) {
                if (LoopRunner.inPlay(simAi, CRATERHOOF) != null && lethalNow(simAi, simOpp)) {
                    lastNote = "wins after " + i + " plays";
                    return true;
                }
                final List<SpellAbility> moves = new ArrayList<>(StormSolver.movesOf(simAi));
                final SpellAbility step = next(simAi, moves, simOpp, s -> true);
                if (step == null) {
                    lastNote = "no win: the chain ended after " + i + " plays";
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

    /**
     * What a tutor (Chord of Calling, Green Sun's Zenith, Eldritch Evolution, Intuition-like effects) fetches in an Elves
     * deck: Craterhoof Behemoth once Glimpse of Nature is in hand or played, else Glimpse; then the creatures that keep the
     * chain going. Null for no opinion.
     */
    public static CardCollection picks(final Player ai, final Iterable<Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0 || !isElfDeck(ai)) {
            return null;
        }
        final boolean glimpseHere = LoopRunner.inHand(ai, GLIMPSE) || glimpseActive(ai);
        final String[] order = glimpseHere
                ? new String[] { CRATERHOOF, "Elvish Visionary", "Heritage Druid", "Nettle Sentinel", GLIMPSE }
                : new String[] { GLIMPSE, CRATERHOOF, "Elvish Visionary", "Heritage Druid", "Nettle Sentinel" };
        final CardCollection out = new CardCollection();
        for (final String name : order) {
            if (name.equals(CRATERHOOF) ? LoopRunner.inHand(ai, name) : false) {
                continue;
            }
            for (final Card c : pool) {
                if (c.getName().equals(name) && !out.contains(c)) {
                    out.add(c);
                    break;
                }
            }
            if (out.size() >= n) {
                break;
            }
        }
        return out.isEmpty() ? null : out;
    }
}
