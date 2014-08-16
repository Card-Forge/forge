package forge.itemmanager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.CardRulesPredicates.Presets;
import forge.card.MagicColor;
import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.interfaces.IButton;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.util.BinaryUtil;
import forge.util.PredicateString.StringOp;

import java.util.*;

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
        
        BooleanExpression expression = new BooleanExpression(text, inName, inType, inText, inCost);
        
        Predicate<CardRules> filter = expression.evaluate();
        if (filter != null) {
            return Predicates.compose(filter, PaperCard.FN_GET_RULES);
        }
        
        String[] splitText = text.replaceAll(",", "").replaceAll("  ", " ").split(" ");

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

    private static class Tokenizer implements Iterator<String> {
        private String string;
        private int index = 0;

        private Tokenizer(String string) {
            this.string = string;
        }

        @Override
        public boolean hasNext() {
            return index < string.length();
        }

        @Override
        public String next() {
            return string.charAt(index++) + "";
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        public String lookAhead() {
            if (hasNext()) {
                return string.charAt(index) + "";
            }
            else {
                return "";
            }
        }
    }
    
    private static class BooleanExpression {
        private static enum Operation {
            AND("&&"), OR("||"), NOT("!"), OPEN_PAREN("("), CLOSE_PAREN(")"), ESCAPE("\\");

            private final String token;

            private Operation(String token) {
                this.token = token;
            }
        }
        
        private String text;
        private Stack<String> stack = new Stack<>();
        
        private String currentValue = "";
        
        private boolean inName = false;
        private boolean inType = false;
        private boolean inText = false;
        private boolean inCost = false;
        
        private BooleanExpression(String text, boolean inName, boolean inType, boolean inText, boolean inCost) {
            this.text = text;
            this.inName = inName;
            this.inType = inType;
            this.inText = inText;
            this.inCost = inCost;
            parse();
        }
        
        private void parse() {
            Tokenizer tokenizer = new Tokenizer(text);
            
            String currentChar;
            boolean escapeNext = false;
            
            while (tokenizer.hasNext()) {
                currentChar = tokenizer.next();
                
                if (escapeNext) {
                    currentValue += currentChar;
                    escapeNext = false;
                }
                else if (currentChar.equals(Operation.ESCAPE.token)) {
                    escapeNext = true;
                }
                else {
                    if ((currentChar + tokenizer.lookAhead()).equals(Operation.AND.token)) {
                        tokenizer.next();
                        pushTokenToStack(Operation.AND.token);
                    }
                    else if ((currentChar + tokenizer.lookAhead()).equals(Operation.OR.token)) {
                        tokenizer.next();
                        pushTokenToStack(Operation.OR.token);
                    }
                    else if (currentChar.equals(Operation.OPEN_PAREN.token)) {
                        pushTokenToStack(Operation.OPEN_PAREN.token);
                    }
                    else if (currentChar.equals(Operation.CLOSE_PAREN.token)) {
                        pushTokenToStack(Operation.CLOSE_PAREN.token);
                    }
                    else if (currentChar.equals(Operation.NOT.token)) {
                        pushTokenToStack(Operation.NOT.token);
                    }
                    else {
                        currentValue += currentChar;
                    }
                }
            }
            
            if (!currentValue.trim().isEmpty()) {
                stack.push(currentValue.trim());
            }
        }
        
        private void pushTokenToStack(String token) {
            currentValue = currentValue.trim();
            
            if (!currentValue.isEmpty()) {
                stack.push(currentValue);
                currentValue = "";
            }
            
            stack.push(token);
        }
        
        private Predicate<CardRules> evaluate() {
            Collections.reverse(stack); //Reverse the stack so we're popping off the start
            Predicate<CardRules> rules = null;
            
            Stack<String> evaluationStack = new Stack<>();
            
            while (stack.size() > 0) {
                String stackItem = stack.pop();
                
                if (stackItem.equals(Operation.CLOSE_PAREN.token)) {
                    rules = evaluateUntilToken(evaluationStack, rules, Operation.OPEN_PAREN.token);
                }
                else {
                    evaluationStack.push(stackItem);
                }
            }
            
            return evaluateUntilToken(evaluationStack, rules, "");
        }
        
        private Predicate<CardRules> evaluateUntilToken(Stack<String> evaluationStack, Predicate<CardRules> rules, String token) {
            Predicate<CardRules> outputRules = rules;
            
            Operation currentOperation = null;

            while (!evaluationStack.isEmpty()) {
                String stackItem = evaluationStack.pop();

                if (!token.isEmpty() && token.equals(stackItem)) {
                    break;
                }
                
                if (isOperation(stackItem)) {
                    if (stackItem.equals(Operation.AND.token)) {
                        currentOperation = Operation.AND;
                    }
                    else if (stackItem.equals(Operation.OR.token)) {
                        currentOperation = Operation.OR;
                    }
                    else if (stackItem.equals(Operation.NOT.token)) {
                        if (outputRules == null) {
                            return null;
                        }
                        outputRules = Predicates.not(outputRules);
                    }
                }
                else {
                    if (currentOperation == null) {
                        if (outputRules == null) {
                            outputRules = evaluateValue(stackItem);
                        }
                    }
                    else {
                        if (outputRules == null) {
                            return null;
                        }

                        switch (currentOperation) {
                        case AND:
                            outputRules = Predicates.and(outputRules, evaluateValue(stackItem));
                            break;
                        case OR:
                            outputRules = Predicates.or(outputRules, evaluateValue(stackItem));
                            break;
                        default:
                            break;
                        }

                        currentOperation = null;
                    }
                }
            }
            
            return outputRules;
        }
        
        private Predicate<CardRules> evaluateValue(String value) {
            List<Predicate<CardRules>> predicates = new ArrayList<Predicate<CardRules>>();
            if (inName) {
                predicates.add(CardRulesPredicates.name(StringOp.CONTAINS_IC, value));
            }
            if (inType) {
                predicates.add(CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, value));
            }
            if (inText) {
                predicates.add(CardRulesPredicates.rules(StringOp.CONTAINS_IC, value));
            }
            if (inCost) {
                predicates.add(CardRulesPredicates.cost(StringOp.CONTAINS_IC, value));
            }
            if (predicates.size() > 0) {
                return Predicates.or(predicates);
            }
            return Predicates.alwaysTrue();
        }
        
        private boolean isOperation(String token) {
            for (Operation o : Operation.values()) {
                if (token.equals(o.token)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static <T extends InventoryItem> Predicate<T> buildItemTextFilter(String text) {
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }

        return new ItemTextPredicate<>(text);
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
    }
    
    public static Predicate<PaperCard> buildColorFilter(Map<SItemManagerUtil.StatTypes, ? extends IButton> buttonMap) {
        byte colors = 0;

        if (buttonMap.get(StatTypes.WHITE).isSelected()) {
            colors |= MagicColor.WHITE;
        }
        if (buttonMap.get(StatTypes.BLUE).isSelected()) {
            colors |= MagicColor.BLUE;
        }
        if (buttonMap.get(StatTypes.BLACK).isSelected()) {
            colors |= MagicColor.BLACK;
        }
        if (buttonMap.get(StatTypes.RED).isSelected()) {
            colors |= MagicColor.RED;
        }
        if (buttonMap.get(StatTypes.GREEN).isSelected()) {
            colors |= MagicColor.GREEN;
        }

        boolean wantColorless = buttonMap.get(StatTypes.COLORLESS).isSelected();
        boolean wantMulticolor = buttonMap.get(StatTypes.MULTICOLOR).isSelected();

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
