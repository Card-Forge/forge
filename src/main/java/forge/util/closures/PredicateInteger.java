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
package forge.util.closures;

/**
 * Special predicate class to perform integer operations.
 * 
 * @param <T>
 *            the generic type
 */
public abstract class PredicateInteger<T> extends Predicate<T> {

    /** The operator. */
    private final ComparableOp operator;

    /**
     * Op.
     * 
     * @param op1
     *            the op1
     * @param op2
     *            the op2
     * @return true, if successful
     */
    protected final boolean op(final int op1, final int op2) {
        switch (this.getOperator()) {
        case GREATER_THAN:
            return op1 > op2;
        case LESS_THAN:
            return op1 < op2;
        case GT_OR_EQUAL:
            return op1 >= op2;
        case LT_OR_EQUAL:
            return op1 <= op2;
        case EQUALS:
            return op1 == op2;
        case NOT_EQUALS:
            return op1 != op2;
        default:
            return false;
        }
    }

    /**
     * Instantiates a new integer predicate.
     * 
     * @param operator
     *            the operator
     */
    public PredicateInteger(final ComparableOp operator) {
        this.operator = operator;
    }

    /**
     * @return the operator
     */
    public ComparableOp getOperator() {
        return operator;
    }
}
