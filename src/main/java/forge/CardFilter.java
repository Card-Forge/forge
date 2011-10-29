package forge;

import java.util.List;

import net.slightlymagic.braids.util.UtilFunctions;
import net.slightlymagic.braids.util.generator.GeneratorFunctions;
import net.slightlymagic.braids.util.lambda.Lambda1;

import com.google.code.jyield.Generator;

/**
 * <p>
 * CardFilter class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFilter {

    /**
     * Filter a sequence (iterable) of cards to a list of equal or smaller size
     * whose names contain the given substring.
     * 
     * We perform the substring search without sensitivity to case.
     * 
     * @param toBeFiltered
     *            an {@link java.lang.Iterable} of Card instances
     * @param substring
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList cardListNameFilter(final Iterable<Card> toBeFiltered, final String substring) {
        String s;

        CardList listFilter = new CardList();
        for (Card card : toBeFiltered) {
            s = card.getName().toLowerCase();

            if (s.indexOf(substring.toLowerCase()) >= 0) {
                listFilter.add(card);

            }

        }

        return listFilter;
    }

    /**
     * <p>
     * CardListTextFilter.
     * </p>
     * 
     * TODO style: rename this method so it starts with a lowercase letter
     * 
     * @param all
     *            a {@link forge.CardList} object.
     * @param name
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList cardListTextFilter(final CardList all, final String name) {
        Card cardName;
        String s;
        s = "";
        CardList listFilter = new CardList();
        for (int i = 0; i < all.size(); i++) {
            cardName = all.getCard(i);
            s = cardName.getText().toLowerCase();

            if (s.indexOf(name.toLowerCase()) >= 0) {
                listFilter.add(cardName);

            }

        }

        return listFilter;
    }

    /**
     * <p>
     * CardListColorFilter.
     * </p>
     * 
     * TODO style: rename this method so it starts with a lowercase letter
     * 
     * @param all
     *            a {@link forge.CardList} object.
     * @param name
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList cardListColorFilter(final CardList all, final String name) {
        Card cardName = new Card();
        CardList listFilter = new CardList();

        if (name == "black") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!CardUtil.getColors(cardName).contains(Constant.Color.BLACK)) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "blue") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!CardUtil.getColors(cardName).contains(Constant.Color.BLUE)) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "green") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!CardUtil.getColors(cardName).contains(Constant.Color.GREEN)) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "red") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!CardUtil.getColors(cardName).contains(Constant.Color.RED)) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "white") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!CardUtil.getColors(cardName).contains(Constant.Color.WHITE)) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name.equals("colorless")) {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!CardUtil.getColors(cardName).contains(Constant.Color.COLORLESS)) {
                    listFilter.add(cardName);
                }

            }
        }

        return listFilter;
    }

    /**
     * <p>
     * CardListTypeFilter.
     * </p>
     * 
     * TODO style: rename this method so it starts with a lowercase letter
     * 
     * @param all
     *            a {@link forge.CardList} object.
     * @param name
     *            a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList cardListTypeFilter(final CardList all, final String name) {
        Card cardName = new Card();
        CardList listFilter = new CardList();

        if (name == "artifact") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!cardName.isArtifact()) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "creature") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!cardName.isCreature()) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "enchantment") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!cardName.isEnchantment()) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "instant") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!cardName.isInstant()) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "land") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!cardName.isLand()) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name == "planeswalker") {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!cardName.isPlaneswalker()) {
                    listFilter.add(cardName);
                }

            }
        }

        if (name.equals("sorcery")) {
            for (int i = 0; i < all.size(); i++) {
                cardName = all.getCard(i);
                if (!cardName.isSorcery()) {
                    listFilter.add(cardName);
                }

            }
        }

        return listFilter;
    }

    /**
     * Filter an iterable sequence of Cards; note this is a static method that
     * is very similar to the non-static one.
     * 
     * @param iterable
     *            the sequence of cards to examine
     * 
     * @param filt
     *            determines which cards are present in the resulting list
     * 
     * @return a list of Cards that meet the filtering criteria; may be empty,
     *         but never null
     */
    public static CardList filter(final Iterable<Card> iterable, final CardListFilter filt) {
        CardList result = new CardList();
        for (Card card : iterable) {
            if (filt.addCard(card)) {
                result.add(card);
            }
        }

        return result;
    }

    /**
     * Filter a Generator of Cards based on their colors; this does not cause
     * the generator to be evaluated, but rather defers the filtering to when
     * the result's generate method is called (e.g., by YieldUtils.toIterable).
     * 
     * @param inputGenerator
     *            the sequence to filter; must not be null
     * 
     * @param cardColor
     *            a {@link java.lang.String} object; "Multicolor" is also
     *            accepted. Must not be null.
     * 
     * @return a new Generator containing cards only of the desired color or
     *         multicolored cards.
     */
    public static Generator<Card> getColor(final Generator<Card> inputGenerator, final String cardColor) {
        UtilFunctions.checkNotNull("inputGenerator", inputGenerator);
        UtilFunctions.checkNotNull("cardColor", cardColor);

        final boolean weWantMulticolor = cardColor.equals("Multicolor");

        Lambda1<Boolean, Card> predicate = new Lambda1<Boolean, Card>() {
            public Boolean apply(final Card c) {
                if (c == null) {
                    return false;
                }

                if (weWantMulticolor && c.getColor() != null && c.getColor().size() > 1) {
                    return true;
                } else if (c.isColor(cardColor) && c.getColor() != null && c.getColor().size() == 1) {
                    return true;
                }

                return false;
            }
        };

        return GeneratorFunctions.filterGenerator(predicate, inputGenerator);
    } // getColor()

    /**
     * Filter a Generator of cards so that it contains only the ones that exist
     * in certain sets.
     * 
     * @param inputGenerator
     *            a sequence Generator of Card instances; must not be null.
     * 
     * @param sets
     *            an ArrayList of Strings identifying the valid sets; must not
     *            be null.
     * 
     * @return a {@link forge.CardList} object.
     */
    public static Generator<Card> getSets(final Generator<Card> inputGenerator, final List<String> sets) {
        UtilFunctions.checkNotNull("inputGenerator", inputGenerator);
        UtilFunctions.checkNotNull("sets", sets);

        Lambda1<Boolean, Card> predicate = new Lambda1<Boolean, Card>() {
            public Boolean apply(final Card c) {
                if (c == null) {
                    return false;
                }

                for (SetInfo set : c.getSets()) {
                    if (set != null && sets.contains(set.toString())) {
                        return true;
                    }
                }

                return false;
            }
        };

        return GeneratorFunctions.filterGenerator(predicate, inputGenerator);
    } // getSets(Generator,ArrayList)

}
