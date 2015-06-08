package forge.itemmanager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.interfaces.IButton;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.util.BinaryUtil;
import forge.util.PredicateString.StringOp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static Predicate<PaperCard> buildTextFilter(String text, boolean invert, boolean inName, boolean inType, boolean inText, boolean inCost) {
        text = text.trim();
        
        if (text.isEmpty()) {
            return Predicates.alwaysTrue();
        }

        if (BooleanExpression.isExpression(text)) {
            BooleanExpression expression = new BooleanExpression(text, inName, inType, inText, inCost);
            
            try {
                Predicate<CardRules> filter = expression.evaluate();
                if (filter != null) {
                    return Predicates.compose(invert ? Predicates.not(filter) : filter, PaperCard.FN_GET_RULES);
                }
            }
            catch (Exception ignored) {
                ignored.printStackTrace();
                //Continue with standard filtering if the expression is not valid.
            }
        }

        List<String> splitText = getSplitText(text);
        List<Predicate<CardRules>> terms = new ArrayList<>();
        for (String s : splitText) {
            List<Predicate<CardRules>> subands = new ArrayList<>();

            if (inName) { subands.add(CardRulesPredicates.name(StringOp.CONTAINS_IC, s));       }
            if (inType) { subands.add(CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, s)); }
            if (inText) { subands.add(CardRulesPredicates.rules(StringOp.CONTAINS_IC, s));      }
            if (inCost) { subands.add(CardRulesPredicates.cost(StringOp.CONTAINS_IC, s));       }

            terms.add(Predicates.or(subands));
        }
        Predicate<CardRules> textFilter = invert ? Predicates.not(Predicates.or(terms)) : Predicates.and(terms);

        return Predicates.compose(textFilter, PaperCard.FN_GET_RULES);
    }

    private static List<String> getSplitText(String text) {
        boolean inQuotes = false;
        String entry = "";
        List<String> splitText = new ArrayList<String>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
            case ' ':
                if (!inQuotes) { //if not in quotes, end current entry
                    if (entry.length() > 0) {
                        splitText.add(entry);
                        entry = "";
                    }
                    continue;
                }
                break;
            case '"':
                inQuotes = !inQuotes;
                continue; //don't append quotation character itself
            case '\\':
                if (i < text.length() - 1 && text.charAt(i + 1) == '"') {
                    ch = '"'; //allow appending escaped quotation character
                    i++; //prevent chaging inQuotes for that character
                }
                break;
            case ',':
                if (!inQuotes) { //ignore commas outside quotes
                    continue;
                }
                break;
            }
            entry += ch;
        }
        if (entry.length() > 0) {
            splitText.add(entry);
        }
        return splitText;
    }

    public static <T extends InventoryItem> Predicate<T> buildItemTextFilter(String text) {
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }

        return new ItemTextPredicate<>(text);
    }

    private static class ItemTextPredicate<T extends InventoryItem> implements Predicate<T> {
        private final List<String> splitText;

        private ItemTextPredicate(String text) {
            splitText = getSplitText(text.toLowerCase());
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
    }
    
    public static Predicate<PaperCard> buildColorFilter(Map<SItemManagerUtil.StatTypes, ? extends IButton> buttonMap) {
        byte colors0 = 0;

        if (buttonMap.get(StatTypes.WHITE).isSelected()) {
            colors0 |= MagicColor.WHITE;
        }
        if (buttonMap.get(StatTypes.BLUE).isSelected()) {
            colors0 |= MagicColor.BLUE;
        }
        if (buttonMap.get(StatTypes.BLACK).isSelected()) {
            colors0 |= MagicColor.BLACK;
        }
        if (buttonMap.get(StatTypes.RED).isSelected()) {
            colors0 |= MagicColor.RED;
        }
        if (buttonMap.get(StatTypes.GREEN).isSelected()) {
            colors0 |= MagicColor.GREEN;
        }

        final byte colors = colors0;
        final boolean wantColorless = buttonMap.get(StatTypes.COLORLESS).isSelected();
        final boolean wantMulticolor = buttonMap.get(StatTypes.MULTICOLOR).isSelected();

        return new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                CardRules rules = card.getRules();
                ColorSet color = rules.getColor();
                boolean allColorsFilteredOut = colors == 0;

                //use color identity for lands, which allows filtering to just lands that can be played in your deck
                boolean useColorIdentity = rules.getType().isLand() && !allColorsFilteredOut && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_FILTER_LANDS_BY_COLOR_IDENTITY);
                if (useColorIdentity) {
                    color = rules.getColorIdentity();
                }

                boolean result = true;
                if (wantMulticolor) {
                    if (colors == 0) { //handle showing all multi-color cards if all 5 colors are filtered
                        result = color.isMulticolor() || (wantColorless && color.isColorless());
                    } else if (colors != ColorSet.ALL_COLORS.getColor()) {
                        if (useColorIdentity && !allColorsFilteredOut) {
                            result = color.hasAnyColor(colors);
                        } else {
                            result = rules.canCastWithAvailable(colors);
                        }
                    }
                } else {
                    result = !color.isMulticolor();
                    if (colors != ColorSet.ALL_COLORS.getColor()) {
                        if (useColorIdentity && !allColorsFilteredOut) {
                            result = result && color.hasAnyColor(colors);
                        } else {
                            result = result && rules.canCastWithAvailable(colors);
                        }
                    }
                }
                if (!wantColorless) {
                    if (colors != 0 && colors != ColorSet.ALL_COLORS.getColor()) {
                        //if colorless filtered out ensure phyrexian cards don't appear
                        //unless at least one of their colors is selected
                        result = result && color.hasAnyColor(colors);
                    }
                    result = result && !color.isColorless();
                }
                return result;
            }
        };
    }

    public static Predicate<DeckProxy> buildDeckColorFilter(final Map<StatTypes, ? extends IButton> buttonMap) {
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(DeckProxy input) {
                byte colorProfile = input.getColor().getColor();
                if (colorProfile == 0) {
                    return buttonMap.get(StatTypes.DECK_COLORLESS).isSelected();
                }

                boolean wantMulticolor = buttonMap.get(StatTypes.DECK_MULTICOLOR).isSelected();
                if (!wantMulticolor && BinaryUtil.bitCount(colorProfile) > 1) {
                    return false;
                }

                byte colors = 0;
                if (buttonMap.get(StatTypes.DECK_WHITE).isSelected()) {
                    colors |= MagicColor.WHITE;
                }
                if (buttonMap.get(StatTypes.DECK_BLUE).isSelected()) {
                    colors |= MagicColor.BLUE;
                }
                if (buttonMap.get(StatTypes.DECK_BLACK).isSelected()) {
                    colors |= MagicColor.BLACK;
                }
                if (buttonMap.get(StatTypes.DECK_RED).isSelected()) {
                    colors |= MagicColor.RED;
                }
                if (buttonMap.get(StatTypes.DECK_GREEN).isSelected()) {
                    colors |= MagicColor.GREEN;
                }

                return colors == 0 && wantMulticolor && BinaryUtil.bitCount(colorProfile) > 1 || (colorProfile & colors) == colorProfile;
            }
        };
    }

    public static void showOnlyStat(StatTypes clickedStat, IButton clickedButton, Map<StatTypes, ? extends IButton> buttonMap) {
        boolean foundSelected = false;
        for (Map.Entry<StatTypes, ? extends IButton> btn : buttonMap.entrySet()) {
            if (btn.getKey() != clickedStat) {
                if (btn.getKey() == StatTypes.MULTICOLOR) {
                    switch (clickedStat) {
                    case WHITE:
                    case BLUE:
                    case BLACK:
                    case RED:
                    case GREEN:
                        //ensure multicolor filter selected after right-clicking a color filter
                        if (!btn.getValue().isSelected()) {
                            btn.getValue().setSelected(true);
                        }
                        continue;
                    default:
                        break;
                    }
                }
                else if (btn.getKey() == StatTypes.DECK_MULTICOLOR) {
                    switch (clickedStat) {
                    case DECK_WHITE:
                    case DECK_BLUE:
                    case DECK_BLACK:
                    case DECK_RED:
                    case DECK_GREEN:
                        //ensure multicolor filter selected after right-clicking a color filter
                        if (!btn.getValue().isSelected()) {
                            btn.getValue().setSelected(true);
                        }
                        continue;
                    default:
                        break;
                    }
                }
                if (btn.getValue().isSelected()) {
                    foundSelected = true;
                    btn.getValue().setSelected(false);
                }
            }
        }
        if (!clickedButton.isSelected()) {
            clickedButton.setSelected(true);
        }
        else if (!foundSelected) {
            //if statLabel only label in group selected, re-select all other labels in group
            for (Map.Entry<StatTypes, ? extends IButton> btn : buttonMap.entrySet()) {
                if (btn.getKey() != clickedStat) {
                    if (!btn.getValue().isSelected()) {
                        btn.getValue().setSelected(true);
                    }
                }
            }
        }
    }

    public static Predicate<PaperCard> buildFormatFilter(Set<GameFormat> formats, boolean allowReprints) {
        List<Predicate<PaperCard>> predicates = new ArrayList<>();
        for (GameFormat f : formats) {
            predicates.add(allowReprints ? f.getFilterRules() : f.getFilterPrinted());
        }
        return Predicates.or(predicates);
    }

    public static <T> Predicate<T> optimizedAnd(Predicate<T> p1, Predicate<T> p2) {
        return p1 == null ? p2 : (p2 == null ? p1 : Predicates.and(p1, p2));
    }
}
