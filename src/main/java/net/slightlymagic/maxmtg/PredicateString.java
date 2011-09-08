package net.slightlymagic.maxmtg;

import org.apache.commons.lang3.StringUtils;

/** 
 * Special predicate class to perform string operations.
 */
public abstract class PredicateString<T> extends Predicate<T> {
    /** Possible operators for string operands. */
    public enum StringOp { CONTAINS, NOT_CONTAINS, EQUALS, NOT_EQUALS }
    
    protected final StringOp operator;
    protected final boolean op(final String op1, final String op2) {
        switch (operator) {
            case CONTAINS: return StringUtils.containsIgnoreCase(op1, op2);
            case NOT_CONTAINS: return !StringUtils.containsIgnoreCase(op1, op2);
            case EQUALS: return op1.equalsIgnoreCase(op2);
            case NOT_EQUALS: return op1.equalsIgnoreCase(op2);
            default: return false;
        }
    }

    public PredicateString(final StringOp operator) {this.operator = operator; }
}    