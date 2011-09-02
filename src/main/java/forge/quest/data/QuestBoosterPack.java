package forge.quest.data;

import forge.card.CardDb;
import forge.card.CardPrinted;
import forge.card.CardRarity;
import forge.card.CardRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.slightlymagic.maxmtg.Predicate;

// The BoosterPack generates cards for the Card Pool in Quest Mode
/**
 * <p>QuestBoosterPack class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public final class QuestBoosterPack {

    public static List<CardPrinted> getQuestStarterDeck(final Predicate<CardPrinted> filter,
            final int numCommon, final int numUncommon, final int numRare)
    {
        ArrayList<CardPrinted> cards = new ArrayList<CardPrinted>();

        // Each color should have around the same amount of monocolored cards
        // There should be 3 Colorless cards for every 4 cards in a single color
        // There should be 1 Multicolor card for every 4 cards in a single color

        List<Predicate<CardRules>> colorFilters = new ArrayList<Predicate<CardRules>>();
        colorFilters.add(CardRules.Predicates.Presets.isMulticolor);

        for (int i = 0; i < 4; i++) {
            if (i != 2) { colorFilters.add(CardRules.Predicates.Presets.isColorless); }

            colorFilters.add(CardRules.Predicates.Presets.isWhite);
            colorFilters.add(CardRules.Predicates.Presets.isRed);
            colorFilters.add(CardRules.Predicates.Presets.isBlue);
            colorFilters.add(CardRules.Predicates.Presets.isBlack);
            colorFilters.add(CardRules.Predicates.Presets.isGreen);
        }

        Iterable<CardPrinted> cardpool = CardDb.instance().getAllUniqueCards();

        cards.addAll(generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.isCommon), numCommon, colorFilters));
        cards.addAll(generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.isUncommon), numUncommon, colorFilters));
        cards.addAll(generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.isRareOrMythic), numRare, colorFilters));
        return cards;
    }

    /**
     * Create the list of card names at random from the given pool.
     *
     * @param source  an Iterable<CardPrinted>
     * @param filter  Predicate<CardPrinted>
     * @param cntNeeded  an int
     * @param allowedColors a List<Predicate<CardRules>>
     * @return a list of card names
     */
    private static ArrayList<CardPrinted> generateDefinetlyColouredCards(
            final Iterable<CardPrinted> source,
            final Predicate<CardPrinted> filter,
            final int cntNeeded,
            final List<Predicate<CardRules>> allowedColors)
    {
        // If color is null, use colorOrder progression to grab cards
        ArrayList<CardPrinted> result = new ArrayList<CardPrinted>();

        int size = allowedColors == null ? 0 : allowedColors.size();
        Collections.shuffle(allowedColors);

        int cntMade = 0, iAttempt = 0;

        // This will prevent endless loop @ wh
        int allowedMisses = (2 + size + 2) * cntNeeded; // lol, 2+2 is not magic constant!

        while (cntMade < cntNeeded && allowedMisses > 0) {
            CardPrinted card = null;

            if (size > 0) {
                final Predicate<CardRules> color2 = allowedColors.get(iAttempt % size);
                if (color2 != null) {
                    card = Predicate.and(filter, color2, CardPrinted.fnGetRules).random(source);
                }
            }

            if (card == null) {
                // We can't decide on a color, so just pick a card.
                card = filter.random(source);
            }

            if (card != null && !result.contains(card)) {
                result.add(card);
                cntMade++;
            }
            else { allowedMisses--; }
            iAttempt++;
        }

        return result;
    }


    private static ArrayList<CardPrinted> generateDistinctCards(
            final Iterable<CardPrinted> source,
            final Predicate<CardPrinted> filter,
            final int cntNeeded)
    {
        ArrayList<CardPrinted> result = new ArrayList<CardPrinted>();
        int cntMade = 0;

        // This will prevent endless loop @ wh
        int allowedMisses = (2 + 2) * cntNeeded; // lol, 2+2 is not magic constant!

        while (cntMade < cntNeeded && allowedMisses > 0) {
            CardPrinted card = filter.random(source);

            if (card != null && !result.contains(card)) {
                result.add(card);
                cntMade++;
            }
            else { allowedMisses--; }
        }

        return result;
    }


    // Left if only for backwards compatibility
    public ArrayList<CardPrinted> generateCards(final int num, final CardRarity rarity, final String color) {
        Predicate<CardPrinted> whatYouWant = getPredicateForConditions(rarity, color);
        return generateDistinctCards(CardDb.instance().getAllUniqueCards(), whatYouWant, num);
    }

    public static ArrayList<CardPrinted> generateCards(final Predicate<CardPrinted> filter, int num, CardRarity rarity, String color) {
        Predicate<CardPrinted> whatYouWant = Predicate.and(filter, getPredicateForConditions(rarity, color));
        return generateDistinctCards(CardDb.instance().getAllUniqueCards(), whatYouWant, num);
    }

    private static Predicate<CardPrinted> getPredicateForConditions(final CardRarity rarity, final String color)
    {
        Predicate<CardPrinted> rFilter;
        switch (rarity) {
            case Rare: rFilter = CardPrinted.Predicates.Presets.isRareOrMythic; break;
            case Common: rFilter = CardPrinted.Predicates.Presets.isCommon; break;
            case Uncommon: rFilter = CardPrinted.Predicates.Presets.isUncommon; break;
            default: rFilter = Predicate.getTrue(CardPrinted.class);
        }

        Predicate<CardRules> colorFilter;
        if (StringUtils.isBlank(color)) {
            colorFilter = Predicate.getTrue(CardRules.class);
        } else {
            String col = color.toLowerCase();
            if (col.startsWith("wh")) {
                colorFilter = CardRules.Predicates.Presets.isWhite;
            } else if (col.startsWith("bla")) {
                colorFilter = CardRules.Predicates.Presets.isBlack;
            } else if (col.startsWith("blu")) {
                colorFilter = CardRules.Predicates.Presets.isBlue;
            } else if (col.startsWith("re")) {
                colorFilter = CardRules.Predicates.Presets.isRed;
            } else if (col.startsWith("col")) {
                colorFilter = CardRules.Predicates.Presets.isColorless;
            } else if (col.startsWith("gre")) {
                colorFilter = CardRules.Predicates.Presets.isGreen;
            } else if (col.startsWith("mul")) {
                colorFilter = CardRules.Predicates.Presets.isMulticolor;
            } else {
                colorFilter = Predicate.getTrue(CardRules.class);
            }
        }
        return Predicate.and(rFilter, colorFilter, CardPrinted.fnGetRules);
    }


}

