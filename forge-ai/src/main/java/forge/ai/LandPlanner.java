package forge.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Plans the land drops of the next turns so that the spells in hand (and the commander) can be cast as early as the
 * colours allow, which matters most for decks with several colours.
 * <p>
 * The old rule of thumb scored every land by how many <em>new</em> colours it adds, whatever the hand needs. This
 * planner looks at what the hand needs instead. For every land that can be played now it tries the lands that could
 * follow it on the next two turns, and for each such sequence it schedules the spells turn by turn: on every turn the
 * best spells that can <em>jointly</em> be paid with the mana sources of that turn are cast (a source that enters
 * tapped only counts from the next turn on, and a dual land can pay only one colour at a time, which is checked by
 * matching the coloured costs to the sources). A spell cast sooner is worth more than the same spell cast later, and
 * a small bonus for the colours the library still needs breaks ties between lands that do the same for the hand.
 * <p>
 * Only the cards the AI can see are used (its hand, its battlefield, its commander, and the colours of its own
 * library as deck statistics), nothing about the opponent. The planner never plays a land by itself: it only answers
 * which of the lands the caller already allowed is best, or says that the difference is too small to matter, in which
 * case the caller keeps its own rule.
 */
public final class LandPlanner {
    /** -Dforge.landplanner.debug=true prints every decision to stderr. */
    private static final boolean DEBUG = Boolean.getBoolean("forge.landplanner.debug");

    /** How many turns are planned, this one included. */
    static final int HORIZON = 3;
    /** What a spell is worth when it can be cast on turn 0 (this turn), 1 and 2. */
    private static final double[] DECAY = { 1.0, 0.7, 0.5 };
    /** The best land must beat the next one by this much (about a tenth of a cheap spell) to be chosen by the planner. */
    static final double MIN_MARGIN = 10;
    /** Value of the first/second source of a colour that the library still asks for, at most (times the demand 0..1). */
    private static final double LIBRARY_COLOR_BONUS = 30;
    private static final int MAX_SPELLS = 12;

    private static final String[] TYPE_LETTERS = { "W", "U", "B", "R", "G", "C" };
    private static final byte[] TYPE_BITS = { ManaAtom.WHITE, ManaAtom.BLUE, ManaAtom.BLACK, ManaAtom.RED,
            ManaAtom.GREEN, ManaAtom.COLORLESS };

    private LandPlanner() {
    }

    /** One unit of mana the AI will be able to make: the types it may be (one of them) and from which turn it is there. */
    static final class Src {
        final byte mask;
        final int from;

        Src(final byte mask, final int from) {
            this.mask = mask;
            this.from = from;
        }
    }

    /** A spell the hand wants to cast. */
    static final class Spell {
        final String name;
        /** The coloured parts of the cost, each as the set of mana types that can pay it. */
        final byte[] coloured;
        final int generic;
        final double weight;

        Spell(final String name, final byte[] coloured, final int generic, final double weight) {
            this.name = name;
            this.coloured = coloured;
            this.generic = generic;
            this.weight = weight;
        }
    }

    /**
     * @param ai           the AI player
     * @param playable     the lands it may play now (the caller has already filtered them), at least two
     * @param entersTapped says whether a land will enter the battlefield tapped
     * @return the land the plan prefers, or null when the lands are too close to call (or nothing can be planned)
     */
    public static Card choose(final Player ai, final CardCollection playable, final Predicate<Card> entersTapped) {
        if (playable == null || playable.size() < 2) {
            return null;
        }
        try {
            final List<Spell> spells = spellsWanted(ai);
            final List<Src> base = battlefieldSources(ai);
            final double[] demand = libraryDemand(ai);
            final List<Card> handLands = new ArrayList<>(CardLists.filter(ai.getCardsIn(ZoneType.Hand), Card::isLand));

            Card best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            double second = Double.NEGATIVE_INFINITY;
            final Set<String> seen = new HashSet<>();
            for (final Card land : playable) {
                if (!seen.add(land.getName() + "|" + entersTapped.test(land))) {
                    continue;
                }
                final List<Card> rest = new ArrayList<>(handLands);
                rest.remove(land);
                final List<Src> srcs = new ArrayList<>(base);
                addLand(srcs, land, ai, 0, entersTapped);
                final double score = bestFuture(ai, srcs, rest, 1, spells, demand, entersTapped);
                if (DEBUG) {
                    System.err.println("[landplanner] " + land + (entersTapped.test(land) ? " (tapped)" : "") + " = " + score);
                }
                if (score > bestScore) {
                    second = bestScore;
                    bestScore = score;
                    best = land;
                } else if (score > second) {
                    second = score;
                }
            }
            if (best == null || second == Double.NEGATIVE_INFINITY || bestScore - second < MIN_MARGIN) {
                return null;
            }
            if (DEBUG) {
                System.err.println("[landplanner] => " + best + " by " + (bestScore - second));
            }
            return best;
        } catch (final RuntimeException e) {
            return null; // a refinement only: the caller's rule decides
        }
    }

