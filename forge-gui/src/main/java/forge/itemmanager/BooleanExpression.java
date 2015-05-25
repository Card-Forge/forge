package forge.itemmanager;


import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.util.PredicateString.StringOp;

import java.util.*;

public class BooleanExpression {

    private Stack<Operator> operators = new Stack<>();
    private Stack<Predicate<CardRules>> operands = new Stack<>();

    private StringTokenizer expression;

    private boolean inName, inType, inText, inCost;

    private enum Operator {

        AND("&", 0), OR("|", 0), NOT("!", 1), OPEN_PAREN("(", 2), CLOSE_PAREN(")", 2), ESCAPE("\\", -1);

        private final String token;
        private final int precedence;

        Operator(final String token, final int precedence) {
            this.token = token;
            this.precedence = precedence;
        }

    }

    public BooleanExpression(final String expression, final boolean inName, final boolean inType, final boolean inText, final boolean inCost) {
        this.expression = new StringTokenizer(expression);
        this.inName = inName;
        this.inType = inType;
        this.inText = inText;
        this.inCost = inCost;
    }

    public Predicate<CardRules> evaluate() {

        String currentValue = "";
        boolean escapeNext = false;

        while (expression.hasNext()) {

            String token = expression.next();
            Operator operator = null;

            if (token.equals(Operator.AND.token)) {
                operator = Operator.AND;
            } else if (token.equals(Operator.OR.token)) {
                operator = Operator.OR;
            } else if (token.equals(Operator.OPEN_PAREN.token)) {
                operator = Operator.OPEN_PAREN;
            } else if (token.equals(Operator.CLOSE_PAREN.token)) {
                operator = Operator.CLOSE_PAREN;
            } else if (token.equals(Operator.NOT.token) && currentValue.trim().isEmpty()) { //Ignore ! operators that aren't the first token in a search term (Don't use '!' in 'Kaboom!')
                operator = Operator.NOT;
            } else if (token.equals(Operator.ESCAPE.token)) {
                escapeNext = true;
                continue;
            }

            if (operator == null) {
                currentValue += token;
            } else {

                if (escapeNext) {
                    escapeNext = false;
                    currentValue += token;
                    continue;
                }

                if (!currentValue.trim().isEmpty()) {
                    operands.push(valueOf(currentValue.trim()));
                }

                currentValue = "";

                if (!operators.isEmpty() && operator.precedence < operators.peek().precedence) {
                    resolve(true);
                } else if (!operators.isEmpty() && operator == Operator.CLOSE_PAREN) {

                    while (!operators.isEmpty() && operators.peek() != Operator.OPEN_PAREN) {
                        resolve(true);
                    }

                }

                operators.push(operator);

            }

        }

        if (!currentValue.trim().isEmpty()) {
            operands.push(valueOf(currentValue.trim()));
        }

        while (!operators.isEmpty()) {
            resolve(true);
        }

        return operands.get(0);

    }

    private void resolve(final boolean alwaysPopOperator) {

        Predicate<CardRules> right;
        Predicate<CardRules> left;

        switch (operators.peek()) {
            case AND:
                operators.pop();
                right = operands.pop();
                left = operands.pop();
                operands.push(Predicates.and(left, right));
                break;
            case OR:
                operators.pop();
                right = operands.pop();
                left = operands.pop();
                operands.push(Predicates.or(left, right));
                break;
            case NOT:
                operators.pop();
                left = operands.pop();
                operands.push(Predicates.not(left));
                break;
            default:
                if (alwaysPopOperator) {
                    operators.pop();
                }
                break;
        }

    }

    private Predicate<CardRules> valueOf(final String value) {

        List<Predicate<CardRules>> predicates = new ArrayList<>();
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
        if (!predicates.isEmpty()) {
            return Predicates.or(predicates);
        }
        return Predicates.alwaysTrue();

    }

    public static boolean isExpression(final String string) {
        return string.contains(Operator.AND.token) || string.contains(Operator.OR.token) || string.trim().startsWith(Operator.NOT.token);
    }

}
