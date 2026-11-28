package forge.ai.simulation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.ai.ManaSourceAllocation;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.GameObject;
import forge.game.GameObjectPredicates;
import forge.game.ability.AbilityUtils;
import forge.card.mana.ManaCostShard;
import forge.card.mana.ManaCost;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.cost.CostExert;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostPayEnergy;
import forge.game.cost.CostPayLife;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTap;
import forge.game.cost.CostTapType;
import forge.game.replacement.ReplacementType;
import forge.card.CardStateName;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.util.Expressions;
import forge.game.zone.ZoneType;

/**
 * Judges an opening hand (keep or mulligan, and which cards to put on the bottom) for the deck's plan.
 * <p>
 * The score combines three things, all read from the deck itself:
 * <ul>
 * <li>how likely the hand is to reach the lands its plan needs by the turn it needs them (the exact hypergeometric
 * chance from the cards left in the library, counting the draw step only when the player is on the draw);</li>
 * <li>how many cheap plays it has that its lands can cast (colours included), so that it survives the first turns;</li>
 * <li>how many of the deck's key cards for its plan it holds: cheap creatures for aggro, cheap burn for burn,
 * interaction and card draw for control, mana development for ramp, tutors, rituals and cantrips for combo.</li>
 * </ul>
 * A hand that cannot do anything (no lands, all lands, nothing castable) is scored down hard.
 */
public final class OpeningHand {
    private static final Pattern COUNTER_KIND = Pattern.compile("CounterType\\$ (\\w+)");
    private static final Pattern COUNTER_NUMBER = Pattern.compile("CounterNum\\$ (\\w+)");

    private OpeningHand() {
    }

    /** A hand that can make Marit Lage by turn two or three is worth keeping whatever else it holds. */
    private static final int DEPTHS_READY_HAND = 25;
    /** A dredge deck plays about 12 lands: two by turn three, and a cheap outlet counts as a play. */
    private static final Want DREDGE_WANT = new Want(2, 3, 2, 2, 2);
    /** An outlet plus something to throw away: the engine runs from turn one or two. */
    private static final int DREDGE_READY_HAND = 20;

    /** What the main combo of the deck wants from its first turns; null: the archetype decides. */
    private static Want comboWant(final DeckPlan plan) {
        final ComboPlan.Combo primary = plan == null ? null : plan.combos.primary();
        if (primary == null) {
            return null;
        }
        switch (primary.kind) {
            case REANIMATOR:       return new Want(2, 3, 2, 1, 2);
            case MASS_REANIMATION: return new Want(2, 3, 2, 2, 2);
            case MILL:             return new Want(3, 4, 3, 2, 2);
            case CHEAT_FATTY:      return new Want(3, 4, 3, 1, 2);
            case AFFINITY:         return new Want(2, 2, 2, 3, 2);
            case GO_WIDE:          return new Want(2, 3, 2, 3, 2);
            case STORM:            return new Want(2, 3, 2, 1, 2);
            default:               return null;
        }
    }

    /** Score a kept hand of this size must reach: smaller hands are judged less strictly, 4 or fewer are always kept. */
    static int keepBar(final int handSize) {
        if (handSize >= 7) {
            return 55;
        }
        if (handSize == 6) {
            return 45;
        }
        if (handSize == 5) {
            return 30;
        }
        return Integer.MIN_VALUE;
    }

    /** What a plan wants from its first turns. */
    private static final class Want {
        final int lands;
        final int byTurn;
        final int playCmc;
        final int plays;
        final int keys;

        Want(final int lands, final int byTurn, final int playCmc, final int plays, final int keys) {
            this.lands = lands;
            this.byTurn = byTurn;
            this.playCmc = playCmc;
            this.plays = plays;
            this.keys = keys;
        }
    }

    private static Want want(final DeckPlan.Archetype a) {
        switch (a) {
            case AGGRO:
                return new Want(3, 3, 3, 3, 2);
            case BURN:
                return new Want(3, 4, 3, 2, 2);
            case CONTROL:
                return new Want(4, 5, 4, 2, 2);
            case RAMP:
                return new Want(3, 3, 4, 2, 2);
            case LANDS:
                return new Want(4, 4, 3, 1, 1); // the lands are the deck: more of them, and a single engine card is enough
            case COMBO:
                return new Want(3, 4, 3, 1, 2);
            default:
                return new Want(3, 4, 4, 2, 2);
        }
    }

    /** Whether to keep the hand; {@code cardsToReturn} are what the mulligan rule makes the player put back. */
    public static boolean keep(final Player ai, final CardCollectionView hand, final int cardsToReturn,
                               final boolean onPlay, final DeckPlan plan, final int mulliganThreshold) {
        final int finalSize = hand.size() - cardsToReturn;
        if (finalSize < mulliganThreshold || hand.isEmpty()) {
            return true; // already too small to be worth going lower
        }
        final Library lib = new Library(ai);
        if (lib.lands == 0) {
            return true; // a deck without lands has nothing to judge by
        }
        final CardCollection kept = new CardCollection(hand);
        kept.removeAll(bottom(ai, hand, cardsToReturn, onPlay, plan, lib));
        if (hasOnlyLandsAndCreatureAuras(kept)) {
            return false; // creature Auras are not a threat without a creature to enchant (#12929)
        }
        if ((!playable(kept, finalSize) || !castsSomething(kept, lib)) && !dredgeOneLander(kept, plan)) {
            return false; // no key card, combo piece or good score makes up for a hand that cannot cast anything
        }
        if (plan != null && plan.combos.has(ComboPlan.Kind.TUTOR_STORM)) {
            return plan.combos.keepsTutorStormHand(kept, finalSize); // judged as the combo player would, not by the generic score
        }
        if (TidePlan.isTideDeck(ai)) {
            return TidePlan.keepsHand(kept, finalSize); // High Tide: lands and the pieces of the loop, not the generic score
        }
        final double score = score(kept, lib, onPlay, plan);
        return score >= keepBar(finalSize);
    }

    /**
     * A hand containing only lands and creature Auras has no creature to enchant and no meaningful opening play.
     * Keep this check independent of the smart score so combo bonuses or a lands classification cannot rescue it;
     * the classic mulligan heuristic uses it too.
     */
    public static boolean hasOnlyLandsAndCreatureAuras(final CardCollectionView hand) {
        boolean hasAura = false;
        for (final Card c : hand) {
            if (c.isCreature()) {
                return false;
            }
            if (c.isLand()) {
                continue;
            }
            if (!isCreatureAura(c)) {
                return false;
            }
            hasAura = true;
        }
        return hasAura;
    }

    /** For tests and the game log: why a hand is kept or not. */
    public static String explain(final Player ai, final CardCollectionView hand, final boolean onPlay,
                                 final DeckPlan plan) {
        final Library lib = new Library(ai);
        return "dredgePlan=" + (plan != null && plan.combos.has(ComboPlan.Kind.DREDGE))
                + ", playable=" + playable(hand, hand.size())
                + ", oneLander=" + dredgeOneLander(hand, plan)
                + ", score=" + score(hand, lib, onPlay, plan)
                + ", bar=" + keepBar(hand.size())
                + ", libLands=" + lib.lands + "/" + lib.size
                + ", handColors=" + handColors(hand);
    }

    private static int handColors(final CardCollectionView hand) {
        byte colors = 0;
        for (final Card c : hand) {
            if (c.isLand() || isMdfcLand(c)) {
                colors |= producedColors(c);
            }
        }
        return colors;
    }

    /**
     * The rules no hand may break, whatever its spells are worth: at least one land that makes mana (two for a
     * 6 or 7 card hand unless a cheap mana source helps the single land) and not nearly all lands.
     */
    static boolean playable(final CardCollectionView kept, final int size) {
        int lands = 0;
        for (final Card c : kept) {
            if ((c.isLand() || isMdfcLand(c)) && isEarlyManaLand(c)) {
                lands++;
            }
        }
        final int cheapSources = cheapManaSources(kept);
        if (lands == 0) {
            return false;
        }
        if (lands == 1 && size >= 6 && cheapSources < 2) {
            return false;
        }
        return !(size >= 6 && lands >= size - 1);
    }

    /**
     * Whether a spell of the hand can be cast with the colours of the lands in it (#12984: a Plains and green one-drops).
     * A colour that the deck makes plentifully may still come, but a hand whose every spell waits for it is no keep.
     */
    static boolean castsSomething(final CardCollectionView kept, final Library lib) {
        final boolean hasBody = kept.stream().anyMatch(Card::isCreature);
        boolean spell = false;
        boolean unplayableAura = false;
        for (final Card c : kept) {
            if (c.isLand() || isMdfcLand(c)) {
                continue;
            }
            if (!hasBody && isCreatureAura(c)) {
                unplayableAura = true;
                continue;
            }
            spell = true;
        }
        if (!spell) {
            return !unplayableAura; // only lands are judged by the other rules; dead Auras are not lands
        }
        return new HandMana(kept, lib).allocate(sources -> castsWith(kept, sources));
    }

    /**
     * The mana the lands of a hand make, by the rules of {@link #addLandMana}: spending restrictions and the abilities
     * that have to be paid for count here too. A colourless land (Wastes) stays colourless. A fetch land is a source only through the one library card it finds: that card has to be one the
     * fetch ability may search for (Evolving Wilds: basic lands only), and a card can be found by one fetch land only.
     */
    private static final class HandMana {
        /** Nodes of the search for a split of the library's lands that a hand may cost before it is not judged. */
        private static final int MAX_NODES = 20000;

