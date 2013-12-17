package forge.gui.toolbox.itemmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.CardRulesPredicates.Presets;
import forge.card.MagicColor;
import forge.gui.toolbox.FLabel;
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
     * queries color filters for state and returns a predicate.
     * <br><br>
     * Handles "multicolor" label, which is quite tricky.
     */
    public static Predicate<PaperCard> buildColorAndTypeFilter(Map<SItemManagerUtil.StatTypes, FLabel> statLabels) {
        final List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();

        byte colors = 0;
        boolean wantColorless = false;
        boolean wantMulticolor = false;
        for (SItemManagerUtil.StatTypes s : SItemManagerUtil.StatTypes.values()) {
            switch (s) {
            case WHITE:
                if (statLabels.get(s).getSelected()) { colors |= MagicColor.WHITE; }
                break;
            case BLUE:
                if (statLabels.get(s).getSelected()) { colors |= MagicColor.BLUE; }
                break;
            case BLACK:
                if (statLabels.get(s).getSelected()) { colors |= MagicColor.BLACK; }
                break;
            case RED:
                if (statLabels.get(s).getSelected()) { colors |= MagicColor.RED; }
                break;
            case GREEN:
                if (statLabels.get(s).getSelected()) { colors |= MagicColor.GREEN; }
                break;
            case COLORLESS:
                wantColorless = statLabels.get(s).getSelected();
                break;
            case MULTICOLOR:
                wantMulticolor = statLabels.get(s).getSelected();
                break;
            case LAND: case ARTIFACT: case CREATURE: case ENCHANTMENT: case PLANESWALKER: case INSTANT: case SORCERY:
                if (statLabels.get(s).getSelected()) { types.add(s.predicate); }
                break;
            case TOTAL: case PACK:
                // ignore
                break;
            default:
                throw new RuntimeException("unhandled enum value: " + s);
            }
        }

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
        else if (colors != MagicColor.ALL_COLORS) {
            preFinal = Predicates.and(CardRulesPredicates.canCastWithAvailable(colors), Predicates.not(Presets.IS_MULTICOLOR));
        }
        if (!wantColorless) {
            if (colors != 0 && colors != MagicColor.ALL_COLORS) {
                //if colorless filtered out ensure phyrexian cards don't appear
                //unless at least one of their colors is selected
                preFinal = Predicates.and(preFinal, CardRulesPredicates.isColor(colors));
            }
            preFinal = optimizedAnd(preFinal, Predicates.not(Presets.IS_COLORLESS));
        }

        if (preFinal == null && types.size() == 7) {
            return Predicates.alwaysTrue();
        }

        Predicate<PaperCard> typesFinal = Predicates.compose(Predicates.or(types), PaperCard.FN_GET_RULES);
        if (preFinal == null) {
            return typesFinal;
        }

        Predicate<PaperCard> colorFinal = Predicates.compose(preFinal, PaperCard.FN_GET_RULES);
        if (types.size() == 7) {
            return colorFinal;
        }

        return Predicates.and(colorFinal, typesFinal);
    }

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
