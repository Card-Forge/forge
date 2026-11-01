package forge.ai.simulation;

import java.util.*;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.card.mana.ManaCost;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostPayLife;
import forge.game.cost.CostExile;
import forge.game.cost.CostSacrifice;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordWithCost;
import forge.game.keyword.Keyword;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
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
 *   <li>{@link Kind#TUTOR_STORM}: a storm finisher that is found by tutors (Infernal Tutor, Beseech the Mirror,
 *       Wishes, Wishclaw Talisman) and cast with fast mana (rituals, moxen, Lotus Petal, Lion's Eye Diamond); the finisher
 *       is held until the storm count makes it lethal;</li>
 *   <li>{@link Kind#CREATURE_COPY}: Splinter Twin or Kiki-Jiki with a creature that untaps or copies;</li>
 *   <li>{@link Kind#CASCADE_PAYOFF}: cascade spells whose only possible hit is the payoff;</li>
 *   <li>{@link Kind#CHEAT_FATTY}: Show and Tell, Sneak Attack, Through the Breach and the like, with huge creatures
 *       that are worth far more put into play than cast;</li>
 *   <li>{@link Kind#REPLENISH}: Replenish and the like. Enchantments are put into the graveyard on purpose (Intuition,
 *       Attunement, looting) and come back all at once, so the payoff waits until the graveyard is worth it;</li>
 *   <li>{@link Kind#GO_WIDE}: token and cheap creature decks with an overrun finisher (Craterhoof Behemoth, Overrun,
 *       Triumph of the Hordes): the finisher waits in hand until the board makes it lethal;</li>
 *   <li>{@link Kind#DREDGE}: Golgari Grave-Troll, Stinkweed Imp and kin with the cards that work from the graveyard
 *       (Bridge from Below, Ichorid, Prized Amalgam). The graveyard is the resource of the deck: the dredgers and the
 *       cards that work from there are thrown away on purpose (looting, Careful Study), the right dredger is picked
 *       for the size of the library, and Narcomoeba, which only works from the library, is kept;</li>
 *   <li>{@link Kind#DARK_DEPTHS}: Dark Depths with Vampire Hexmage or Thespian's Stage. A Dark Depths on the table
 *       that has lost its ice counters is a 20/20 flying indestructible Marit Lage, so the deck plays the land first
 *       and then the piece that removes the counters. Lands are read for this one, the other kinds only read spells.</li>
 * </ul>
 * Every number in here is a small evaluator bonus (a 2/2 creature is worth about 150) that is only given while the
 * combo is still alive, so a deck that merely contains one stray combo card is never taken for a combo deck: each
 * kind needs a minimum of pieces before it is reported.
 */
public final class ComboPlan {
    public enum Kind { MASS_REANIMATION, REANIMATOR, STORM, CREATURE_COPY, CASCADE_PAYOFF, CHEAT_FATTY, GO_WIDE,
        MILL, AFFINITY, DREDGE, MADNESS, ATOG, REPLENISH, TUTOR_STORM, DARK_DEPTHS }

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
        final List<Card> lands = new ArrayList<>();
        int scanned = 0;
        for (final Card c : cards) {
            scanned++;
            if (c.isToken() || c.isImmutable()) {
                continue;
            }
            if (c.isLand()) {
                lands.add(c); // only Dark Depths and Thespian's Stage need them
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
        addIfFound(found, tutorStorm(spells));
        addIfFound(found, creatureCopy(spells));
        addIfFound(found, cascadePayoff(spells));
        addIfFound(found, cheatFatty(spells));
        addIfFound(found, goWide(spells));
        addIfFound(found, mill(spells));
        addIfFound(found, affinity(spells));
        addIfFound(found, dredge(spells));
        addIfFound(found, madness(spells));
        addIfFound(found, atog(spells));
        addIfFound(found, replenish(spells));
        addIfFound(found, darkDepths(spells, lands));
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

    /** Whether this is the only combo the deck was found to be built around. */
    public boolean onlyHas(final Kind kind) {
        return combos.size() == 1 && combos.get(0).kind == kind;
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

    /**
     * Whether this player is playing a Living End style deck: Living End itself is still in the library (found by name,
     * so that the deck is played right even when no deck plan was read), or the scan found a mass reanimation whose
     * payoff is waiting in the library ({@link #wantsCyclingPriority}).
     */
    public boolean isMassReanimationDeck(final Player ai) {
        if (ai == null) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (c.getName().equals("Living End")) {
                return true;
            }
        }
        return wantsCyclingPriority(ai);
    }

    /** What a Living End style deck thinks of a play, ahead of the normal heuristics ({@link #massReanimationRuling}). */
    public enum Ruling {
        /** The deck has nothing to say: the normal heuristics decide. */
        NO_OPINION,
        /** Do not even look at the play: it would waste the payoff (a cascade spell before the graveyard is ready). */
        SKIP,
        /** Play it: cycling and fuel while the library can afford it, the cascade spell once the swap is worth it. */
        PLAY
    }

    /**
     * The plays of a Living End style deck ({@link #isMassReanimationDeck}), decided here and not by the general play
     * loop: cycling and the fuel plays go first while the library has 10 cards or more (and the life they cost can
     * be spared), and a cascade spell waits until the swap pays (more than four creatures in the graveyard, or
     * {@link #swapIsWorthIt}) and the board is not already full (four creatures), or the AI is in danger and has room
     * for a blocker. Only {@link Ruling#PLAY} overrides the heuristics; a cost that cannot be paid or spared is no
     * opinion, not a refusal.
     *
     * @param lifeInDanger whether {@code ComputerUtil.aiLifeInDanger} says so: slow, so asked once per decision by the
     *                     caller and not per play
     */
    public Ruling massReanimationRuling(final Player ai, final SpellAbility sa, final boolean lifeInDanger) {
        if (ai == null || sa == null || sa.getHostCard() == null) {
            return Ruling.NO_OPINION;
        }
        if ((sa.isCycling() || isFuelPlay(sa)) && sa.canCastTiming(ai)
                && ai.getCardsIn(ZoneType.Library).size() >= MASS_FUEL_MIN_LIBRARY) {
            if (!ComputerUtilCost.canPayCost(sa, ai, sa.isTrigger())) {
                return Ruling.NO_OPINION;
            }
            if (sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostPayLife.class)
                    && !ai.cantLoseForZeroOrLessLife()
                    && ai.getLife() <= sa.getPayCosts().getCostPartByType(CostPayLife.class).getAbilityAmount(sa) * 2) {
                return Ruling.NO_OPINION; // the life is needed more than the card
            }
            return Ruling.PLAY;
        }
        if (!sa.getHostCard().hasKeyword(Keyword.CASCADE)) {
            return Ruling.NO_OPINION;
        }
        final int creatures = ai.getCreaturesInPlay().size();
        if (lifeInDanger) {
            return creatures >= MASS_FULL_BOARD ? Ruling.NO_OPINION : Ruling.PLAY;
        }
        if (CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), CardPredicates.CREATURES).size() > MASS_CREATURES_READY
                || swapIsWorthIt(ai)) {
            if (creatures >= MASS_FULL_BOARD) {
                return Ruling.SKIP;
            }
            return !sa.getHostCard().isPermanent() && sa.canCastTiming(ai)
                    && ComputerUtilCost.canPayCost(sa, ai, sa.isTrigger()) ? Ruling.PLAY : Ruling.NO_OPINION;
        }
        return Ruling.SKIP;
    }

    /** Cycling and fuel are only played while the library has this many cards: the deck must not deck itself. */
    private static final int MASS_FUEL_MIN_LIBRARY = 10;
    /** More creatures than this in the graveyard make a cascade into the swap worth it. */
    private static final int MASS_CREATURES_READY = 4;
    /** With this many creatures in play the swap would cost the AI its own board. */
    private static final int MASS_FULL_BOARD = 4;

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
        final boolean dredge = find(Kind.DREDGE) != null;
        if (!mass && !dredge && find(Kind.REANIMATOR) == null) {
            return false;
        }
        if (mass && isCyclingLike(sa)) {
            return true;
        }
        if (dredge && sa.isSpell() && isDiscardCostSpell(sa)) {
            return true; // Cathartic Reunion and the like: the discard is the point, what is drawn is the bonus
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
        final Combo dredge = find(Kind.DREDGE);
        if (dredge != null && dredgeWantsFuel(player, dredge)) {
            return true; // a dredger or a card that works from the graveyard is in hand and no engine is running yet
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
                case REPLENISH:
                    sum += replenishTerm(ai, combo);
                    break;
                case TUTOR_STORM:
                    sum += tutorStormTerm(ai, combo);
                    break;
                case DARK_DEPTHS:
                    sum += darkDepthsTerm(ai);
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

    /** Dredging with fewer cards than this left in the library costs the deck more than the cards it mills are worth. */
    static final int DREDGE_SAFE_LIBRARY = 12;
    private static final int DREDGE_CARD_VALUE = 12;
    private static final int DREDGE_SHORT_PENALTY = 25;
    /** Below this many cards in the library everything in the graveyard is worth less, in proportion. */
    private static final int DREDGE_FULL_VALUE_LIBRARY = 16;

    /**
     * Progress of a dredge deck, in evaluator points. One dredger in the graveyard keeps the engine running, a second
     * and a third are only back-ups (a draw can only be replaced once), while every card that works from the
     * graveyard (Bridge from Below, Ichorid, Prized Amalgam, Dread Return) is worth its full share. A payoff that only
     * works from the library (Narcomoeba) is worth nothing in the graveyard: it cannot flip from there. A short
     * library makes the whole graveyard worth less, and 8 cards or fewer is a deck about to dredge itself out.
     */
    private int dredgeTerm(final Player ai, final Combo combo) {
        final int lib = ai.getCardsIn(ZoneType.Library).size();
        if (lib <= 8) {
            return -200; // the deck must not dredge itself out
        }
        int dredgers = 0;
        int natives = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            if (c.hasKeyword(Keyword.DREDGE)) {
                dredgers++;
            } else if (combo.payoffs.contains(c.getName()) && worksFromGraveyard(c)) {
                natives++;
            }
        }
        int sum = Math.min(dredgers, 1) * 40 + Math.max(0, Math.min(dredgers, 3) - 1) * 15 + Math.min(natives, 4) * 55;
        if (lib < DREDGE_FULL_VALUE_LIBRARY) {
            sum = sum * lib / DREDGE_FULL_VALUE_LIBRARY;
        }
        return Math.min(250, sum);
    }

    /**
     * Whether the card does its work from the graveyard: a dredge, flashback, unearth or escape card, or one with a
     * trigger, replacement effect, static ability or activated ability that is live there (Bridge from Below, Ichorid
     * and Prized Amalgam trigger from the graveyard). Read from the card, not from a list of names.
     */
    public static boolean worksFromGraveyard(final Card c) {
        if (c == null || c.isLand()) {
            return false;
        }
        if (c.hasKeyword(Keyword.DREDGE) || c.hasKeyword(Keyword.FLASHBACK) || c.hasKeyword(Keyword.UNEARTH)
                || c.hasKeyword(Keyword.ESCAPE)) {
            return true;
        }
        for (final Trigger t : c.getTriggers()) {
            if (t.getActiveZone() != null && t.getActiveZone().contains(ZoneType.Graveyard)) {
                return true;
            }
        }
        for (final ReplacementEffect r : c.getReplacementEffects()) {
            if (r.getActiveZone() != null && r.getActiveZone().contains(ZoneType.Graveyard)) {
                return true;
            }
        }
        for (final StaticAbility s : c.getStaticAbilities()) {
            if (s.getActiveZone() != null && s.getActiveZone().contains(ZoneType.Graveyard)) {
                return true;
            }
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getRestrictions() != null
                    && sa.getRestrictions().getZone() == ZoneType.Graveyard) {
                return true;
            }
        }
        return false;
    }

    /** A card a dredge deck would rather have in the graveyard than in the hand: a dredger, or a card that works from there. */
    private static boolean belongsInGraveyard(final Card c, final Combo dredge) {
        return c.hasKeyword(Keyword.DREDGE) || dredge.payoffs.contains(c.getName()) && worksFromGraveyard(c);
    }

    /** Whether a dredge deck has a reason to put cards into its graveyard before anything else this turn. */
    private boolean dredgeWantsFuel(final Player player, final Combo dredge) {
        if (player.getCardsIn(ZoneType.Library).size() <= DREDGE_SAFE_LIBRARY) {
            return false; // the library is the limit now, not the graveyard
        }
        int dredgersAway = 0;
        for (final Card c : player.getCardsIn(ZoneType.Graveyard)) {
            dredgersAway += c.hasKeyword(Keyword.DREDGE) ? 1 : 0;
        }
        if (dredgersAway >= 2) {
            return false; // the engine is running
        }
        for (final Card c : player.getCardsIn(ZoneType.Hand)) {
            if (belongsInGraveyard(c, dredge)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Which dredge card to use when several are in the graveyard. The game asks for one replacement effect and, with
     * no opinion, takes the first of the list, whatever the library holds. The bigger the dredge the more fuel it
     * mills, but every card it leaves the library short of {@link #DREDGE_SAFE_LIBRARY} costs more than that card is
     * worth, so a deep library takes Golgari Grave-Troll and a thin one Life from the Loam. Null (no opinion) for a
     * deck that is not a dredge deck and when something other than dredge is among the options.
     */
    public ReplacementEffect chooseDredge(final Player ai, final List<ReplacementEffect> options) {
        if (ai == null || options == null || options.size() < 2 || find(Kind.DREDGE) == null) {
            return null;
        }
        final int lib = ai.getCardsIn(ZoneType.Library).size();
        ReplacementEffect best = null;
        int bestScore = Integer.MIN_VALUE;
        for (final ReplacementEffect re : options) {
            if (!re.isKeyword(Keyword.DREDGE) || re.getHostCard() == null) {
                return null; // some other replacement effect is among them: the normal rules choose
            }
            final int score = dredgeChoiceScore(re.getHostCard().getKeywordMagnitude(Keyword.DREDGE), lib);
            if (score > bestScore) {
                bestScore = score;
                best = re;
            }
        }
        return best;
    }

    /** What dredging this many cards is worth with this many cards in the library; hopeless when there are too few. */
    public static int dredgeChoiceScore(final int amount, final int library) {
        if (amount <= 0 || amount > library) {
            return Integer.MIN_VALUE / 2;
        }
        final int shortfall = Math.max(0, DREDGE_SAFE_LIBRARY - (library - amount));
        return DREDGE_CARD_VALUE * amount - DREDGE_SHORT_PENALTY * shortfall;
    }

    // ------------------------------------------------------------------------------------------------ Dark Depths

    private static final String DARK_DEPTHS_NAME = "Dark Depths";
    private static final String THESPIANS_STAGE = "Thespian's Stage";
    private static final String MARIT_LAGE = "Marit Lage";
    /** A Dark Depths and the piece that removes its counters are on the table: the next play is a 20/20. */
    private static final int DEPTHS_READY = 300;

    /**
     * A spell or creature that removes every counter from a permanent (Vampire Hexmage), read from its ability.
     * Thespian's Stage is the other way to a Dark Depths with no counters, but it is a land and is named where it is
     * needed.
     */
    static boolean isCounterWiper(final Card c) {
        if (c == null || c.isLand()) {
            return false;
        }
        if (c.getName().equals("Vampire Hexmage")) {
            return true;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.RemoveCounter && "All".equals(sa.getParamOrDefault("CounterNum", ""))
                    && "All".equals(sa.getParamOrDefault("CounterType", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * A spell that puts a land from the library onto the battlefield and sacrifices a land to do it (Crop Rotation,
     * Sylvan Scrying is not one: it goes to the hand). Read from the ability, not from a name.
     */
    static boolean isLandTutor(final Card c) {
        if (c == null || c.isLand() || !(c.isInstant() || c.isSorcery())) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isSpell() && sa.getApi() == ApiType.ChangeZone && "Library".equals(sa.getParamOrDefault("Origin", ""))
                    && "Battlefield".equals(sa.getParamOrDefault("Destination", ""))
                    && sa.getParamOrDefault("ChangeType", "").contains("Land")
                    && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasIceCounters(final Card c) {
        return c.getName().equals(DARK_DEPTHS_NAME) && c.getCounters(CounterType.getType("ICE")) > 0;
    }

    private static boolean isNamed(final Iterable<Card> cards, final String name) {
        for (final Card c : cards) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Whether one of the pieces that take the counters off is in play or in hand (a Hexmage, Thespian's Stage). */
    private static boolean haveWiper(final CardCollectionView board, final CardCollectionView hand) {
        for (final CardCollectionView zone : new CardCollectionView[] { board, hand }) {
            for (final Card c : zone) {
                if (c.getName().equals(THESPIANS_STAGE) || isCounterWiper(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * What a land search should fetch for a Dark Depths deck: Dark Depths while there is none on the table or in hand
     * (it is the card the deck is built around), then Thespian's Stage once a Dark Depths with its counters is out and
     * nothing takes them off yet. Null for no opinion: the normal choice is made.
     */
    private Card depthsSearchPick(final Player ai, final CardCollectionView options) {
        if (find(Kind.DARK_DEPTHS) == null) {
            return null;
        }
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        boolean depthsOut = false;
        for (final Card c : board) {
            if (c.getName().equals(MARIT_LAGE)) {
                return null;
            }
            depthsOut |= hasIceCounters(c);
        }
        Card depths = null;
        Card stage = null;
        for (final Card c : options) {
            if (c.getName().equals(DARK_DEPTHS_NAME) && depths == null) {
                depths = c;
            } else if (c.getName().equals(THESPIANS_STAGE) && stage == null) {
                stage = c;
            }
        }
        if (!depthsOut && depths != null && !isNamed(hand, DARK_DEPTHS_NAME)) {
            return depths;
        }
        if (depthsOut && stage != null && !haveWiper(board, hand)) {
            return stage;
        }
        return null;
    }

    /**
     * Which land a Dark Depths deck plays first. The general rule scores a land by the mana it makes, so a Dark Depths
     * (no mana) waits in the hand until nothing else is left, and the Stage is played as just another colorless land.
     * Here the mana comes first (two sources, enough for a Hexmage or the Stage's own ability), then Dark Depths once a
     * piece that takes the counters off is in play or in hand, then the Stage once Dark Depths is out. Null for no
     * opinion; never for a lone land, there is nothing to choose then.
     */
    public Card chooseLand(final Player ai, final CardCollection playable) {
        if (find(Kind.DARK_DEPTHS) == null || ai == null || playable == null || playable.size() < 2) {
            return null;
        }
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        int mana = 0;
        boolean depthsOut = false;
        for (final Card c : board) {
            if (c.getName().equals(MARIT_LAGE)) {
                return null;
            }
            depthsOut |= hasIceCounters(c);
            if (c.isLand() && !c.getName().equals(THESPIANS_STAGE) && !c.getManaAbilities().isEmpty()) {
                mana++;
            }
        }
        if (mana < 2) {
            return null; // the mana first: the normal rule picks the best source
        }
        Card depths = null;
        Card stage = null;
        for (final Card c : playable) {
            if (c.getName().equals(DARK_DEPTHS_NAME) && depths == null) {
                depths = c;
            } else if (c.getName().equals(THESPIANS_STAGE) && stage == null) {
                stage = c;
            }
        }
        if (!depthsOut && depths != null && haveWiper(board, hand)) {
            return depths;
        }
        if (depthsOut && stage != null && !isNamed(board, THESPIANS_STAGE)) {
            return stage;
        }
        return null;
    }

    /**
     * What a Dark Depths deck thinks of the activated abilities that take the counters off, ahead of the normal
     * heuristics. The ability of Dark Depths itself (3 mana for one of ten counters) is ordinary as far as the general
     * rules can tell, so they pay for it while Thespian's Stage or a Hexmage sits there doing nothing: with a piece
     * that takes all of them off in play or in hand it is {@link Ruling#SKIP}. The piece itself is {@link Ruling#PLAY}
     * once a Dark Depths with its counters is out and the cost can be paid, aimed at that Dark Depths (the general
     * rules would pick any land for the Stage, or find no use for it at all: copying a legend the AI already has).
     * Everything else, and every other deck, is {@link Ruling#NO_OPINION}.
     */
    public Ruling darkDepthsRuling(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || sa.getHostCard() == null || !sa.isActivatedAbility()
                || find(Kind.DARK_DEPTHS) == null) {
            return Ruling.NO_OPINION;
        }
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        Card depths = null;
        for (final Card c : board) {
            if (c.getName().equals(MARIT_LAGE)) {
                return Ruling.NO_OPINION; // done
            }
            if (depths == null && hasIceCounters(c)) {
                depths = c;
            }
        }
        if (depths == null) {
            return Ruling.NO_OPINION;
        }
        final Card host = sa.getHostCard();
        final String api = sa.getApi() == null ? "" : sa.getApi().name();
        if (host.getName().equals(DARK_DEPTHS_NAME)) {
            return api.equals("RemoveCounter") && haveWiper(board, ai.getCardsIn(ZoneType.Hand))
                    ? Ruling.SKIP : Ruling.NO_OPINION;
        }
        final boolean stage = host.getName().equals(THESPIANS_STAGE) && api.equals("Clone");
        final boolean hexmage = isCounterWiper(host) && api.startsWith("RemoveCounter");
        if (!(stage || hexmage) || !host.isInZone(ZoneType.Battlefield) || !sa.usesTargeting() || !sa.canTarget(depths)
                || !ComputerUtilCost.canPayCost(sa, ai, false)) {
            return Ruling.NO_OPINION;
        }
        sa.resetTargets();
        sa.getTargets().add(depths);
        return Ruling.PLAY;
    }

    private static Combo darkDepths(final List<Card> spells, final List<Card> lands) {
        int depths = 0;
        int stages = 0;
        int wipers = 0;
        final Set<String> enablers = new LinkedHashSet<>();
        for (final Card c : lands) {
            if (c.getName().equals(DARK_DEPTHS_NAME)) {
                depths++;
            } else if (c.getName().equals(THESPIANS_STAGE)) {
                stages++;
                enablers.add(c.getName());
            }
        }
        int tutors = 0;
        for (final Card c : spells) {
            if (isCounterWiper(c)) {
                wipers++;
                enablers.add(c.getName());
            } else if (isLandTutor(c)) {
                tutors++;
                enablers.add(c.getName()); // Crop Rotation puts Dark Depths onto the battlefield at instant speed
            }
        }
        // a deck built around it plays a few of each: one stray Hexmage next to a lone Dark Depths is not a combo deck
        if (depths < 2 || wipers + stages < 3) {
            return null;
        }
        final Set<String> payoffs = new LinkedHashSet<>();
        payoffs.add(DARK_DEPTHS_NAME);
        return new Combo(Kind.DARK_DEPTHS, 30 + 6 * Math.min(depths, 4) + 5 * Math.min(wipers + stages, 8)
                + 2 * Math.min(tutors, 4), payoffs, enablers, Collections.<String>emptySet(), false,
                "depths=" + depths + ", hexmage=" + wipers + ", stage=" + stages + ", tutor=" + tutors);
    }

    /**
     * Progress towards Marit Lage, in evaluator points. The land comes first (it makes no mana and does nothing by
     * itself, which is why the rollouts would never see a reason to play it); then the piece that takes the counters
     * off, which is a creature that can be sacrificed at once, or Thespian's Stage with the mana to use it. When the
     * token is on the table the evaluator counts the 20/20 itself, so there is nothing left to add. The ready board
     * is worth less than the token it turns into, which is why the rollouts still go and make it.
     */
    private int darkDepthsTerm(final Player ai) {
        final CounterType ice = CounterType.getType("ICE");
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        boolean depthsOut = false;
        boolean wiperOut = false;
        boolean stageOut = false;
        int mana = 0;
        for (final Card c : board) {
            final String n = c.getName();
            if (n.equals(MARIT_LAGE)) {
                return 0;
            }
            if (n.equals(DARK_DEPTHS_NAME) && c.getCounters(ice) > 0) {
                depthsOut = true;
            } else if (n.equals(THESPIANS_STAGE)) {
                stageOut = true;
            } else if (isCounterWiper(c)) {
                wiperOut = true;
            } else if (c.isLand() && !c.getManaAbilities().isEmpty()) {
                mana++; // what pays for Hexmage or the Stage; the Stage taps for its own ability
            }
        }
        boolean wiperInHand = false;
        boolean stageInHand = false;
        boolean depthsInHand = false;
        for (final Card c : hand) {
            wiperInHand |= isCounterWiper(c);
            stageInHand |= c.getName().equals(THESPIANS_STAGE);
            depthsInHand |= c.getName().equals(DARK_DEPTHS_NAME);
        }
        if (depthsOut) {
            if (wiperOut) {
                return DEPTHS_READY;
            }
            if (stageOut && mana >= 2) {
                return DEPTHS_READY - 20;
            }
            if (wiperInHand && mana >= 2) {
                return 220;
            }
            return stageInHand || stageOut ? 120 : ONE_PIECE_BONUS;
        }
        if (depthsInHand) {
            return wiperOut || wiperInHand || stageOut || stageInHand ? 90 : 30;
        }
        boolean tutorInHand = false;
        for (final Card c : hand) {
            tutorInHand |= isLandTutor(c);
        }
        if (tutorInHand && isNamed(ai.getCardsIn(ZoneType.Library), DARK_DEPTHS_NAME)) {
            // Crop Rotation can put Dark Depths onto the table, so the piece that takes its counters off is worth more
            return wiperOut || wiperInHand || stageOut || stageInHand ? 110 : 50;
        }
        return wiperOut ? 25 : 0;
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

    /** A discard outlet play must wait: a castable madness card would be the one thrown away, and the moment is wrong. */
    public boolean outletMustWait(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null || find(Kind.MADNESS) == null || !isDiscardOutletPlay(sa)) {
            return false;
        }
        sa.setActivatingPlayer(ai);
        return !castableMadness(ai, sa).isEmpty() && !madnessWindowOpen(ai, sa);
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
        final SpellAbility sa = c.getFirstSpellAbility();
        if (sa == null) {
            return false;
        }
        sa.setActivatingPlayer(ai);
        if (!madnessHasLegalTarget(sa)) {
            return false;
        }
        if (m.isZero()) {
            return true;
        }
        return forge.ai.ComputerUtilMana.canPayManaCost(new ManaCostBeingPaid(m), sa, ai, false);
    }

    /**
     * A madness card that needs a target (a counterspell with nothing to counter, removal with nothing to hit) is
     * exiled and then put into the graveyard when the madness trigger resolves: the cast is impossible, so it must
     * never be thrown away for madness. Only asks; no target is chosen or left behind.
     */
    private static boolean madnessHasLegalTarget(final SpellAbility sa) {
        try {
            if (!sa.usesTargeting()) {
                return true;
            }
            final forge.game.spellability.TargetRestrictions tr = sa.getTargetRestrictions();
            if (tr == null) {
                return true;
            }
            if (tr.getZone().contains(ZoneType.Stack)) {
                return tr.getNumCandidates(sa) > 0; // hasCandidates says yes to every stack target
            }
            return tr.hasCandidates(sa);
        } catch (final RuntimeException e) {
            return true; // cannot tell: the old behaviour
        }
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
            final SpellAbility cast = c.getFirstSpellAbility();
            if (cast != null) {
                cast.setActivatingPlayer(ai);
                if (!madnessHasLegalTarget(cast)) {
                    continue; // nothing to aim it at: the card would only be lost
                }
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

    // ---------------------------------------------------------------------------------------- Replenish decks

    /** The graveyard has to hold about two good enchantments before the payoff is cast (Opalescence and a Parallax Wave). */
    static final int REPLENISH_WORTH_MIN = 300;
    /** Share (percent) of what the graveyard's enchantments are worth, while the payoff can be cast or only drawn. */
    private static final int REPLENISH_SHARE_ACCESS = 65;
    private static final int REPLENISH_SHARE_NO_ACCESS = 25;

    /** What an enchantment is worth on the battlefield; 0 for anything else (an Aura stays in the graveyard). */
    static int enchantmentValue(final Card c) {
        return c.isEnchantment() && !c.isAura() && !c.isLand() ? 50 + 30 * c.getCMC() : 0;
    }

    private static int graveyardEnchantmentValue(final Player ai) {
        int sum = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            sum += enchantmentValue(c);
        }
        return sum;
    }

    /** Return all enchantment cards of the graveyard to the battlefield (Replenish, and the like). */
    static boolean isEnchantmentRecursion(final Card c) {
        if (c.isLand() || c.isCreature()) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if ((a.getApi() == ApiType.ChangeZoneAll || a.getApi() == ApiType.ChangeZone)
                        && a.getParamOrDefault("Origin", "").contains("Graveyard")
                        && "Battlefield".equals(a.getParamOrDefault("Destination", ""))
                        && a.getParamOrDefault("ChangeType", "").contains("Enchantment")
                        && !a.getParamOrDefault("ChangeType", "").contains("Aura")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A way to fill the graveyard: discarding or milling yourself, cycling, or a library search that puts the cards it
     * does not take there (Intuition). Activated abilities count (Attunement draws three and discards four).
     */
    private static boolean isGraveyardFiller(final Card c) {
        if (c.isLand()) {
            return false;
        }
        if (hasCycling(c)) {
            return true;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if ("Intuition".equals(sa.getParam("AILogic"))) {
                return true;
            }
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                final ApiType api = a.getApi();
                final String who = a.getParamOrDefault("Defined", "You");
                if (api == ApiType.Discard && !a.usesTargeting() && who.contains("You")
                        || api == ApiType.Mill && !a.usesTargeting() && who.contains("You")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Combo replenish(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        int payoffCopies = 0;
        int enchantments = 0;
        int fillers = 0;
        for (final Card c : spells) {
            if (isEnchantmentRecursion(c)) {
                payoffs.add(c.getName());
                payoffCopies++;
                continue;
            }
            if (enchantmentValue(c) > 0) {
                enchantments++;
            }
            if (isGraveyardFiller(c)) {
                enablers.add(c.getName());
                fillers++;
            }
        }
        if (payoffCopies < 2 || enchantments < 8 || fillers < 4) {
            return null;
        }
        return new Combo(Kind.REPLENISH, 30 + 4 * Math.min(payoffCopies, 4) + 2 * Math.min(enchantments, 16)
                + 2 * Math.min(fillers, 10), payoffs, enablers, Collections.<String>emptySet(), false,
                "payoff=" + payoffs + ", enchantments=" + enchantments + ", fillers=" + fillers);
    }

    /** The enchantments in the graveyard are worth part of what Replenish makes of them, more while it is in hand. */
    private int replenishTerm(final Player ai, final Combo combo) {
        final boolean access = anyNamed(ai.getCardsIn(ZoneType.Hand), combo.payoffs);
        if (!access && !anyNamed(ai.getCardsIn(ZoneType.Library), combo.payoffs)) {
            return 0; // every copy is used up: nothing left to prepare
        }
        return Math.min(350, graveyardEnchantmentValue(ai) * (access ? REPLENISH_SHARE_ACCESS : REPLENISH_SHARE_NO_ACCESS) / 100);
    }

    /** Whether the payoff is held back: cast on a graveyard without its enchantments it would be a wasted card. */
    public boolean payoffMustWait(final Player ai, final SpellAbility sa) {
        final Combo rep = find(Kind.REPLENISH);
        if (rep == null || ai == null || sa == null || !sa.isSpell() || sa.getHostCard() == null
                || !rep.payoffs.contains(sa.getHostCard().getName())) {
            return false;
        }
        return graveyardEnchantmentValue(ai) < REPLENISH_WORTH_MIN;
    }

    /**
     * What to search for in a Replenish deck. Alone (a tutor that takes one card) the payoff first, then the best
     * enchantment. For Intuition, where the card the opponent leaves goes into the hand and the rest into the
     * graveyard, three different enchantments, the best first: whichever one is given, the other two are in the
     * graveyard for the payoff.
     */
    private CardCollection replenishPicks(final Player ai, final Iterable<Card> pool, final int n, final Combo combo) {
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        final List<Card> enchantments = new ArrayList<>();
        Card payoff = null;
        for (final Card c : pool) {
            if (combo.payoffs.contains(c.getName())) {
                payoff = payoff == null ? c : payoff;
            } else if (enchantmentValue(c) > 0) {
                enchantments.add(c);
            }
        }
        enchantments.sort((a, b) -> Integer.compare(enchantmentValue(b), enchantmentValue(a)));
        final CardCollection picks = new CardCollection();
        if (n == 1) {
            if (payoff != null && !anyNamed(hand, combo.payoffs)) {
                picks.add(payoff);
            } else if (!enchantments.isEmpty()) {
                picks.add(enchantments.get(0));
            }
            return picks.size() == 1 ? picks : null;
        }
        final Set<String> names = new LinkedHashSet<>();
        for (final Card c : enchantments) {
            if (picks.size() < n && names.add(c.getName())) {
                picks.add(c);
            }
        }
        for (final Card c : enchantments) {
            if (picks.size() < n && !picks.contains(c)) {
                picks.add(c);
            }
        }
        if (picks.size() < n && payoff != null && !picks.contains(payoff)) {
            picks.add(payoff); // fewer than n enchantments are left: the payoff makes up the number
        }
        return picks.size() == n ? picks : null;
    }

    /**
     * What a search that sends all but one of the cards to the graveyard (Intuition) is worth to a Replenish deck, in
     * evaluator points: the opponent gives the AI the card that is worth least, so the others are what is gained. Not
     * limited by how full the graveyard already is, which the progress term is (it stops counting at a point):
     * every enchantment that is added comes back with the payoff.
     */
    public int graveyardGain(final Player ai, final Iterable<Card> picks) {
        final Combo rep = find(Kind.REPLENISH);
        if (rep == null || ai == null || picks == null) {
            return 0;
        }
        int sum = 0;
        int least = Integer.MAX_VALUE;
        int count = 0;
        for (final Card c : picks) {
            final int v = enchantmentValue(c);
            sum += v;
            least = Math.min(least, v);
            count++;
        }
        if (count < 2) {
            return 0;
        }
        final boolean access = anyNamed(ai.getCardsIn(ZoneType.Hand), rep.payoffs);
        return Math.min(300, (sum - least) * (access ? REPLENISH_SHARE_ACCESS : REPLENISH_SHARE_NO_ACCESS) / 100);
    }

    // ------------------------------------------------------------------------------------------------- tutors

    /**
     * What to search the library for, for a madness or Psychatog deck: the piece that is missing (a discard outlet when
     * a madness card waits in hand, a madness card when an outlet is out, the atog while there is none), then cards
     * that are not yet in hand, and a land while the AI is short of them. When the opponent picks one of the cards
     * (Intuition) the best {@code n} different cards are asked for, so that the one the opponent leaves is still good.
     * Every other combo is served the same way: the piece that is missing first (a payoff when the enablers are
     * there and the other way round), a second copy last. Decks that want cards in the graveyard are left alone when
     * the cards that are not picked go there (Intuition): the normal logic knows what to put in the graveyard.
     * Null when the deck has no combo, or nothing clearly worth fetching is in the pool: the normal choice is made.
     */
    public CardCollection tutorPicks(final Player ai, final Iterable<Card> pool, final int n) {
        final Combo mad = find(Kind.MADNESS);
        final Combo atog = find(Kind.ATOG);
        if (combos.isEmpty() || ai == null || pool == null || n <= 0) {
            return null;
        }
        final Combo rep = find(Kind.REPLENISH);
        if (rep != null) {
            final CardCollection forReplenish = replenishPicks(ai, pool, n, rep);
            if (forReplenish != null) {
                return forReplenish;
            }
        }
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        final CardCollectionView board = ai.getCardsIn(ZoneType.Battlefield);
        final java.util.Map<Card, Integer> score = new java.util.HashMap<>();
        final List<Card> ranked = new ArrayList<>();
        for (final Card c : pool) {
            score.put(c, tutorScore(ai, c, mad, atog, hand, board, n > 1));
            ranked.add(c);
        }
        ranked.sort((a, b) -> Integer.compare(score.get(b), score.get(a)));
        final CardCollection picks = new CardCollection();
        final Set<String> names = new LinkedHashSet<>();
        for (final Card c : ranked) { // different cards first
            if (picks.size() < n && score.get(c) > 0 && names.add(c.getName())) {
                picks.add(c);
            }
        }
        for (final Card c : ranked) { // then copies, if there are fewer than n different good cards
            if (picks.size() < n && score.get(c) > 0 && !picks.contains(c)) {
                picks.add(c);
            }
        }
        return picks.size() == n ? picks : null;
    }

    /** The one card to search for, or null for no opinion. */
    public Card tutorPick(final Player ai, final Iterable<Card> pool) {
        final CardCollection picks = tutorPicks(ai, pool, 1);
        return picks == null ? null : picks.getFirst();
    }

    /**
     * A piece of any other combo: wanted most when its other half is already there, then when nothing is, and hardly
     * when the same kind of piece is already at hand. Storm payoffs wait for a few rituals, go-wide finishers for a board.
     */
    private int pieceScore(final Card c, final Combo combo, final CardCollectionView hand, final CardCollectionView board,
                           final boolean graveyardRest) {
        final String n = c.getName();
        final boolean payoff = combo.payoffs.contains(n);
        final boolean enabler = combo.enablers.contains(n) || combo.cascaders.contains(n);
        if (!payoff && !enabler) {
            return 0;
        }
        if (graveyardRest && (combo.kind == Kind.MASS_REANIMATION || combo.kind == Kind.REANIMATOR
                || combo.kind == Kind.DREDGE || combo.kind == Kind.MILL || combo.kind == Kind.REPLENISH)) {
            return 0; // what is left over goes to the graveyard, which is where these decks want cards
        }
        boolean havePayoff = false;
        boolean haveEnabler = false;
        int enablersInHand = 0;
        int creatures = 0;
        for (final Card x : hand) {
            havePayoff |= combo.payoffs.contains(x.getName());
            final boolean e = combo.enablers.contains(x.getName()) || combo.cascaders.contains(x.getName());
            haveEnabler |= e;
            enablersInHand += e ? 1 : 0;
        }
        for (final Card x : board) {
            havePayoff |= combo.payoffs.contains(x.getName());
            haveEnabler |= combo.enablers.contains(x.getName());
            creatures += x.isCreature() ? 1 : 0;
        }
        int s;
        if (payoff) {
            s = havePayoff ? 15 : haveEnabler ? 100 : 70;
            if (combo.kind == Kind.STORM && enablersInHand < 2 || combo.kind == Kind.GO_WIDE && creatures < 3) {
                s = Math.min(s, 20); // not yet: the payoff is wasted without the rituals or the board
            }
        } else {
            s = haveEnabler ? 15 : havePayoff ? 100 : 70;
        }
        return s * (50 + combo.strength / 2) / 100;
    }

    private int tutorScore(final Player ai, final Card c, final Combo mad, final Combo atog,
                           final CardCollectionView hand, final CardCollectionView board, final boolean graveyardRest) {
        int lands = 0;
        for (final Card x : hand) {
            lands += x.isLand() ? 1 : 0;
        }
        for (final Card x : board) {
            lands += x.isLand() ? 1 : 0;
        }
        if (c.isLand()) {
            // Crop Rotation and the like: the land that is a piece of the deck (Dark Depths, Thespian's Stage) first
            int piece = 0;
            for (final Combo combo : combos) {
                if (combo.kind == Kind.DARK_DEPTHS) {
                    piece += pieceScore(c, combo, hand, board, graveyardRest);
                }
            }
            return piece > 0 ? piece : lands < 4 ? 45 : -40;
        }
        int s = 0;
        for (final Combo combo : combos) {
            if (combo.kind != Kind.MADNESS && combo.kind != Kind.ATOG) {
                s += pieceScore(c, combo, hand, board, graveyardRest);
            }
        }
        if (mad != null) {
            boolean madInHand = false;
            for (final Card x : hand) {
                madInHand |= x.hasKeyword(Keyword.MADNESS);
            }
            final boolean outlet = hasOutlet(ai, mad);
            if (c.hasKeyword(Keyword.MADNESS)) {
                s += 70 + (outlet && !madInHand ? 50 : 0) - (madInHand ? 30 : 0);
            } else if (isDiscardOutlet(c)) {
                s += 70 + (!outlet && madInHand ? 60 : 0) - (outlet ? 25 : 0);
            }
        }
        if (atog != null) {
            boolean atogOut = false;
            for (final Card x : hand) {
                atogOut |= atog.payoffs.contains(x.getName());
            }
            for (final Card x : board) {
                atogOut |= atog.payoffs.contains(x.getName());
            }
            if (atog.payoffs.contains(c.getName())) {
                s += atogOut ? 20 : 90;
            } else if (c.isInstant() || c.isSorcery()) {
                s += 25;
            }
        }
        if (s <= 0) {
            return 0;
        }
        for (final Card x : hand) {
            if (x.getName().equals(c.getName())) {
                s -= 60; // one is already at hand
                break;
            }
        }
        if (c.isCreature()) {
            s += Math.min(20, ComputerUtilCard.evaluateCreature(c) / 20);
        }
        return Math.max(s, 1);
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

    // ----------------------------------------------------------------------------------- tutor storm

    /** A storm card that wins the game: Tendrils of Agony, Grapeshot, Brain Freeze, Empty the Warrens. */
    public static boolean isStormFinisher(final Card c) {
        return !c.isLand() && !c.isCreature()
                && (c.hasKeyword(Keyword.STORM) || AiCardLists.has(AiCardLists.Role.STORM_PAYOFF, c.getName()));
    }

    /**
     * Whether the hand holds what the kill is made of and cannot be given up: the storm finisher, or the engine that
     * casts the graveyard again. Lion's Eye Diamond and the like discard the hand: not to pay for another spell then.
     */
    public static boolean holdsKillPiece(final Player ai) {
        for (final Card h : ai.getCardsIn(ZoneType.Hand)) {
            if (isStormFinisher(h) || (!h.isLand() && !h.isCreature() && isRecursionAbility(h.getSpellAbilities()))) {
                return true;
            }
        }
        return false;
    }

    /** A card the kill is built from: the finisher, a tutor, the engine, fast mana. */
    public static boolean isTutorStormPiece(final Card c) {
        return isStormFinisher(c) || isLibraryTutor(c) || isRecursionEngine(c) || isFastMana(c);
    }

    /** A ritual, a mox, Lotus Petal, Lion's Eye Diamond: a cheap spell that is worth mana. */
    private static boolean isFastMana(final Card c) {
        return !c.isLand() && !c.isCreature() && c.getCMC() <= 2
                && (hasApi(c, ApiType.Mana) || hasApi(c, ApiType.ManaReflected));
    }

    /**
     * A spell or ability that finds any card (or a card of a broad type) in the library or the sideboard and puts it
     * into the hand, or exiles it to be cast: Infernal Tutor, Beseech the Mirror, Wishclaw Talisman, the Wishes.
     */
    private static boolean isLibraryTutor(final Card c) {
        if (c.isLand() || c.isCreature()) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if (a.getApi() != ApiType.ChangeZone) {
                    continue;
                }
                final String origin = a.getParamOrDefault("Origin", "");
                final boolean fromSideboard = origin.contains("Sideboard");
                if (!origin.contains("Library") && !fromSideboard) {
                    continue;
                }
                final String dest = a.getParamOrDefault("Destination", "");
                if (!dest.equals("Hand") && !dest.equals("Exile")) {
                    continue;
                }
                final String type = a.getParamOrDefault("ChangeType", "");
                if (type.startsWith("Card") || fromSideboard && !type.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Infernal Tutor: finds any card, but only with an empty hand ("Hellbent"); otherwise it finds a copy of a card of the hand. */
    public static boolean isHellbentTutor(final Card c) {
        return isLibraryTutor(c) && c.getOracleText() != null && c.getOracleText().contains("Hellbent");
    }

    /** Gaea's Will, Yawgmoth's Will: cast the spells of the graveyard again. */
    private static boolean isRecursionEngine(final Card c) {
        if (c.isLand() || c.isCreature()) {
            return false;
        }
        return isRecursionAbility(c.getSpellAbilities());
    }

    public static boolean isRecursionAbility(final Collection<SpellAbility> abilities) {
        for (final SpellAbility sa : abilities) {
            if ("YawgmothsWill".equals(sa.getParamOrDefault("AILogic", ""))) {
                return true;
            }
        }
        return false;
    }
    private static Combo tutorStorm(final List<Card> spells) {
        final Set<String> payoffs = new LinkedHashSet<>();
        final Set<String> enablers = new LinkedHashSet<>();
        int finishers = 0;
        int mana = 0;
        int tutors = 0;
        int engines = 0;
        for (final Card c : spells) {
            if (isStormFinisher(c)) {
                payoffs.add(c.getName());
                finishers++;
            } else if (isLibraryTutor(c)) {
                enablers.add(c.getName());
                tutors++;
            } else if (isRecursionEngine(c)) {
                enablers.add(c.getName());
                engines++;
            } else if (isFastMana(c)) {
                enablers.add(c.getName());
                mana++;
            }
        }
        if (finishers < 1 || tutors < 3 || mana < 8) {
            return null;
        }
        final int strength = 25 + 4 * Math.min(mana, 8) + 3 * Math.min(tutors, 6) + 8 * Math.min(finishers, 3)
                + (engines > 0 ? 6 : 0);
        return new Combo(Kind.TUTOR_STORM, strength, payoffs, enablers, Collections.<String>emptySet(), false,
                "finishers=" + payoffs + ", tutors=" + tutors + ", fast mana=" + mana + ", engines=" + engines);
    }

    /** How much one copy of a storm finisher takes from the opponent: 2 for Tendrils of Agony, 1 for Grapeshot, else 0. */
    static int perCopyDamage(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                final ApiType api = a.getApi();
                final String amount = api == ApiType.LoseLife ? a.getParam("LifeAmount")
                        : api == ApiType.DealDamage ? a.getParam("NumDmg") : null;
                if (amount != null) {
                    try {
                        return Integer.parseInt(amount.trim());
                    } catch (final NumberFormatException e) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    /** The number of spells cast this turn by everybody: the storm count the next storm spell would copy itself that often for. */
    static int stormCount(final Player ai) {
        return ai.getGame().getStack().getSpellsCastThisTurn().size();
    }

    /**
     * Whether casting the storm card now takes the last life of the only opponent: the spell and its copies (one for
     * every spell cast before it this turn) each take {@link #perCopyDamage} life. False when the card does not
     * simply take life, or when there is more than one opponent.
     */
    public static boolean stormIsLethal(final Player ai, final Card finisher) {
        if (ai == null || finisher == null || !finisher.hasKeyword(Keyword.STORM)) {
            return false;
        }
        final int per = perCopyDamage(finisher);
        if (per <= 0 || ai.getOpponents().size() != 1) {
            return false;
        }
        final Player opp = ai.getWeakestOpponent();
        return opp != null && (long) per * (stormCount(ai) + 1) >= opp.getLife();
    }

    /** A cheap guess at the mana the player can spend this turn: what floats in the pool and the untapped mana sources. */
    private static int potentialMana(final Player ai) {
        int mana = ai.getManaPool().totalMana();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isTapped() && !c.getManaAbilities().isEmpty()) {
                mana++;
            }
        }
        return mana;
    }

    /**
     * Progress of a tutor storm deck, in evaluator points: the finisher (or a tutor that finds it) in the hand, mana to
     * cast it, a storm count to make it lethal; most of it only while the finisher can still be found.
     */
    private int tutorStormTerm(final Player ai, final Combo combo) {
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        Card finisher = null;
        for (final Card c : hand) {
            if (combo.payoffs.contains(c.getName())) {
                finisher = c;
                break;
            }
        }
        final boolean inLibrary = anyNamed(ai.getCardsIn(ZoneType.Library), combo.payoffs);
        final boolean tutorInHand = handHasTutor(hand);
        if (finisher == null && !(inLibrary && tutorInHand)) {
            return 0; // the finisher is out of reach, or it is not in the library and not in the hand: nothing to prepare
        }
        int sum = finisher != null ? 120 : 40;
        final int mana = potentialMana(ai);
        final int storm = stormCount(ai);
        sum += Math.min(mana, 10) * 10;
        sum += Math.min(storm, 12) * 12;
        if (finisher != null) {
            final Player opp = ai.getWeakestOpponent();
            final int per = perCopyDamage(finisher);
            if (opp != null && per > 0 && opp.getLife() > 0) {
                final long damage = (long) per * (storm + 1);
                sum += (int) Math.min(100L, damage * 100 / opp.getLife()) * 2;
                if (mana >= finisher.getCMC() && damage >= opp.getLife()) {
                    sum += 300; // castable, and lethal: only the cast is missing
                }
            }
        }
        return sum;
    }

    /**
     * How ready a tutor storm deck is to kill on a later turn, when it cannot this turn: the finisher and the engine in
     * the hand, tutors to find them, fast mana still unspent in the hand, mana that stays in play (moxen, lands). The
     * storm count is worth nothing here, it is gone at the end of the turn: a ritual cast without a kill loses points.
     */
    public int setupScore(final Player ai) {
        if (find(Kind.TUTOR_STORM) == null || ai == null) {
            return 0;
        }
        boolean finisher = false;
        boolean engine = false;
        int tutors = 0;
        int fast = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (isStormFinisher(c)) {
                finisher = true;
            } else if (isRecursionEngine(c)) {
                engine = true;
            } else if (isLibraryTutor(c)) {
                tutors++;
            } else if (isFastMana(c)) {
                fast++;
            }
        }
        int permanents = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isLand() && !c.getManaAbilities().isEmpty() && !sacrificesForMana(c)) {
                permanents++;
            }
        }
        return (finisher ? 120 : 0) + (engine ? 80 : 0) + 40 * Math.min(tutors, 3) + 25 * Math.min(fast, 6)
                + 35 * Math.min(permanents, 4) + 15 * Math.min(ai.getLandsInPlay().size(), 5);
    }

    /** The progress of a tutor storm deck in evaluator points (0 for any other deck); the solver ranks its states by it. */
    public int stormProgress(final Player ai) {
        final Combo combo = find(Kind.TUTOR_STORM);
        return combo == null || ai == null ? 0 : tutorStormTerm(ai, combo);
    }

    /** Whether the storm card simply takes life per copy (Tendrils, Grapeshot): only then is "lethal" something to compute. */
    public static boolean takesLifePerCopy(final Card c) {
        return c != null && perCopyDamage(c) > 0;
    }

    /**
     * A play a tutor storm deck keeps until the solver has found the kill: the fast mana (rituals, moxen, Lotus Petal,
     * Lion's Eye Diamond) only builds the storm count and the mana of one turn, and spent without a kill it is gone.
     * The solver plays these itself, whatever this says.
     */
    public boolean holdsForKill(final SpellAbility sa) {
        if (find(Kind.TUTOR_STORM) == null || sa == null || sa.isLandAbility() || sa.getHostCard() == null) {
            return false;
        }
        final Card c = sa.getHostCard();
        if (!isFastMana(c)) {
            return false;
        }
        if (c.isPermanent() && !sacrificesForMana(c)) {
            return false; // Moxen stay on the battlefield: playing them early costs nothing and builds mana
        }
        if (c.isPermanent() && sa.getActivatingPlayer() != null) {
            // set the Bargain up: a permanent to sacrifice (Lotus Petal, a Mox) is played ahead while a Bargain tutor
            // (Beseech the Mirror) waits in the hand and there is nothing to sacrifice yet
            final Player ai = sa.getActivatingPlayer();
            boolean bargainTutor = false;
            for (final Card h : ai.getCardsIn(ZoneType.Hand)) {
                if (isLibraryTutor(h) && h.hasKeyword(Keyword.BARGAIN)) {
                    bargainTutor = true;
                    break;
                }
            }
            boolean fodder = false;
            for (final Card b : ai.getCardsIn(ZoneType.Battlefield)) {
                if (!b.isLand() && b.isPermanent()) {
                    fodder = true;
                    break;
                }
            }
            if (bargainTutor && !fodder) {
                return false;
            }
        }
        return true;
    }

    /**
     * A cheap permanent that is Bargain fodder and one more storm count on the kill turn, but costs a card or does
     * nothing on any other turn (Giant's Boulder, Skateboard, Chrome Mox that imprints a card): the solver plays them
     * in a line, the AI does not play them for nothing. One is let through while a Bargain tutor waits in the hand and
     * there is nothing to sacrifice yet.
     */
    public boolean wastesFodder(final SpellAbility sa) {
        if (find(Kind.TUTOR_STORM) == null || sa == null || !sa.isSpell() || sa.getHostCard() == null) {
            return false;
        }
        final Card c = sa.getHostCard();
        if (c.isLand() || c.isCreature() || !c.isPermanent() || c.getCMC() > 1 || isStormFinisher(c)) {
            return false;
        }
        final boolean imprint = c.getOracleText() != null && c.getOracleText().contains("Imprint");
        if (producesFreeMana(c) && !imprint) {
            return false; // a Mox that costs nothing is mana for the rest of the game: holdsForKill decides about those
        }
        final Player ai = sa.getActivatingPlayer();
        if (ai != null) {
            boolean bargainTutor = false;
            for (final Card h : ai.getCardsIn(ZoneType.Hand)) {
                if (isLibraryTutor(h) && h.hasKeyword(Keyword.BARGAIN)) {
                    bargainTutor = true;
                    break;
                }
            }
            boolean fodder = false;
            for (final Card b : ai.getCardsIn(ZoneType.Battlefield)) {
                if (!b.isLand() && b.isPermanent()) {
                    fodder = true;
                    break;
                }
            }
            if (bargainTutor && !fodder) {
                return false;
            }
        }
        return true;
    }

    /** A mana ability that costs no mana (Chrome Mox, Mox Opal, Lotus Petal), unlike Giant's Boulder's {1},{T}. */
    private static boolean producesFreeMana(final Card c) {
        for (final SpellAbility m : c.getManaAbilities()) {
            if (m.getPayCosts() != null && m.getPayCosts().getCostMana() == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * The mulligan rule of a tutor storm deck: keep a hand that can act, not a slow one. It needs a land (checked by the
     * caller) and either two tutors, or a tutor with mana to cast it (fast mana, or the engine), or plenty of fast mana
     * for the tutors it will draw. Disruption alone (Thoughtseize, Duress, Veil of Summer) is not a hand unless it is so
     * small that going lower is worse.
     */
    public boolean keepsTutorStormHand(final CardCollectionView kept, final int size) {
        if (size <= 5) {
            return true;
        }
        int tutors = 0;
        int mana = 0;
        int engines = 0;
        int lands = 0;
        for (final Card c : kept) {
            if (c.isLand()) {
                lands++;
            } else if (isLibraryTutor(c)) {
                tutors++;
            } else if (isRecursionEngine(c)) {
                engines++;
            } else if (isFastMana(c)) {
                mana++;
            }
        }
        if (tutors >= 2) {
            return true;
        }
        if (tutors == 1 && (mana >= 1 || engines >= 1 || lands >= 2)) {
            return true;
        }
        return mana + lands >= 4 && (tutors + engines >= 1 || mana >= 3);
    }

    /** Infernal Tutor with other cards in hand only finds a copy of the revealed card: keep it for Hellbent. */
    public boolean wastesTutor(final SpellAbility sa) {
        if (find(Kind.TUTOR_STORM) == null || sa == null || sa.getHostCard() == null || !sa.isSpell()
                || !"Infernal Tutor".equals(sa.getHostCard().getName())) {
            return false;
        }
        final Player ai = sa.getActivatingPlayer();
        return ai != null && ai.getCardsIn(ZoneType.Hand).size() > 1;
    }

    /** Spells in the graveyard that an engine (Gaea's Will) can cast again. */
    private static int replayable(final Player ai) {
        int n = 0;
        for (final Card g : ai.getCardsIn(ZoneType.Graveyard)) {
            if (!g.isLand() && !isRecursionEngine(g)) {
                n++;
            }
        }
        return n;
    }

    /** What one copy of the finisher of this deck takes: Tendrils 2, Grapeshot 1 (0 when unknown). */
    private static int finisherPer(final Player ai, final Combo combo) {
        for (final ZoneType z : new ZoneType[] { ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile }) {
            for (final Card c : ai.getCardsIn(z)) {
                if (combo.payoffs.contains(c.getName()) && perCopyDamage(c) > 0) {
                    return perCopyDamage(c);
                }
            }
        }
        return 0;
    }

    /** Whether a storm count of this size (plus the finisher itself) takes the last life of the opponent. */
    private static boolean lethalAt(final Player ai, final Combo combo, final int storm) {
        final int per = finisherPer(ai, combo);
        final Player opp = ai.getWeakestOpponent();
        return per > 0 && opp != null && ai.getOpponents().size() == 1 && (long) per * (storm + 1) >= opp.getLife();
    }

    /**
     * Gaea's Will cast from the hand before it can make the kill: the graveyard it replays plus the storm count must
     * reach lethal. The free cast of Beseech the Mirror is not a play of the AI, so it is never held by this.
     */
    public boolean wastesEngine(final SpellAbility sa) {
        final Combo combo = find(Kind.TUTOR_STORM);
        if (combo == null || sa == null || !sa.isSpell() || sa.getHostCard() == null
                || !isRecursionEngine(sa.getHostCard()) || !sa.getHostCard().isInZone(ZoneType.Hand)) {
            return false;
        }
        final Player ai = sa.getActivatingPlayer();
        if (ai == null) {
            return false;
        }
        // the engine itself, every replay, and the finisher are all spells that count
        return !lethalAt(ai, combo, stormCount(ai) + 1 + replayable(ai));
    }

    /** Whether the setup is complete: the storm count, the fast mana left in hand and a working engine reach lethal. */
    private boolean setupComplete(final Player ai, final Combo combo, final CardCollectionView hand) {
        boolean engine = false;
        for (final Card c : hand) {
            engine |= isRecursionEngine(c);
        }
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            engine |= isRecursionEngine(c);
        }
        final int replay = engine ? replayable(ai) + 1 : 0;
        return lethalAt(ai, combo, stormCount(ai) + fastManaInHand(hand) + replay + 1);
    }

    /**
     * How far a state is along the pattern of the deck, for the solver: the Bargain is set up, the free spell found the
     * engine (Gaea's Will), the engine cast, the graveyard it can cast again (rituals, tutors), mana and storm count.
     * The storm card comes last, so it is worth nothing here until it is lethal (the solver casts it as the last play).
     */
    public int solverScore(final Player ai) {
        int sum = stormProgress(ai);
        boolean bargainTutor = false;
        boolean engineInHand = false;
        for (final Card h : ai.getCardsIn(ZoneType.Hand)) {
            if (isLibraryTutor(h) && h.hasKeyword(Keyword.BARGAIN)) {
                bargainTutor = true;
            }
            if (isRecursionEngine(h)) {
                engineInHand = true;
            }
        }
        boolean fodder = false;
        for (final Card b : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!b.isLand() && b.isPermanent()) {
                fodder = true;
            }
        }
        if (bargainTutor && fodder) {
            sum += 60; // the first line is ready
        }
        if (engineInHand) {
            sum += 80; // found by the free spell: now it needs spells in the graveyard and the mana to cast it
        }
        boolean engineSpent = false;
        for (final Card g : ai.getCardsIn(ZoneType.Graveyard)) {
            if (isRecursionEngine(g)) {
                engineSpent = true;
            }
        }
        if (engineSpent) {
            int replayable = 0;
            for (final Card g : ai.getCardsIn(ZoneType.Graveyard)) {
                if (!g.isLand() && !isRecursionEngine(g)) {
                    replayable++;
                }
            }
            sum += 200 + 25 * Math.min(replayable, 8); // the engine is working: the graveyard is the storm count to come
        }
        return sum;
    }

    private static boolean handHasTutor(final CardCollectionView hand) {
        for (final Card c : hand) {
            if (isLibraryTutor(c)) {
                return true;
            }
        }
        return false;
    }

    private static int fastManaInHand(final CardCollectionView hand) {
        int n = 0;
        for (final Card h : hand) {
            if (isFastMana(h)) {
                n++;
            }
        }
        return n;
    }

    /** Lotus Petal, Lion's Eye Diamond: the mana is gone with the permanent, so they are kept for the kill. */
    private static boolean sacrificesForMana(final Card c) {
        for (final SpellAbility m : c.getManaAbilities()) {
            if (m.getPayCosts() != null && m.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What a tutor of a tutor storm deck should find. The finisher when the mana for it is there (or it is the only
     * way to get it), otherwise what makes the mana and the storm count: with a free cast (Beseech the Mirror) the
     * engine that casts the graveyard again when there is something to cast, else a ritual; with a card for the hand
     * mana first, then the finisher. Null when nothing in the list is a piece of the deck.
     */
    private Card tutorStormPick(final Player ai, final Combo combo, final CardCollectionView options,
                                final ZoneType destination) {
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        final boolean finisherInHand = anyNamed(hand, combo.payoffs);
        final int mana = potentialMana(ai);
        final boolean freeCast = destination == ZoneType.Exile;
        int pieces = 0; // spells in the graveyard that an engine can cast again
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            if (isFastMana(c) || combo.enablers.contains(c.getName())) {
                pieces++;
            }
        }
        Card best = null;
        int bestValue = 0;
        boolean engineFound = false; // Gaea's Will is still among the choices (library)
        for (final Card c : options) {
            engineFound |= isRecursionEngine(c);
        }
        final boolean ready = setupComplete(ai, combo, hand);
        for (final Card c : options) {
            int v;
            if (freeCast && (c.isLand() || c.getCMC() > 4)) {
                continue; // cannot be cast for free (Beseech the Mirror: mana value 4 or less)
            }
            if (combo.payoffs.contains(c.getName())) {
                if (freeCast && !stormIsLethal(ai, c)) {
                    continue; // cast free below lethal it is wasted: leave it in the library
                }
                if (finisherInHand) {
                    v = 5; // a second one is a spare at best
                } else {
                    // fetch the finisher once the kill is reachable and castable; before that the engine and mana come first
                    v = mana + fastManaInHand(hand) >= c.getCMC() - 1 && ready ? 500 : 150;
                }
            } else if (isRecursionEngine(c)) {
                // the free cast of Beseech the Mirror is the engine's turn: Gaea's Will casts the graveyard again, and the
                // Beseech itself is in that graveyard by then. Found into the hand it is only worth it with pieces to cast
                v = freeCast ? (pieces >= 1 ? 300 : 150) : (lethalAt(ai, combo, stormCount(ai) + 1 + replayable(ai)) ? 260 : 60);
            } else if (freeCast && isLibraryTutor(c) && c.hasKeyword(Keyword.BARGAIN)) {
                // a second Beseech: cast free it is not bargained, so it puts its find in the hand (the finisher, once the
                // setup is complete), and it is one more spell for the storm count
                v = engineFound ? 120 : 280;
            } else if (isFastMana(c)) {
                // before the kill is reachable, mana and storm count come first, whatever is already in play
                v = (!ready || mana < 5) ? 200 : 90;
            } else if (isLibraryTutor(c)) {
                v = 110;
            } else if (c.isLand()) {
                v = 1;
            } else if (combo.enablers.contains(c.getName())) {
                v = 40;
            } else {
                v = 0;
            }
            if (v > bestValue) {
                bestValue = v;
                best = c;
            }
        }
        return best;
    }

    /**
     * The card to put back from the hand (Brainstorm). The generic "worst card" is the cheapest one, which for this deck
     * is exactly the rituals and Lotus Petal that the kill is made of. Lands beyond what is needed come first, then the
     * cards that do not help the kill (discard, Veil of Summer), and the finisher, the engine, tutors and fast mana last.
     */
    private static Card tutorStormPutBack(final Player ai, final CardCollectionView options) {
        int lands = ai.getLandsInPlay().size();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                lands++;
            }
        }
        Card worst = null;
        int worstValue = Integer.MAX_VALUE;
        for (final Card c : options) {
            final int v;
            if (isStormFinisher(c)) {
                v = 100;
            } else if (isRecursionEngine(c)) {
                v = 90;
            } else if (isLibraryTutor(c)) {
                v = 80;
            } else if (isFastMana(c)) {
                v = 70;
            } else if (c.isLand()) {
                v = lands > 3 ? 20 : 60; // the third or fourth land is needed, the fifth is not
            } else {
                v = 10;
            }
            if (v < worstValue) {
                worstValue = v;
                worst = c;
            }
        }
        return worst;
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
        final Combo tutorStorm = find(Kind.TUTOR_STORM);
        if (tutorStorm != null && (destination == ZoneType.Hand || destination == ZoneType.Exile)
                && (origin.contains(ZoneType.Library) || origin.contains(ZoneType.Sideboard))) {
            final Card pick = tutorStormPick(ai, tutorStorm, options, destination);
            if (pick != null) {
                return pick;
            }
        }
        if (tutorStorm != null && destination == ZoneType.Exile && origin.contains(ZoneType.Hand)) {
            return tutorStormPutBack(ai, options); // Chrome Mox imprints the card the kill needs least
        }
        if (tutorStorm != null && destination == ZoneType.Library && origin.contains(ZoneType.Hand)) {
            return tutorStormPutBack(ai, options); // Brainstorm: what goes back is what the kill needs least
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
        if (destination == ZoneType.Battlefield && origin.contains(ZoneType.Library)) {
            return depthsSearchPick(ai, options); // Crop Rotation: the land the deck is built around, or no opinion
        }
        if (destination == ZoneType.Graveyard && origin.contains(ZoneType.Library) && wantsGraveyardFuel()) {
            for (final Card c : options) {
                if (!c.isCreature()) {
                    continue;
                }
                // a legend that is already in play and working is no use in the graveyard either
                final int v = (isBigTarget(c) && !ComboGuard.duplicatesWorkingLegend(ai, c) ? 1000 : 0)
                        + (find(Kind.DREDGE) != null && c.hasKeyword(Keyword.DREDGE) ? 400 : 0)
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
        return find(Kind.MASS_REANIMATION) != null || find(Kind.REANIMATOR) != null || find(Kind.REPLENISH) != null
                || find(Kind.DREDGE) != null;
    }

    /**
     * What to throw away when the player has to discard (a looting spell, an outlet, the cleanup step) for a deck that
     * wants creatures in the graveyard: the huge creatures of a reanimator deck first, the best creatures of a mass
     * reanimation deck, and never the payoff, a reanimation spell or a cascade spell. Null when the deck has no
     * opinion and the normal choice should be made.
     */
    public CardCollection preferredDiscards(final Player ai, final CardCollection valid, final int min) {
        if (min > 0 && valid != null && ai != null && find(Kind.MADNESS) != null) {
            // A forced discard (cleanup, an opponent's spell, Careful Study) is a free madness trigger: throw away the
            // madness cards that can be cast right now, the best first.
            final List<Card> castable = new ArrayList<>();
            for (final Card c : valid) {
                if (c.hasKeyword(Keyword.MADNESS) && canCastForMadness(ai, c)) {
                    castable.add(c);
                }
            }
            if (castable.size() >= min) {
                castable.sort((a, b) -> Integer.compare(madnessValue(b), madnessValue(a)));
                return new CardCollection(castable.subList(0, min));
            }
        }
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
            if (combo.kind == Kind.DREDGE && belongsInGraveyard(c, combo)) {
                continue; // Bridge from Below, Ichorid, a dredger: worth more in the graveyard than in the hand
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
        final Combo dredge = find(Kind.DREDGE);
        if (dredge != null && belongsInGraveyard(c, dredge)) {
            // a dredger first while none is in the graveyard (it is the engine), then the cards that work from there
            int dredgersAway = 0;
            for (final Card g : ai.getCardsIn(ZoneType.Graveyard)) {
                dredgersAway += g.hasKeyword(Keyword.DREDGE) ? 1 : 0;
            }
            return 300 + (c.hasKeyword(Keyword.DREDGE) ? (dredgersAway < 2 ? 60 : -40) + c.getKeywordMagnitude(Keyword.DREDGE) : 0);
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
        if (find(Kind.REPLENISH) != null && enchantmentValue(c) > 0) {
            return 100 + enchantmentValue(c); // it comes back with the payoff, and does more there than in the hand
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

    /** A spell whose extra cost is to discard other cards from the hand (Cathartic Reunion, Tormenting Voice). */
    private static boolean isDiscardCostSpell(final SpellAbility sa) {
        if (sa.getPayCosts() == null || !sa.getPayCosts().hasSpecificCostType(CostDiscard.class)) {
            return false;
        }
        final CostDiscard d = sa.getPayCosts().getCostPartByType(CostDiscard.class);
        return d != null && !d.payCostFromSource();
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