        private final Library lib;
        private final CardCollectionView hand;
        /** The lands of the hand that make mana themselves. */
        private final List<Card> own = new ArrayList<>();
        /** For each library land: the fetch lands of the hand (bit by bit) that may find it. */
        private final int[] finders;
        private final int fetches;

        HandMana(final CardCollectionView hand, final Library lib) {
            this.lib = lib;
            this.hand = hand;
            final List<Card> fetchLands = new ArrayList<>();
            for (final Card c : hand) {
                if ((c.isLand() || isMdfcLand(c)) && isEarlyManaLand(c)) {
                    if (fetchesLand(c)) {
                        fetchLands.add(c);
                    } else {
                        own.add(c);
                    }
                }
            }
            fetches = fetchLands.size();
            finders = new int[lib.landCards.size()];
            for (int j = 0; j < fetches; j++) {
                final Card fetch = fetchLands.get(j);
                for (final SpellAbility sa : fetch.getSpellAbilities()) {
                    if (!isLandSearch(sa)) {
                        continue;
                    }
                    final String type = sa.getParamOrDefault("ChangeType", "Card");
                    for (int i = 0; i < finders.length; i++) {
                        if ((finders[i] & (1 << j)) == 0 && mayFind(lib.landCards.get(i), type, fetch, sa)) {
                            finders[i] |= 1 << j;
                        }
                    }
                }
            }
        }

        /** Every colour the hand can make for the spell, though not necessarily all together. */
        byte reachable(final Card spell) {
            byte colors = colorsFor(manaWith(List.of()), spell, hand);
            for (int i = 0; i < finders.length; i++) {
                if (finders[i] != 0) {
                    // the found land may pay for one of the hand's, or meet its condition
                    colors |= colorsFor(manaWith(List.of(lib.landCards.get(i))), spell, hand);
                }
            }
            return colors;
        }

        /**
         * The mana of the lands of the hand and of the lands that the fetch lands found. Activation conditions are
         * judged with the found lands among the cards around, and without the fetch lands, which are gone.
         */
        private List<ManaUnit> manaWith(final List<Card> found) {
            final CardCollection around = new CardCollection();
            for (final Card c : hand) {
                if (!((c.isLand() || isMdfcLand(c)) && isEarlyManaLand(c) && fetchesLand(c))) {
                    around.add(c);
                }
            }
            around.addAll(found);
            final List<ManaUnit> pool = new ArrayList<>();
            for (final Card land : own) {
                addLandMana(pool, land, around);
            }
            for (final Card land : found) {
                addLandMana(pool, land, around);
            }
            return pool;
        }

        /** Whether some choice of the cards the fetch lands find makes {@code test} true for the sources that result. */
        boolean allocate(final Predicate<List<ManaUnit>> test) {
            return allocate(0, new boolean[finders.length], new ArrayList<>(), test, new int[1]);
        }

        private boolean allocate(final int fetch, final boolean[] used, final List<Card> found,
                                 final Predicate<List<ManaUnit>> test, final int[] nodes) {
            if (fetch == fetches) {
                return test.test(manaWith(found));
            }
            if (++nodes[0] > MAX_NODES) {
                // too many ways to split the library to try them all: the best split found greedily, which is checked
                // like any other, so a yes is a verified yes and a no is the cautious answer
                return test.test(manaWith(greedy(fetch, used, found)));
            }
            final Set<String> tried = new HashSet<>();
            boolean finds = false;
            for (int i = 0; i < finders.length; i++) {
                if (used[i] || (finders[i] & (1 << fetch)) == 0) {
                    continue;
                }
                // two lands with the same colours and types (a type may meet a condition) that the same later fetch
                // lands may find are the same choice; a land whose abilities are kept apart is only the same as itself
                final Card land = lib.landCards.get(i);
                final String kind = isPlainLand(land) ? lib.landMasks.get(i) + " " + land.getType() : "#" + i;
                final String key = kind + " " + (finders[i] >>> (fetch + 1));
                if (!tried.add(key)) {
                    continue;
                }
                finds = true;
                used[i] = true;
                found.add(land);
                final boolean ok = allocate(fetch + 1, used, found, test, nodes);
                found.remove(found.size() - 1);
                used[i] = false;
                if (ok) {
                    return true;
                }
            }
            // a found land only adds a source, so finding one is never worse; with nothing to find, it makes no mana
            return !finds && allocate(fetch + 1, used, found, test, nodes);
        }

        /** The lands found when each remaining fetch land takes the unused land that adds the most colours. */
        private List<Card> greedy(final int from, final boolean[] used, final List<Card> found) {
            final List<Card> out = new ArrayList<>(found);
            final boolean[] taken = used.clone();
            byte have = 0;
            for (final ManaUnit u : manaWith(out)) {
                have |= u.colors;
            }
            for (int f = from; f < fetches; f++) {
                int best = -1;
                int bestNew = -1;
                for (int i = 0; i < finders.length; i++) {
                    if (taken[i] || (finders[i] & (1 << f)) == 0) {
                        continue;
                    }
                    final int added = Integer.bitCount(lib.landMasks.get(i) & ~have & 0xFF);
                    if (added > bestNew) {
                        bestNew = added;
                        best = i;
                    }
                }
                if (best >= 0) {
                    taken[best] = true;
                    out.add(lib.landCards.get(best));
                    have |= lib.landMasks.get(best);
                }
            }
            return out;
        }

        /** Whether the colours of the spell are all made together by some choice of what the fetch lands find. */
        boolean makesColorsOf(final Card spell) {
            if (spell.getManaCost().getColorProfile() == 0) {
                return true;
            }
            // one source for each coloured pip: a Tropical Island makes green or blue, never {G}{U} by itself
            return allocate(sources -> canPayPips(sources, spell, hand));
        }
    }

    /**
     * Whether the search of a fetch land may find the card. Card.isValid takes one restriction, so the alternatives of
     * the script ("Plains,Island" for Flooded Strand) are split here: any one of them is enough.
     */
    private static boolean mayFind(final Card candidate, final String type, final Card fetch, final SpellAbility sa) {
        for (final String restriction : type.split(",")) {
            if (candidate.isValid(restriction, fetch.getController(), fetch, sa)) {
                return true;
            }
        }
        return false;
    }

    /** A fetch land's ability: search the library for a card and put it onto the battlefield. */
    private static boolean isLandSearch(final SpellAbility sa) {
        return sa.getApi() == ApiType.ChangeZone && "Library".equals(sa.getParam("Origin"))
                && "Battlefield".equals(sa.getParam("Destination"));
    }

    /** Whether a spell of the hand can be paid with this mana, one mana for each coloured pip. */
    private static boolean castsWith(final CardCollectionView kept, final List<ManaUnit> pool) {
        for (final Card c : kept) {
            if (!c.isLand() && !isMdfcLand(c) && !isCreatureAura(c)
                    && c.getManaCost().canBePaidWithAvailable(colorsFor(pool, c, kept)) // hybrid shards: either colour pays
                    && canPayPips(pool, c, kept)) {
                return true;
            }
        }
        return false;
    }

    /** Whether every mana is free to spend and every land makes just one: then only the colours matter. */
    private static boolean isPlain(final List<ManaUnit> pool) {
        return pool.stream().allMatch(u -> u.limit == null && u.choice == null && !u.unfed);
    }

    /** The colours of the mana that may be spent on the card, through the lands that have to be paid for too. */
    private static byte colorsFor(final List<ManaUnit> pool, final Card card, final CardCollectionView hand) {
        byte colors = 0;
        for (final ManaUnit u : pool) {
            if (!u.unfed && (u.limit == null
                    || spellsOf(card).stream().anyMatch(spell -> meetsRestriction(u, spell)))) {
                colors |= u.colors;
            }
        }
        for (final List<ManaUnit> through : filtered(pool, hand, Collections.emptySet())) {
            colors |= colorsFor(through, card, hand);
        }
        return (byte) (colors & MagicColor.ALL_COLORS);
    }

    /**
     * Whether the mana pays all the coloured pips of the card together. How much mana the rest of the cost takes is
     * not looked at; the pips left to other kinds of payment are those of {@link ManaSourceAllocation}.
     */
    private static boolean canPayPips(final List<ManaUnit> pool, final Card card, final CardCollectionView hand) {
        if (isPlain(pool)) {
            final List<Byte> colors = new ArrayList<>();
            for (final ManaUnit u : pool) {
                colors.add(u.colors);
            }
            return ManaSourceAllocation.canPayColoredPips(card.getManaCost(), colors);
        }
        final List<ManaCostShard> pips = new ArrayList<>();
        for (final ManaCostShard shard : card.getManaCost()) {
            if (!shard.isPhyrexian() && !shard.isOr2Generic() && !shard.isColorless() && shard.getColorMask() != 0) {
                pips.add(shard);
            }
        }
        return pips.isEmpty() || spellsOf(card).stream().anyMatch(spell ->
                hasPayment(pool, pips, 0, spell, null, hand, Collections.emptySet()));
    }

    /** The ways to cast the card. */
    private static List<SpellAbility> spellsOf(final Card card) {
        final List<SpellAbility> spells = new ArrayList<>();
        for (final SpellAbility sa : card.getSpellAbilities()) {
            if (sa.isSpell()) {
                spells.add(sa);
            }
        }
        return spells;
    }

