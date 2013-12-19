package forge.gui.toolbox.itemmanager;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.util.PredicateString.StringOp;

/** 
 * Static factory; holds blocks of form elements and predicates
 * which are used in various editing environments.
 * <br><br>
 * <i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SFilterUtil {
    /**
     * builds a string search filter
     */
    public static Predicate<PaperCard> buildTextFilter(String text, boolean invert, boolean inName, boolean inType, boolean inText) {
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }

        String[] splitText = text.replaceAll(",", "").replaceAll("  ", " ").split(" ");

        List<Predicate<CardRules>> terms = new ArrayList<Predicate<CardRules>>();
        for (String s : splitText) {
            List<Predicate<CardRules>> subands = new ArrayList<Predicate<CardRules>>();

            if (inName) { subands.add(CardRulesPredicates.name(StringOp.CONTAINS_IC, s));       }
            if (inType) { subands.add(CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, s)); }
            if (inText) { subands.add(CardRulesPredicates.rules(StringOp.CONTAINS_IC, s));      }

            terms.add(Predicates.or(subands));
        }
        Predicate<CardRules> textFilter = invert ? Predicates.not(Predicates.or(terms)) : Predicates.and(terms);

        return Predicates.compose(textFilter, PaperCard.FN_GET_RULES);
    }
    
    public static <T extends InventoryItem> Predicate<T> buildItemTextFilter(String text) {
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }

        return new ItemTextPredicate<T>(text);
    }

    private static class ItemTextPredicate<T extends InventoryItem> implements Predicate<T> {
        private final String[] splitText;

        private ItemTextPredicate(String text) {
            splitText = text.toLowerCase().replaceAll(",", "").replaceAll("  ", " ").split(" ");
        }
        
        @Override
        public boolean apply(T input) {
            String name = input.getName().toLowerCase();
            for (String s : splitText) {
                if (name.contains(s)) {
                    return true;
                }
            }
            return false;
        }
    };

    public static <T> Predicate<T> optimizedAnd(Predicate<T> p1, Predicate<T> p2) {
        return p1 == null ? p2 : (p2 == null ? p1 : Predicates.and(p1, p2));
    }
}