    /** The best score over the lands that could be played on the turns after this one. */
    private static double bestFuture(final Player ai, final List<Src> srcs, final List<Card> rest, final int turn,
                                     final List<Spell> spells, final double[] demand, final Predicate<Card> entersTapped) {
        if (turn >= HORIZON || rest.isEmpty()) {
            return score(srcs, spells, demand);
        }
        double best = Double.NEGATIVE_INFINITY;
        final Set<String> seen = new HashSet<>();
        for (final Card land : rest) {
            if (!seen.add(land.getName() + "|" + entersTapped.test(land))) {
                continue;
            }
            final List<Card> remaining = new ArrayList<>(rest);
            remaining.remove(land);
            final List<Src> next = new ArrayList<>(srcs);
            addLand(next, land, ai, turn, entersTapped);
            best = Math.max(best, bestFuture(ai, next, remaining, turn + 1, spells, demand, entersTapped));
        }
        return best;
    }

    // ----------------------------------------------------------------------------------------------- the schedule

    /** Spells cast as early as the sources allow, each worth less the later it is cast; plus the library colour bonus. */
    static double score(final List<Src> srcs, final List<Spell> spells, final double[] demand) {
        final boolean[] done = new boolean[spells.size()];
        double total = 0;
        for (int t = 0; t < HORIZON; t++) {
            final List<Src> avail = new ArrayList<>();
            for (final Src s : srcs) {
                if (s.from <= t) {
                    avail.add(s);
                }
            }
            final List<byte[]> colouredSoFar = new ArrayList<>();
            int genericSoFar = 0;
            for (int i = 0; i < spells.size(); i++) { // spells are sorted by weight, best first
                if (done[i]) {
                    continue;
                }
                final Spell sp = spells.get(i);
                final List<byte[]> coloured = new ArrayList<>(colouredSoFar);
                coloured.add(sp.coloured);
                if (canPay(coloured, genericSoFar + sp.generic, avail)) {
                    colouredSoFar.add(sp.coloured);
                    genericSoFar += sp.generic;
                    done[i] = true;
                    total += sp.weight * DECAY[t];
                }
            }
        }
        // the colours the library will ask for: a first source of a colour is worth more than a second
        final int[] sources = new int[5];
        for (final Src s : srcs) {
            for (int c = 0; c < 5; c++) {
                if ((s.mask & TYPE_BITS[c]) != 0) {
                    sources[c]++;
                }
            }
        }
        for (int c = 0; c < 5; c++) {
            final double share = sources[c] == 0 ? 0 : sources[c] == 1 ? 0.6 : 1.0;
            total += LIBRARY_COLOR_BONUS * demand[c] * share;
        }
        return total;
    }

    /**
     * Whether the sources can pay all the coloured parts and the generic mana together. The coloured parts are matched
     * to distinct sources (a dual land pays one of its colours), whatever is left pays the generic mana.
     */
    static boolean canPay(final List<byte[]> colouredCosts, final int generic, final List<Src> avail) {
        final List<Byte> reqs = new ArrayList<>();
        for (final byte[] cost : colouredCosts) {
            for (final byte b : cost) {
                reqs.add(b);
            }
        }
        if (reqs.size() + generic > avail.size()) {
            return false;
        }
        // the pickiest requirements first, so that the search stays small
        reqs.sort((a, b) -> Integer.compare(Integer.bitCount(a), Integer.bitCount(b)));
        final byte[] masks = new byte[avail.size()];
        for (int i = 0; i < masks.length; i++) {
            masks[i] = avail.get(i).mask;
        }
        return match(reqs, 0, masks, new boolean[masks.length], avail.size() - reqs.size() - generic);
    }

