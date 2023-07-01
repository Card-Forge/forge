/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  MaxMtg
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.util;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;

/**
 * Special predicate class to perform string operations.
 * 
 * @param <T>
 *            the generic type
 */
public abstract class PredicateString<T> implements Predicate<T> {
    /** Possible operators for string operands. */
    public enum StringOp {
        /** The CONTAINS. */
        CONTAINS,
        /** The CONTAINS ignore case. */
        CONTAINS_IC,
        /** The EQUALS. */
        EQUALS,
        /** The EQUALS. */
        EQUALS_IC
    }

    /** The operator. */
    private final StringOp operator;

    /**
     * Op.
     * 
     * @param op1
     *            the op1
     * @param op2
     *            the op2
     * @return true, if successful
     */
    protected final boolean op(final String op1, final String op2) {
        switch (this.getOperator()) {
        case CONTAINS_IC:
            return StringUtils.containsIgnoreCase(op1, op2);
        case CONTAINS:
            return StringUtils.contains(op1, op2);
        case EQUALS:
            return op1.equals(op2);
        case EQUALS_IC:
            return op1.equalsIgnoreCase(op2);
        default:
            return false;
        }
    }

    /**
     * Instantiates a new predicate string.
     * 
     * @param operator
     *            the operator
     */
    public PredicateString(final StringOp operator) {
        this.operator = operator;
    }

    /**
     * @return the operator
     */
    public StringOp getOperator() {
        return operator;
    }

    public static PredicateString<String> contains(final String what) {
        return new PredicateString<String>(StringOp.CONTAINS) {
            @Override
            public boolean apply(String subject) {
                return op(subject, what);
            }
        };
    }
    public static PredicateString<String> containsIgnoreCase(final String what) {
        return new PredicateString<String>(StringOp.CONTAINS_IC) {
            @Override
            public boolean apply(String subject) {
                return op(subject, what);
            }
        };
    }
    public static PredicateString<String> equals(final String what) {
        return new PredicateString<String>(StringOp.EQUALS) {
            @Override
            public boolean apply(String subject) {
                return op(subject, what);
            }
        };
    }

}
