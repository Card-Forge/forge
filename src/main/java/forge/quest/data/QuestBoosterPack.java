package forge.quest.data;

import forge.Card;
import forge.CardFilter;
import forge.Constant;
import forge.properties.NewConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

import net.slightlymagic.braids.util.generator.GeneratorFunctions;

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
    public ArrayList<String> getQuestStarterDeck(Generator<Card> allCards, int numCommon, int numUncommon, int numRare, boolean standardPool) {
        ArrayList<String> names = new ArrayList<String>();

        // Each color should have around the same amount of monocolored cards
        // There should be 3 Colorless cards for every 4 cards in a single color
        // There should be 1 Multicolor card for every 4 cards in a single color

        ArrayList<String> started = new ArrayList<String>();
        started.add("Multicolor");
        for (int i = 0; i < 4; i++) {
            if (i != 2)
                started.add(Constant.Color.Colorless);

            started.addAll(Arrays.asList(Constant.Color.onlyColors));
        }

        if (standardPool) {
            // filter Cards for cards appearing in Standard Sets
            ArrayList<String> sets = new ArrayList<String>();

            //TODO: It would be handy if the list of any sets can be chosen
            // Can someone clarify that? I don't understand it. -Braids
            sets.add("M12");
            sets.add("NPH");
            sets.add("MBS");
            sets.add("SOM");
            sets.add("M11");
            sets.add("ROE");
            sets.add("WWK");
            sets.add("ZEN");

            allCards = CardFilter.getSets(allCards, sets);
        }

        names.addAll(generateCards(allCards, numCommon, Constant.Rarity.Common, null, started));
        names.addAll(generateCards(allCards, numUncommon, Constant.Rarity.Uncommon, null, started));
        names.addAll(generateCards(allCards, numRare, Constant.Rarity.Rare, null, started));

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
    public ArrayList<String> generateCards(Generator<Card> allCards, int num, String rarity, String color, ArrayList<String> colorOrder) 
    {
        // If color is null, use colorOrder progression to grab cards
        ArrayList<String> names = new ArrayList<String>();

        int size = colorOrder.size();
        Collections.shuffle(colorOrder);

        allCards = CardFilter.getRarity(allCards, rarity);
        int count = 0, i = 0;

        while (count < num) {
            String name = null;

            if (color == null) {
                final String color2 = colorOrder.get(i % size);

                if (color2 != null) {
                    // Mantis Issue 77: avoid calling
                    // getCardName(Generator<Card>, String) with null as 2nd
                    // parameter.
                    name = getCardName(allCards, color2);
                }
            }

            if (name == null) {
                // We can't decide on a color, so just pick a card.
                name = getCardName(allCards);
            }

            if (name != null && !names.contains(name)) {
                names.add(name);
                count++;
            }
            i++;
        }

        return names;
    }

    /**
     * Convenience for generateCards(cards, num, rarity, color, this.choices);
     *
     * @see #generateCards(Generator, int, String, String, ArrayList)
     */
    public ArrayList<String> generateCards(Generator<Card> cards, int num, String rarity, String color) {
        return generateCards(cards, num, rarity, color, choices);
    }

    /**
     * Retrieve a card name at random from the given pool of cards;
     * the card must have a specific color.
     *
     * This forces one evaluation of the allCards Generator.
     *
     * @param allCards  the card pool to use
     * @param color a {@link java.lang.String} object.
     * @return  a random card name with the given color from allCards
     */
    public String getCardName(Generator<Card> allCards, String color) {
        return getCardName(CardFilter.getColor(allCards, color));
    }

    /**
     * Fetch a random card name from the given pool.
     * 
     * This forces one evaluation of the cards Generator.
     *
     * @param cards  the card pool from which to select
     * @return a card name from cards
     */
    public String getCardName(Generator<Card> cards) {
    	Card selected = null;
    	try {
    		selected = GeneratorFunctions.selectRandom(cards);
    	} 
    	catch (NoSuchElementException ignored) {
    		;
    	}
        if (selected == null) {
        	// Previously, it was thought that this 
        	// Only should happen if something is programmed wrong
        	// But empirical evidence contradicts this.
        	return null;
        }
        
        return selected.getName();
    }
}
