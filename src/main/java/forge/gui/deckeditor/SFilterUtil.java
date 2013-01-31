package forge.gui.deckeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCardCatalog.RangeTypes;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSpinner;
import forge.item.CardPrinted;
import forge.util.ComparableOp;
import forge.util.Pair;
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
    public static Predicate<CardPrinted> buildColorAndTypeFilter(Map<SEditorUtil.StatTypes, FLabel> statLabels) {
        final List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();
        final List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();
        
        boolean wantMulticolor = false;
        Predicate<CardRules> preExceptMulti = null;
        for (SEditorUtil.StatTypes s : SEditorUtil.StatTypes.values()) {
            switch (s) {
            case WHITE: case BLUE: case BLACK: case RED: case GREEN: case COLORLESS:
                if (statLabels.get(s).isSelected()) { colors.add(s.predicate); }
                break;
            case MULTICOLOR:
                wantMulticolor = statLabels.get(s).isSelected();
                preExceptMulti = wantMulticolor ? null : Predicates.not(s.predicate);
                break;
            case LAND: case ARTIFACT: case CREATURE: case ENCHANTMENT: case PLANESWALKER: case INSTANT: case SORCERY:
                if (statLabels.get(s).isSelected()) { types.add(s.predicate); }
                break;
                
            case TOTAL:
                // ignore
                break;
                
            default:
                throw new RuntimeException("unhandled enum value: " + s);
            }
        }

        Predicate<CardRules> preColors = colors.size() == 6 ? null : Predicates.or(colors);
        Predicate<CardRules> preFinal = colors.isEmpty() && wantMulticolor ?
                CardRulesPredicates.Presets.IS_MULTICOLOR : optimizedAnd(preExceptMulti, preColors);

        if (null == preFinal && 7 == types.size()) {
            return Predicates.alwaysTrue();
        }

        Predicate<CardPrinted> typesFinal = Predicates.compose(Predicates.or(types), CardPrinted.FN_GET_RULES);
        if (null == preFinal) {
            return typesFinal;
        }
        
        Predicate<CardPrinted> colorFinal = Predicates.compose(preFinal, CardPrinted.FN_GET_RULES);
        if (7 == types.size()) {
            return colorFinal;
        }
        
        return Predicates.and(colorFinal, typesFinal);
    }

    /**
     * builds a string search filter
     */
    public static Predicate<CardPrinted> buildTextFilter(String text, boolean invert, boolean inName, boolean inType, boolean inText) {
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }
        
        String[] splitText = text
                    .replaceAll(",", "")
                    .replaceAll("  ", " ")
                    .toLowerCase().split(" ");

        List<Predicate<CardRules>> terms = new ArrayList<Predicate<CardRules>>();
        for (String s : splitText) {
            List<Predicate<CardRules>> subands = new ArrayList<Predicate<CardRules>>();

            if (inName) { subands.add(CardRulesPredicates.name(StringOp.CONTAINS_IC, s)); }
            if (inType) { subands.add(CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, s)); }
            
            // rules cannot compare in ignore-case way
            if (inText) { subands.add(CardRulesPredicates.rules(StringOp.CONTAINS, s)); }

            terms.add(Predicates.or(subands));
        }
        Predicate<CardRules> textFilter = invert ? Predicates.not(Predicates.or(terms)) : Predicates.and(terms);
 
        return Predicates.compose(textFilter, CardPrinted.FN_GET_RULES);
    }

    private static Predicate<CardRules> getCardRulesFieldPredicate(int min, int max, CardRulesPredicates.LeafNumber.CardField field) {
        boolean hasMin = 0 != min;
        boolean hasMax = 10 != max;

        Predicate<CardRules> pMin = !hasMin ? null : new CardRulesPredicates.LeafNumber(field, ComparableOp.GT_OR_EQUAL, min);
        Predicate<CardRules> pMax = !hasMax ? null : new CardRulesPredicates.LeafNumber(field, ComparableOp.LT_OR_EQUAL, max);

        return optimizedAnd(pMin, pMax);
    }

    private static <T> Predicate<T> optimizedAnd(Predicate<T> p1, Predicate<T> p2)
    {
        return p1 == null ? p2 : (p2 == null ? p1 : Predicates.and(p1, p2));
    }

    /**
     * builds a filter for an interval on a card field
     */
    public static Predicate<CardPrinted> buildIntervalFilter(
            Map<RangeTypes, Pair<FSpinner, FSpinner>> spinners, VCardCatalog.RangeTypes field) {
        Pair<FSpinner, FSpinner> sPair = spinners.get(field);
        Predicate<CardRules> fieldFilter = getCardRulesFieldPredicate(
                Integer.valueOf(sPair.a.getValue().toString()), Integer.valueOf(sPair.b.getValue().toString()), field.cardField);

        if (null != fieldFilter && VCardCatalog.RangeTypes.CMC != field)
        {
            fieldFilter = Predicates.and(fieldFilter, CardRulesPredicates.Presets.IS_CREATURE);
        }

        if (fieldFilter == null) {
            return Predicates.alwaysTrue();
        } else {
            return Predicates.compose(fieldFilter, CardPrinted.FN_GET_RULES);
        }
    }
}