    /** Whether the lands of the hand (and what its fetch lands find) pay for the coloured pips of the spell together. */
    static boolean makesColorsOf(final CardCollectionView hand, final Player ai, final Card spell) {
        return new HandMana(hand, new Library(ai)).makesColorsOf(spell);
    }

    static boolean castsSomething(final CardCollectionView kept, final Player ai) {
        return castsSomething(kept, new Library(ai));
    }

    /** One land and a 1 mana outlet in a dredge deck: Faithless Looting on turn one is a normal keep. */
    /** One land and a 1 mana outlet that land can pay for in a dredge deck: Faithless Looting on turn one is a keep. */
    private static boolean dredgeOneLander(final CardCollectionView kept, final DeckPlan plan) {
        if (plan == null || !plan.combos.has(ComboPlan.Kind.DREDGE)) {
            return false;
        }
        int lands = 0;
        final List<ManaUnit> mana = new ArrayList<>();
        for (final Card c : kept) {
            if ((c.isLand() && CardPredicates.LANDS_PRODUCING_MANA.test(c)) || (isMdfcLand(c) && isManaLand(c))) {
                lands++;
                addLandMana(mana, c, kept);
            }
        }
        if (lands != 1) {
            return false;
        }
        for (final Card c : kept) {
            if (!c.isLand() && c.getCMC() <= 1 && ComboPlan.isDredgeOutlet(c)
                    && (c.getManaCost().getColorProfile() & ~colorsFor(mana, c, kept)) == 0) {
                return true;
            }
        }
        return false;
    }

    /** The cards to put on the bottom so that the rest scores best (London mulligan). */
    public static CardCollection toBottom(final Player ai, final CardCollectionView hand, final int count,
                                          final boolean onPlay, final DeckPlan plan) {
        return bottom(ai, hand, count, onPlay, plan, new Library(ai));
    }

