package forge.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.item.PaperCard;
import forge.util.PredicateString.StringOp;
import forge.util.collect.FCollection;

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
        /** The Ability */
        ABILITY,
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
    public List<Pair<Type, String>> filters = null;

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

    public boolean contains(Type type, String hint) {
        if (filters == null) {
            return false;
        }
        for (Pair<Type, String> filter : filters) {
            if (filter.getLeft() == type && filter.getRight().contains(hint)) {
                return true;
            }
        }
        return false;
    }
    public boolean is(Type type, String hints[]) {
        if (filters == null) {
            return false;
        }
        int num = 0;
        for (String hint : hints) {
            for (Pair<Type, String> filter : filters) {
                System.out.println(filter.getLeft() + " = type = " +  type + "    "+ filter.getRight() + " =filter=   " + hint);
                if (filter.getLeft() == type && filter.getRight().equals(hint)) {
                    num++;
                    if (num == hints.length) {
                        return true;
                    }
                }
            }
        }
        return false;
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
                // if a type is used more than once intersect respective matches
                if (ret.containsKey(type)) {
                    Iterables.retainAll(cards, new FCollection<>(ret.get(type)));
                }
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
    public Iterable<PaperCard> filter(Iterable<PaperCard> cardList) {
        return Iterables.concat(filterByType(cardList).values());
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

        // this is case ABILITY, but other types can also use this when the implicit parsing would miss
        String[] params = param.split("\\|");
        for (String ability : params) {
            Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.deckHas(type, ability), PaperCard.FN_GET_RULES));
        }
        // bonus if a DeckHas can satisfy the type with multiple ones
        if (params.length > 1) {
            Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.deckHasExactly(type, params), PaperCard.FN_GET_RULES));
        }

        for (String p : params) {
            switch (type) {
            case COLOR:
                ColorSet cc = ColorSet.fromNames(p);
                if (cc.isColorless()) {
                    Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.Presets.IS_COLORLESS, PaperCard.FN_GET_RULES));
                } else {
                    Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.isColor(cc.getColor()), PaperCard.FN_GET_RULES));
                }
                break;
            case KEYWORD:
                Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.hasKeyword(p), PaperCard.FN_GET_RULES));
                break;
            case NAME:
                Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.name(StringOp.EQUALS, p), PaperCard.FN_GET_RULES));
                break;
            case TYPE:
                Iterables.addAll(cards, getMatchingItems(cardList, CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, p), PaperCard.FN_GET_RULES));
                break;
            case NONE:
            case ABILITY: // already done above
                break;
            }
        }
        return cards;
    }

    private Iterable<PaperCard> getMatchingItems(Iterable<PaperCard> source, Predicate<CardRules> predicate, Function<PaperCard, CardRules> fn) {
        return Iterables.filter(source, Predicates.compose(predicate, fn));
    }

}
