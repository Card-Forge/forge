package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import forge.ai.ManaSourceAllocation;
import forge.card.MagicColor;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.card.CardStateName;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
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
        int cheapSources = 0;
        for (final Card c : kept) {
            if (c.isLand() || isMdfcLand(c)) {
                if (isEarlyManaLand(c)) {
                    lands++;
                }
            } else if (isCheapManaSource(c)) {
                cheapSources++;
            }
        }
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
        boolean spell = false;
        for (final Card c : kept) {
            if (!c.isLand() && !isMdfcLand(c) && !isCreatureAura(c)) {
                spell = true;
                break;
            }
        }
        if (!spell) {
            return true; // only lands: judged by the other rules
        }
        return new HandMana(kept, lib).allocate(sources -> castsWith(kept, sources));
    }

    /**
     * The mana the lands of a hand make. A land that taps for mana is one fixed source (a colourless one, Wastes, stays
     * colourless). A fetch land is a source only through the one library card it finds: that card has to be one the
     * fetch ability may search for (Evolving Wilds: basic lands only), and a card can be found by one fetch land only.
     */
    private static final class HandMana {
        /** Nodes of the search for a split of the library's lands that a hand may cost before it is not judged. */
        private static final int MAX_NODES = 20000;

        private final Library lib;
        private final List<Byte> fixed = new ArrayList<>();
        /** For each library land: the fetch lands of the hand (bit by bit) that may find it. */
        private final int[] finders;
        private final int fetches;
        private final byte reachable;

        HandMana(final CardCollectionView hand, final Library lib) {
            this.lib = lib;
            final List<Card> fetchLands = new ArrayList<>();
            byte own = 0;
            for (final Card c : hand) {
                if ((c.isLand() || isMdfcLand(c)) && isEarlyManaLand(c)) {
                    if (fetchesLand(c)) {
                        fetchLands.add(c);
                    } else {
                        final byte colors = producedColors(c);
                        fixed.add(colors);
                        own |= colors;
                    }
                }
            }
            fetches = fetchLands.size();
            finders = new int[lib.landCards.size()];
            byte found = 0;
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
                            found |= lib.landMasks.get(i);
                        }
                    }
                }
            }
            reachable = (byte) (own | found);
        }

        /** Every colour the hand can make, though not necessarily all together. */
        byte reachable() {
            return reachable;
        }

        /** Whether some choice of the cards the fetch lands find makes {@code test} true for the sources that result. */
        boolean allocate(final Predicate<List<Byte>> test) {
            return allocate(0, new boolean[finders.length], new ArrayList<>(fixed), test, new int[1]);
        }

        private boolean allocate(final int fetch, final boolean[] used, final List<Byte> sources,
                                 final Predicate<List<Byte>> test, final int[] nodes) {
            if (fetch == fetches) {
                return test.test(sources);
            }
            if (++nodes[0] > MAX_NODES) {
                // too many ways to split the library to try them all: the best split found greedily, which is checked
                // like any other, so a yes is a verified yes and a no is the cautious answer
                return test.test(greedy(fetch, used, sources));
            }
            final Set<Long> tried = new HashSet<>();
            boolean finds = false;
            for (int i = 0; i < finders.length; i++) {
                if (used[i] || (finders[i] & (1 << fetch)) == 0) {
                    continue;
                }
                // two lands with the same colours that the same later fetch lands may find are the same choice
                final long key = (lib.landMasks.get(i).longValue() << 32) | (finders[i] >>> (fetch + 1));
                if (!tried.add(key)) {
                    continue;
                }
                finds = true;
                used[i] = true;
                sources.add(lib.landMasks.get(i));
                final boolean ok = allocate(fetch + 1, used, sources, test, nodes);
                sources.remove(sources.size() - 1);
                used[i] = false;
                if (ok) {
                    return true;
                }
            }
            // a found land only adds a source, so finding one is never worse; with nothing to find, it makes no mana
            return !finds && allocate(fetch + 1, used, sources, test, nodes);
        }

        /** The sources when each remaining fetch land takes the unused land that adds the most colours. */
        private List<Byte> greedy(final int from, final boolean[] used, final List<Byte> sources) {
            final List<Byte> out = new ArrayList<>(sources);
            final boolean[] taken = used.clone();
            byte have = 0;
            for (final byte s : out) {
                have |= s;
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
                    out.add(lib.landMasks.get(best));
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
            return allocate(sources -> ManaSourceAllocation.canPayColoredPips(spell.getManaCost(), sources));
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

    /** Whether a spell of the hand can be paid with these sources, one source for each coloured pip. */
    private static boolean castsWith(final CardCollectionView kept, final List<Byte> sources) {
        byte colors = 0;
        for (final byte s : sources) {
            colors |= s;
        }
        for (final Card c : kept) {
            if (!c.isLand() && !isMdfcLand(c) && !isCreatureAura(c)
                    && c.getManaCost().canBePaidWithAvailable(colors) // hybrid shards: either colour pays
                    && ManaSourceAllocation.canPayColoredPips(c.getManaCost(), sources)) {
                return true;
            }
        }
        return false;
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
        byte colors = 0;
        for (final Card c : kept) {
            if ((c.isLand() && CardPredicates.LANDS_PRODUCING_MANA.test(c)) || (isMdfcLand(c) && isManaLand(c))) {
                lands++;
                colors |= producedColors(c);
            }
        }
        if (lands != 1) {
            return false;
        }
        for (final Card c : kept) {
            if (!c.isLand() && c.getCMC() <= 1 && ComboPlan.isDredgeOutlet(c)
                    && (c.getManaCost().getColorProfile() & ~colors) == 0) {
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
            colors |= ManaConversion.producedColors(sa); // short letters ("R"), and it sets the activator itself
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
        int rocks = 0;
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
                if (isCheapManaSource(c)) {
                    rocks++;
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
        final int missing = need & ~mana.reachable();
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

    private static boolean isCheapManaSource(final Card c) {
        if (c.getCMC() > 2 || c.isInstant() || c.isSorcery()) {
            return false;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Mana && sa.isAbility() && sa.getPayCosts() != null && sa.getPayCosts().hasTapCost()) {
                return true;
            }
        }
        return false;
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