    private static CardCollection bottom(final Player ai, final CardCollectionView hand, final int count,
                                         final boolean onPlay, final DeckPlan plan, final Library lib) {
        final CardCollection remaining = new CardCollection(hand);
        final CardCollection out = new CardCollection();
        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            Card worst = null;
            double best = -Double.MAX_VALUE;
            for (final Card c : remaining) {
                final CardCollection rest = new CardCollection(remaining);
                rest.remove(c);
                final double s = rawScore(rest, lib, onPlay, plan);
                if (s > best) { // ties keep the earlier card, so equal lands are bottomed in hand order
                    best = s;
                    worst = c;
                }
            }
            remaining.remove(worst);
            out.add(worst);
        }
        return out;
    }

    /** What is left to draw from. */
    private static final class Library {
        final int size;
        final int lands;
        final int[] producers = new int[5];
        /** The cards of the library that are lands making coloured mana, and the colours of each (what a fetch land can find). */
        final List<Card> landCards = new ArrayList<>();
        final List<Byte> landMasks = new ArrayList<>();

        Library(final Player ai) {
            final CardCollectionView lib = ai.getCardsIn(ZoneType.Library);
            int l = 0;
            for (final Card c : lib) {
                if (isManaLand(c)) {
                    l++;
                    final byte colors = producedColors(c);
                    if (colors != 0) {
                        landCards.add(c);
                        landMasks.add(colors);
                    }
                    for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                        if ((colors & MagicColor.WUBRG[i]) != 0) {
                            producers[i]++;
                        }
                    }
                }
            }
            size = lib.size();
            lands = l;
        }

        int producersOf(final byte color) {
            for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                if (MagicColor.WUBRG[i] == color) {
                    return producers[i];
                }
            }
            return 0;
        }
    }

    static byte producedColors(final Card land) {
        byte colors = 0;
        final Iterable<SpellAbility> abilities = isMdfcLand(land)
                ? land.getState(CardStateName.Backside).getManaAbilities() : land.getManaAbilities();
        for (final SpellAbility sa : abilities) {
            if (activationRequirementsMet(land, sa, null)) {
                colors |= ManaConversion.producedColors(sa); // short letters ("R"), and it sets the activator itself
            }
        }
        return colors;
    }

    /** 0..100: how good the hand is for the plan. */
    static double score(final CardCollectionView hand, final Library lib, final boolean onPlay, final DeckPlan plan) {
        return Math.max(0, Math.min(100, rawScore(hand, lib, onPlay, plan)));
    }

    /** The same before it is clamped: what the bottoming compares, so that strong hands do not tie at 100. */
    private static double rawScore(final CardCollectionView hand, final Library lib, final boolean onPlay, final DeckPlan plan) {
        final DeckPlan.Archetype arch = plan == null ? DeckPlan.Archetype.MIDRANGE : plan.archetype;
        final boolean dredge = plan != null && plan.combos.has(ComboPlan.Kind.DREDGE);
        final Want forCombo = dredge ? DREDGE_WANT : comboWant(plan);
        final Want w = forCombo != null ? forCombo : want(arch);
        final int size = hand.size();
        int lands = 0;
        final int rocks = cheapManaSources(hand);
        int landKeys = 0;
        boolean access = false;
        boolean quickWiper = false;
        boolean stage = false;
        final boolean depths = plan != null && plan.combos.has(ComboPlan.Kind.DARK_DEPTHS);
        final HandMana mana = new HandMana(hand, lib); // the colours the lands make together, fetch lands included
        final List<Card> spells = new ArrayList<>();
        int outlets = 0;
        int pieces = 0;
        for (final Card c : hand) {
            if (depths) {
                access |= ComboPlan.isDepthsAccess(c);
                stage |= ComboPlan.isStage(c);
                quickWiper |= ComboPlan.isCounterWiper(c);
            }
            if (c.isLand() || isMdfcLand(c)) {
                if (isEarlyManaLand(c)) {
                    lands++;
                }
                if (depths && ComboPlan.isDepthsPiece(c)) {
                    landKeys++; // Dark Depths and the Stage are lands, and the pieces the deck is built around
                }
            } else {
                spells.add(c);
                if (dredge) {
                    outlets += ComboPlan.isDredgeOutlet(c) ? 1 : 0;
                    pieces += ComboPlan.isDredgeHandPiece(c) ? 1 : 0;
                }
            }
        }
        if (spells.isEmpty() && landKeys == 0) {
            return 0;
        }
        // a ramp deck counts its cheap mana sources as lands once there are two lands to cast them
        final int sources = lands + (arch == DeckPlan.Archetype.RAMP && lands >= 2 ? Math.min(rocks, 2) : 0);
        final int draws = Math.max(0, w.byTurn - (onPlay ? 1 : 0));
        final double hit = atLeast(w.lands - sources, draws, lib.size, lib.lands);

        double early = 0;
        int keys = landKeys;
        // an Aura for a creature is no play and no key card in a hand without a creature to put it on (#12929)
        final boolean hasBody = spells.stream().anyMatch(c -> c.isCreature());
        for (final Card c : spells) {
            if (!hasBody && isCreatureAura(c)) {
                continue;
            }
            if (c.getCMC() <= w.playCmc) {
                early += colorCredit(c, mana, lib);
            }
            if (isKeyCard(c, arch) || plan != null && (plan.combos.isKeyCard(c) || plan.commander.isKeyCard(c))
                || dredge && (ComboPlan.isDredgeOutlet(c) || ComboPlan.isDredgeHandPiece(c))) {
                keys++;
            }
        }
        final double playsPart = Math.min(1.0, early / w.plays);
        final double keysPart = Math.min(1.0, (double) keys / w.keys);
        final double quality = 0.6 * playsPart + 0.4 * keysPart;
        double score = 100 * (0.55 * hit + 0.45 * quality) * Math.sqrt(hit);

        if (early == 0 && keys == 0 && size >= 6 && !spells.isEmpty() && forCombo == null && !dredge
                && arch != DeckPlan.Archetype.LANDS) {
            score -= 20; // lands and spells that cannot be cast (Auras without a creature): a keep only on the lands' merit
        }
        if (lands == 0) {
            score -= 60; // bottoming and mulligan rules must never leave a hand without a land
        }
        if (size >= 5 && lands >= size - 1) {
            score -= 35; // flooded: nothing to spend the mana on
        } else if (size >= 6 && lands >= size - 2 && spells.size() <= 2 && arch != DeckPlan.Archetype.RAMP
                && arch != DeckPlan.Archetype.LANDS) {
            score -= 15;
        }
        if (depths && access && (quickWiper || stage)) {
            // Dark Depths (or the spell that fetches it), a piece that takes the counters off, and the two mana that
            // pays for it (the Stage taps itself for its own ability, so it is no source for it)
            final int others = (quickWiper ? lands : lands - 1) + (lands >= 2 ? Math.min(rocks, 1) : 0);
            if (others >= 2) {
                score += DEPTHS_READY_HAND;
            }
        }
        if (dredge && lands >= 1 && outlets >= 1 && pieces >= 1) {
            score += DREDGE_READY_HAND;
        }
        if (plan != null) {
            score += plan.combos.handBonus(hand);
        }
        score += StifleCombo.handBonus(hand);
        return score;
    }

    /** 1 when the lands in hand make the colours of the spell, less when a missing colour may still be drawn. */
    private static double colorCredit(final Card c, final HandMana mana, final Library lib) {
        if (mana.makesColorsOf(c)) {
            return 1.0;
        }
        final byte need = c.getManaCost().getColorProfile();
        final int missing = need & ~mana.reachable(c);
        if (missing == 0) {
            return 0.5; // every colour is within reach of the fetch lands, but not all of them at once
        }
        for (final byte col : MagicColor.WUBRG) {
            if ((missing & col) != 0 && lib.producersOf(col) < 4) {
                return 0.0; // a colour the deck can hardly make
            }
        }
        return 0.5;
    }

    /**
     * How many cheap mana sources of the hand work together. A source counts only if the hand can put it on the
     * battlefield: its mana cost and colours are paid by the lands, plus the sources that came before it (cheapest
     * first, so a Mox pays for the Signet after it), and so is the mana its own ability costs (a Signet's {1}). Sources
     * that need nothing else come first; one that taps other permanents (Springleaf Drum) then counts only if the hand
     * has permanents for it that can be put on the battlefield the same way and that nothing else uses: not a source
     * of mana itself (a Llanowar Elves tapped for the Drum makes no mana of its own) and not the tap cost of another
     * source. A source also has to add mana: a Signet pays one for two, a Mana Cylix pays one for one and only filters.
     * The mana is tracked unit by unit, with its colours, so a cost is paid in full ({2}{U}{U}{U}{U} needs four blue).
     * Mana spent on one card is not taken off for the next: this is a judgement over the first turns.
     */
    static int cheapManaSources(final CardCollectionView hand) {
        final List<Card> free = new ArrayList<>();
        final List<Card> needy = new ArrayList<>();
        for (final Card c : hand) {
            if (c.isLand() || isMdfcLand(c)) {
                continue;
            }
            if (c.getCMC() > 2 || c.isInstant() || c.isSorcery()) {
                continue;
            }
            final CostNeed need = costNeed(c);
            if (need == CostNeed.NONE) {
                free.add(c);
            } else if (need == CostNeed.OTHER_PERMANENTS) {
                needy.add(c);
            }
        }
        free.sort(Comparator.comparingInt(Card::getCMC));
        needy.sort(Comparator.comparingInt(Card::getCMC));

        final Set<Card> unavailable = new HashSet<>(); // counted as a source, or already tapped for another one
        final List<ManaUnit> pool = new ArrayList<>(); // the mana the lands and the sources counted so far can make
        for (final Card c : hand) {
            if ((c.isLand() || isMdfcLand(c)) && isEarlyManaLand(c)) {
                addLandMana(pool, c, hand);
            }
        }
        int sources = 0;
        for (final Card c : free) {
            final Flow flow = manaFlow(c);
            if (flow != null && flow.net >= 1 && canDeploy(c, pool, hand, unavailable) && flow.paid <= unrestricted(pool)
                    && (!flow.restricted || canSpendRestricted(c, flow, hand, pool, unavailable))) {
                sources++;
                unavailable.add(c);
                addOutput(pool, c, flow);
            }
        }
        for (final Card c : needy) {
            final Flow flow = manaFlow(c);
            if (flow == null || flow.net < 1 || !canDeploy(c, pool, hand, unavailable) || flow.paid > unrestricted(pool)
                    || flow.restricted && !canSpendRestricted(c, flow, hand, pool, unavailable)) {
                continue;
            }
            final Set<Card> tapped = tapResources(c, hand, unavailable, pool);
            if (tapped != null) {
                sources++;
                addOutput(pool, c, flow);
                unavailable.addAll(tapped);
                unavailable.add(c);
            }
        }
        return sources;
    }

    /**
     * One mana of the hand: the colours it can be (none: colourless), and, for mana that may only be spent in some
     * ways, the mana ability and the card that make it. A land with a restricted mana ability also names the ability
     * that makes each of its mana ({@code choice}): its abilities are alternatives, as tapping it for one rules out
     * the others.
     */
    private static final class ManaUnit {
        final byte colors;
        final AbilityManaPart limit;
        final Card source;
        final SpellAbility choice;
        /** Not mana yet: stands for a land that makes none until its ability is paid for. */
        boolean unfed;
        /** Mana from an ability that has already been activated (for example, a filter land or Phyrexian Tower). */
        boolean activated;

        static ManaUnit unfed(final Card land, final SpellAbility ability) {
            final ManaUnit unit = new ManaUnit((byte) 0, null, land, ability);
            unit.unfed = true;
            return unit;
        }

        static ManaUnit consumed(final Card card) {
            final ManaUnit unit = new ManaUnit((byte) 0, null, card);
            unit.unfed = true;
            return unit;
        }

        ManaUnit(final byte colors, final AbilityManaPart limit, final Card source) {
            this(colors, limit, source, null);
        }

        ManaUnit(final byte colors, final AbilityManaPart limit, final Card source, final SpellAbility choice) {
            this.colors = colors;
            this.limit = limit;
            this.source = source;
            this.choice = choice;
        }

        /** Whether the two cannot both be had: they come from different abilities of the same land. */
        boolean excludes(final ManaUnit other) {
            return choice != null && other.choice != null && source == other.source && choice != other.choice;
        }
    }

    /** The colour of a "Produced" token: none for {C}, every colour for what is not one (a chosen colour, "Any"). */
    private static byte colorOf(final String token) {
        switch (token) {
            case "W": return MagicColor.WHITE;
            case "U": return MagicColor.BLUE;
            case "B": return MagicColor.BLACK;
            case "R": return MagicColor.RED;
            case "G": return MagicColor.GREEN;
            case "C": return 0;
            default: return MagicColor.ALL_COLORS; // a chosen colour, "Any": the best case
        }
    }

    /** The mana of an activation, one unit for each mana it makes: a Signet makes a {W} and a {U}, not either twice. */
    private static List<ManaUnit> outputOf(final Card c, final Flow flow) {
        final List<ManaUnit> units = new ArrayList<>();
        final AbilityManaPart limit = flow.restricted ? flow.sa.getManaPart() : null;
        final int amount = Integer.parseInt(flow.sa.getParamOrDefault("Amount", "1"));
        final String[] tokens = flow.sa.getParamOrDefault("Produced", "").trim().split("\\s+");
        final boolean combo = "Combo".equals(tokens[0]);
        for (int i = 0; i < amount; i++) {
            if (combo || "Any".equals(tokens[0])) {
                // one mana of one of the colours that follow ("Combo B G U"), or of any: a single unit, not one for each token
                byte colors = 0;
                for (int t = 1; t < tokens.length; t++) {
                    colors |= colorOf(tokens[t]);
                }
                units.add(new ManaUnit(combo && tokens.length > 1 && !"Any".equals(tokens[1]) ? colors : MagicColor.ALL_COLORS,
                        limit, c));
                continue;
            }
            for (final String token : tokens) {
                units.add(new ManaUnit(colorOf(token), limit, c)); // "W U" makes both
            }
        }
        return units;
    }

    /** Takes what the activation costs out of the pool (the least flexible mana first) and adds what it makes. */
    private static void addOutput(final List<ManaUnit> pool, final Card c, final Flow flow) {
        final List<ManaUnit> made = outputOf(c, flow);
        payActivation(pool, flow.paid);
        if (!flow.restricted) { // restricted mana pays for what it is restricted to, not for other sources
            pool.addAll(made);
        }
    }

    /**
     * Adds the mana of a land of the hand. A land that makes one mana at a time, with no spending restriction and no
     * mana to pay, is one mana of any colour it makes. Otherwise each ability keeps its own output and restriction: Ancient
     * Tomb makes two, and Sliver Hive makes {C} for anything or a colour for a Sliver spell, never a colour for anything.
     */
    private static void addLandMana(final List<ManaUnit> pool, final Card c, final CardCollectionView hand) {
        if (isPlainLand(c)) {
            pool.add(new ManaUnit((byte) (producedColors(c) & MagicColor.ALL_COLORS), null, null));
            return;
        }
        final int before = pool.size();
        SpellAbility fed = null;
        for (final SpellAbility sa : c.getManaAbilities()) {
            if (!activationRequirementsMet(c, sa, hand)) {
                continue;
            }
            if (!paysOtherCostsItself(c, sa)) {
                if (externalSacrificeCost(sa) != null) {
                    pool.add(ManaUnit.unfed(c, sa));
                }
                continue; // an activation that needs another permanent is considered by filtered()
            }
            if (needsMana(sa)) {
                fed = sa; // makes nothing by itself: see filtered
                continue;
            }
            final AbilityManaPart limit = isRestricted(sa) ? sa.getManaPart() : null;
            if (hasFixedAmount(sa) && (manaProduced(sa) == 1 || makesItsFullAmount(c, sa))) {
                pool.addAll(landOutput(c, sa));
            } else { // reflected mana, a variable amount, or more that asks for something first: one mana
                pool.add(new ManaUnit(ManaConversion.producedColors(sa), limit, c, sa));
            }
        }
        if (fed != null && pool.size() == before) {
            pool.add(ManaUnit.unfed(c, fed)); // Skycloud Expanse: no mana until another source pays for it
        }
    }

    /** Whether the card's land is one mana of any colour it makes, with nothing else to know about it. */
    private static boolean isPlainLand(final Card c) {
        return !c.isLand() && !isMdfcLand(c) || !hasRestrictedMana(c) && !makesSeveralMana(c)
                && c.getManaAbilities().stream().noneMatch(OpeningHand::hasPresenceRequirement)
                && c.getManaAbilities().stream().allMatch(sa -> !needsMana(sa) && paysOtherCostsItself(c, sa));
    }

    private static boolean hasPresenceRequirement(final SpellAbility sa) {
        final SpellAbilityRestriction restriction = sa.getRestrictions();
        return restriction != null && restriction.getIsPresent() != null;
    }

    /**
     * Checks an ability's actual activation requirements. IsPresent normally checks the current battlefield; during
     * opening-hand evaluation, lands and castable zero-cost permanents in the hand may be played first, so include
     * those as hypothetical battlefield permanents. Other restriction checks are still delegated to the engine.
     */
    private static boolean activationRequirementsMet(final Card land, final SpellAbility sa,
                                                     final CardCollectionView hand) {
        ManaConversion.withActivator(sa);
        if (!sa.metConditions()) {
            return false;
        }
        final SpellAbilityRestriction restriction = sa.getRestrictions();
        if (restriction == null) {
            return true;
        }
        final Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            return false;
        }
        // The land is hypothetically on the battlefield. Check who/when may activate it, but defer its zone check.
        if (!restriction.checkActivatorRestrictions(land, sa) || !restriction.checkTimingRestrictions(land, sa)) {
            return false;
        }
        if (restriction.getLimitToCheck() != null
                && sa.getActivationsThisTurn() >= AbilityUtils.calculateAmount(land, restriction.getLimitToCheck(), sa)) {
            return false;
        }
        if (restriction.getGameLimitToCheck() != null
                && sa.getActivationsThisGame() >= AbilityUtils.calculateAmount(
                land, restriction.getGameLimitToCheck(), sa)) {
            return false;
        }
        final String present = restriction.getIsPresent();
        if (present != null) {
            final List<GameObject> candidates = new ArrayList<>();
            if (restriction.getPresentDefined() != null) {
                candidates.addAll(AbilityUtils.getDefinedObjects(land, restriction.getPresentDefined(), sa));
            } else {
                for (final ZoneType zone : restriction.getPresentZones()) {
                    for (final Card card : land.getGame().getCardsIn(zone)) {
                        candidates.add(card);
                    }
                    if (zone == ZoneType.Battlefield && hand != null) {
                        for (final Card card : hand) {
                            if ((card.isLand() || isMdfcLand(card)) && isEarlyManaLand(card)) {
                                candidates.add(card);
                            }
                        }
                        // A zero-cost permanent such as Ornithopter can be cast before the land's mana ability.
                        // Do not count permanents that need mana here: this check does not consume mana from the
                        // pool, and counting one could let the same source pay for both the permanent and the spell.
                        for (final Card card : hand) {
                            if (card != land && !card.isLand() && !isMdfcLand(card) && isFreePermanentEnabler(card)) {
                                candidates.add(card);
                            }
                        }
                    }
                }
            }
            final Predicate<GameObject> matches = GameObjectPredicates.restriction(
                    present.split(","), activator, land, sa);
            final int count = (int) candidates.stream().filter(matches).count();
            final String compare = restriction.getPresentCompare();
            final int right = AbilityUtils.calculateAmount(land, compare.substring(2), sa);
            if (!Expressions.compare(count, compare, right)) {
                return false;
            }
            // Check the rest of the activation restrictions without rechecking IsPresent against the real battlefield.
            restriction.setIsPresent(null);
            try {
                return restriction.checkOtherRestrictions(land, sa, activator);
            } finally {
                restriction.setIsPresent(present);
            }
        }
        return restriction.checkOtherRestrictions(land, sa, activator);
    }

    /** A single creature (or other valid permanent) from the hand may pay this land's sacrifice cost. */
    private static CostSacrifice externalSacrificeCost(final SpellAbility sa) {
        if (sa.getPayCosts() == null || !sa.getPayCosts().hasTapCost()) {
            return null;
        }
        CostSacrifice sacrifice = null;
        for (final CostPart part : sa.getPayCosts().getCostParts()) {
            if (part instanceof CostTap || part instanceof CostPartMana || part instanceof CostPayLife) {
                continue;
            }
            if (part instanceof CostSacrifice && !part.payCostFromSource() && sacrifice == null
                    && "1".equals(part.getAmount())) {
                sacrifice = (CostSacrifice) part;
            } else {
                return null;
            }
        }
        return sacrifice;
    }

    /** Whether a permanent in hand has a normal cast cost made up of mana cost parts only. */
    private static boolean isSimpleFodderSpell(final SpellAbility spell) {
        return spell.isSpell() && spell.getPayCosts() != null
                && spell.getPayCosts().getCostParts().stream().allMatch(part -> part instanceof CostPartMana);
    }

    /**
     * Whether a permanent can be put onto the battlefield from the hand without spending mana. These permanents can
     * safely satisfy IsPresent during opening-hand evaluation because doing so does not consume any of the mana that
     * the land's ability or the spell being evaluated may need. Additional costs (discard, sacrifice, etc.) are not
     * guessed at.
     */
    private static boolean isFreePermanentEnabler(final Card card) {
        if (!card.isPermanent()) {
            return false;
        }
        for (final SpellAbility spell : card.getSpellAbilities()) {
            if (!isSimpleFodderSpell(spell)) {
                continue;
            }
            final CostPartMana cost = spell.getPayCosts().getCostMana();
            if (cost == null || cost.getMana().getCMC() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the land can pay by itself, when it is first played, what the ability asks besides mana: tapping, life,
     * sacrificing or exerting itself, and counters or energy that it gets on entering (Gemstone Mine, Vivid Crag,
     * Aether Hub). Not another card (Phyrexian Tower's creature), nor counters it has to build up (Mirrodin's Core).
     */
    private static boolean paysOtherCostsItself(final Card land, final SpellAbility sa) {
        return sa.getPayCosts() == null || sa.getPayCosts().getCostParts().stream().allMatch(part ->
                part instanceof CostTap || part instanceof CostPartMana || part instanceof CostPayLife
                || (part instanceof CostSacrifice || part instanceof CostExert) && part.payCostFromSource()
                || part instanceof CostRemoveCounter && part.payCostFromSource() && part.getAmount().matches("\\d+")
                        && countersOnEntering(land, ((CostRemoveCounter) part).counter)
                                >= Integer.parseInt(part.getAmount())
                || part instanceof CostPayEnergy && part.getAmount().matches("\\d+")
                        && energyOnEntering(land) >= Integer.parseInt(part.getAmount()));
    }

    /** The counters of this kind the card enters with, read from its script. */
    private static int countersOnEntering(final Card c, final CounterType type) {
        int n = 0;
        for (final KeywordInterface k : c.getKeywords()) {
            final String[] words = k.getOriginal().split(":");
            if (words.length > 2 && "etbCounter".equals(words[0]) && words[2].matches("\\d+")
                    && type.equals(CounterType.getType(words[1]))) {
                n += Integer.parseInt(words[2]);
            }
        }
        for (final String script : c.getSVars().values()) { // Vivid Crag: part of how it enters tapped
            if (script.contains("PutCounter") && script.contains("ETB$ True")) {
                n += countersPut(script, type);
            }
        }
        return n;
    }

    /** The energy the card's own entering gives. */
    private static int energyOnEntering(final Card c) {
        int n = 0;
        for (final Trigger t : c.getTriggers()) {
            if (isOwnEntering(t) && t.hasParam("Execute")) {
                n += countersPut(c.getSVar(t.getParam("Execute")), CounterType.getType("ENERGY"));
            }
        }
        return n;
    }

    /** How many counters of this kind the script of a PutCounter effect puts; 0 for another kind or no fixed number. */
    private static int countersPut(final String script, final CounterType type) {
        final Matcher kind = COUNTER_KIND.matcher(script);
        if (!kind.find() || !type.equals(CounterType.getType(kind.group(1)))) {
            return 0;
        }
        final Matcher amount = COUNTER_NUMBER.matcher(script);
        if (!amount.find()) {
            return 1;
        }
        return amount.group(1).matches("\\d+") ? Integer.parseInt(amount.group(1)) : 0;
    }

    private static boolean isOwnEntering(final Trigger t) {
        return t.getMode() == TriggerType.ChangesZone && "Battlefield".equals(t.getParam("Destination"))
                && "Card.Self".equals(t.getParam("ValidCard"));
    }

    /** Whether some ability of the land can be counted on for two or more mana. */
    private static boolean makesSeveralMana(final Card c) {
        for (final SpellAbility sa : c.getManaAbilities()) {
            if (!needsMana(sa) && hasFixedAmount(sa) && manaProduced(sa) > 1 && makesItsFullAmount(c, sa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether an opening hand gets all the mana of the ability by tapping the land, perhaps paying life too (Ancient
     * Tomb). Not when something else is asked first: a creature to sacrifice (Phyrexian Tower), counters (Hickory
     * Woodlot), lands to give up on entering (Lotus Vale) or to return once it has (Azorius Chancery).
     */
    private static boolean makesItsFullAmount(final Card c, final SpellAbility sa) {
        return sa.getPayCosts() != null
                && sa.getPayCosts().getCostParts().stream()
                        .allMatch(part -> part instanceof CostTap || part instanceof CostPayLife)
                && worksOnceInPlay(c, sa)
                && c.getTriggers().stream().noneMatch(OpeningHand::isOwnEntering);
    }

    private static boolean needsMana(final SpellAbility sa) {
        return sa.getPayCosts() != null && sa.getPayCosts().getCostMana() != null
                && sa.getPayCosts().getCostMana().getMana().getCMC() > 0;
    }

    /** Whether the ability is a plain mana ability that says how much it makes with a number. */
    private static boolean hasFixedAmount(final SpellAbility sa) {
        return sa.getApi() == ApiType.Mana && sa.getParamOrDefault("Amount", "1").matches("\\d+");
    }

    private static boolean hasRestrictedMana(final Card c) {
        for (final SpellAbility sa : c.getManaAbilities()) {
            if (isRestricted(sa)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the mana of the ability may be spent only in some ways. */
    private static boolean isRestricted(final SpellAbility sa) {
        final AbilityManaPart part = sa.getManaPart();
        return part != null && (sa.hasParam("RestrictValid") || !part.getManaRestrictions().isEmpty());
    }

    /** How much of the mana can pay for an ability: the restricted mana (Ancient Ziggurat's) cannot. */
    private static int unrestricted(final List<ManaUnit> pool) {
        return mostTogether(pool, u -> u.limit == null && !u.unfed);
    }

    /** How many of the matching mana can be had at once: of each land with a choice, only its best ability counts. */
    private static int mostTogether(final List<ManaUnit> units, final Predicate<ManaUnit> matching) {
        int n = 0;
        final Set<Card> counted = new HashSet<>();
        for (final ManaUnit u : units) {
            if (!matching.test(u)) {
                continue;
            }
            if (u.choice == null) {
                n++;
            } else if (counted.add(u.source)) {
                int best = 0;
                for (final ManaUnit first : units) {
                    if (first.source == u.source && first.choice != null) {
                        int same = 0;
                        for (final ManaUnit v : units) {
                            if (v.source == u.source && v.choice == first.choice && matching.test(v)) {
                                same++;
                            }
                        }
                        best = Math.max(best, same);
                    }
                }
                n += best;
            }
        }
        return n;
    }

    /**
     * Takes what an activation costs out of the pool: unrestricted mana, the least flexible first. Tapping a land for
     * one of its abilities takes the mana of its other abilities with it, so a land is tapped for the ability that
     * makes the most.
     */
    private static void payActivation(final List<ManaUnit> pool, final int amount) {
        for (int i = 0; i < amount; i++) {
            ManaUnit least = null;
            for (final ManaUnit u : pool) {
                if (u.limit == null && !u.unfed && isFromBestAbility(pool, u)
                        && (least == null || Integer.bitCount(u.colors) < Integer.bitCount(least.colors))) {
                    least = u;
                }
            }
            if (least == null) {
                return;
            }
            pool.remove(least);
            pool.removeIf(least::excludes);
        }
    }

    /** Whether no other ability of the same land makes more unrestricted mana than the one that makes this one. */
    private static boolean isFromBestAbility(final List<ManaUnit> pool, final ManaUnit unit) {
        if (unit.choice == null) {
            return true;
        }
        final List<ManaUnit> land = new ArrayList<>(pool);
        land.removeIf(u -> u.source != unit.source || u.limit != null);
        return mostTogether(land, u -> true) == land.stream().filter(u -> u.choice == unit.choice).count();
    }

    /**
     * Whether the mana can pay the whole cost: each coloured part with a mana that fits it and the generic part with
     * what is left. With {@code spell}, restricted mana counts when the spell meets its restriction, and then only
     * for the parts it may pay; {@code mustUse} asks for a payment in which restricted mana of that card is used.
     * The abilities of lands that have to be paid for are used when other mana at hand pays for them.
     */
    private static boolean canPay(final List<ManaUnit> pool, final ManaCost cost, final SpellAbility spell,
                                  final Card mustUse, final CardCollectionView hand, final Set<Card> unavailable) {
        final List<ManaCostShard> shards = new ArrayList<>();
        for (final ManaCostShard shard : cost) {
            shards.add(shard);
        }
        return hasPayment(pool, shards, cost.getGenericCost(), spell, mustUse, hand, unavailable);
    }

    /** Whether there is a way to pay, through the lands that have to be paid for if need be: the first one will do. */
    private static boolean hasPayment(final List<ManaUnit> pool, final List<ManaCostShard> shards, final int generic,
                                      final SpellAbility spell, final Card mustUse, final CardCollectionView hand,
                                      final Set<Card> unavailable) {
        if (payShards(pool, shards, 0, -1, new boolean[pool.size()], generic, spell, mustUse, false,
                new HashSet<>(), spare -> true)) {
            return true;
        }
        for (final List<ManaUnit> through : filtered(pool, hand, unavailable)) {
            if (hasPayment(through, shards, generic, spell, mustUse, hand, unavailable)) {
                return true;
            }
        }
        return false;
    }

    /** The mana that each way of paying from the pool as it is leaves over, once for each set of mana left. */
    private static List<List<ManaUnit>> paymentResults(final List<ManaUnit> pool, final List<ManaCostShard> shards,
                                                       final int generic, final SpellAbility spell) {
        final List<List<ManaUnit>> results = new ArrayList<>();
        payShards(pool, shards, 0, -1, new boolean[pool.size()], generic, spell, null, false, new HashSet<>(),
                spare -> {
                    results.add(spare);
                    return false; // all of them are wanted
                });
        return results;
    }

    /** How many lands that have to be paid for one payment may go through. */
    private static final int MAX_FILTERS = 2;

    /**
     * The pools that result from paying for one ability of a land that asks for one mana: Cascade Bluffs makes two blue
     * or red mana only when another source pays {U/R}. That mana is spent, and the land's other mana goes, as it taps
     * once. Abilities that ask for more mana than that are not judged: to the safe side they add nothing. Nor is a
     * payment through more than {@value #MAX_FILTERS} such lands: every order of them would have to be tried.
     */
    private static List<List<ManaUnit>> filtered(final List<ManaUnit> pool, final CardCollectionView hand,
                                                 final Set<Card> unavailable) {
        final List<List<ManaUnit>> out = new ArrayList<>();
        if (pool.stream().filter(u -> u.activated)
                .map(u -> u.source).distinct().count() >= MAX_FILTERS) {
            return out;
        }
        final Set<Card> lands = new HashSet<>();
        for (final ManaUnit idle : pool) {
            // a land that still has the mana of a free ability, or none yet: it is untapped
            if (idle.choice == null || idle.activated || !lands.add(idle.source)) {
                continue;
            }
            for (final SpellAbility sa : idle.source.getManaAbilities()) {
                if (!activationRequirementsMet(idle.source, sa, hand)) {
                    continue;
                }
                if (isFilter(idle.source, sa)) {
                    final ManaCostShard asked = activationShard(sa);
                    for (final ManaUnit feeder : pool) {
                        if (feeder.source == idle.source && feeder.choice != null || !unitPays(feeder, asked, null)) {
                            continue;
                        }
                        final List<ManaUnit> through = new ArrayList<>(pool);
                        through.remove(feeder);
                        through.removeIf(u -> feeder.excludes(u) || u.source == idle.source);
                        through.addAll(activatedLandOutput(idle.source, sa));
                        out.add(through);
                    }
                    continue;
                }

                final CostSacrifice sacrifice = externalSacrificeCost(sa);
                if (sacrifice == null || hand == null || !worksOnceInPlay(idle.source, sa)) {
                    continue;
                }
                for (final Card fodder : hand) {
                    if (fodder == idle.source || unavailable.contains(fodder) || !fodder.isPermanent()
                            || !matchesType(fodder, idle.source, sacrifice.getType())
                            || pool.stream().anyMatch(u -> u.source == fodder)) {
                        continue;
                    }
                    for (final SpellAbility spell : fodder.getSpellAbilities()) {
                        if (!isSimpleFodderSpell(spell)) {
                            continue;
                        }
                        final CostPartMana cost = spell.getPayCosts().getCostMana();
                        final List<ManaCostShard> shards = new ArrayList<>();
                        if (cost != null) {
                            for (final ManaCostShard shard : cost.getMana()) {
                                shards.add(shard);
                            }
                        }
                        // Casting the fodder spends its mana first; the land is then tapped and the creature sacrificed.
                        final int generic = cost == null ? 0 : cost.getMana().getGenericCost();
                        for (final List<ManaUnit> paid : paymentResults(pool, shards, generic, spell)) {
                            // The land must still be untapped after casting the fodder; its {C} mode and its
                            // sacrifice mode are alternatives, not two activations of the same permanent.
                            if (pool.stream().filter(u -> u.source == idle.source).anyMatch(u -> !paid.contains(u))) {
                                continue;
                            }
                            final List<ManaUnit> through = new ArrayList<>(paid);
                            through.removeIf(u -> u.source == idle.source || u.source == fodder);
                            through.addAll(activatedLandOutput(idle.source, sa));
                            through.add(ManaUnit.consumed(fodder));
                            out.add(through);
                        }
                    }
                }
            }
        }
        return out;
    }

    private static boolean isFilter(final Card land, final SpellAbility sa) {
        return needsMana(sa) && sa.getPayCosts().getCostMana().getMana().getCMC() == 1 && hasFixedAmount(sa)
                && sa.getPayCosts().getCostParts().stream().allMatch(part -> part instanceof CostTap
                        || part instanceof CostPartMana || part instanceof CostPayLife)
                && worksOnceInPlay(land, sa);
    }

    /** What the one mana of an activation has to be: its coloured part, or anything for {1}. */
    private static ManaCostShard activationShard(final SpellAbility sa) {
        for (final ManaCostShard shard : sa.getPayCosts().getCostMana().getMana()) {
            return shard;
        }
        return ManaCostShard.GENERIC;
    }

    /** The mana of one ability of a land, marked as that ability's. */
    private static List<ManaUnit> landOutput(final Card land, final SpellAbility sa) {
        final AbilityManaPart limit = isRestricted(sa) ? sa.getManaPart() : null;
        final List<ManaUnit> units = new ArrayList<>();
        // not judged by manaFlow: a land has no mana cost, which that asks for of a card that has to be cast
        for (final ManaUnit u : outputOf(land, new Flow(0, manaProduced(sa), limit != null, sa))) {
            units.add(new ManaUnit(u.colors, u.limit, land, sa));
        }
        return units;
    }

    /** Mana from an ability whose activation cost has already been paid; the source cannot be activated again. */
    private static List<ManaUnit> activatedLandOutput(final Card land, final SpellAbility sa) {
        final List<ManaUnit> units = landOutput(land, sa);
        for (final ManaUnit unit : units) {
            unit.activated = true;
        }
        return units;
    }

    /**
     * Pays the coloured parts from {@code i} on, then the generic mana. The mana left over after paying the whole
     * cost is offered to {@code enough}, once for each different set of it; the search ends when that returns true.
     *
     * @param previous the mana that paid the part before this one: equal parts take their mana in the order of the
     *                 pool, as the other orders spend the same mana
     * @param seen     the sets of leftover mana already offered
     * @return whether {@code enough} ended the search
     */
    private static boolean payShards(final List<ManaUnit> pool, final List<ManaCostShard> shards, final int i,
                                     final int previous, final boolean[] used, final int generic,
                                     final SpellAbility spell, final Card mustUse, final boolean usedIt,
                                     final Set<BitSet> seen, final Predicate<List<ManaUnit>> enough) {
        if (i == shards.size()) {
            return payGeneric(pool, used, 0, generic, spell, mustUse, usedIt, seen, enough);
        }
        final int first = i > 0 && shards.get(i) == shards.get(i - 1) ? previous + 1 : 0;
        for (int u = first; u < used.length; u++) {
            if (isAvailable(pool, used, u) && unitPays(pool.get(u), shards.get(i), spell)) {
                used[u] = true;
                final boolean done = payShards(pool, shards, i + 1, u, used, generic, spell, mustUse,
                        usedIt || isLimitedManaOf(pool.get(u), mustUse), seen, enough);
                used[u] = false;
                if (done) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Pays the generic mana with mana from {@code from} on, so that each choice is tried in one order only. */
    private static boolean payGeneric(final List<ManaUnit> pool, final boolean[] used, final int from,
                                      final int remaining, final SpellAbility spell, final Card mustUse,
                                      final boolean usedIt, final Set<BitSet> seen,
                                      final Predicate<List<ManaUnit>> enough) {
        if (remaining == 0) {
            if (mustUse != null && !usedIt) {
                return false;
            }
            final List<ManaUnit> spare = new ArrayList<>();
            final BitSet left = new BitSet(used.length);
            for (int u = 0; u < used.length; u++) {
                if (isAvailable(pool, used, u)) {
                    spare.add(pool.get(u));
                    left.set(u);
                }
            }
            // either ability of one land can leave the same mana, so different payments can end up alike
            return seen.add(left) && enough.test(spare);
        }
        for (int u = from; u <= used.length - remaining; u++) { // enough mana has to be left for the rest
            if (isAvailable(pool, used, u) && unitPays(pool.get(u), ManaCostShard.GENERIC, spell)) {
                used[u] = true;
                final boolean done = payGeneric(pool, used, u + 1, remaining - 1, spell, mustUse,
                        usedIt || isLimitedManaOf(pool.get(u), mustUse), seen, enough);
                used[u] = false;
                if (done) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether the mana is not used yet, and its land is not tapped for another of its abilities. */
    private static boolean isAvailable(final List<ManaUnit> pool, final boolean[] used, final int index) {
        if (used[index]) {
            return false;
        }
        for (int u = 0; u < used.length; u++) {
            if (used[u] && pool.get(u).excludes(pool.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLimitedManaOf(final ManaUnit unit, final Card card) {
        return unit.limit != null && unit.source == card;
    }

    /**
     * Whether restricted mana may be spent on the spell. A land of the hand that has not chosen its creature type yet
     * (Cavern of Souls) can choose the one of the creature it is wanted for.
     */
    private static boolean meetsRestriction(final ManaUnit unit, final SpellAbility spell) {
        final String restriction = unit.limit.getManaRestrictions();
        if (!unit.source.hasChosenType() && restriction.contains("Creature+ChosenType")) {
            return unit.limit.meetsManaRestrictions(spell, restriction.replace("Creature+ChosenType", "Creature"));
        }
        return unit.limit.meetsManaRestrictions(spell);
    }

    /** Whether one mana can pay one part of the cost of the spell, with the restrictions of the mana and the spell. */
    private static boolean unitPays(final ManaUnit unit, final ManaCostShard shard, final SpellAbility spell) {
        if (unit.unfed || unit.limit != null && (spell == null || !meetsRestriction(unit, spell))) {
            return false;
        }
        for (final byte color : unit.colors == 0 ? new byte[] {(byte) ManaAtom.COLORLESS} : MagicColor.WUBRG) {
            if (unit.colors != 0 && (unit.colors & color) == 0 || !shard.canBePaidWithManaOfColor(color)) {
                continue;
            }
            if (unit.limit == null || unit.limit.meetsManaShardRestrictions(shard, color)
                    && spell.allowsPayingWithShard(unit.source, color)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the mana at hand pays for the card, in full: its cost, and the colour of every part of it. */
    private static boolean canDeploy(final Card c, final List<ManaUnit> pool, final CardCollectionView hand,
                                     final Set<Card> unavailable) {
        if (c.getManaCost().isNoCost()) {
            return false;
        }
        // restricted mana (Ancient Ziggurat's) counts when the spell that casts the card meets the restriction
        for (final SpellAbility spell : c.getSpellAbilities()) {
            if (spell.isSpell() && spell.getPayCosts() != null && spell.getPayCosts().getCostMana() != null
                    && canPay(pool, c.getManaCost(), spell, null, hand, unavailable)) {
                return true;
            }
        }
        return false;
    }

    /** What a mana ability costs in mana, what it adds to the mana there is, and whether it may be spent only in some ways. */
    private static final class Flow {
        final int paid;
        final int net;
        final boolean restricted;
        final SpellAbility sa;

        Flow(final int paid, final int net, final boolean restricted, final SpellAbility sa) {
            this.paid = paid;
            this.net = net;
            this.restricted = restricted;
            this.sa = sa;
        }
    }

    /**
     * What the card's mana ability costs in mana and what it adds to the mana there is: a Signet pays one and makes
     * two (+1), a Mana Cylix pays one and makes one (+0: it only filters), a Mox pays nothing and makes one (+1).
     * Of the abilities that work, one without a spending restriction comes first, then the one that adds the most
     * (then costs the least).
     *
     * @return null when no ability works as it is
     */
    private static Flow manaFlow(final Card c) {
        Flow best = null;
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() != ApiType.Mana || !sa.isAbility() || sa.getPayCosts() == null
                    || !sa.getPayCosts().hasTapCost() || !worksAsItIs(c, sa)) {
                continue;
            }
            int paid = 0;
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (part instanceof CostPartMana) {
                    paid += ((CostPartMana) part).getMana().getCMC();
                }
            }
            final boolean restricted = isRestricted(sa);
            final int net = manaProduced(sa) - paid;
            if (best == null || best.restricted && !restricted
                    || best.restricted == restricted && (net > best.net || net == best.net && paid < best.paid)) {
                best = new Flow(paid, net, restricted, sa);
            }
        }
        return best;
    }

    /**
     * Whether the restricted mana of the card can pay for a spell of the hand, as the game would let it: the spell
     * meets the restriction, and the whole cost is paid by the lands and the sources counted so far together with this
     * mana, which pays a part of it that its restrictions allow. Creeping Peeper's {U} pays an enchantment such as
     * Curiosity, not Pacifism without white mana, and not a {2}{U}{U}{U}{U} with one more blue mana only. What only
     * the deck might hold later is not looked at.
     */
    private static boolean canSpendRestricted(final Card c, final Flow flow, final CardCollectionView hand,
                                              final List<ManaUnit> pool, final Set<Card> unavailable) {
        final List<ManaUnit> with = new ArrayList<>(pool);
        payActivation(with, flow.paid); // what its own ability costs is not there for the spell
        with.addAll(outputOf(c, flow));
        for (final Card other : hand) {
            if (other == c || other.isLand()) {
                continue;
            }
            for (final SpellAbility spell : other.getSpellAbilities()) {
                if (spell.isSpell() && spell.getPayCosts() != null && spell.getPayCosts().getCostMana() != null
                        && canPay(with, spell.getPayCosts().getCostMana().getMana(), spell, c, hand, unavailable)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The mana one activation makes: the amount times what is produced ("W U" is two; "Any" and "Combo" one). */
    private static int manaProduced(final SpellAbility sa) {
        final String[] produced = sa.getParamOrDefault("Produced", "").trim().split("\\s+");
        final int each = produced.length == 0 || produced[0].isEmpty() || "Any".equals(produced[0])
                || "Combo".equals(produced[0]) ? 1 : produced.length;
        return each * Integer.parseInt(sa.getParamOrDefault("Amount", "1")); // worksAsItIs: a fixed number
    }

    /** What the mana ability of a card asks for besides tapping the card itself. */
    private enum CostNeed { NOT_A_SOURCE, NONE, OTHER_PERMANENTS }

    private static CostNeed costNeed(final Card c) {
        CostNeed best = CostNeed.NOT_A_SOURCE;
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() != ApiType.Mana || !sa.isAbility() || sa.getPayCosts() == null
                    || !sa.getPayCosts().hasTapCost() || !worksAsItIs(c, sa)) {
                continue;
            }
            boolean other = false;
            boolean ok = true;
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (part instanceof CostTap || part instanceof CostPartMana
                        || part instanceof CostSacrifice && part.payCostFromSource()) {
                    continue;
                }
                if (part instanceof CostTapType && part.getAmount().matches("\\d+")) {
                    other = true;
                } else {
                    ok = false; // another cost is not judged here: to the safe side the card is no source
                    break;
                }
            }
            if (ok && !other) {
                return CostNeed.NONE;
            }
            if (ok) {
                best = CostNeed.OTHER_PERMANENTS;
            }
        }
        return best;
    }

    /**
     * The other cards of the hand that pay the tap costs of the card's mana ability: permanents of the type asked for
     * that the hand can cast, and not in {@code unavailable}. Null if there are not enough of them.
     */
    private static Set<Card> tapResources(final Card c, final CardCollectionView hand, final Set<Card> unavailable,
                                          final List<ManaUnit> pool) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() != ApiType.Mana || !sa.isAbility() || sa.getPayCosts() == null
                    || !sa.getPayCosts().hasTapCost() || !worksAsItIs(c, sa)) {
                continue;
            }
            final Set<Card> needed = new HashSet<>();
            boolean payable = true;
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (!(part instanceof CostTapType)) {
                    continue; // judged by costNeed
                }
                int missing = Integer.parseInt(part.getAmount());
                for (final Card other : hand) {
                    if (missing == 0) {
                        break;
                    }
                    if ((other != c || ((CostTapType) part).canTapSource) && other.isPermanent() && !other.isLand()
                            && !unavailable.contains(other) && !needed.contains(other)
                            && canDeploy(other, pool, hand, unavailable) && matchesType(other, c, part.getType())) {
                        needed.add(other);
                        missing--;
                    }
                }
                if (missing > 0) {
                    payable = false;
                    break;
                }
            }
            if (payable) {
                return needed;
            }
        }
        return null;
    }

    private static boolean matchesType(final Card candidate, final Card source, final String type) {
        for (final String restriction : type.split(";")) {
            if (candidate.isValid(restriction, source.getController(), source, null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the card is a source of mana for nothing but tapping it, perhaps sacrificing it too: a Mox Emerald or a
     * Lotus Petal can stand in for a land. A Mox Diamond, a Mox Opal or a Lion's Eye Diamond cannot.
     */
    public static boolean isFreeManaSource(final Card c) {
        for (final SpellAbility sa : c.getManaAbilities()) {
            if (sa.getApi() == ApiType.Mana && sa.getPayCosts() != null
                    && sa.getPayCosts().getCostParts().stream().allMatch(part -> part instanceof CostTap
                            || part instanceof CostSacrifice && part.payCostFromSource())
                    && worksAsItIs(c, sa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the mana ability can be counted on from an opening hand. Judged from the script, and to the safe side:
     * the card can be cast (Mox Tantalite cannot, it has to be suspended), entering asks for nothing (Mox Diamond),
     * the ability has no condition (Mox Opal's metalcraft) and the amount is a fixed one (Everflowing Chalice).
     */
    private static boolean worksAsItIs(final Card c, final SpellAbility sa) {
        return !c.getManaCost().isNoCost() && worksOnceInPlay(c, sa);
    }

    /** {@link #worksAsItIs} without the question of casting the card, which is none for a land. */
    private static boolean worksOnceInPlay(final Card c, final SpellAbility sa) {
        for (final String condition : new String[] {"Activation", "IsPresent", "CheckSVar", "ActivationPhases"}) {
            if (sa.hasParam(condition)) {
                return false;
            }
        }
        if (!sa.getParamOrDefault("Amount", "1").matches("\\d+")) {
            return false;
        }
        // a replacement of its own entering that only changes how it enters (tapped, with counters) is "Updated"
        return c.getReplacementEffects().stream().noneMatch(r -> r.getMode() == ReplacementType.Moved
                && "Battlefield".equals(r.getParam("Destination")) && "Card.Self".equals(r.getParam("ValidCard"))
                && !"Updated".equals(r.getParam("ReplacementResult")));
    }

    /** The cards that make the plan work, read from what the card does. */
    static boolean isKeyCard(final Card c, final DeckPlan.Archetype arch) {
        final int cmc = c.getCMC();
        switch (arch) {
            case AGGRO:
                return c.isCreature() && cmc <= 2 || c.hasKeyword(Keyword.HASTE) && cmc <= 3;
            case BURN:
                return cmc <= 3 && (hasApi(c, ApiType.DealDamage) && !c.isCreature() || c.isCreature() && cmc <= 2);
            case CONTROL:
                return cmc <= 4 && (hasApi(c, ApiType.Counter) || hasApi(c, ApiType.Destroy)
                        || hasApi(c, ApiType.DestroyAll) || hasApi(c, ApiType.DamageAll) || hasApi(c, ApiType.Draw)
                        || hasApi(c, ApiType.Sacrifice) || hasApi(c, ApiType.Fight));
            case RAMP:
                return cmc <= 3 && (hasApi(c, ApiType.Mana) && !c.isInstant() && !c.isSorcery() || fetchesLand(c));
            case LANDS:
                return cmc <= 3 && !c.isLand() && (DeckPlan.isLandMatters(c) || fetchesLand(c));
            case COMBO:
                return c.hasKeyword(Keyword.STORM) || cmc <= 2 && (hasApi(c, ApiType.Mana) || hasApi(c, ApiType.Draw))
                        || fetchesToHand(c);
            default:
                return cmc >= 2 && cmc <= 4 && !c.isLand();
        }
    }

    private static boolean hasApi(final Card c, final ApiType api) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return true;
            }
        }
        return false;
    }

    private static boolean fetchesLand(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (isLandSearch(sa)) {
                return true;
            }
        }
        return false;
    }

    /** A land that makes mana, or one that finds a land (a fetch land is a land drop that gets the colour needed). */
    /**
     * A land that makes mana in the first turns: not one that asks for many lands before it works (Temple of the False God
     * with its five), which is a land drop in the hand and no mana for the plays of turns one to four.
     */
    static boolean isEarlyManaLand(final Card c) {
        return isManaLand(c) && LegalityGuard.landsNeededToWork(c) <= 2;
    }

    /** An Aura that enchants a creature. */
    static boolean isCreatureAura(final Card c) {
        if (!c.isAura()) {
            return false;
        }
        for (final forge.game.keyword.KeywordInterface k : c.getKeywords()) {
            if (k.getKeyword() == forge.game.keyword.Keyword.ENCHANT && k.getOriginal().toLowerCase().contains("creature")) {
                return true;
            }
        }
        return false;
    }

    static boolean isManaLand(final Card c) {
        if (isMdfcLand(c)) {
            return !c.getState(CardStateName.Backside).getManaAbilities().isEmpty();
        }
        return c.isLand() && (CardPredicates.LANDS_PRODUCING_MANA.test(c) || fetchesLand(c));
    }

    /** A modal double-faced card with a land on its back (Agadeem's Awakening): a land drop as well as a spell. */
    static boolean isMdfcLand(final Card c) {
        return c.isModal() && !c.isLand() && c.getState(CardStateName.Backside) != null
                && c.getState(CardStateName.Backside).getType().isLand();
    }

    private static boolean fetchesToHand(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone && "Library".equals(sa.getParam("Origin"))
                    && "Hand".equals(sa.getParam("Destination"))) {
                return true;
            }
        }
        return false;
    }

    /** P(at least {@code k} of the {@code draws} cards drawn from {@code size} are among the {@code good}). */
    static double atLeast(final int k, final int draws, final int size, final int good) {
        if (k <= 0) {
            return 1.0;
        }
        final int n = Math.min(draws, size);
        if (n < k || good < k) {
            return 0.0;
        }
        double miss = 0; // P(fewer than k)
        for (int i = 0; i < k; i++) {
            miss += hyper(i, n, size, good);
        }
        return Math.max(0.0, 1.0 - miss);
    }

    private static double hyper(final int i, final int n, final int size, final int good) {
        if (i > good || n - i > size - good || n - i < 0) {
            return 0.0;
        }
        return Math.exp(logChoose(good, i) + logChoose(size - good, n - i) - logChoose(size, n));
    }

    private static double logChoose(final int n, final int k) {
        double r = 0;
        for (int i = 1; i <= k; i++) {
            r += Math.log(n - k + i) - Math.log(i);
        }
        return r;
    }
}
