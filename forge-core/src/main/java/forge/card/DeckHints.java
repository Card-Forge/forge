package forge.card;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.item.PaperCard;
import forge.util.PredicateString;
import forge.util.PredicateString.StringOp;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * DeckHints provides the ability for a Card to "want" another Card or type of
 * Cards in its random deck.
 * 
 */
public class DeckHints {

    /**
     * Enum of types of DeckHints.
     */
    public enum Type {

        /** The Color. */
        COLOR,
        /** The Keyword. */
        KEYWORD,
        /** The Name. */
        NAME,
        /** The Type. */
        TYPE,
        /** The None. */
        NONE
    }

    private boolean valid = false;
    private List<Pair<Type, String>> filters = null;

    /**
     * Construct a DeckHints from the SVar string.
     * 
     * @param hints
     *            SVar for DeckHints
     */
    public DeckHints(String hints) {
        String[] pieces = hints.split("\\&");
        if (pieces.length > 0) {
            for (String piece : pieces) {
                Pair<Type, String> pair = parseHint(piece.trim());
                if (pair != null) {
                    if (filters == null) {
                        filters = new ArrayList<>();
                    }
                    filters.add(pair);
                    valid = true;
                }
            }
        }
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * Returns a Map of Cards by Type from the given Iterable<PaperCard> that match this
     * DeckHints. I.e., other cards that this Card needs in its deck.
     *
     * @param cardList
     *            list of cards to be filtered
     * @return Map of Cards that match this DeckHints by Type.
     */
    public Map<Type, Iterable<PaperCard>> filterByType(Iterable<PaperCard> cardList) {
        Map<Type, Iterable<PaperCard>> ret = new HashMap<>();
        for (Pair<Type, String> pair : filters) {
            Type type = pair.getLeft();
            String param = pair.getRight();
            Iterable<PaperCard> cards = getCardsForFilter(cardList, type, param);
            if (cards != null) {
                ret.put(type, cards);
            }
        }
        return ret;
    }

    /**
     * Returns a list of Cards from the given List<PaperCard> that match this
     * DeckHints. I.e., other cards that this Card needs in its deck.
     *
     * @param cardList
     *            list of cards to be filtered
     * @return List<PaperCard> of Cards that match this DeckHints.
     */
    public List<PaperCard> filter(Iterable<PaperCard> cardList) {
        List<PaperCard> ret = new ArrayList<>();
        for (Pair<Type, String> pair : filters) {
            Type type = pair.getLeft();
            String param = pair.getRight();
            Iterable<PaperCard> cards = getCardsForFilter(cardList, type, param);
            if (cards != null) {
                Iterables.addAll(ret, cards);
            }
        }
        return ret;
    }

    private Pair<Type, String> parseHint(String hint) {
        Pair<Type, String> pair = null;
        String[] pieces = hint.split("\\$");
        if (pieces.length == 2) {
            try {
                Type typeValue = Type.valueOf(pieces[0].toUpperCase());
                for (Type t : Type.values()) {
                    if (typeValue == t) {
                        pair = Pair.of(t, pieces[1]);
                        break;
                    }
                }
            } catch (IllegalArgumentException e) {
                // will remain null
            }
        }
        return pair;
    }

    private Iterable<PaperCard> getCardsForFilter(Iterable<PaperCard> cardList, Type type, String param) {
        List<PaperCard> cards = new ArrayList<>();
        switch (type) {
            case TYPE:
                String[] types = param.split("\\|");
                for (String t : types) {
                    Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.subType(t), PaperCard.FN_GET_RULES));
                }
                break;
            case COLOR:
                String[] colors = param.split("\\|");
                for (String color : colors) {
                    ColorSet cc = ColorSet.fromNames(color);
                    if (cc.isColorless()) {
                        Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.Presets.IS_COLORLESS, PaperCard.FN_GET_RULES));
                    } else {
                        Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.isColor(cc.getColor()), PaperCard.FN_GET_RULES));
                    }
                }
                break;
            case KEYWORD:
                String[] keywords = param.split("\\|");
                for (String keyword : keywords) {
                    Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.hasKeyword(keyword), PaperCard.FN_GET_RULES));
                }
                break;
            case NAME:
                String[] names = param.split("\\|");
                for (String name : names) {
                    Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.name(StringOp.EQUALS, name), PaperCard.FN_GET_RULES));
                }
                break;
        }
        return cards;
    }

    private Iterable<PaperCard> getMatchingItems(Iterable<PaperCard> source, Predicate<CardRules> predicate, Function<PaperCard, CardRules> fn) {
        return Iterables.filter(source, Predicates.compose(predicate, fn));
    }

}
