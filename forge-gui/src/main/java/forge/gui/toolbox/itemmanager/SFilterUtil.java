package forge.gui.toolbox.itemmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.MagicColor;
import forge.card.CardRulesPredicates.Presets;
import forge.game.GameFormat;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
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
    
    public static Predicate<PaperCard> buildColorFilter(Map<SItemManagerUtil.StatTypes, FLabel> buttonMap) {
        byte colors = 0;

        if (buttonMap.get(StatTypes.WHITE).getSelected()) {
            colors |= MagicColor.WHITE;
        }
        if (buttonMap.get(StatTypes.BLUE).getSelected()) {
            colors |= MagicColor.BLUE;
        }
        if (buttonMap.get(StatTypes.BLACK).getSelected()) {
            colors |= MagicColor.BLACK;
        }
        if (buttonMap.get(StatTypes.RED).getSelected()) {
            colors |= MagicColor.RED;
        }
        if (buttonMap.get(StatTypes.GREEN).getSelected()) {
            colors |= MagicColor.GREEN;
        }

        boolean wantColorless = buttonMap.get(StatTypes.COLORLESS).getSelected();
        boolean wantMulticolor = buttonMap.get(StatTypes.MULTICOLOR).getSelected();

        Predicate<CardRules> preFinal = null;
        if (wantMulticolor) {
            if (colors == 0) { //handle showing all multi-color cards if all 5 colors are filtered
                preFinal = Presets.IS_MULTICOLOR;
                if (wantColorless) {
                    preFinal = Predicates.or(preFinal, Presets.IS_COLORLESS);
                }
            }
            else if (colors != MagicColor.ALL_COLORS) {
                preFinal = CardRulesPredicates.canCastWithAvailable(colors);
            }
        }
        else {
            preFinal = Predicates.not(Presets.IS_MULTICOLOR);
            if (colors != MagicColor.ALL_COLORS) {
                preFinal = Predicates.and(CardRulesPredicates.canCastWithAvailable(colors), preFinal);
            }
        }
        if (!wantColorless) {
            if (colors != 0 && colors != MagicColor.ALL_COLORS) {
                //if colorless filtered out ensure phyrexian cards don't appear
                //unless at least one of their colors is selected
                preFinal = Predicates.and(preFinal, CardRulesPredicates.isColor(colors));
            }
            preFinal = SFilterUtil.optimizedAnd(preFinal, Predicates.not(Presets.IS_COLORLESS));
        }

        if (preFinal == null) {
            return new Predicate<PaperCard>() { //use custom return true delegate to validate the item is a card
                @Override
                public boolean apply(PaperCard card) {
                    return true;
                }
            };
        }
        return Predicates.compose(preFinal, PaperCard.FN_GET_RULES);
    }

    public static Predicate<PaperCard> buildFormatFilter(Set<GameFormat> formats, boolean allowReprints) {
        List<Predicate<PaperCard>> predicates = new ArrayList<Predicate<PaperCard>>();
        for (GameFormat f : formats) {
            predicates.add(allowReprints ? f.getFilterRules() : f.getFilterPrinted());
        }
        return Predicates.or(predicates);
    }

    public static <T> Predicate<T> optimizedAnd(Predicate<T> p1, Predicate<T> p2) {
        return p1 == null ? p2 : (p2 == null ? p1 : Predicates.and(p1, p2));
    }
}
