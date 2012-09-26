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

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Function;

/**
 * Predicate class allows to select items or type <U>, which are or contain an
 * object of type <T>, matching to some criteria set by predicate. No need to
 * write that simple operation by hand.
 * 
 * Implements com.google.common.base.Predicates, so you may use this in Guava collection management routines.
 * 
 * @param <T>
 *            - class to check condition against
 * @author Max
 */

public abstract class Predicate<T> implements com.google.common.base.Predicate<T>{

    /**
     * Possible operators on two predicates.
     * 
     * @author Max
     */
    public enum PredicatesOp {

        /** The AND. */
        AND,
        /** The OR. */
        OR,
        /** The XOR. */
        XOR,
        /** The EQ. */
        EQ,
        /** The NOR. */
        NOR,
        /** The NAND. */
        NAND,
        /** */
        GT,
        /** */
        LT
    }

    /**
     * This is the main method, predicates were made for.
     * 
     * @param subject
     *            the subject
     * @return true, if is true
     */
    public abstract boolean apply(T subject);

    // Overloaded only in LeafConstant
    /**
     * These are checks against constants, they will let simpler
     * expressions be built.
     * 
     * @return true, if is 1
     */
    public boolean is1() {
        return false;
    }

    /**
     * Checks if is 0.
     * 
     * @return true, if is 0
     */
    public boolean is0() {
        return false;
    }

    // 1. operations on pure T ... check(T card), list.add(card)
    // 2. operations on something U containing CardOracles ...
    // check(accessor(U)), list.add(U)
    // 3. gets T from U, saves U transformed into v ... check(accessor(U)),
    // list.add(transformer(U))

    // selects are fun
    public final <U> List<U> select(final Iterable<U> source, final Lambda1<T, U> accessor) {
        final ArrayList<U> result = new ArrayList<U>();
        if (source != null) {
            for (final U c : source) {
                if (this.apply(accessor.apply(c))) {
                    result.add(c);
                }
            }
        }
        return result;
    }




    // Static builder methods - they choose concrete implementation by
    // themselves
    /**
     * Brigde (transforms a predicate of type T into a predicate
     * of type U, using a bridge function passed as an argument).
     * 
     * @param <U>
     *            the generic type
     * @param <T>
     *            the generic type
     * @param predicate
     *            the predicate
     * @param fnBridge
     *            the fn bridge
     * @return the predicate
     */
    public static final <U, T> Predicate<U> brigde(final com.google.common.base.Predicate<T> predicate, final Function<U, T> fnBridge) {
        return new Bridge<T, U>(predicate, fnBridge);
    }

    public final <U> Predicate<U> brigde(final Function<U, T> fnBridge) {
        return new Bridge<T, U>(this, fnBridge);
    }
    


    /**
     * Compose.
     * 
     * @param <T>
     *            the generic type
     * @param operand1
     *            the operand1
     * @param operator
     *            the operator
     * @param operand2
     *            the operand2
     * @return the predicate
     */
    public static <T> Predicate<T> compose(final Predicate<T> operand1, final PredicatesOp operator,
            final Predicate<T> operand2) {
        return new Node<T>(operand1, operator, operand2);
    }

    // Predefined operators: and, or
    /**
     * And.
     * 
     * @param <T>
     *            the generic type
     * @param operand1
     *            the operand1
     * @param operand2
     *            the operand2
     * @return the predicate
     */
    public static <T> Predicate<T> and(final Predicate<T> operand1, final Predicate<T> operand2) {
        if (operand1.is1()) {
            return operand2;
        }
        if ((operand2 == null) || operand2.is1()) {
            return operand1;
        }
        return new NodeAnd<T>(operand1, operand2);
    }

    /**
     * And.
     * 
     * @param <T>
     *            the generic type
     * @param operand
     *            the operand
     * @return the predicate
     */
    public static <T> Predicate<T> and(final Iterable<Predicate<T>> operand) {
        return new MultiNodeAnd<T>(operand);
    }

    /**
     * And.
     * 
     * @param <T>
     *            the generic type
     * @param <U>
     *            the generic type
     * @param operand1
     *            the operand1
     * @param operand2
     *            the operand2
     * @param bridge
     *            the bridge
     * @return the predicate
     */
    public static <T, U> Predicate<T> and(final Predicate<T> operand1, final Predicate<U> operand2,
            final Lambda1<U, T> fnBridge) {
        return new NodeAnd<T>(operand1, operand2.brigde(fnBridge));
    }

