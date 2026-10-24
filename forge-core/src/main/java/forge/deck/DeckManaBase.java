package forge.deck;

import forge.StaticData;
import forge.card.CardRules;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that a generated deck can actually cast what is in it, and repairs it when it cannot.
 * <p>
 * Decks generated from archetype statistics (or from a few key cards) can end up with spells of a colour the lands never
 * produce: three Explore in a deck whose lands make only blue and black are dead cards for the whole game, and a fetch
 * land that finds "Forest or Island" in a deck without a Forest only ever finds Islands. The generated decks are used by
 * the AI, which cannot do anything about it, so the fix is made when the deck is built.
 * <p>
 * Colours come from the lands (including fetch lands, for the basic land types that really are in the deck) and from
 * mana creatures and rocks. A spell whose colours are not all available is uncastable. A colour that several spells
 * need becomes a real colour of the deck (basic lands of it are added in place of the most plentiful basics), otherwise
 * the few spells are swapped out for more of what the deck can cast.
 */
public final class DeckManaBase {
    private DeckManaBase() { }

    /** A colour needed by at least this many copies of otherwise uncastable spells is worth building lands for. */
    static final int REAL_COLOR_COPIES = 5;
    /** Basic lands added for such a colour. */
    static final int REAL_COLOR_BASICS = 4;
    /** Never add lands beyond this when refilling; more copies of the spells are added instead. */
    static final int MAX_LANDS_WHEN_REFILLING = 25;
    /** The most basic lands of one colour that may be turned into another colour's lands. */
    static final int MIN_BASICS_KEPT = 5;

    private static final Pattern ADD_MANA = Pattern.compile("Add ([^.]*)");

    /** The colours (a {@link MagicColor} mask) the lands of the main deck can produce. */
    public static byte landColors(final CardPool main) {
        byte colors = 0;
        final List<PaperCard> fetchers = new ArrayList<>();
        for (final Map.Entry<PaperCard, Integer> e : main) {
            final CardRules rules = e.getKey().getRules();
            if (!rules.getType().isLand()) {
                continue;
            }
            if (isFetchLand(rules)) {
                fetchers.add(e.getKey());
            } else {
                colors |= rules.getDeckbuildingColors().getColor();
                colors |= producedColors(rules); // "Add {C}", "Add one mana of any color"
            }
        }
        // a fetch land finds only what is in the deck: the colours of its basic land types that some land really has
        for (final PaperCard fetcher : fetchers) {
            final byte named = fetcher.getRules().getDeckbuildingColors().getColor();
            for (int i = 0; i < MagicColor.Constant.BASIC_LANDS.size(); i++) {
                final byte color = (byte) (1 << i);
                if ((colors & color) != 0 || (named != 0 && (named & color) == 0)) {
                    continue; // already available, or not something this fetch land can find
                }
                if (hasLandOfType(main, MagicColor.Constant.BASIC_LANDS.get(i))) {
                    colors |= color;
                }
            }
        }
        return colors;
    }

