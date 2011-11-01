package net.slightlymagic.maxmtg;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.slightlymagic.braids.util.lambda.Lambda1;

/**
 * Predicate class allows to select items or type <U>, which are or contain an
 * object of type <T>, matching to some criteria set by predicate. No need to
 * write that simple operations by hand.
 * 
 * PS: com.google.common.base.Predicates contains almost the same functionality,
 * except for they keep filtering, transformations aside from the predicate in
 * class Iterables
 * 
 * @param <T>
 *            - class to check condition against
 * @author Max
 */

public abstract class Predicate<T> {

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
        NAND
    }

    /**
     * Possible operators for comparables.
     * 
     * @author Max
     * 
     */
    public enum ComparableOp {

        /** The EQUALS. */
        EQUALS,
        /** The NO t_ equals. */
        NOT_EQUALS,
        /** The GREATE r_ than. */
        GREATER_THAN,
        /** The LES s_ than. */
        LESS_THAN,
        /** The G t_ o r_ equal. */
        GT_OR_EQUAL,
        /** The L t_ o r_ equal. */
        LT_OR_EQUAL
    }

    // This is the main method, predicates were made for.
    /**
     * Checks if is true.
     * 
     * @param subject
     *            the subject
     * @return true, if is true
     */
    public abstract boolean isTrue(T subject);

    // These are checks against constants, they will let build simpler
    // expressions
    // Overloaded only in LeafConstant
    /**
     * Checks if is 1.
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
    /**
     * Select.
     * 
     * @param source
     *            the source
     * @return the list
     */
    public final List<T> select(final Iterable<T> source) {
        final ArrayList<T> result = new ArrayList<T>();
        if (source != null) {
            for (final T c : source) {
                if (this.isTrue(c)) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    /**
     * Select.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @return the list
     */
    public final <U> List<U> select(final Iterable<U> source, final Lambda1<T, U> accessor) {
        final ArrayList<U> result = new ArrayList<U>();
        if (source != null) {
            for (final U c : source) {
                if (this.isTrue(accessor.apply(c))) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    /**
     * Select.
     * 
     * @param <U>
     *            the generic type
     * @param <V>
     *            the value type
     * @param source
     *            the source
     * @param cardAccessor
     *            the card accessor
     * @param transformer
     *            the transformer
     * @return the list
     */
    public final <U, V> List<V> select(final Iterable<U> source, final Lambda1<T, U> cardAccessor,
            final Lambda1<V, U> transformer) {
        final ArrayList<V> result = new ArrayList<V>();
        if (source != null) {
            for (final U c : source) {
                if (this.isTrue(cardAccessor.apply(c))) {
                    result.add(transformer.apply(c));
                }
            }
        }
        return result;
    }

    // Check if any element meeting the criteria is present
    /**
     * Any.
     * 
     * @param source
     *            the source
     * @return true, if successful
     */
    public final boolean any(final Iterable<T> source) {
        if (source != null) {
            for (final T c : source) {
                if (this.isTrue(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Any.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @return true, if successful
     */
    public final <U> boolean any(final Iterable<U> source, final Lambda1<T, U> accessor) {
        if (source != null) {
            for (final U c : source) {
                if (this.isTrue(accessor.apply(c))) {
                    return true;
                }
            }
        }
        return false;
    }

    // select top 1
    /**
     * First.
     * 
     * @param source
     *            the source
     * @return the t
     */
    public final T first(final Iterable<T> source) {
        if (source != null) {
            for (final T c : source) {
                if (this.isTrue(c)) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * First.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @return the u
     */
    public final <U> U first(final Iterable<U> source, final Lambda1<T, U> accessor) {
        if (source != null) {
            for (final U c : source) {
                if (this.isTrue(accessor.apply(c))) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * First.
     * 
     * @param <U>
     *            the generic type
     * @param <V>
     *            the value type
     * @param source
     *            the source
     * @param cardAccessor
     *            the card accessor
     * @param transformer
     *            the transformer
     * @return the v
     */
    public final <U, V> V first(final Iterable<U> source, final Lambda1<T, U> cardAccessor,
            final Lambda1<V, U> transformer) {
        if (source != null) {
            for (final U c : source) {
                if (this.isTrue(cardAccessor.apply(c))) {
                    return transformer.apply(c);
                }
            }
        }
        return null;
    }

    // splits are even more fun
    /**
     * Split.
     * 
     * @param source
     *            the source
     * @param trueList
     *            the true list
     * @param falseList
     *            the false list
     */
    public final void split(final Iterable<T> source, final List<T> trueList, final List<T> falseList) {
        if (source == null) {
            return;
        }
        for (final T c : source) {
            if (this.isTrue(c)) {
                trueList.add(c);
            } else {
                falseList.add(c);
            }
        }
    }

    /**
     * Split.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @param trueList
     *            the true list
     * @param falseList
     *            the false list
     */
    public final <U> void split(final Iterable<U> source, final Lambda1<T, U> accessor, final List<U> trueList,
            final List<U> falseList) {
        if (source == null) {
            return;
        }
        for (final U c : source) {
            if (this.isTrue(accessor.apply(c))) {
                trueList.add(c);
            } else {
                falseList.add(c);
            }
        }
    }

    /**
     * Split.
     * 
     * @param <U>
     *            the generic type
     * @param <V>
     *            the value type
     * @param source
     *            the source
     * @param cardAccessor
     *            the card accessor
     * @param transformer
     *            the transformer
     * @param trueList
     *            the true list
     * @param falseList
     *            the false list
     */
    public final <U, V> void split(final Iterable<U> source, final Lambda1<T, U> cardAccessor,
            final Lambda1<V, U> transformer, final List<V> trueList, final List<V> falseList) {
        if (source == null) {
            return;
        }
        for (final U c : source) {
            if (this.isTrue(cardAccessor.apply(c))) {
                trueList.add(transformer.apply(c));
            } else {
                falseList.add(transformer.apply(c));
            }
        }
    }

    // Unique
    /**
     * Unique by last.
     * 
     * @param <K>
     *            the key type
     * @param source
     *            the source
     * @param fnUniqueKey
     *            the fn unique key
     * @return the iterable
     */
    public final <K> Iterable<T> uniqueByLast(final Iterable<T> source, final Lambda1<K, T> fnUniqueKey) {
        final Map<K, T> uniques = new Hashtable<K, T>();
        for (final T c : source) {
            if (this.isTrue(c)) {
                uniques.put(fnUniqueKey.apply(c), c);
            }
        }
        return uniques.values();
    }

    /**
     * Unique by last.
     * 
     * @param <K>
     *            the key type
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param fnUniqueKey
     *            the fn unique key
     * @param accessor
     *            the accessor
     * @return the iterable
     */
    public final <K, U> Iterable<U> uniqueByLast(final Iterable<U> source, final Lambda1<K, U> fnUniqueKey,
            final Lambda1<T, U> accessor) { // this might be exotic
        final Map<K, U> uniques = new Hashtable<K, U>();
        for (final U c : source) {
            if (this.isTrue(accessor.apply(c))) {
                uniques.put(fnUniqueKey.apply(c), c);
            }
        }
        return uniques.values();
    }

    /**
     * Unique by first.
     * 
     * @param <K>
     *            the key type
     * @param source
     *            the source
     * @param fnUniqueKey
     *            the fn unique key
     * @return the iterable
     */
    public final <K> Iterable<T> uniqueByFirst(final Iterable<T> source, final Lambda1<K, T> fnUniqueKey) {
        final Map<K, T> uniques = new Hashtable<K, T>();
        for (final T c : source) {
            final K key = fnUniqueKey.apply(c);
            if (this.isTrue(c) && !uniques.containsKey(key)) {
                uniques.put(fnUniqueKey.apply(c), c);
            }
        }
        return uniques.values();
    }

    /**
     * Unique by first.
     * 
     * @param <K>
     *            the key type
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param fnUniqueKey
     *            the fn unique key
     * @param accessor
     *            the accessor
     * @return the iterable
     */
    public final <K, U> Iterable<U> uniqueByFirst(final Iterable<U> source, final Lambda1<K, U> fnUniqueKey,
            final Lambda1<T, U> accessor) { // this might be exotic
        final Map<K, U> uniques = new Hashtable<K, U>();
        for (final U c : source) {
            final K key = fnUniqueKey.apply(c);
            if (this.isTrue(accessor.apply(c)) && !uniques.containsKey(key)) {
                uniques.put(fnUniqueKey.apply(c), c);
            }
        }
        return uniques.values();
    }

    // Count
    /**
     * Count.
     * 
     * @param source
     *            the source
     * @return the int
     */
    public final int count(final Iterable<T> source) {
        int result = 0;
        if (source != null) {
            for (final T c : source) {
                if (this.isTrue(c)) {
                    result++;
                }
            }
        }
        return result;
    }

    /**
     * Count.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @return the int
     */
    public final <U> int count(final Iterable<U> source, final Lambda1<T, U> accessor) {
        int result = 0;
        if (source != null) {
            for (final U c : source) {
                if (this.isTrue(accessor.apply(c))) {
                    result++;
                }
            }
        }
        return result;
    }

    // Aggregates?
    /**
     * Aggregate.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @param valueAccessor
     *            the value accessor
     * @return the int
     */
    public final <U> int aggregate(final Iterable<U> source, final Lambda1<T, U> accessor,
            final Lambda1<Integer, U> valueAccessor) {
        int result = 0;
        if (source != null) {
            for (final U c : source) {
                if (this.isTrue(accessor.apply(c))) {
                    result += valueAccessor.apply(c);
                }
            }
        }
        return result;
    }

    // Random - algorithm adapted from Braid's GeneratorFunctions
    /**
     * Random.
     * 
     * @param source
     *            the source
     * @return the t
     */
    public final T random(final Iterable<T> source) {
        int n = 0;
        T candidate = null;
        for (final T item : source) {
            if (!this.isTrue(item)) {
                continue;
            }
            if ((Math.random() * ++n) < 1) {
                candidate = item;
            }
        }
        return candidate;
    }

    /**
     * Random.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @return the u
     */
    public final <U> U random(final Iterable<U> source, final Lambda1<T, U> accessor) {
        int n = 0;
        U candidate = null;
        for (final U item : source) {
            if (!this.isTrue(accessor.apply(item))) {
                continue;
            }
            if ((Math.random() * ++n) < 1) {
                candidate = item;
            }
        }
        return candidate;
    }

    // Get several random values
    // should improve to make 1 pass over source and track N candidates at once
    /**
     * Random.
     * 
     * @param source
     *            the source
     * @param count
     *            the count
     * @return the list
     */
    public final List<T> random(final Iterable<T> source, final int count) {
        final List<T> result = new ArrayList<T>();
        for (int i = 0; i < count; ++i) {
            final T toAdd = this.random(source);
            if (toAdd == null) {
                break;
            }
            result.add(toAdd);
        }
        return result;
    }

    /**
     * Random.
     * 
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param accessor
     *            the accessor
     * @param count
     *            the count
     * @return the list
     */
    public final <U> List<U> random(final Iterable<U> source, final Lambda1<T, U> accessor, final int count) {
        final List<U> result = new ArrayList<U>();
        for (int i = 0; i < count; ++i) {
            final U toAdd = this.random(source, accessor);
            if (toAdd == null) {
                break;
            }
            result.add(toAdd);
        }
        return result;
    }

    /**
     * Random.
     * 
     * @param <V>
     *            the value type
     * @param source
     *            the source
     * @param count
     *            the count
     * @param transformer
     *            the transformer
     * @return the list
     */
    public final <V> List<V> random(final Iterable<T> source, final int count, final Lambda1<V, T> transformer) {
        final List<V> result = new ArrayList<V>();
        for (int i = 0; i < count; ++i) {
            final T toAdd = this.random(source);
            if (toAdd == null) {
                break;
            }
            result.add(transformer.apply(toAdd));
        }
        return result;
    }

    // Static builder methods - they choose concrete implementation by
    // themselves
    /**
     * Brigde.
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
    public static <U, T> Predicate<U> brigde(final Predicate<T> predicate, final Lambda1<T, U> fnBridge) {
        return new Bridge<T, U>(predicate, fnBridge);
    }

    /**
     * Instance of.
     * 
     * @param <U>
     *            the generic type
     * @param <T>
     *            the generic type
     * @param predicate
     *            the predicate
     * @param clsTarget
     *            the cls target
     * @return the predicate
     */
    public static <U, T> Predicate<U> instanceOf(final Predicate<T> predicate, final Class<T> clsTarget) {
        return new BridgeToInstance<T, U>(predicate, clsTarget);
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
            final Lambda1<U, T> bridge) {
        return new NodeAndBridged<T, U>(operand1, operand2, bridge);
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
    public boolean isTrue(final T card) {
        return !this.filter.isTrue(card);
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
    private final Predicate<T> filter;
    /**
     * 
     */
    private final Lambda1<T, U> fnBridge;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand Predicate<T>
     * @param fnTfromU Lambda1<T, U>
     */
    public Bridge(final Predicate<T> operand, final Lambda1<T, U> fnTfromU) {
        this.filter = operand;
        this.fnBridge = fnTfromU;
    }

    @Override
    public boolean isTrue(final U card) {
        return this.filter.isTrue(this.fnBridge.apply(card));
    }

    @Override
    public boolean is1() {
        return this.filter.is1();
    }
}

/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 * @param <U>
 */
final class BridgeToInstance<T, U> extends Predicate<U> {
    /**
     * 
     */
    private final Predicate<T> filter;

    /**
     * 
     */
    private final Class<T> clsBridge;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * 
     * @param operand Predicate
     * @param clsT Class
     */
    public BridgeToInstance(final Predicate<T> operand, final Class<T> clsT) {
        this.filter = operand;
        this.clsBridge = clsT;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isTrue(final U card) {
        return this.clsBridge.isInstance(card) && this.filter.isTrue((T) card);
    }

    @Override
    public boolean is1() {
        return this.filter.is1();
    }
}

// binary operators
/**
 * 
 * TODO: Write javadoc for this type.
 * 
 * @param <T>
 */
class Node<T> extends Predicate<T> {
    private final PredicatesOp operator;
    /**
     * 
     */
    private final Predicate<T> filter1;
    /**
     * 
     */
    private final Predicate<T> filter2;

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
    public boolean isTrue(final T card) {
        switch (this.operator) {
        case AND:
            return this.getFilter1().isTrue(card) && this.getFilter2().isTrue(card);
        case NAND:
            return !(this.getFilter1().isTrue(card) && this.getFilter2().isTrue(card));
        case OR:
            return this.getFilter1().isTrue(card) || this.getFilter2().isTrue(card);
        case NOR:
            return !(this.getFilter1().isTrue(card) || this.getFilter2().isTrue(card));
        case XOR:
            return this.getFilter1().isTrue(card) ^ this.getFilter2().isTrue(card);
        case EQ:
            return this.getFilter1().isTrue(card) == this.getFilter2().isTrue(card);
        default:
            return false;
        }
    }

    /**
     * @return the filter1
     */
    public Predicate<T> getFilter1() {
        return filter1;
    }

    /**
     * @return the filter2
     */
    public Predicate<T> getFilter2() {
        return filter2;
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
    public boolean isTrue(final T card) {
        return this.getFilter1().isTrue(card) || this.getFilter2().isTrue(card);
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
    public boolean isTrue(final T card) {
        return this.getFilter1().isTrue(card) && this.getFilter2().isTrue(card);
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
    public boolean isTrue(final T card) {
        return this.filter1.isTrue(card) || this.filter2.isTrue(this.bridge.apply(card));
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
    public boolean isTrue(final T card) {
        return this.filter1.isTrue(card) && this.filter2.isTrue(this.bridge.apply(card));
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
    public boolean isTrue(final T subject) {
        for (final Predicate<T> p : this.getOperands()) {
            if (!p.isTrue(subject)) {
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
    public boolean isTrue(final T subject) {
        for (final Predicate<T> p : this.getOperands()) {
            if (p.isTrue(subject)) {
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
    public boolean isTrue(final T card) {
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
