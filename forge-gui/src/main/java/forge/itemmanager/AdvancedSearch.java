package forge.itemmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.GameFormat;
import forge.model.FModel;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;

public class AdvancedSearch {
    private enum FilterOption {
        CARD_NAME("Name", FilterOperator.STRING_OPS, new StringValueSelector()),
        CARD_RULES_TEXT("Rules Text", FilterOperator.STRING_OPS, new StringValueSelector()),
        CARD_EXPANSION("Expansion", FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<CardEdition>(FModel.getMagicDb().getSortedEditions())),
        CARD_FORMAT("Format", FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<GameFormat>((List<GameFormat>)FModel.getFormats().getOrderedList())),
        CARD_COLOR("Color", FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<String>(MagicColor.Constant.COLORS_AND_COLORLESS)),
        CARD_TYPE("Type", FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<String>(CardType.getSortedCoreAndSuperTypes())),
        CARD_SUB_TYPE("Subtype", FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<String>(CardType.getSortedSubTypes())),
        CARD_CMC("CMC", FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_GENERIC_COST("Generic Cost", FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_POWER("Power", FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_TOUGHNESS("Toughness", FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_MANA_COST("Mana Cost", FilterOperator.STRING_OPS, new StringValueSelector())/*,
        CARD_RARITY("Rarity", FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector(CardRarity.values()))*/;

        private final String name;
        private final FilterOperator[] operatorOptions;
        private final FilterValueSelector<?> valueSelector;

        private FilterOption(String name0, FilterOperator[] operatorOptions0, FilterValueSelector<?> valueSelector0) {
            name = name0;
            operatorOptions = operatorOptions0;
            valueSelector = valueSelector0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum FilterOperator {
        //Numeric operators
        EQUALS("is equal to", "{0}=={1}", 1),
        NOT_EQUALS("is not equal to", "{0}!={1}", 1),
        GREATER_THAN("is greater than", "{0}>{1}", 1),
        LESS_THAN("is less than", "{0}<{1}", 1),
        GT_OR_EQUAL("is greater than or equal to", "{0}>={1}", 1),
        LT_OR_EQUAL("is less than or equal to", "{0}<={1}", 1),
        BETWEEN_INCLUSIVE("is between (inclusive)", "{1}<={0}<={2}", 2),
        BETWEEN_EXCLUSIVE("is between (exclusive)", "{1}<{0}<{2}", 2),

        //String operators
        IS("is", "{0} is '{1}'", 1),
        IS_NOT("is not", "{0} is not '{1}'", 1),
        CONTAINS("contains", "{0} contains '{1}'", 1),
        STARTS_WITH("starts with", "{0} starts with '{1}'", 1),
        ENDS_WITH("ends with", "{0} ends with '{1}'", 1),

        //Custom list operators
        IS_EXACTLY("is exactly", "{0} is exactly '{X}'", -1),
        IS_ANY("is any of", "{0} is '{X:or}'", -1),
        IS_ALL("is all of", "{0} is '{X:and}'", -1),
        IS_NONE("is none of", "{0} is not '{X:or}'", -1),
        INCLUDES_ANY("includes any of", "{0} includes '{X:or}'", -1),
        INCLUDES_ALL("includes all of", "{0} includes '{X:and}'", -1);

        public static final FilterOperator[] NUMBER_OPS = new FilterOperator[] {
            EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GT_OR_EQUAL, LT_OR_EQUAL, BETWEEN_INCLUSIVE, BETWEEN_EXCLUSIVE
        };
        public static final FilterOperator[] STRING_OPS = new FilterOperator[] {
            IS, IS_NOT, CONTAINS, STARTS_WITH, ENDS_WITH
        };
        public static final FilterOperator[] CUSTOM_LIST_OPS = new FilterOperator[] {
            IS_EXACTLY, IS_ANY, IS_ALL, IS_NONE, INCLUDES_ANY, INCLUDES_ALL
        };
        
        private final String caption, formatStr;
        private final int valueCount;
        
        private FilterOperator(String caption0, String formatStr0, int valueCount0) {
            caption = caption0;
            formatStr = formatStr0;
            valueCount = valueCount0;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    private static abstract class FilterValueSelector<T> {
        public abstract List<T> getValues(String message, int count);
    }

    private static class NumericValueSelector extends FilterValueSelector<Integer> {
        private final int min, max;

        public NumericValueSelector(int min0, int max0) {
            min = min0;
            max = max0;
        }

        @Override
        public List<Integer> getValues(String message, int count) {
            String msg = message;
            if (count == 2) {
                msg += " (Lower Bound)";
            }
            Integer lowerBound = SGuiChoose.getInteger(msg, min, max);
            if (lowerBound == null) { return null; }

            List<Integer> values = new ArrayList<Integer>();
            values.add(lowerBound);
            if (count == 2) { //prompt for upper bound if needed
                msg = message + " (Upper Bound)";
                Integer upperBound = SGuiChoose.getInteger(msg, lowerBound, max);
                if (upperBound == null) { return null; }
                values.add(upperBound);
            }
            return values;
        }
    }

    private static class StringValueSelector extends FilterValueSelector<String> {
        public StringValueSelector() {
        }

        @Override
        public List<String> getValues(String message, int count) {
            String value = SOptionPane.showInputDialog("", message);
            if (value == null) { return null; }

            List<String> values = new ArrayList<String>();
            values.add(value);
            return values;
        }
    }

    private static class CustomListValueSelector<T> extends FilterValueSelector<T> {
        private final Collection<T> choices;

        public CustomListValueSelector(Collection<T> choices0) {
            choices = choices0;
        }

        @Override
        public List<T> getValues(String message, int count) {
            List<T> values = SGuiChoose.getChoices(message, 0, choices.size(), choices);
            if (values == null || values.size() == 0) {
                return null;
            }
            return values;
        }
    }
}