    /**
     * Every colour the deck can make: its lands, plus what the mana creatures and rocks it can cast produce (which in
     * turn may make more spells castable).
     */
    public static byte availableColors(final CardPool main) {
        byte colors = landColors(main);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (final Map.Entry<PaperCard, Integer> e : main) {
                final CardRules rules = e.getKey().getRules();
                if (rules.getType().isLand()) {
                    continue;
                }
                final byte produced = producedColors(rules);
                if ((produced & ~colors) != 0 && rules.canCastWithAvailable(colors)) {
                    colors |= produced;
                    grew = true;
                }
            }
        }
        return colors;
    }

    /** The distinct cards of the main deck that cannot be cast with the colours the deck can make. */
    public static List<PaperCard> uncastable(final CardPool main) {
        final byte available = availableColors(main);
        final List<PaperCard> result = new ArrayList<>();
        for (final Map.Entry<PaperCard, Integer> e : main) {
            final CardRules rules = e.getKey().getRules();
            if (!rules.getType().isLand() && !rules.canCastWithAvailable(available)) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    /**
     * Repairs the deck in place so that everything in the main deck can be cast, keeping its size.
     *
     * @return the number of cards that were changed (added or removed); zero when the deck was fine
     */
    public static int fix(final Deck deck) {
        final CardPool main = deck.getMain();
        final int size = main.countAll();
        int changed = 0;

        // 1. a colour that enough spells need is a real colour of the deck: give it lands
        for (int pass = 0; pass < 2; pass++) {
            final byte available = availableColors(main);
            final int[] needed = new int[5];
            for (final PaperCard card : uncastable(main)) {
                final byte missing = (byte) (card.getRules().getManaCost().getColorProfile() & ~available);
                for (int i = 0; i < 5; i++) {
                    if ((missing & (1 << i)) != 0) {
                        needed[i] += main.count(card);
                    }
                }
            }
            int best = -1;
            for (int i = 0; i < 5; i++) {
                if (needed[i] >= REAL_COLOR_COPIES && (best < 0 || needed[i] > needed[best])) {
                    best = i;
                }
            }
            if (best < 0) {
                break;
            }
            changed += addBasicsFor(main, best);
        }

        // 2. what is still uncastable is swapped out
        final List<PaperCard> dead = uncastable(main);
        if (!dead.isEmpty()) {
            final CardPool side = deck.getOrCreate(DeckSection.Sideboard);
            for (final PaperCard card : dead) {
                final int n = main.count(card);
                main.remove(card, n);
                side.add(card, n);
                changed += n;
            }
        }

        // 3. back to the original size with what the deck can use
        int missing = size - main.countAll();
        while (missing > 0) {
            final PaperCard add = refillCard(main);
            if (add == null) {
                break;
            }
            main.add(add);
            changed++;
            missing--;
        }
        return changed;
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static boolean isFetchLand(final CardRules rules) {
        final String text = rules.getMainPart().getOracleText();
        return text != null && text.contains("Search your library");
    }

    private static boolean hasLandOfType(final CardPool main, final String basicType) {
        for (final Map.Entry<PaperCard, Integer> e : main) {
            final CardRules rules = e.getKey().getRules();
            if (rules.getType().isLand() && !isFetchLand(rules) && rules.getType().hasSubtype(basicType)) {
                return true;
            }
        }
        return false;
    }

    /** Colours a non-land card puts into the mana pool ("{T}: Add {G}", "Add one mana of any color"). */
    static byte producedColors(final CardRules rules) {
        final String text = rules.getMainPart().getOracleText();
        if (text == null || !text.contains("Add ")) {
            return 0;
        }
        byte colors = 0;
        final Matcher m = ADD_MANA.matcher(text);
        while (m.find()) {
            final String what = m.group(1);
            if (what.toLowerCase(Locale.ROOT).contains("any color") || what.toLowerCase(Locale.ROOT).contains("any one color")) {
                return MagicColor.ALL_COLORS;
            }
            for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                if (what.contains("{" + MagicColor.toShortString(MagicColor.WUBRG[i]).toUpperCase(Locale.ROOT) + "}")) {
                    colors |= MagicColor.WUBRG[i];
                }
            }
            if (what.contains("{C}")) {
                colors |= ManaAtom.COLORLESS; // colourless mana: what {C} in a cost needs (Eldrazi)
            }
        }
        return colors;
    }

    /** Turns some basic lands of the deck's most plentiful colour into basics of the colour with the given index. */
    private static int addBasicsFor(final CardPool main, final int colorIndex) {
        final PaperCard basic = basicLand(main, colorIndex);
        if (basic == null) {
            return 0;
        }
        int changed = 0;
        for (int i = 0; i < REAL_COLOR_BASICS; i++) {
            final PaperCard donor = mostPlentifulBasic(main, colorIndex);
            if (donor == null) {
                break;
            }
            main.remove(donor, 1);
            main.add(basic, 1);
            changed += 2;
        }
        return changed;
    }

    /** The basic land of which the deck has most (above the number that is kept), or null. */
    private static PaperCard mostPlentifulBasic(final CardPool main, final int exceptColorIndex) {
        PaperCard best = null;
        int bestCount = MIN_BASICS_KEPT;
        for (final Map.Entry<PaperCard, Integer> e : main) {
            final CardRules rules = e.getKey().getRules();
            if (!rules.getType().isBasicLand() || rules.getType().hasSubtype(MagicColor.Constant.BASIC_LANDS.get(exceptColorIndex))) {
                continue;
            }
            final int count = main.countByName(e.getKey());
            if (count > bestCount) {
                bestCount = count;
                best = e.getKey();
            }
        }
        return best;
    }

    /** A basic land of the colour, in the printing the deck already uses for its basics when there is one. */
    private static PaperCard basicLand(final CardPool main, final int colorIndex) {
        final String name = MagicColor.Constant.BASIC_LANDS.get(colorIndex);
        for (final Map.Entry<PaperCard, Integer> e : main) {
            if (e.getKey().getName().equals(name)) {
                return e.getKey();
            }
        }
        String edition = null;
        for (final Map.Entry<PaperCard, Integer> e : main) {
            if (e.getKey().getRules().getType().isBasicLand()) {
                edition = e.getKey().getEdition();
                break;
            }
        }
        PaperCard card = edition == null ? null : StaticData.instance().getCommonCards().getCard(name, edition);
        if (card == null) {
            card = StaticData.instance().getCommonCards().getUniqueByName(name);
        }
        return card;
    }

    /**
     * The card that takes the place of one that was removed: a basic land of the colour the deck is shortest of while
     * it has few lands, else one more copy of the spell it plays most copies of (never past four, one for legends).
     */
    private static PaperCard refillCard(final CardPool main) {
        final byte available = availableColors(main);
        final int lands = main.countAll(c -> c.getRules().getType().isLand());
        if (lands < MAX_LANDS_WHEN_REFILLING) {
            final PaperCard land = shortestBasic(main, available);
            if (land != null) {
                return land;
            }
        }
        PaperCard best = null;
        int bestCount = 0;
        for (final Map.Entry<PaperCard, Integer> e : main) {
            final CardRules rules = e.getKey().getRules();
            if (rules.getType().isLand() || !rules.canCastWithAvailable(available)) {
                continue;
            }
            final int count = main.countByName(e.getKey());
            final int max = rules.getType().isLegendary() || rules.getType().isPlaneswalker() ? 1 : 4;
            if (count < max && count > bestCount) {
                bestCount = count;
                best = e.getKey();
            }
        }
        if (best != null) {
            return best;
        }
        return shortestBasic(main, available);
    }

    /** The basic land of the available colour with the most coloured mana symbols per land that makes it. */
    private static PaperCard shortestBasic(final CardPool main, final byte available) {
        final double[] demand = new double[5];
        final int[] sources = new int[5];
        for (final Map.Entry<PaperCard, Integer> e : main) {
            final CardRules rules = e.getKey().getRules();
            final int copies = e.getValue();
            if (rules.getType().isLand()) {
                final byte colors = rules.getDeckbuildingColors().getColor();
                for (int i = 0; i < 5; i++) {
                    if ((colors & (1 << i)) != 0) {
                        sources[i] += copies;
                    }
                }
            } else {
                final ManaCost cost = rules.getManaCost();
                final int[] shards = cost.getColorShardCounts(); // WUBRGC
                for (int i = 0; i < 5; i++) {
                    demand[i] += shards[i] * copies;
                }
            }
        }
        int best = -1;
        double bestRatio = -1;
        for (int i = 0; i < 5; i++) {
            if ((available & (1 << i)) == 0 || demand[i] <= 0) {
                continue;
            }
            final double ratio = demand[i] / (sources[i] + 1);
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = i;
            }
        }
        return best < 0 ? null : basicLand(main, best);
    }
}
