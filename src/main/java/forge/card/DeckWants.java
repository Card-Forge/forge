package forge.card;

import java.util.ArrayList;
import java.util.List;

import forge.item.CardPrinted;
import forge.util.closures.PredicateString;
import forge.util.closures.PredicateString.StringOp;

/**
 * DeckWants provides the ability for a Card to "want" another Card or type of
 * Cards in its random deck.
 * 
 */
public class DeckWants {

    /**
     * Enum of types of DeckWants.
     */
    public enum Type { CARD, COLOR, COLORANY, COLORALL, KEYWORDANY, NAME, TYPE, TYPEANY, NONE }

    private Type type = Type.NONE;
    private String filterParam = null;

    /**
     * Construct a DeckWants from the SVar string.
     * 
     * @param wants
     *            SVar for DeckWants
     */
    public DeckWants(String wants) {
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
     * DeckWants. I.e., other cards that this Card needs in its deck.
     * 
     * @param cardList
     *            list of cards to be filtered
     * @return CardList of Cards that match this DeckWants.
     */
    public List<CardPrinted> filter(List<CardPrinted> cardList) {
        List<CardPrinted> ret;
        switch (type) {
        case TYPE:
            ret = CardRules.Predicates.subType(filterParam).select(cardList, CardPrinted.FN_GET_RULES);
            break;
//        case TYPEANY:
//            ret = new CardList();
//            String[] types = filterParam.split("\\|");
//            for (String type : types) {
//                CardList found = cardList.getType(type.trim());
//                if (found.size() > 0) {
//                    ret.addAll(found);
//                }
//            }
//        case CARD:
//            ret = cardList.getName(filterParam);
//            break;
        case COLOR:
            CardColor color = CardColor.fromNames(filterParam);
            ret = CardRules.Predicates.isColor(color.getColor()).select(cardList, CardPrinted.FN_GET_RULES);
            break;
//        case COLORANY:
//            ret = new CardList();
//            String[] colors = filterParam.split("\\|");
//            for (String color : colors) {
//                CardList found = cardList.getColorByManaCost(color.trim().toLowerCase());
//                if (found.size() > 0) {
//                    ret.addAll(found);
//                }
//            }
//            break;
//        case COLORALL:
//            ret = new CardList();
//            int numFound = 0;
//            colors = filterParam.split("\\|");
//            for (String color : colors) {
//                CardList found = cardList.getColorByManaCost(color.trim().toLowerCase());
//                if (found.size() > 0) {
//                    ret.addAll(found);
//                    numFound++;
//                }
//            }
//            if (numFound < colors.length) {
//                ret.clear();
//            }
//            break;
//        case KEYWORDANY:
//            ret = new ArrayList<CardPrinted>();
//            String[] keywords = filterParam.split("\\|");
//            for (String keyword : keywords) {
//                ret.addAll(CardRules.Predicates..select(cardList, CardPrinted.FN_GET_RULES));
//            }
//            break;
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
