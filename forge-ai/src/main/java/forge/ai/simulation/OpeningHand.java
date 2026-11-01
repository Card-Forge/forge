package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.card.MagicColor;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
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
        if (!playable(kept, finalSize)) {
            return false; // no key card, combo piece or good score makes up for a hand that cannot cast anything
        }
        if (plan != null && plan.combos.has(ComboPlan.Kind.TUTOR_STORM)) {
            return plan.combos.keepsTutorStormHand(kept, finalSize); // judged as the combo player would, not by the generic score
        }
        final double score = score(kept, lib, onPlay, plan);
        return score >= keepBar(finalSize);
    }

    /**
     * The rules no hand may break, whatever its spells are worth: at least one land that makes mana (two for a
     * 6 or 7 card hand unless a cheap mana source helps the single land) and not nearly all lands.
     */
    static boolean playable(final CardCollectionView kept, final int size) {
        int lands = 0;
        int cheapSources = 0;
        for (final Card c : kept) {
            if (c.isLand()) {
                if (CardPredicates.LANDS_PRODUCING_MANA.test(c)) {
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
                final double s = score(rest, lib, onPlay, plan);
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

        Library(final Player ai) {
            final CardCollectionView lib = ai.getCardsIn(ZoneType.Library);
            int l = 0;
            for (final Card c : lib) {
                if (CardPredicates.LANDS_PRODUCING_MANA.test(c)) {
                    l++;
                    final byte colors = producedColors(c);
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
        for (final SpellAbility sa : land.getManaAbilities()) {
            ManaConversion.withActivator(sa);
            for (final byte col : MagicColor.WUBRG) {
                if (sa.canProduce(MagicColor.toLongString(col))) {
                    colors |= col;
                }
            }
        }
        return colors;
    }

    /** 0..100: how good the hand is for the plan. */
    static double score(final CardCollectionView hand, final Library lib, final boolean onPlay, final DeckPlan plan) {
        final DeckPlan.Archetype arch = plan == null ? DeckPlan.Archetype.MIDRANGE : plan.archetype;
        final Want w = want(arch);
        final int size = hand.size();
        int lands = 0;
        int rocks = 0;
        byte colors = 0;
        final List<Card> spells = new ArrayList<>();
        for (final Card c : hand) {
            if (c.isLand()) {
                if (CardPredicates.LANDS_PRODUCING_MANA.test(c)) {
                    lands++;
                    colors |= producedColors(c);
                }
            } else {
                spells.add(c);
                if (isCheapManaSource(c)) {
                    rocks++;
                }
            }
        }
        if (spells.isEmpty()) {
            return 0;
        }
        // a ramp deck counts its cheap mana sources as lands once there are two lands to cast them
        final int sources = lands + (arch == DeckPlan.Archetype.RAMP && lands >= 2 ? Math.min(rocks, 2) : 0);
        final int draws = Math.max(0, w.byTurn - (onPlay ? 1 : 0));
        final double hit = atLeast(w.lands - sources, draws, lib.size, lib.lands);

        double early = 0;
        int keys = 0;
        for (final Card c : spells) {
            if (c.getCMC() <= w.playCmc) {
                early += colorCredit(c, colors, lib);
            }
            if (isKeyCard(c, arch) || plan != null && (plan.combos.isKeyCard(c) || plan.commander.isKeyCard(c))) {
                keys++;
            }
        }
        final double playsPart = Math.min(1.0, early / w.plays);
        final double keysPart = Math.min(1.0, (double) keys / w.keys);
        final double quality = 0.6 * playsPart + 0.4 * keysPart;
        double score = 100 * (0.55 * hit + 0.45 * quality) * Math.sqrt(hit);

        if (lands == 0) {
            score -= 60; // bottoming and mulligan rules must never leave a hand without a land
        }
        if (size >= 5 && lands >= size - 1) {
            score -= 35; // flooded: nothing to spend the mana on
        } else if (size >= 6 && lands >= size - 2 && spells.size() <= 2 && arch != DeckPlan.Archetype.RAMP
                && arch != DeckPlan.Archetype.LANDS) {
            score -= 15;
        }
        return Math.max(0, Math.min(100, score));
    }

    /** 1 when the lands in hand make the colours of the spell, less when a missing colour may still be drawn. */
    private static double colorCredit(final Card c, final byte handColors, final Library lib) {
        final byte need = c.getManaCost().getColorProfile();
        final int missing = need & ~handColors;
        if (missing == 0) {
            return 1.0;
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
            if (sa.getApi() == ApiType.ChangeZone && "Library".equals(sa.getParam("Origin"))
                    && "Battlefield".equals(sa.getParam("Destination"))) {
                return true;
            }
        }
        return false;
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
