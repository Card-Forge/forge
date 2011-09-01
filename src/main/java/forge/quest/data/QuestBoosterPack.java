package forge.quest.data;

import forge.Card;
import forge.CardFilter;
import forge.Constant;
import forge.card.CardDb;
import forge.card.CardPrinted;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.properties.NewConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

import net.slightlymagic.braids.util.generator.GeneratorFunctions;
import net.slightlymagic.maxmtg.Predicate;

import com.google.code.jyield.Generator;

// The BoosterPack generates cards for the Card Pool in Quest Mode
/**
 * <p>QuestBoosterPack class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestBoosterPack implements NewConstants {
    ArrayList<String> choices;

    /**
     * <p>Constructor for QuestBoosterPack.</p>
     */
    public QuestBoosterPack() {
        choices = new ArrayList<String>();
        choices.add("Multicolor");

        for (String s : Constant.Color.Colors) {
            choices.add(s);
            choices.add(s);
        }
    }

	/**
	 * <p>
	 * getQuestStarterDeck.
	 * </p>
	 * 
	 * @param allCards
	 *            the card pool from which we can generate the deck
	 * 
	 * @param numCommon
	 *            a int.
	 * 
	 * @param numUncommon
	 *            a int.
	 * 
	 * @param numRare
	 *            a int.
	 * 
	 * @param standardPool
	 *            whether to restrict the card pool to what is currently
	 *            considered the Standard block. To update the sets that are
	 *            considered standard, modify this method.
	 * 
	 * @return a {@link java.util.ArrayList} object.
	 */
    public List<CardPrinted> getQuestStarterDeck( final Predicate<CardPrinted> filter, 
            int numCommon, int numUncommon, int numRare ) {
        ArrayList<CardPrinted> names = new ArrayList<CardPrinted>();

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

        names.addAll(generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.isCommon), numCommon, colorFilters));
        names.addAll(generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.isUncommon), numUncommon, colorFilters));
        names.addAll(generateDefinetlyColouredCards(cardpool,
                Predicate.and(filter, CardPrinted.Predicates.Presets.isRareOrMythic), numRare, colorFilters));

        return names;
    }
    
    

    /**
     * Create the list of card names at random from the given pool.
     *
     * @param allCards  the card pool to use
     * @param num  how many card names to add to the result
     * @param rarity  only allow cards of this rarity
     * @param color  may be null; if not null, only cards of this color may be added
     * @param colorOrder  we shuffle this as a side effect of calling this method
     * @return a list of card names
     */
    public ArrayList<CardPrinted> generateDefinetlyColouredCards(
            Iterable<CardPrinted> source,
            Predicate<CardPrinted> filter,
            int cntNeeded,
            List<Predicate<CardRules>> allowedColors)
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


    public ArrayList<CardPrinted> generateDistinctCards(
            final Iterable<CardPrinted> source,
            final Predicate<CardPrinted> filter,
            final int cntNeeded)
    {
        ArrayList<CardPrinted> result = new ArrayList<CardPrinted>();
        int cntMade = 0, iAttempt = 0;

        // This will prevent endless loop @ wh
        int allowedMisses = (2 + 2) * cntNeeded; // lol, 2+2 is not magic constant!

        while (cntMade < cntNeeded && allowedMisses > 0) {
            CardPrinted card = filter.random(source);

            if (card != null && !result.contains(card)) {
                result.add(card);
                cntMade++;
            }
            else { allowedMisses--; }
            iAttempt++;
        }

        return result;
    }


    // Left if only for backwards compatibility
    public ArrayList<CardPrinted> generateCards(int num, CardRarity rarity, String color) {
        Predicate<CardPrinted> whatYouWant = getPredicateForConditions(rarity, color);
        return generateDistinctCards(CardDb.instance().getAllUniqueCards(), whatYouWant, num);
    }

    public ArrayList<CardPrinted> generateCards(Predicate<CardPrinted> filter, int num, CardRarity rarity, String color) {
        Predicate<CardPrinted> whatYouWant = Predicate.and(filter, getPredicateForConditions(rarity, color));
        return generateDistinctCards(CardDb.instance().getAllUniqueCards(), whatYouWant, num);
    }

    protected Predicate<CardPrinted> getPredicateForConditions(CardRarity rarity, String color)
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
            if (col.startsWith("wh")) colorFilter = CardRules.Predicates.Presets.isWhite;
            else if (col.startsWith("bla")) colorFilter = CardRules.Predicates.Presets.isBlack;
            else if (col.startsWith("blu")) colorFilter = CardRules.Predicates.Presets.isBlue;
            else if (col.startsWith("re")) colorFilter = CardRules.Predicates.Presets.isRed;
            else if (col.startsWith("col")) colorFilter = CardRules.Predicates.Presets.isColorless;
            else if (col.startsWith("gre")) colorFilter = CardRules.Predicates.Presets.isGreen;
            else if (col.startsWith("mul")) colorFilter = CardRules.Predicates.Presets.isMulticolor;
            else colorFilter = Predicate.getTrue(CardRules.class);
        }
        return Predicate.and(rFilter, colorFilter, CardPrinted.fnGetRules);
    }


}