    /**
     * Or.
     * 
     * @param <T>
     *            the generic type
     * @param operand1
     *            the operand1
     * @param operand2
     *            the operand2
     * @return the predicate
     */
    public static <T> Predicate<T> or(final Predicate<T> operand1, final Predicate<T> operand2) {
        return new NodeOr<T>(operand1, operand2);
    }

    /**
     * Or.
     * 
     * @param <T>
     *            the generic type
     * @param operand
     *            the operand
     * @return the predicate
     */
    public static <T> Predicate<T> or(final Iterable<Predicate<T>> operand) {
        return new MultiNodeOr<T>(operand);
    }

    /**
     * Or.
     * 
     * @param <T>
     *            the generic type
     * @param <U>
     *            the generic type
     * @param operand1
     *            the operand1
     * @param operand2
     *            the operand2
     * @param bridge
     *            the bridge
     * @return the predicate
     */
    public static <T, U> Predicate<T> or(final Predicate<T> operand1, final Predicate<U> operand2,
            final Lambda1<U, T> bridge) {
        return new NodeOrBridged<T, U>(operand1, operand2, bridge);
    }

    /**
     * Not.
     * 
     * @param <T>
     *            the generic type
     * @param operand1
     *            the operand1
     * @return the predicate
     */
    public static <T> Predicate<T> not(final Predicate<T> operand1) {
        return new Not<T>(operand1);
    }

    /**
     * Gets the true.
     * 
     * @param <T>
     *            the generic type
     * @param cls
     *            the cls
     * @return the true
     */
    public static <T> Predicate<T> getTrue(final Class<T> cls) {
        return new LeafConstant<T>(true);
    }

    /**
     * Gets the false.
     * 
     * @param <T>
     *            the generic type
     * @param cls
     *            the cls
     * @return the false
     */
    public static <T> Predicate<T> getFalse(final Class<T> cls) {
        return new LeafConstant<T>(false);
    }
}

// Concrete implementations
// unary operators
/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
final class Not<T> extends Predicate<T> {
    /**
     * 
     */
    private final Predicate<T> filter;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand Predicate<T>
     */
    public Not(final Predicate<T> operand) {
        this.filter = operand;
    }

    @Override
    public boolean apply(final T card) {
        return !this.filter.apply(card);
    }
}

/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 * @param <U>
 */
final class Bridge<T, U> extends Predicate<U> {
    /**
     * 
     */
    private final com.google.common.base.Predicate<T> filter;
    /**
     * 
     */
    private final Function<U, T> fnBridge;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand Predicate<T>
     * @param fnTfromU Lambda1<T, U>
     */
    public Bridge(final com.google.common.base.Predicate<T> operand, final Function<U, T> fnTfromU) {
        this.filter = operand;
        this.fnBridge = fnTfromU;
    }

    @Override
    public boolean apply(final U card) {
        return this.filter.apply(this.fnBridge.apply(card));
    }
}


// binary operators
/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
/* PERFORMANCE CRITICAL - DO NOT GENERATE GETTERS HERE*/ 
class Node<T> extends Predicate<T> {
    /* PERFORMANCE CRITICAL - DO NOT GENERATE GETTERS HERE*/ 
    private final PredicatesOp operator;
    protected final Predicate<T> filter1;
    protected final Predicate<T> filter2;
    /* PERFORMANCE CRITICAL - DO NOT GENERATE GETTERS HERE*/ 
    
    /**
     * 
     * Node.
     * 
     * @param operand1 Predicate<T>
     * @param op PredicatesOp
     * @param operand2 Predicate<T>
     */
    public Node(final Predicate<T> operand1, final PredicatesOp op, final Predicate<T> operand2) {
        this.operator = op;
        this.filter1 = operand1;
        this.filter2 = operand2;
    }

    @Override
    public boolean apply(final T card) {
        switch (this.operator) {
        case AND:
            return this.filter1.apply(card) && this.filter2.apply(card);
        case GT:
            return this.filter1.apply(card) && !this.filter2.apply(card);
        case LT:
            return !this.filter1.apply(card) && this.filter2.apply(card);
        case NAND:
            return !(this.filter1.apply(card) && this.filter2.apply(card));
        case OR:
            return this.filter1.apply(card) || this.filter2.apply(card);
        case NOR:
            return !(this.filter1.apply(card) || this.filter2.apply(card));
        case XOR:
            return this.filter1.apply(card) ^ this.filter2.apply(card);
        case EQ:
            return this.filter1.apply(card) == this.filter2.apply(card);
        default:
            return false;
        }
    }


}

