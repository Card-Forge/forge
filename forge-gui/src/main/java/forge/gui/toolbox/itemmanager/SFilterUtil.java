package forge.gui.toolbox.itemmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCardCatalog.RangeTypes;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSpinner;
import forge.item.PaperCard;
import forge.util.ComparableOp;
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
        final List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();
        final List<Predicate<CardRules>> notColors = new ArrayList<Predicate<CardRules>>();
        final List<Predicate<CardRules>> types = new ArrayList<Predicate<CardRules>>();

        boolean wantMulticolor = false;
        for (SItemManagerUtil.StatTypes s : SItemManagerUtil.StatTypes.values()) {
            switch (s) {
            case WHITE: case BLUE: case BLACK: case RED: case GREEN:
                if (statLabels.get(s).getSelected()) { colors.add(s.predicate); }
                else { notColors.add(Predicates.not(s.predicate)); }
                break;
            case COLORLESS:
                if (statLabels.get(s).getSelected()) { colors.add(s.predicate); }
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

        Predicate<CardRules> preFinal;
        Predicate<CardRules> preColors = colors.size() == 6 ? null : Predicates.or(colors);

        //ensure multicolor cards with filtered out colors don't show up
        //unless card is hybrid playable using colors that aren't filtered out
        if (wantMulticolor) {
            if (colors.isEmpty()) {
                preFinal = CardRulesPredicates.Presets.IS_MULTICOLOR;
            }
            else if (!notColors.isEmpty()) {
                if (notColors.size() == 5) {
                    //if all 5 colors filtered, show only cards that either multicolor or colorless
                    preFinal = Predicates.or(CardRulesPredicates.Presets.IS_MULTICOLOR, preColors);
                }
                else {
                    preFinal = optimizedAnd(
                            Predicates.or(
                                    Predicates.not(CardRulesPredicates.Presets.IS_MULTICOLOR),
                                    Predicates.and(notColors)
                            ),
                            preColors);
                }
            }
            else {
                preFinal = preColors;
            }
        }
        else {
            preFinal = optimizedAnd(Predicates.not(CardRulesPredicates.Presets.IS_MULTICOLOR), preColors);
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

    private static Predicate<CardRules> getCardRulesFieldPredicate(int min, int max, CardRulesPredicates.LeafNumber.CardField field) {
        boolean hasMin = 0 != min;
        boolean hasMax = 10 != max;

        Predicate<CardRules> pMin = !hasMin ? null : new CardRulesPredicates.LeafNumber(field, ComparableOp.GT_OR_EQUAL, min);
        Predicate<CardRules> pMax = !hasMax ? null : new CardRulesPredicates.LeafNumber(field, ComparableOp.LT_OR_EQUAL, max);

        return optimizedAnd(pMin, pMax);
    }

    private static <T> Predicate<T> optimizedAnd(Predicate<T> p1, Predicate<T> p2) {
        return p1 == null ? p2 : (p2 == null ? p1 : Predicates.and(p1, p2));
    }

    /**
     * builds a filter for an interval on a card field
     */
    public static Predicate<PaperCard> buildIntervalFilter(
            Map<RangeTypes, Pair<FSpinner, FSpinner>> spinners, VCardCatalog.RangeTypes field) {
        Pair<FSpinner, FSpinner> sPair = spinners.get(field);
        Predicate<CardRules> fieldFilter = getCardRulesFieldPredicate(
                Integer.valueOf(sPair.getLeft().getValue().toString()),
                Integer.valueOf(sPair.getRight().getValue().toString()), field.cardField);

        if (null != fieldFilter && VCardCatalog.RangeTypes.CMC != field)
        {
            fieldFilter = Predicates.and(fieldFilter, CardRulesPredicates.Presets.IS_CREATURE);
        }

        if (fieldFilter == null) {
            return Predicates.alwaysTrue();
        } else {
            return Predicates.compose(fieldFilter, PaperCard.FN_GET_RULES);
        }
    }
}