    /** {@code spare} is how many sources may stay unused: the sources minus the coloured parts minus the generic mana. */
    private static boolean match(final List<Byte> reqs, final int i, final byte[] masks, final boolean[] used, final int spare) {
        if (i == reqs.size()) {
            return spare >= 0;
        }
        final byte req = reqs.get(i);
        final Set<Byte> tried = new HashSet<>(); // sources with the same mask are interchangeable
        for (int s = 0; s < masks.length; s++) {
            if (used[s] || (masks[s] & req) == 0 || !tried.add(masks[s])) {
                continue;
            }
            used[s] = true;
            final boolean ok = match(reqs, i + 1, masks, used, spare);
            used[s] = false;
            if (ok) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------------- what is wanted

    /** The spells in hand and the commander in the command zone, best first; at most {@link #MAX_SPELLS}. */
    static List<Spell> spellsWanted(final Player ai) {
        final List<Spell> out = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                continue;
            }
            addSpell(out, c, 0, 1.0);
        }
        for (final Card c : ai.getCardsIn(ZoneType.Command)) {
            if (c.isCommander() && !c.isLand()) {
                int tax = 0;
                try {
                    tax = 2 * ai.getCommanderCast(c);
                } catch (final RuntimeException e) {
                    tax = 0;
                }
                addSpell(out, c, tax, 1.5); // the card the deck is built around is worth getting online
            }
        }
        out.sort((a, b) -> Double.compare(b.weight, a.weight));
        while (out.size() > MAX_SPELLS) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private static void addSpell(final List<Spell> out, final Card c, final int extraGeneric, final double factor) {
        final ManaCost cost = c.getManaCost();
        if (cost == null || cost.isNoCost()) {
            return;
        }
        final List<Byte> coloured = new ArrayList<>();
        int generic = Math.max(0, cost.getGenericCost()) + extraGeneric;
        for (final ManaCostShard shard : cost) {
            if (shard.isPhyrexian() || shard.isOfKind(ManaAtom.IS_X)) {
                continue; // paid with life, or X = 0 for planning
            }
            if (shard.isOr2Generic()) {
                generic += 2; // {2/W}: planned as the expensive way, which never over-promises
                continue;
            }
            final byte mask = (byte) (shard.getShard() & ManaAtom.ALL_MANA_TYPES);
            if (mask == 0) {
                generic++;
            } else {
                coloured.add(mask);
            }
        }
        final byte[] arr = new byte[coloured.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = coloured.get(i);
        }
        final int cmc = Math.min(8, Math.max(1, cost.getCMC()));
        out.add(new Spell(c.getName(), arr, generic, (60 + 30 * cmc) * factor));
    }

    /** How much of its spells the library has a pip of each colour for, 0..1 (WUBRG order). */
    static double[] libraryDemand(final Player ai) {
        final double[] demand = new double[5];
        int spells = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (c.isLand() || c.getManaCost() == null || c.getManaCost().isNoCost()) {
                continue;
            }
            spells++;
            final int[] pips = c.getManaCost().getColorShardCounts();
            for (int i = 0; i < 5 && i < pips.length; i++) {
                if (pips[i] > 0) {
                    demand[i]++;
                }
            }
        }
        if (spells == 0) {
            return demand;
        }
        for (int i = 0; i < 5; i++) {
            demand[i] = Math.min(1.0, 2.0 * demand[i] / spells); // a colour in half of the spells counts in full
        }
        return demand;
    }

    // ----------------------------------------------------------------------------------------------- the sources