/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
final class NodeOr<T> extends Node<T> {
    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand1 Predicate<T>
     * @param operand2 Predicate<T>
     */
    public NodeOr(final Predicate<T> operand1, final Predicate<T> operand2) {
        super(operand1, PredicatesOp.OR, operand2);
    }

    @Override
    public boolean apply(final T card) {
        return this.filter1.apply(card) || this.filter2.apply(card);
    }
}

/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
final class NodeAnd<T> extends Node<T> {
    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand1 Predicate<T>
     * @param operand2 Predicate<T>
     */
    public NodeAnd(final Predicate<T> operand1, final Predicate<T> operand2) {
        super(operand1, PredicatesOp.AND, operand2);
    }

    @Override
    public boolean apply(final T card) {
        return this.filter1.apply(card) && this.filter2.apply(card);
    }
}

// Bridged OR and AND
/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 * @param <U>
 */
final class NodeOrBridged<T, U> extends Predicate<T> {
    private final Predicate<T> filter1;
    private final Predicate<U> filter2;
    private final Lambda1<U, T> bridge;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand1 Predicate
     * @param operand2 Predicate
     * @param accessor Lambda
     */
    public NodeOrBridged(final Predicate<T> operand1, final Predicate<U> operand2, final Lambda1<U, T> accessor) {
        this.filter1 = operand1;
        this.filter2 = operand2;
        this.bridge = accessor;
    }

    @Override
    public boolean apply(final T card) {
        return this.filter1.apply(card) || this.filter2.apply(this.bridge.apply(card));
    }
}

/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 * @param <U>
 */
final class NodeAndBridged<T, U> extends Predicate<T> {
    private final Predicate<T> filter1;
    private final Predicate<U> filter2;
    private final Lambda1<U, T> bridge;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand1 Predicate
     * @param operand2 Predicate
     * @param accessor Lambda
     */
    public NodeAndBridged(final Predicate<T> operand1, final Predicate<U> operand2, final Lambda1<U, T> accessor) {
        this.filter1 = operand1;
        this.filter2 = operand2;
        this.bridge = accessor;
    }

    @Override
    public boolean apply(final T card) {
        return this.filter1.apply(card) && this.filter2.apply(this.bridge.apply(card));
    }
}

// multi-operand operators
/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
abstract class MultiNode<T> extends Predicate<T> {
    /**
     * 
     */
    private final Iterable<Predicate<T>> operands;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param filters Iterable<Predicate<T>>
     */
    public MultiNode(final Iterable<Predicate<T>> filters) {
        this.operands = filters;
    }

    /**
     * @return the operands
     */
    public Iterable<Predicate<T>> getOperands() {
        return operands;
    }
}

/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
final class MultiNodeAnd<T> extends MultiNode<T> {
    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param filters Iterable<Predicate<T>>
     */
    public MultiNodeAnd(final Iterable<Predicate<T>> filters) {
        super(filters);
    }

    @Override
    public boolean apply(final T subject) {
        for (final Predicate<T> p : this.getOperands()) {
            if (!p.apply(subject)) {
                return false;
            }
        }
        return true;
    }
}

/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
final class MultiNodeOr<T> extends MultiNode<T> {
    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param filters Iterable<Predicate<T>>
     */
    public MultiNodeOr(final Iterable<Predicate<T>> filters) {
        super(filters);
    }

    @Override
    public boolean apply(final T subject) {
        for (final Predicate<T> p : this.getOperands()) {
            if (p.apply(subject)) {
                return true;
            }
        }
        return false;
    }
}



/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
class LeafConstant<T> extends Predicate<T> {
    private final boolean bValue;

    @Override
    public boolean is1() {
        return this.bValue;
    }

    @Override
    public boolean is0() {
        return !this.bValue;
    }

    @Override
    public boolean apply(final T card) {
        return this.bValue;
    }

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param value boolean
     */
    public LeafConstant(final boolean value) {
        this.bValue = value;
    }
}
