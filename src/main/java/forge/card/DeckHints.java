package forge.card;

import java.util.ArrayList;
import java.util.List;

import forge.item.CardPrinted;
import forge.util.closures.PredicateString.StringOp;

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
        COLOR, KEYWORD, NAME, TYPE,  NONE
    }

    private Type type = Type.NONE;
    private String filterParam = null;

    /**
     * Construct a DeckHints from the SVar string.
     * 
     * @param wants
     *            SVar for DeckHints
     */
    public DeckHints(String wants) {
        String[] pieces = wants.split("\\$");
        if (pieces.length == 2) {
            try {
                Type typeValue = Type.valueOf(pieces[0].toUpperCase());
                for (Type t : Type.values()) {
                    if (typeValue == t) {
                        type = t;
                        break;
                    }
                }
            } catch (IllegalArgumentException e) {
                // type will remain NONE
            }

            filterParam = pieces[1];
        }
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns a list of Cards from the given CardList that match this
     * DeckHints. I.e., other cards that this Card needs in its deck.
     * 
     * @param cardList
     *            list of cards to be filtered
     * @return CardList of Cards that match this DeckHints.
     */
    public List<CardPrinted> filter(List<CardPrinted> cardList) {
        List<CardPrinted> ret;
        switch (type) {
        case TYPE:
            ret = new ArrayList<CardPrinted>();
            String[] types = filterParam.split("\\|");
            for (String type : types) {
                ret.addAll(CardRules.Predicates.subType(type).select(cardList, CardPrinted.FN_GET_RULES));
            }
            break;
        case COLOR:
            ret = new ArrayList<CardPrinted>();
            String[] colors = filterParam.split("\\|");
            for (String color : colors) {
                CardColor cc = CardColor.fromNames(color);
                ret.addAll(CardRules.Predicates.isColor(cc.getColor()).select(cardList, CardPrinted.FN_GET_RULES));
            }
            break;
        case KEYWORD:
            ret = new ArrayList<CardPrinted>();
            String[] keywords = filterParam.split("\\|");
            for (String keyword : keywords) {
                ret.addAll(CardRules.Predicates.hasKeyword(keyword).select(cardList, CardPrinted.FN_GET_RULES));
            }
            break;
        case NAME:
            ret = new ArrayList<CardPrinted>();
            String[] names = filterParam.split("\\|");
            for (String name : names) {
                ret.addAll(CardRules.Predicates.name(StringOp.EQUALS, name).select(cardList, CardPrinted.FN_GET_RULES));
            }
            break;
        default:
            ret = cardList;
            break;
        }
        return ret;
    }

}
