package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.card.mana.ManaCost;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterEnumType;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostExile;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordWithCost;
import forge.game.keyword.Keyword;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The combos a deck is built around, read once from the whole list and then fixed for the game, like {@link DeckPlan}
 * (which holds one of these).
 * <p>
 * The scan looks at every card the player owns exactly once. When it is finished ({@link #isScanComplete()}) it has
 * picked at most {@link #MAX_COMBOS} combos, strongest first; a deck can be built around one, two or three of them,
 * and no card is looked at again afterwards: during the game only the progress towards the combos is read
 * ({@link #term(Player)}), which is cheap enough for every rollout.
 * <p>
 * Only combos that the rules engine can really carry out when the normal AI plays the cards are listed:
 * <ul>
 *   <li>{@link Kind#MASS_REANIMATION}: Living End, Living Death and the like. Creatures are put into the graveyard on
 *       purpose (cycling, looting) and the payoff is cast from the library by cascade, or by suspend, once the swap
 *       is worth it;</li>
 *   <li>{@link Kind#REANIMATOR}: reanimation spells and big creatures that are better in the graveyard than in hand;</li>
 *   <li>{@link Kind#STORM}: a storm payoff and the rituals and cantrips that feed it;</li>
 *   <li>{@link Kind#CREATURE_COPY}: Splinter Twin or Kiki-Jiki with a creature that untaps or copies;</li>
 *   <li>{@link Kind#CASCADE_PAYOFF}: cascade spells whose only possible hit is the payoff;</li>
 *   <li>{@link Kind#CHEAT_FATTY}: Show and Tell, Sneak Attack, Through the Breach and the like, with huge creatures
 *       that are worth far more put into play than cast;</li>
 *   <li>{@link Kind#GO_WIDE}: token and cheap creature decks with an overrun finisher (Craterhoof Behemoth, Overrun,
 *       Triumph of the Hordes): the finisher waits in hand until the board makes it lethal.</li>
 * </ul>
 * Every number in here is a small evaluator bonus (a 2/2 creature is worth about 150) that is only given while the
 * combo is still alive, so a deck that merely contains one stray combo card is never taken for a combo deck: each
 * kind needs a minimum of pieces before it is reported.
 */
public final class ComboPlan {
    public enum Kind { MASS_REANIMATION, REANIMATOR, STORM, CREATURE_COPY, CASCADE_PAYOFF, CHEAT_FATTY, GO_WIDE,
        MILL, AFFINITY, DREDGE, MADNESS, ATOG }

    /** A deck is built around at most this many combos. */
    public static final int MAX_COMBOS = 3;

    /** The result for a deck without a combo, or for a scan that could not be done. */
    public static final ComboPlan EMPTY = new ComboPlan(Collections.<Combo>emptyList(), 0, false);

    /** A deck with fewer spells than this is never read for combos. */
    static final int MIN_SPELLS = 12;
    /** Bounds for the progress bonus, in evaluator points. */
    private static final int TERM_MAX = 500;
    private static final int TERM_MIN = -300;
    /** Share (percent) of the value of the swap that Living End style payoffs are worth while they can be reached or not. */
    private static final int SWAP_SHARE_ACCESS = 65;
    private static final int SWAP_SHARE_NO_ACCESS = 35;
    /** Share (percent) of the value of the best creature in the graveyard for a reanimator deck. */
    private static final int REANIMATE_SHARE_ACCESS = 80;
    private static final int REANIMATE_SHARE_NO_ACCESS = 35;
    /** The swap is worth casting the payoff for when it gains about a 2/2 creature and a half (evaluator points). */
    private static final int SWAP_WORTH_MIN = 220;
    private static final int ASSEMBLED_BONUS = 150;
    private static final int ONE_PIECE_BONUS = 40;

    /** AI:RemoveDeck:All says "the heuristic AI can't play this"; for a combo piece Flat MC may still try it. */
    public boolean overridesRemAi(final Card c) {
        if (c == null || !c.getRules().getAiHints().getRemAIDecks()) {
            return false;
        }
        for (final Combo combo : combos) {
            if (combo.enablers.contains(c.getName()) || combo.payoffs.contains(c.getName())) {
                return true;
            }
        }
        return false;
    }

    /** Hand attack aimed at yourself is fuel only when it throws away a big creature that a reanimation spell can use. */
    public boolean selfDiscardWanted(final Player ai) {
        final Combo re = find(Kind.REANIMATOR);
        if (re == null || ai == null || ai.getLife() <= 4) {
            return false; // Thoughtseize costs 2 life
        }
        boolean big = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            big |= c.isCreature() && isBigTarget(c);
        }
        return big && reanimationReady(ai, re);
    }

    static boolean isTargetedDiscard(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Discard && a.usesTargeting()) {
                return true;
            }
        }
        return false;
    }

    /** Where the cards of a deck are while a game is on (the sideboard is not part of the deck). */
    static final List<ZoneType> DECK_ZONES = Collections.unmodifiableList(Arrays.asList(ZoneType.Battlefield,
            ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand, ZoneType.Exile, ZoneType.Command));

    /** One combo the deck is built around. */
    public static final class Combo {
        public final Kind kind;
        /** 0..100, how complete and how redundant the combo is in the deck. */
        public final int strength;
        /** What the combo wins or swings the game with. */
        final Set<String> payoffs;
        /** Cards that set the combo up (cyclers, looting, reanimation spells, rituals, the other half of a pair). */
        final Set<String> enablers;
        /** Cascade spells that can find the payoff (only when {@link #cascadeSafe}). */
        final Set<String> cascaders;
        /** Every cascade spell of the deck can only find the payoff or another cascade spell. */
        final boolean cascadeSafe;
        /** Short human readable summary, for statistics and debugging. */
        public final String detail;

        Combo(final Kind kind, final int strength, final Set<String> payoffs, final Set<String> enablers,
              final Set<String> cascaders, final boolean cascadeSafe, final String detail) {
            this.kind = kind;
            this.strength = Math.max(0, Math.min(100, strength));
            this.payoffs = payoffs;
            this.enablers = enablers;
            this.cascaders = cascaders;
            this.cascadeSafe = cascadeSafe;
            this.detail = detail;
        }

        /** Whether the card is one of the pieces of this combo. */
        public boolean isPiece(final Card c) {
            final String n = c.getName();
            return payoffs.contains(n) || enablers.contains(n) || cascaders.contains(n);
        }

        @Override
        public String toString() {
            return kind + "(" + strength + ": " + detail + ")";
        }
    }

    private final List<Combo> combos;
    private final int cardsScanned;
    private final boolean scanComplete;

    private ComboPlan(final List<Combo> combos, final int cardsScanned, final boolean scanComplete) {
        this.combos = Collections.unmodifiableList(combos);
        this.cardsScanned = cardsScanned;
        this.scanComplete = scanComplete;
    }

    // ------------------------------------------------------------------------------------------------- the scan

    /** Reads the deck of this player (every card it owns in a game zone, the sideboard excluded) once. */
    public static ComboPlan scan(final Player player) {
        try {
            return scan(deckCards(player));
        } catch (final RuntimeException e) {
            return EMPTY; // a refinement only: never let it break a game
        }
    }

    /** The cards that make up the player's deck right now, wherever they are. */
    static CardCollectionView deckCards(final Player player) {
        return CardCollection.combine(player.getCardsIn(DECK_ZONES), player.getCardsIn(ZoneType.Stack));
    }

    /**
     * Reads a list of cards once and keeps the strongest combos (at most {@link #MAX_COMBOS}). The returned plan
     * always reports {@link #isScanComplete()}: every card was looked at, whether or not a combo was found.
     */
    public static ComboPlan scan(final Iterable<Card> cards) {
        final List<Card> spells = new ArrayList<>();
        int scanned = 0;
        for (final Card c : cards) {
            scanned++;
            if (c.isLand() || c.isToken() || c.isImmutable()) {
                continue;
            }
            spells.add(c);
        }
        if (spells.size() < MIN_SPELLS) {
            return new ComboPlan(Collections.<Combo>emptyList(), scanned, true);
        }
        final List<Combo> found = new ArrayList<>();
        addIfFound(found, massReanimation(spells));
        addIfFound(found, reanimator(spells));
        addIfFound(found, storm(spells));
        addIfFound(found, creatureCopy(spells));
        addIfFound(found, cascadePayoff(spells));
        addIfFound(found, cheatFatty(spells));
        addIfFound(found, goWide(spells));
        addIfFound(found, mill(spells));
        addIfFound(found, affinity(spells));
        addIfFound(found, dredge(spells));
        addIfFound(found, madness(spells));
        addIfFound(found, atog(spells));
        // a cascade deck that finds a mass reanimation spell is that combo, not a second one
        boolean hasMass = false;
        for (final Combo c : found) {
            hasMass |= c.kind == Kind.MASS_REANIMATION;
        }
        if (hasMass) {
            found.removeIf(c -> c.kind == Kind.CASCADE_PAYOFF);
        }
        found.sort((a, b) -> Integer.compare(b.strength, a.strength));
        while (found.size() > MAX_COMBOS) {
            found.remove(found.size() - 1);
        }
        return new ComboPlan(found, scanned, true);
    }

    private static void addIfFound(final List<Combo> found, final Combo c) {
        if (c != null) {
            found.add(c);
        }
    }

    // --------------------------------------------------------------------------------------------- the questions

    /** True once every card has been read; after that the plan never changes. */
    public boolean isScanComplete() {
        return scanComplete;
    }

    /** How many cards the scan looked at (lands and tokens included). */
    public int getCardsScanned() {
        return cardsScanned;
    }

    public List<Combo> getCombos() {
        return combos;
    }

    public boolean isEmpty() {
        return combos.isEmpty();
    }

    /** The strongest combo of the deck, or null. */
    public Combo primary() {
        return combos.isEmpty() ? null : combos.get(0);
    }

    /**
     * One line on what the scan found, for the statistics and the game log: how many cards were read and what they
     * add up to ("scanned 40 cards, 2 combos: ...").
     */
    public String summary() {
        if (!scanComplete) {
            return "deck not scanned";
        }
        return "scanned " + cardsScanned + " cards, " + (combos.isEmpty() ? "no combo" : combos.size() + " combo(s): " + combos);
    }

    public boolean has(final Kind kind) {
        return find(kind) != null;
    }

    private Combo find(final Kind kind) {
        for (final Combo c : combos) {
            if (c.kind == kind) {
                return c;
            }
        }
        return null;
    }

    /** Whether the card is one of the pieces of any combo of the deck (for keeping or mulliganing a hand). */
    public boolean isKeyCard(final Card c) {
        for (final Combo combo : combos) {
            if (combo.isPiece(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether cycling (and the other ways to put creature cards into the graveyard) comes first for this player: it
     * has a mass reanimation deck and the payoff is still in its library, where cascade can find it. This is the
     * general form of "the deck plays Living End".
     */
    public boolean wantsCyclingPriority(final Player player) {
        final Combo mass = find(Kind.MASS_REANIMATION);
        return mass != null && anyNamed(player.getCardsIn(ZoneType.Library), mass.payoffs);
    }

    /** Whether the cascade spells of a mass reanimation or cascade deck can only find the payoff. */
    public boolean isCascadeSafe() {
        final Combo mass = find(Kind.MASS_REANIMATION);
        if (mass != null && mass.cascadeSafe) {
            return true;
        }
        return find(Kind.CASCADE_PAYOFF) != null;
    }

    /**
     * Whether this play puts cards into the graveyard on purpose: for a mass reanimation deck cycling and the instants
     * and sorceries that discard, loot or mill their own player; for a reanimator deck the discard and looting spells
     * (cycling a land or a spell does not put a big creature there).
     */
    public boolean isFuelPlay(final SpellAbility sa) {
        if (sa == null) {
            return false;
        }
        final Combo mad = find(Kind.MADNESS);
        if (mad != null && isDiscardOutletPlay(sa)) {
            final Player who = sa.getActivatingPlayer() != null ? sa.getActivatingPlayer() : sa.getHostCard().getController();
            if (madnessWindowOpen(who, sa) && !castableMadness(who, sa).isEmpty()) {
                return true; // the right moment, and the madness card can really be cast afterwards
            }
        }
        final boolean mass = find(Kind.MASS_REANIMATION) != null;
        if (!mass && find(Kind.REANIMATOR) == null) {
            return false;
        }
        if (mass && isCyclingLike(sa)) {
            return true;
        }
        final Card host = sa.getHostCard();
        if (host == null || !sa.isSpell() || !(host.isInstant() || host.isSorcery())) {
            return false;
        }
        if (isSelfFuelSpell(sa)) {
            return true;
        }
        Player who = sa.getActivatingPlayer();
        if (who == null) {
            who = host.getController();
        }
        return isTargetedDiscard(sa) && selfDiscardWanted(who);
    }

    /**
     * Whether the fuel plays go to the front of the line for this player right now: a mass reanimation deck whose
     * payoff is still in the library ({@link #wantsCyclingPriority}), or a reanimator deck with a reanimation spell
     * or a big creature still to be found.
     */
    public boolean fuelFirst(final Player player) {
        final Combo mad = find(Kind.MADNESS);
        if (mad != null && madnessReady(player, mad) && player.getGame().getPhaseHandler().isPlayerTurn(player)
                && player.getGame().getPhaseHandler().is(PhaseType.MAIN2)) {
            return true; // only the Main 2 outlets are brought forward
        }
        if (find(Kind.MASS_REANIMATION) != null) {
            return wantsCyclingPriority(player);
        }
        final Combo re = find(Kind.REANIMATOR);
        return re != null && (anyNamed(player.getCardsIn(ZoneType.Hand), re.payoffs)
                || anyNamed(player.getCardsIn(ZoneType.Hand), re.enablers)
                || anyNamed(player.getCardsIn(ZoneType.Library), re.payoffs));
    }

    /**
     * Whether casting the cascade spell (or the payoff itself) now swaps the creatures in the AI's favour: the swing
     * is what it gets back minus what it loses, minus the same for the opponent. Cast too early it would waste the
     * payoff on an empty graveyard, which is why the deck waits (and cycles) until this is true.
     */
    public boolean swapIsWorthIt(final Player ai) {
        final Combo mass = find(Kind.MASS_REANIMATION);
        return mass != null && ai != null && swapSwing(ai) >= SWAP_WORTH_MIN;
    }

    // ------------------------------------------------------------------------------------------ the evaluation

    /**
     * Progress of the combos of the deck in a position, in evaluator points: added to the score of a rollout so that
     * the steps before the payoff (cycling a creature, putting a big creature into the graveyard, keeping the two
     * halves of a pair) are worth something even though nothing happens yet at the end of the turn.
     */
    public int term(final Player ai) {
        if (combos.isEmpty() || ai == null) {
            return 0;
        }
        int sum = 0;
        for (final Combo combo : combos) {
            switch (combo.kind) {
                case MASS_REANIMATION:
                    sum += massReanimationTerm(ai, combo);
                    break;
                case REANIMATOR:
                    sum += reanimatorTerm(ai, combo);
                    break;
                case CREATURE_COPY:
                    sum += pairTerm(ai, combo);
                    break;
                case CHEAT_FATTY:
                    sum += cheatTerm(ai, combo);
                    break;
                case GO_WIDE:
                    sum += goWideTerm(ai, combo);
                    break;
                case MILL:
                    sum += millTerm(ai);
                    break;
                case AFFINITY:
                    sum += affinityTerm(ai, combo);
                    break;
                case DREDGE:
                    sum += dredgeTerm(ai, combo);
                    break;
                case MADNESS:
                    sum += madnessTerm(ai, combo);
                    break;
                case ATOG:
                    sum += atogTerm(ai, combo);
                    break;
                default:
                    break; // storm and cascade are carried out by the playout itself, see DeckPlan.longPlayouts
            }
        }
        return Math.max(TERM_MIN, Math.min(TERM_MAX, sum));
    }

    /**
     * What swapping the creatures is worth: the graveyards come back and the boards go away, for both players, so the
     * gain is what the AI gets back minus what it loses, minus the same for the opponent.
     */
    private int massReanimationTerm(final Player ai, final Combo combo) {
        final boolean access = payoffReachable(ai, combo);
        if (!access && !anyNamed(ai.getCardsIn(ZoneType.Library), combo.payoffs)) {
            return 0; // the payoff is gone (used, discarded or exiled): nothing left to prepare
        }
        return swapSwing(ai) * (access ? SWAP_SHARE_ACCESS : SWAP_SHARE_NO_ACCESS) / 100;
    }

    /** What the swap gains the AI in evaluator points: its graveyard minus its board, minus the same for the opponent. */
    private static int swapSwing(final Player ai) {
        final Player opp = ai.getWeakestOpponent();
        int swing = creatureValue(ai.getCardsIn(ZoneType.Graveyard)) - creatureValue(ai.getCardsIn(ZoneType.Battlefield));
        if (opp != null) {
            swing -= creatureValue(opp.getCardsIn(ZoneType.Graveyard)) - creatureValue(opp.getCardsIn(ZoneType.Battlefield));
        }
        return swing;
    }

    /** The payoff is in hand, suspended, or a cascade spell that can only find it can be cast. */
    private boolean payoffReachable(final Player ai, final Combo combo) {
        if (anyNamed(ai.getCardsIn(ZoneType.Hand), combo.payoffs)) {
            return true;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Exile)) {
            if (c.getCounters(CounterEnumType.TIME) > 0
                    && (combo.payoffs.contains(c.getName()) || combo.cascadeSafe && c.hasKeyword(Keyword.CASCADE))) {
                return true; // suspended
            }
        }
        if (!combo.cascadeSafe || !anyNamed(ai.getCardsIn(ZoneType.Library), combo.payoffs)) {
            return false;
        }
        int lands = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand()) {
                lands++;
            }
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.hasKeyword(Keyword.CASCADE) && c.getCMC() <= lands + 1) {
                return true;
            }
        }
        return false;
    }

    /** A big creature in the graveyard is worth most when a reanimation spell is ready to bring it back. */
    private int reanimatorTerm(final Player ai, final Combo combo) {
        int best = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            // a legend whose namesake is already in play and working cannot be brought back to any use
            if (c.isCreature() && isBigTarget(c) && !ComboGuard.duplicatesWorkingLegend(ai, c)) {
                best = Math.max(best, ComputerUtilCard.evaluateCreature(c));
            }
        }
        final Player opp = ai.getWeakestOpponent();
        final boolean ready = reanimationReady(ai, combo);
        if (ready && opp != null) {
            for (final Card c : opp.getCardsIn(ZoneType.Graveyard)) {
                if (c.isCreature() && isBigTarget(c) && !ComboGuard.duplicatesWorkingLegend(ai, c)) {
                    best = Math.max(best, ComputerUtilCard.evaluateCreature(c) * 3 / 4);
                }
            }
        }
        return best * (ready ? REANIMATE_SHARE_ACCESS : REANIMATE_SHARE_NO_ACCESS) / 100;
    }

    private boolean reanimationReady(final Player ai, final Combo combo) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (combo.enablers.contains(c.getName()) && isReanimationSpell(c)) {
                return true;
            }
        }
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            if (c.hasKeyword(Keyword.FLASHBACK) && isReanimationSpell(c)) {
                return true;
            }
        }
        return false;
    }

    /** Both halves of a pair in hand or in play are worth more than the two cards apart. */
    private int pairTerm(final Player ai, final Combo combo) {
        boolean engine = false;
        boolean target = false;
        for (final ZoneType z : new ZoneType[] { ZoneType.Hand, ZoneType.Battlefield }) {
            for (final Card c : ai.getCardsIn(z)) {
                if (combo.payoffs.contains(c.getName())) {
                    engine = true;
                } else if (combo.enablers.contains(c.getName())) {
                    target = true;
                }
            }
        }
        return engine && target ? ASSEMBLED_BONUS : engine || target ? ONE_PIECE_BONUS : 0;
    }

    /** A cheating spell together with a huge creature to cheat in, both in hand: the combo is one turn from happening. */
    private int cheatTerm(final Player ai, final Combo combo) {
        boolean spell = false;
        boolean fatty = false;
        int lands = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand()) {
                lands++;
            }
            if (combo.enablers.contains(c.getName())) {
                spell = true; // an engine already in play (Sneak Attack, Quicksilver Amulet) keeps working
            }
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (combo.enablers.contains(c.getName()) && c.getCMC() <= lands + 1) {
                spell = true;
            } else if (combo.payoffs.contains(c.getName()) && !ComboGuard.duplicatesWinPiece(ai, c, this)) {
                fatty = true; // not a second copy of what is already in play: that is no step closer
            }
        }
        return spell && fatty ? ASSEMBLED_BONUS : spell || fatty ? ONE_PIECE_BONUS / 2 : 0;
    }

    /** An overrun finisher is worth holding until the board makes it count: each creature it can pump is points. */
    private int goWideTerm(final Player ai, final Combo combo) {
        boolean finisher = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (combo.payoffs.contains(c.getName())) {
                finisher = true;
                break;
            }
        }
        if (!finisher) {
            return 0;
        }
        int creatures = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature()) {
                creatures++;
            }
        }
        return Math.min(160, 25 * creatures);
    }

    private static int creatureValue(final CardCollectionView cards) {
        int sum = 0;
        for (final Card c : cards) {
            if (c.isCreature()) {
                sum += ComputerUtilCard.evaluateCreature(c);
            }
        }
        return sum;
    }

    private static boolean anyNamed(final CardCollectionView cards, final Set<String> names) {
        for (final Card c : cards) {
            if (names.contains(c.getName())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ the detectors

    private static Combo massReanimation(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        final Set<String> cascaders = new LinkedHashSet<>();
        int payoffCopies = 0;
        int creatures = 0;
        int cyclers = 0;
        int looters = 0;
        int cascade = 0;
        for (final Card c : spells) {
            if (isMassPayoff(c)) {
                payoffs.add(c.getName());
                payoffCopies++;
            }
            if (c.isCreature()) {
                creatures++;
            }
            if (hasCycling(c)) {
                cyclers++;
                enablers.add(c.getName());
            } else if (isSelfFuelCard(c)) {
                looters++;
                enablers.add(c.getName());
            }
            if (c.hasKeyword(Keyword.CASCADE)) {
                cascade++;
                cascaders.add(c.getName());
            }
        }
        if (payoffs.isEmpty() || creatures < 10) {
            return null;
        }
        final boolean safe = cascade >= 2 && cascadeSafe(spells, payoffs);
        final int fuelPieces = cyclers + looters;
        if (fuelPieces < 4 && !(safe && cascade >= 3)) {
            return null;
        }
        final int strength = 25 + 3 * Math.min(fuelPieces, 12) + (safe ? 4 * Math.min(cascade, 8) : 0)
                + 5 * Math.min(payoffCopies, 4);
        return new Combo(Kind.MASS_REANIMATION, strength, payoffs, enablers, safe ? cascaders : Collections.<String>emptySet(),
                safe, "payoff=" + payoffs + ", cyclers=" + cyclers + ", looters=" + looters + ", cascade=" + cascade
                + (safe ? " (finds only the payoff)" : ""));
    }

    private static Combo mill(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        int pieces = 0;
        for (final Card c : spells) {
            if (AiCardLists.has(AiCardLists.Role.MILL_PAYOFF, c.getName())) {
                payoffs.add(c.getName());
                pieces++;
            } else if (millsOpponent(c)) {
                enablers.add(c.getName());
                pieces++;
            }
        }
        if (payoffs.isEmpty() || pieces < 8) {
            return null;
        }
        return new Combo(Kind.MILL, 20 + 3 * Math.min(pieces, 14) + 5 * Math.min(payoffs.size(), 4), payoffs, enablers,
                Collections.<String>emptySet(), false, "mill pieces=" + pieces + ", payoffs=" + payoffs);
    }

    private static boolean millsOpponent(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if (a.getApi() == ApiType.Mill && (a.usesTargeting() || a.getParamOrDefault("Defined", "").contains("Opponent"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int millTerm(final Player ai) {
        final Player opp = ai.getWeakestOpponent();
        if (opp == null) {
            return 0;
        }
        final int lib = opp.getCardsIn(ZoneType.Library).size();
        return Math.min(200, Math.max(0, (40 - lib) * 5)) + (lib <= 10 ? 100 : 0);
    }

    private static Combo affinity(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> artifacts = new LinkedHashSet<>();
        int payoffCopies = 0;
        int cheapArtifacts = 0;
        for (final Card c : spells) {
            if (c.hasKeyword(Keyword.AFFINITY)) {
                payoffs.add(c.getName());
                payoffCopies++;
            } else if (c.isArtifact() && c.getCMC() <= 2) {
                artifacts.add(c.getName());
                cheapArtifacts++;
            }
        }
        if (payoffCopies < 4 || cheapArtifacts < 12) {
            return null;
        }
        return new Combo(Kind.AFFINITY, 25 + 3 * Math.min(payoffCopies, 8) + 2 * Math.min(cheapArtifacts, 16), payoffs,
                artifacts, Collections.<String>emptySet(), false, "affinity=" + payoffCopies + ", artifacts=" + cheapArtifacts);
    }

    private int affinityTerm(final Player ai, final Combo combo) {
        int artifacts = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            artifacts += c.isArtifact() ? 1 : 0;
        }
        final boolean payoffInHand = anyNamed(ai.getCardsIn(ZoneType.Hand), combo.payoffs);
        return Math.min(180, artifacts * (payoffInHand ? 25 : 10));
    }

    private static Combo dredge(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        int dredgers = 0;
        int fuel = 0;
        for (final Card c : spells) {
            if (c.hasKeyword(Keyword.DREDGE)) {
                enablers.add(c.getName());
                dredgers++;
            } else if (isSelfFuelCard(c) || hasCycling(c)) {
                enablers.add(c.getName());
                fuel++;
            }
            if (AiCardLists.has(AiCardLists.Role.DREDGE_PAYOFF, c.getName())) {
                payoffs.add(c.getName());
            }
        }
        if (dredgers < 4 || fuel < 4 || payoffs.isEmpty()) {
            return null;
        }
        return new Combo(Kind.DREDGE, 25 + 5 * Math.min(dredgers, 8) + 3 * Math.min(fuel, 8), payoffs, enablers,
                Collections.<String>emptySet(), false, "dredge=" + dredgers + ", fuel=" + fuel + ", payoffs=" + payoffs);
    }

    private int dredgeTerm(final Player ai, final Combo combo) {
        final int lib = ai.getCardsIn(ZoneType.Library).size();
        if (lib <= 8) {
            return -200; // the deck must not dredge itself out
        }
        int sum = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            if (c.hasKeyword(Keyword.DREDGE)) {
                sum += 30;
            } else if (combo.payoffs.contains(c.getName())) {
                sum += 60;
            }
        }
        return Math.min(250, sum);
    }

    // ------------------------------------------------------------------------------------------------- madness

    /** The mana value of the madness cost of the card, or -1 when it has none the AI can read. */
    static int madnessCost(final Card c) {
        try {
            for (final KeywordInterface ki : c.getKeywords(Keyword.MADNESS)) {
                if (ki instanceof KeywordWithCost) {
                    final forge.game.cost.Cost cost = ((KeywordWithCost) ki).getCost();
                    if (cost != null) {
                        return cost.getTotalMana().getCMC();
                    }
                }
            }
        } catch (final RuntimeException e) {
            // an unreadable cost: no opinion
        }
        return -1;
    }

    /** A card that throws away another card of its controller: an outlet that turns on madness (and fuels an atog). */
    private static boolean isDiscardOutlet(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (isDiscardOutletPlay(sa)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDiscardOutletPlay(final SpellAbility sa) {
        if (sa == null || sa.isCycling()) {
            return false;
        }
        if (sa.isActivatedAbility()) {
            if (sa.getPayCosts() == null || !sa.getPayCosts().hasSpecificCostType(CostDiscard.class)) {
                return false;
            }
            final CostDiscard d = sa.getPayCosts().getCostPartByType(CostDiscard.class);
            return d != null && !d.payCostFromSource();
        }
        if (!sa.isSpell()) {
            return false;
        }
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Discard && a.getParamOrDefault("Defined", "").contains("You")) {
                return true;
            }
        }
        return false;
    }

    private static Combo madness(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        int madnessCards = 0;
        int outlets = 0;
        for (final Card c : spells) {
            if (c.hasKeyword(Keyword.MADNESS)) {
                payoffs.add(c.getName());
                madnessCards++;
            } else if (isDiscardOutlet(c)) {
                enablers.add(c.getName());
                outlets++;
            }
        }
        if (madnessCards < 3 || outlets < 3) {
            return null;
        }
        return new Combo(Kind.MADNESS, 25 + 6 * Math.min(madnessCards, 8) + 5 * Math.min(outlets, 8), payoffs, enablers,
                Collections.<String>emptySet(), false, "madness=" + madnessCards + ", outlets=" + outlets);
    }

    private static boolean hasOutlet(final Player ai, final Combo combo) {
        return anyNamed(ai.getCardsIn(ZoneType.Hand), combo.enablers)
                || anyNamed(ai.getCardsIn(ZoneType.Battlefield), combo.enablers);
    }

    /**
     * A card with madness is in the hand, a way to discard it is at hand, and there is mana for the discard and the
     * madness cost together (counting the land that can still be played).
     */
    boolean madnessReady(final Player ai, final Combo combo) {
        if (ai == null || !hasOutlet(ai, combo)) {
            return false;
        }
        int cheapest = Integer.MAX_VALUE;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.hasKeyword(Keyword.MADNESS)) {
                final int m = madnessCost(c);
                if (m >= 0) {
                    cheapest = Math.min(cheapest, m);
                }
            }
        }
        if (cheapest == Integer.MAX_VALUE) {
            return false;
        }
        return forge.ai.ComputerUtilMana.getAvailableManaEstimate(ai, true) >= cheapest + 1;
    }

    /**
     * The madness card to throw away when the cost or effect asks for a discard and it can be cast for its madness
     * cost right away: the best one that can be paid for. Null when there is none, and the normal choice is made.
     */
    public Card madnessDiscard(final Player ai, final CardCollection valid) {
        if (find(Kind.MADNESS) == null || ai == null || valid == null) {
            return null;
        }
        Card best = null;
        int bestScore = -1;
        for (final Card c : valid) {
            if (!c.hasKeyword(Keyword.MADNESS) || !canCastForMadness(ai, c)) {
                continue;
            }
            final int v = madnessValue(c);
            if (v > bestScore) { bestScore = v; best = c; }
        }
        return best;
    }

    private static int madnessValue(final Card c) {
        return c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : 100 + 20 * c.getCMC();
    }

    private static ManaCost madnessMana(final Card c) {
        try {
            for (final KeywordInterface ki : c.getKeywords(Keyword.MADNESS)) {
                if (ki instanceof KeywordWithCost) {
                    final forge.game.cost.Cost cost = ((KeywordWithCost) ki).getCost();
                    if (cost != null) {
                        return cost.getTotalMana();
                    }
                }
            }
        } catch (final RuntimeException e) {
            // unreadable: no opinion
        }
        return null;
    }

    /** The madness cost alone can be paid with the untapped sources, colours included. */
    private static boolean canCastForMadness(final Player ai, final Card c) {
        final ManaCost m = madnessMana(c);
        if (m == null) {
            return false;
        }
        if (m.isZero()) {
            return true;
        }
        final SpellAbility sa = c.getFirstSpellAbility();
        if (sa == null) {
            return false;
        }
        sa.setActivatingPlayer(ai);
        return forge.ai.ComputerUtilMana.canPayManaCost(new ManaCostBeingPaid(m), sa, ai, false);
    }

    private static boolean isInstantSpeed(final SpellAbility sa) {
        final Card h = sa.getHostCard();
        if (sa.isSpell()) {
            return h.isInstant() || h.hasKeyword(Keyword.FLASH);
        }
        return !sa.getRestrictions().isSorcerySpeed();
    }

    /** How many cards the outlet throws away (99 for "your hand"): only single discards are planned. */
    private static int discardAmount(final SpellAbility sa) {
        if (sa.isActivatedAbility() && sa.getPayCosts() != null) {
            final CostDiscard d = sa.getPayCosts().getCostPartByType(CostDiscard.class);
            if (d != null) {
                return "Hand".equals(d.getType()) ? 99 : d.getAbilityAmount(sa);
            }
        }
        return 1;
    }

    /**
     * When the AI may use a discard outlet for madness. Instant-speed outlets wait for the opponent's end step (the
     * mana would be wasted anyway, the card dodges sorcery-speed removal, and the opponent has no time to answer);
     * sorcery-speed outlets go in the AI's own Main 2. Never with something on the stack, never aimed at anything.
     */
    public boolean madnessWindowOpen(final Player ai, final SpellAbility outlet) {
        if (ai == null || outlet == null || outlet.usesTargeting() || !ai.getGame().getStack().isEmpty()) {
            return false;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (ph.isPlayerTurn(ai)) {
            return ph.is(PhaseType.MAIN2) && !isInstantSpeed(outlet);
        }
        return ph.is(PhaseType.END_OF_TURN) && isInstantSpeed(outlet);
    }

    /** Madness cards in hand whose madness cost can be paid on top of the outlet's own mana cost. */
    CardCollection castableMadness(final Player ai, final SpellAbility outlet) {
        final CardCollection out = new CardCollection();
        if (discardAmount(outlet) != 1) {
            return out;
        }
        final ManaCost outletMana = outlet.getPayCosts() == null ? ManaCost.NO_COST : outlet.getPayCosts().getTotalMana();
        outlet.setActivatingPlayer(ai);
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            final ManaCost m = c == outlet.getHostCard() || !c.hasKeyword(Keyword.MADNESS) ? null : madnessMana(c);
            if (m == null) {
                continue;
            }
            final ManaCostBeingPaid total = new ManaCostBeingPaid(outletMana);
            total.addManaCost(m);
            if (forge.ai.ComputerUtilMana.canPayManaCost(total, outlet, ai, false)) {
                out.add(c);
            }
        }
        return out;
    }

    /** A discard cost may be paid with a madness card only inside the window (see checkDiscardCost). */
    public boolean madnessDiscardAllowed(final Player ai, final SpellAbility sa) {
        return find(Kind.MADNESS) == null || madnessWindowOpen(ai, sa);
    }

    /** The outlet play that is safe and sure right now, or null. The AI plays it without asking Monte Carlo. */
    public SpellAbility sureMadnessPlay(final Player ai, final List<SpellAbility> candidates) {
        if (find(Kind.MADNESS) == null || ai == null || candidates == null) {
            return null;
        }
        SpellAbility best = null;
        int bestValue = -1;
        for (final SpellAbility sa : candidates) {
            if (sa == null || !isDiscardOutletPlay(sa)) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            if (!madnessWindowOpen(ai, sa)) {
                continue;
            }
            int value = -1;
            for (final Card c : castableMadness(ai, sa)) {
                value = Math.max(value, madnessValue(c));
            }
            if (value > bestValue && sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false)) {
                bestValue = value;
                best = sa;
            }
        }
        return best;
    }

    /** What the madness cards in the hand are worth: a lot when they can be cast cheaply, little as plain cards. */
    private int madnessTerm(final Player ai, final Combo combo) {
        final boolean outlet = hasOutlet(ai, combo);
        final boolean ready = madnessReady(ai, combo);
        int sum = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!c.hasKeyword(Keyword.MADNESS)) {
                continue;
            }
            final int value = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : 50 + 30 * c.getCMC();
            sum += value * (ready ? 45 : outlet ? 25 : 12) / 100;
        }
        return Math.min(250, sum);
    }

    // ------------------------------------------------------------------------------------------------- Psychatog

    /** A creature that grows by throwing away cards from the hand or exiling cards from the graveyard. */
    private static boolean isAtog(final Card c) {
        if (!c.isCreature()) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (!sa.isActivatedAbility() || sa.getPayCosts() == null || sa.getApi() != ApiType.Pump) {
                continue;
            }
            if (isDiscardOutletPlay(sa)) {
                return true;
            }
            if (sa.getPayCosts().hasSpecificCostType(CostExile.class)) {
                final CostExile e = sa.getPayCosts().getCostPartByType(CostExile.class);
                if (e != null && e.getFrom().contains(ZoneType.Graveyard)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Combo atog(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        int atogs = 0;
        int fillers = 0;
        for (final Card c : spells) {
            if (isAtog(c)) {
                payoffs.add(c.getName());
                atogs++;
            } else if (c.isInstant() || c.isSorcery()) {
                enablers.add(c.getName());
                fillers++;
            } else if (isSelfFuelCard(c) || hasCycling(c)) {
                enablers.add(c.getName());
                fillers++;
            }
        }
        if (atogs < 3 || fillers < 10) {
            return null;
        }
        return new Combo(Kind.ATOG, 20 + 6 * Math.min(atogs, 8) + 2 * Math.min(fillers, 15), payoffs, enablers,
                Collections.<String>emptySet(), false, "atogs=" + atogs + ", fillers=" + fillers);
    }

    /**
     * An atog turns the graveyard (two cards for +1/+1) and the hand (one card for +1/+1) into power: a full
     * graveyard is worth something even before the atog is out, and a lot while it is on the battlefield.
     */
    private int atogTerm(final Player ai, final Combo combo) {
        boolean onBoard = false;
        boolean inHand = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            onBoard |= combo.payoffs.contains(c.getName());
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            inHand |= combo.payoffs.contains(c.getName());
        }
        final int graveyard = Math.min(12, ai.getCardsIn(ZoneType.Graveyard).size());
        int sum = graveyard * (onBoard ? 10 : inHand ? 6 : 2);
        if (onBoard) {
            sum += Math.min(4, ai.getCardsIn(ZoneType.Hand).size()) * 6;
        }
        return Math.min(160, sum);
    }


    private static Combo reanimator(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>(); // the big creatures
        final Set<String> enablers = new LinkedHashSet<>();
        int reanimation = 0;
        int big = 0;
        int fuelCards = 0;
        for (final Card c : spells) {
            if (isMassPayoff(c)) {
                continue; // that is the other combo
            }
            if (isReanimationSpell(c)) {
                reanimation++;
                enablers.add(c.getName());
            } else if (c.isCreature() && isBigTarget(c)) {
                big++;
                payoffs.add(c.getName());
            } else if (isSelfFuelCard(c) || isTargetedDiscardCard(c) || hasCycling(c) && !c.isCreature()) {
                fuelCards++;
                enablers.add(c.getName());
            }
        }
        if (reanimation < 3 || big < 3) {
            return null;
        }
        final int strength = 25 + 6 * Math.min(reanimation, 8) + 4 * Math.min(big, 6) + 3 * Math.min(fuelCards, 8);
        return new Combo(Kind.REANIMATOR, strength, payoffs, enablers, Collections.<String>emptySet(), false,
                "reanimation=" + reanimation + ", big creatures=" + big + ", discard/loot=" + fuelCards);
    }

    private static Combo storm(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        int payoffCopies = 0;
        int rituals = 0;
        int cantrips = 0;
        for (final Card c : spells) {
            if (c.hasKeyword(Keyword.STORM) || AiCardLists.has(AiCardLists.Role.STORM_PAYOFF, c.getName())) {
                payoffs.add(c.getName());
                payoffCopies++;
            } else if (c.getCMC() <= 2 && !c.isCreature() && hasApi(c, ApiType.Mana)) {
                rituals++;
                enablers.add(c.getName());
            } else if (c.getCMC() <= 1 && !c.isCreature() && hasApi(c, ApiType.Draw)) {
                cantrips++;
                enablers.add(c.getName());
            }
        }
        if (payoffCopies < 2 || rituals + cantrips < 8) {
            return null;
        }
        final int strength = 25 + 4 * Math.min(rituals, 8) + 2 * Math.min(cantrips, 8) + 6 * Math.min(payoffs.size(), 4);
        return new Combo(Kind.STORM, strength, payoffs, enablers, Collections.<String>emptySet(), false,
                "payoff=" + payoffs + ", rituals=" + rituals + ", cantrips=" + cantrips);
    }

    private static Combo creatureCopy(final List<Card> spells) {
        final Set<String> engines = new LinkedHashSet<>();
        final Set<String> targets = new LinkedHashSet<>();
        int targetCopies = 0;
        for (final Card c : spells) {
            if (AiCardLists.has(AiCardLists.Role.COPY_ENGINE, c.getName())) {
                engines.add(c.getName());
            } else if (AiCardLists.has(AiCardLists.Role.COPY_TARGET, c.getName())) {
                targets.add(c.getName());
                targetCopies++;
            }
        }
        if (engines.isEmpty() || targetCopies < 2) {
            return null;
        }
        return new Combo(Kind.CREATURE_COPY, 30 + 8 * Math.min(targetCopies, 6) + 8 * engines.size(), engines, targets,
                Collections.<String>emptySet(), false, "engine=" + engines + ", targets=" + targets);
    }

    private static Combo cascadePayoff(final List<Card> spells) {
        final Set<String> cascaders = new LinkedHashSet<>();
        int cascade = 0;
        int highest = 0;
        for (final Card c : spells) {
            if (c.hasKeyword(Keyword.CASCADE)) {
                cascade++;
                cascaders.add(c.getName());
                highest = Math.max(highest, c.getCMC());
            }
        }
        if (cascade < 4) {
            return null;
        }
        // what the biggest cascade spell can find: every other spell of the deck that is cheaper
        final Set<String> hits = new LinkedHashSet<>();
        for (final Card c : spells) {
            if (!c.hasKeyword(Keyword.CASCADE) && c.getCMC() < highest) {
                hits.add(c.getName());
            }
        }
        if (hits.isEmpty() || hits.size() > 2) {
            return null;
        }
        return new Combo(Kind.CASCADE_PAYOFF, 20 + 5 * Math.min(cascade, 8), hits, Collections.<String>emptySet(),
                cascaders, true, "cascade=" + cascade + ", finds only " + hits);
    }

    private static Combo cheatFatty(final List<Card> spells) {
        final Set<String> fatties = new LinkedHashSet<>();
        final Set<String> cheaters = new LinkedHashSet<>();
        int fattyCopies = 0;
        int cheatCopies = 0;
        for (final Card c : spells) {
            if (isCheatSpell(c)) {
                cheaters.add(c.getName());
                cheatCopies++;
            } else if (c.isCreature() && (AiCardLists.has(AiCardLists.Role.CHEAT_TARGET, c.getName()) || c.getCMC() >= 7)) {
                fatties.add(c.getName());
                fattyCopies++;
            }
        }
        if (cheatCopies < 3 || fattyCopies < 3) {
            return null;
        }
        return new Combo(Kind.CHEAT_FATTY, 25 + 6 * Math.min(cheatCopies, 8) + 4 * Math.min(fattyCopies, 6), fatties,
                cheaters, Collections.<String>emptySet(), false, "cheat=" + cheaters + ", fatties=" + fatties);
    }

    /** A spell or permanent that puts creatures from hand onto the battlefield without paying for them. */
    static boolean isCheatSpell(final Card c) {
        if (c.isLand()) {
            return false;
        }
        if (AiCardLists.has(AiCardLists.Role.CHEAT_SPELL, c.getName())) {
            return true; // by name first: Elvish Piper is a creature
        }
        if (c.isCreature()) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if (a.getApi() == ApiType.ChangeZone && a.getParamOrDefault("Origin", "").equals("Hand")
                        && a.getParamOrDefault("Destination", "").equals("Battlefield")
                        && a.getParamOrDefault("ChangeType", "").contains("Creature")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Combo goWide(final List<Card> spells) {
        final Set<String> finishers = new LinkedHashSet<>();
        final Set<String> bodies = new LinkedHashSet<>();
        int finisherCopies = 0;
        int cheapBodies = 0;
        for (final Card c : spells) {
            if (AiCardLists.has(AiCardLists.Role.OVERRUN, c.getName()) || isOverrun(c)) {
                finishers.add(c.getName());
                finisherCopies++;
            } else if (c.getCMC() <= 3 && (c.isCreature() || hasApi(c, ApiType.Token))) {
                bodies.add(c.getName());
                cheapBodies++;
            }
        }
        if (finisherCopies < 2 || cheapBodies < 16) {
            return null;
        }
        return new Combo(Kind.GO_WIDE, 20 + 5 * Math.min(finisherCopies, 6) + Math.min(cheapBodies, 24), finishers,
                bodies, Collections.<String>emptySet(), false, "finisher=" + finishers + ", bodies=" + cheapBodies);
    }

    /** A spell that gives every creature of its controller a big bonus. */
    static boolean isOverrun(final Card c) {
        if (c.isLand() || c.isCreature() && c.getCMC() < 5) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if (a.getApi() == ApiType.PumpAll && a.getParamOrDefault("ValidCards", "").contains("YouCtrl")
                        && a.getParamOrDefault("NumAtt", "").matches("\\+?[3-9]|\\+?\\d{2,}|X|\\+X")) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------ effect choices

    /**
     * The card an effect should take for the combos of this deck, or null when the deck has no opinion: the win card
     * of a cheating spell (Show and Tell puts the huge creature onto the battlefield, never a land), the best card to
     * put into the graveyard when searching for it (Entomb, Buried Alive).
     */
    public Card choose(final Player ai, final ZoneType destination, final List<ZoneType> origin,
                       final CardCollectionView options) {
        if (combos.isEmpty() || options == null || options.isEmpty()) {
            return null;
        }
        Card best = null;
        int bestValue = Integer.MIN_VALUE;
        if (destination == ZoneType.Battlefield && origin.contains(ZoneType.Hand)) {
            Card land = null;
            for (final Card c : options) {
                if (c.isLand()) {
                    land = land == null ? c : land; // a land is never what a cheating spell is for: only when nothing else is left
                    continue;
                }
                if (ComboGuard.duplicatesWinPiece(ai, c, this)) {
                    continue; // a legend that is already in play, a second Omniscience: it would add nothing
                }
                final int v = cheatValue(c);
                if (v > bestValue) {
                    bestValue = v;
                    best = c;
                }
            }
            return best != null ? best : land;
        }
        if (destination == ZoneType.Graveyard && origin.contains(ZoneType.Library) && wantsGraveyardFuel()) {
            for (final Card c : options) {
                if (!c.isCreature()) {
                    continue;
                }
                // a legend that is already in play and working is no use in the graveyard either
                final int v = (isBigTarget(c) && !ComboGuard.duplicatesWorkingLegend(ai, c) ? 1000 : 0)
                        + ComputerUtilCard.evaluateCreature(c);
                if (v > bestValue) {
                    bestValue = v;
                    best = c;
                }
            }
            return best;
        }
        return null;
    }

    private int cheatValue(final Card c) {
        int v = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : 50 + 30 * c.getCMC();
        for (final Combo combo : combos) {
            if ((combo.kind == Kind.CHEAT_FATTY || combo.kind == Kind.REANIMATOR) && combo.payoffs.contains(c.getName())) {
                v += 1000; // the win card of the deck
            }
        }
        if (c.isCreature() && isBigTarget(c)) {
            v += 300;
        }
        return v;
    }

    private boolean wantsGraveyardFuel() {
        return find(Kind.MASS_REANIMATION) != null || find(Kind.REANIMATOR) != null;
    }

    /**
     * What to throw away when the player has to discard (a looting spell, an outlet, the cleanup step) for a deck that
     * wants creatures in the graveyard: the huge creatures of a reanimator deck first, the best creatures of a mass
     * reanimation deck, and never the payoff, a reanimation spell or a cascade spell. Null when the deck has no
     * opinion and the normal choice should be made.
     */
    public CardCollection preferredDiscards(final Player ai, final CardCollection valid, final int min) {
        if (min <= 0 || valid == null || valid.size() < min || !wantsGraveyardFuel()) {
            return null;
        }
        final List<Card> ranked = new ArrayList<>(valid);
        final java.util.Map<Card, Integer> score = new java.util.HashMap<>();
        for (final Card c : valid) {
            score.put(c, discardScore(ai, c));
        }
        ranked.sort((a, b) -> Integer.compare(score.get(b), score.get(a)));
        if (score.get(ranked.get(Math.min(min, ranked.size()) - 1)) <= 0) {
            return null; // the cards to throw away are not clearly better than the rest: no opinion
        }
        return new CardCollection(ranked.subList(0, min));
    }

    /**
     * The cards of the list without the ones the deck cannot do without while they are in the hand (the payoff, a
     * reanimation spell, a cascade spell): what a cost that exiles or throws away cards must leave alone.
     */
    public CardCollection withoutProtected(final CardCollection cards) {
        if (combos.isEmpty()) {
            return cards;
        }
        final CardCollection safe = new CardCollection();
        for (final Card c : cards) {
            if (!(c.isInZone(ZoneType.Hand) && isProtected(c))) {
                safe.add(c);
            }
        }
        return safe;
    }

    private boolean isProtected(final Card c) {
        for (final Combo combo : combos) {
            final String n = c.getName();
            if (combo.kind == Kind.MADNESS || combo.kind == Kind.ATOG) {
                continue; // a madness card is meant to be discarded; the atog's fuel is its hand
            }
            if (combo.payoffs.contains(n) && !(c.isCreature() && isBigTarget(c)) || combo.cascaders.contains(n)
                    || combo.kind == Kind.REANIMATOR && combo.enablers.contains(n) && isReanimationSpell(c)) {
                return true;
            }
        }
        return false;
    }

    private int discardScore(final Player ai, final Card c) {
        if (isProtected(c)) {
            return -1000; // the payoff, a reanimation spell or a cascade spell: the deck needs it in hand
        }
        if (c.isLand()) {
            int lands = 0;
            for (final Card x : ai.getCardsIn(ZoneType.Battlefield)) {
                if (x.isLand()) {
                    lands++;
                }
            }
            return lands >= 5 ? 15 : -200;
        }
        if (c.isCreature()) {
            if (find(Kind.REANIMATOR) != null && isBigTarget(c)) {
                return 1000 + ComputerUtilCard.evaluateCreature(c);
            }
            if (find(Kind.MASS_REANIMATION) != null) {
                return 100 + ComputerUtilCard.evaluateCreature(c); // the swap brings it back under the AI's control
            }
        }
        return 0;
    }

    /** Every cascade spell can find only the payoff or another cascade spell. */
    private static boolean cascadeSafe(final List<Card> spells, final Set<String> payoffs) {
        boolean any = false;
        for (final Card cas : spells) {
            if (!cas.hasKeyword(Keyword.CASCADE)) {
                continue;
            }
            any = true;
            final int limit = cas.getCMC();
            for (final Card o : spells) {
                if (o.getCMC() < limit && !payoffs.contains(o.getName()) && !o.hasKeyword(Keyword.CASCADE)) {
                    return false;
                }
            }
        }
        return any;
    }

    // ----------------------------------------------------------------------------------------- card questions

    /** Living End, Living Death and the like: by name, by the card script's own marks, and by what the spell does. */
    static boolean isMassPayoff(final Card c) {
        if (c.isLand() || !(c.isInstant() || c.isSorcery())) {
            return false;
        }
        if (AiCardLists.has(AiCardLists.Role.MASS_PAYOFF, c.getName())) {
            return true;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            final String logic = sa.getParamOrDefault("AILogic", "");
            if (logic.equals("LivingDeath") || logic.equals("ReanimateAll")) {
                return true;
            }
        }
        return false;
    }

    static boolean isReanimationSpell(final Card c) {
        if (c.isLand() || isMassPayoff(c)) {
            return false;
        }
        if (AiCardLists.has(AiCardLists.Role.REANIMATION_SPELL, c.getName())) {
            return true;
        }
        if (c.isCreature() || !(c.isInstant() || c.isSorcery())) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if (a.getApi() == ApiType.ChangeZone && a.getParamOrDefault("Origin", "").contains("Graveyard")
                        && a.getParamOrDefault("Destination", "").equals("Battlefield")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A creature that is worth more brought back than cast: a big one, or one of the well known targets. */
    static boolean isBigTarget(final Card c) {
        return AiCardLists.has(AiCardLists.Role.BIG_TARGET, c.getName()) || c.getCMC() >= 6 || c.getNetPower() >= 6;
    }

    private static boolean hasCycling(final Card c) {
        if (c.hasKeyword(Keyword.CYCLING) || c.hasKeyword(Keyword.TYPECYCLING)) {
            return true;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (isCyclingLike(sa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cycling, or an ability that does the same without the keyword (a cost that discards the card itself, like
     * Waker of Waves): the card goes to the graveyard and something is drawn or found.
     */
    static boolean isCyclingLike(final SpellAbility sa) {
        if (sa == null) {
            return false;
        }
        if (sa.isCycling()) {
            return true;
        }
        if (!sa.isActivatedAbility() || sa.getPayCosts() == null || !sa.getPayCosts().hasSpecificCostType(CostDiscard.class)) {
            return false;
        }
        final CostDiscard d = sa.getPayCosts().getCostPartByType(CostDiscard.class);
        return d != null && d.payCostFromSource();
    }

    /** An instant or sorcery that discards, loots or mills its own controller. */
    private static boolean isSelfFuelCard(final Card c) {
        if (c.isLand() || !(c.isInstant() || c.isSorcery())) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isSpell() && isSelfFuelSpell(sa)) {
                return true;
            }
        }
        return false;
    }
    private static boolean isTargetedDiscardCard(final Card c) {
        if (c.isLand() || !(c.isInstant() || c.isSorcery())) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isSpell() && isTargetedDiscard(sa)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSelfFuelSpell(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            final ApiType api = a.getApi();
            if (api == ApiType.Discard && a.getParamOrDefault("Defined", "").contains("You")
                    || api == ApiType.Mill && a.getParamOrDefault("Defined", "").contains("You")
                    || api == ApiType.ChangeZone && "Library".equals(a.getParamOrDefault("Origin", ""))
                            && "Graveyard".equals(a.getParamOrDefault("Destination", ""))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasApi(final Card c, final ApiType api) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return true;
            }
        }
        return false;
    }


    /**
     * Whether the card is a win card of the deck: the big creature of a reanimator or cheating deck, the engine of a
     * copy deck, or one of the well known huge creatures. Used to keep a second copy out of play.
     */
    public boolean isWinCard(final Card c) {
        final String n = c.getName();
        for (final Combo combo : combos) {
            if ((combo.kind == Kind.CHEAT_FATTY || combo.kind == Kind.REANIMATOR || combo.kind == Kind.CREATURE_COPY)
                    && combo.payoffs.contains(n)) {
                return true;
            }
        }
        return AiCardLists.has(AiCardLists.Role.BIG_TARGET, n) || AiCardLists.has(AiCardLists.Role.CHEAT_TARGET, n);
    }

    /** Names of every card that is a piece of one of the combos. */
    public Set<String> pieceNames() {
        final Set<String> all = new LinkedHashSet<>();
        for (final Combo c : combos) {
            all.addAll(c.payoffs);
            all.addAll(c.enablers);
            all.addAll(c.cascaders);
        }
        return all;
    }

    @Override
    public String toString() {
        return combos.toString();
    }
}
