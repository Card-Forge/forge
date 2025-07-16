package forge.itemmanager;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.util.ComparableOp;
import forge.util.PredicateString.StringOp;

import java.util.function.Predicate;

public class AdvancedSearchParser {

    public static Predicate<CardRules> parseAdvancedToken(String token) {
        boolean negated = false;
        if (token.startsWith("-")) {
            token = token.substring(1).trim();
            negated = true;
        }

        String[] operators = {"!=", "<=", ">=", "=", "<", ">", ":", "!"};
        int index = -1;
        String opUsed = null;
        for (String op : operators) {
            int idx = token.indexOf(op);
            if (idx >= 0) {
                index = idx;
                opUsed = op;
                break;
            }
        }
        if (index < 0 || opUsed == null) {
            return null;
        }

        String key = token.substring(0, index).trim().toLowerCase();
        String valueStr = token.substring(index + opUsed.length()).trim();

        try {
            Predicate<CardRules> predicate = null;
            switch (key) {
                case "cmc":
                case "mv":
                case "manavalue":
                    int cmcValue = Integer.parseInt(valueStr);
                    ComparableOp op = null;
                    switch (opUsed) {
                        case "!":
                        case ":":
                        case "=":  op = ComparableOp.EQUALS; break;
                        case "!=": op = ComparableOp.NOT_EQUALS; break;
                        case ">=": op = ComparableOp.GT_OR_EQUAL; break;
                        case ">":  op = ComparableOp.GREATER_THAN; break;
                        case "<=": op = ComparableOp.LT_OR_EQUAL; break;
                        case "<":  op = ComparableOp.LESS_THAN; break;
                    }
                    if (op != null) {
                        predicate = CardRulesPredicates.cmc(op, cmcValue);
                    }
                    break;
                case "t":
                case "type":
                    switch (opUsed) {
                        case ":":
                        case "=":  predicate = CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, valueStr); break;
                        case "!=": predicate = CardRulesPredicates.joinedType(StringOp.CONTAINS_IC, valueStr).negate(); break;
                    }
                    break;
                case "kw":
                case "keyword":
                    switch (opUsed) {
                        case ":":
                        case "=":  predicate = CardRulesPredicates.hasKeyword(valueStr); break;
                        case "!=": predicate = CardRulesPredicates.hasKeyword(valueStr).negate(); break;
                    }
                    break;
                case "c":
                case "color":
                    if (valueStr.equals("c")) {
                        switch (opUsed) {
                            case ":":
                            case "=": predicate = card -> ColorSet.fromMask(card.getColor().getColor()).isColorless(); break;
                            case "!=":
                            case ">": predicate = card -> !ColorSet.fromMask(card.getColor().getColor()).isColorless(); break;
                        }
                    } else if (valueStr.equals("m")) {
                        switch (opUsed) {
                            case ":":
                            case "=": predicate = card -> ColorSet.fromMask(card.getColor().getColor()).countColors() >= 2; break;
                            case "!=":
                            case "<": predicate = card -> ColorSet.fromMask(card.getColor().getColor()).countColors() == 1; break;
                        }
                    } else {
                        byte mask = 0;
                        for (char c : valueStr.toCharArray()) {
                            byte color = MagicColor.fromName(c);
                            if (color == 0) {
                                continue;
                            }
                            mask |= color;
                        }
                        final byte finalMask = mask;

                        switch (opUsed) {
                            case ":": predicate = card -> ColorSet.fromMask(card.getColor().getColor()).hasAllColors(finalMask); break;
                            case "=": 
                            case "!": predicate = card -> ColorSet.fromMask(card.getColor().getColor()).hasExactlyColor(finalMask); break;
                        }
                    }
                    break;
            }
            if (predicate != null && negated) {
                return predicate.negate();
            }
            return predicate;
        } catch (NumberFormatException ignored) {
            // Ignore and return null for invalid number formats
        }
        return null;
    }
}