    /** The mana the AI has on the battlefield: a source that is tapped (or a summoning sick creature) counts from next turn. */
    static List<Src> battlefieldSources(final Player ai) {
        final List<Src> out = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isPhasedOut() || c.getManaAbilities().isEmpty()) {
                continue;
            }
            final int from = c.isTapped() || c.isCreature() && c.isSick() ? 1 : 0;
            addSources(out, c, ai, from);
        }
        return out;
    }

    private static void addLand(final List<Src> srcs, final Card land, final Player ai, final int turn,
                                final Predicate<Card> entersTapped) {
        addSources(srcs, land, ai, turn + (entersTapped.test(land) ? 1 : 0));
    }

    /**
     * Adds the mana a permanent can make every turn: its mana abilities that cost nothing but tapping (an ability
     * that sacrifices something or needs mana of its own is not a source to plan with). A land that makes several
     * types at once (a bounce land) is one source for each type; a land that makes one of several is one source that
     * may be any of them. A land that only fetches (Evolving Wilds) makes the types of the lands it can find.
     */
    private static void addSources(final List<Src> out, final Card c, final Player ai, final int from) {
        final List<SpellAbility> usable = new ArrayList<>();
        for (final SpellAbility ma : ComputerUtilMana.getAIPlayableMana(c)) {
            final Cost cost = ma.getPayCosts();
            if (cost == null || cost.hasManaCost() || !cost.isReusuableResource()) {
                continue;
            }
            usable.add(ma);
        }
        if (usable.isEmpty()) {
            final byte fetched = fetchMask(c, ai);
            if (fetched != 0) {
                out.add(new Src(fetched, from));
            }
            return;
        }
        if (usable.size() == 1) {
            final SpellAbility ma = usable.get(0);
            ma.setActivatingPlayer(ai);
            final AbilityManaPart mp = ma.getManaPart();
            if (mp != null && !mp.isAnyMana() && !mp.isComboMana()) {
                final byte[] each = simultaneous(ma, mp);
                if (each != null) {
                    for (final byte b : each) {
                        out.add(new Src(b, from));
                    }
                    return;
                }
            }
        }
        byte mask = 0;
        int amount = 1;
        for (final SpellAbility ma : usable) {
            ma.setActivatingPlayer(ai);
            mask |= maskOf(ma);
            try {
                amount = Math.max(amount, ma.amountOfManaGenerated(true));
            } catch (final RuntimeException e) {
                // keep one
            }
        }
        if (mask == 0) {
            return;
        }
        for (int i = 0; i < Math.min(amount, 4); i++) {
            out.add(new Src(mask, from));
        }
    }

    /** For "{W}{U}" style abilities: one mask per type made at the same time, null when this is not that. */
    private static byte[] simultaneous(final SpellAbility ma, final AbilityManaPart mp) {
        final String produced;
        try {
            produced = mp.mana(ma);
        } catch (final RuntimeException e) {
            return null;
        }
        if (produced == null) {
            return null;
        }
        final String[] tokens = produced.trim().split("\\s+");
        if (tokens.length < 2) {
            return null;
        }
        final byte[] masks = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            masks[i] = 0;
            for (int t = 0; t < TYPE_LETTERS.length; t++) {
                if (TYPE_LETTERS[t].equals(tokens[i])) {
                    masks[i] = TYPE_BITS[t];
                }
            }
            if (masks[i] == 0) {
                return null; // something we do not understand: plan it as one source instead
            }
        }
        return masks;
    }

    private static byte maskOf(final SpellAbility ma) {
        byte mask = 0;
        for (int t = 0; t < TYPE_LETTERS.length; t++) {
            if (ma.canProduce(TYPE_LETTERS[t])) {
                mask |= TYPE_BITS[t];
            }
        }
        return mask;
    }

    /** The types a land that fetches another land can make: those of the lands it can find in the library. */
    private static byte fetchMask(final Card land, final Player ai) {
        for (final SpellAbility sa : land.getSpellAbilities()) {
            if (sa.getApi() != ApiType.ChangeZone || !"Library".equals(sa.getParamOrDefault("Origin", ""))
                    || !"Battlefield".equals(sa.getParamOrDefault("Destination", ""))) {
                continue;
            }
            final CardCollectionView lib = ai.getCardsIn(ZoneType.Library);
            CardCollection lands = CardLists.filter(lib, Card::isLand);
            final String type = sa.getParamOrDefault("ChangeType", "");
            if (!type.isEmpty()) {
                try {
                    lands = CardLists.getValidCards(lands, type, ai, land, sa);
                } catch (final RuntimeException e) {
                    // keep every land of the library
                }
            }
            byte mask = 0;
            for (final Card l : lands) {
                for (final SpellAbility ma : ComputerUtilMana.getAIPlayableMana(l)) {
                    ma.setActivatingPlayer(ai);
                    mask |= maskOf(ma);
                }
            }
            return mask;
        }
        return 0;
    }
}
