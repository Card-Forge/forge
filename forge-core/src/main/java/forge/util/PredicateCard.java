/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2020 Jamin W. Collins
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

import com.google.common.base.Predicate;
import forge.item.PaperCard;

/**
 * Special predicate class to perform string operations.
 * 
 * @param <T>
 *            the generic type
 */
public abstract class PredicateCard<T> implements Predicate<T> {
    /** Possible operators for string operands. */
    public enum StringOp {
        /** The EQUALS. */
        EQUALS,
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
    protected final boolean op(final PaperCard op1, final PaperCard op2) {
        switch (this.getOperator()) {
        case EQUALS:
            return op1.equals(op2);
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
    public PredicateCard(final StringOp operator) {
        this.operator = operator;
    }

    /**
     * @return the operator
     */
    public StringOp getOperator() {
        return operator;
    }

    public static PredicateCard<PaperCard> equals(final PaperCard what) {
        return new PredicateCard<PaperCard>(StringOp.EQUALS) {
            @Override
            public boolean apply(PaperCard subject) {
                return op(subject, what);
            }
        };
    }

}
