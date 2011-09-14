package net.slightlymagic.maxmtg;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.slightlymagic.braids.util.lambda.Lambda1;
/**
 * Predicate class allows to select items or type <U>, which are or contain an object of type <T>,
 * matching to some criteria set by predicate. No need to write that simple operations by hand.
 * 
 * PS: com.google.common.base.Predicates contains almost the same functionality, except for they keep filtering, 
 * transformations aside from the predicate in class Iterables
 * 
 * @author Max
 *
 * @param <T> - class to check condition against
 */

public abstract class Predicate<T> {
    /**
     * Possible operators on two predicates
     * @author Max
     *
     */
    public enum PredicatesOp { AND, OR, XOR, EQ, NOR, NAND }

    /**
     * Possible operators for comparables.
     * @author Max
     *
     */
    public enum ComparableOp { EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GT_OR_EQUAL, LT_OR_EQUAL }


    // This is the main method, predicates were made for.
    public abstract boolean isTrue(T subject);
    // These are checks against constants, they will let build simpler expressions
    // Overloaded only in LeafConstant 
    public boolean is1() { return false; }
    public boolean is0() { return false; }

    // 1. operations on pure T ... check(T card), list.add(card)
    // 2. operations on something U containing CardOracles ... check(accessor(U)), list.add(U)
    // 3. gets T from U, saves U transformed into v ... check(accessor(U)), list.add(transformer(U))

    // selects are fun
    public final List<T> select(final Iterable<T> source) {
        ArrayList<T> result = new ArrayList<T>();
        if (source != null) { for (T c : source) { if (isTrue(c)) { result.add(c); } } }
        return result;
    }
    public final <U> List<U> select(final Iterable<U> source, final Lambda1<T, U> accessor) {
        ArrayList<U> result = new ArrayList<U>();
        if (source != null) { for (U c : source) { if (isTrue(accessor.apply(c))) { result.add(c); } } }
        return result;
    }
    public final <U, V> List<V> select(final Iterable<U> source, final Lambda1<T, U> cardAccessor,
            final Lambda1<V, U> transformer)
    {
        ArrayList<V> result = new ArrayList<V>();
        if (source != null) { for (U c : source) { if (isTrue(cardAccessor.apply(c))) { result.add(transformer.apply(c)); } } }
        return result;
    }

    // select top 1
    public final T first(final Iterable<T> source) {
        if (source != null) { for (T c : source) { if (isTrue(c)) { return c; } } }
        return null;
    }
    public final <U> U first(final Iterable<U> source, final Lambda1<T, U> accessor) {
        if (source != null) { for (U c : source) { if (isTrue(accessor.apply(c))) { return c; } } }
        return null;
    }
    public final <U, V> V first(final Iterable<U> source, final Lambda1<T, U> cardAccessor,
            final Lambda1<V, U> transformer)
    {
        if (source != null) { for (U c : source) { if (isTrue(cardAccessor.apply(c))) { return transformer.apply(c); } } }
        return null;
    }

    // splits are even more fun
    public final void split(final Iterable<T> source,
            final List<T> trueList, final List<T> falseList)
    {
        if (source == null) { return; }
        for (T c : source) { if (isTrue(c)) { trueList.add(c); } else { falseList.add(c); } }
    }
    public final <U> void split(final Iterable<U> source, final Lambda1<T, U> accessor,
            final List<U> trueList, final List<U> falseList)
    {
        if (source == null) { return; }
        for (U c : source) { if (isTrue(accessor.apply(c))) { trueList.add(c); } else { falseList.add(c); } }
    }
    public final <U, V> void split(final Iterable<U> source, final Lambda1<T, U> cardAccessor,
            final Lambda1<V, U> transformer, final List<V> trueList, final List<V> falseList)
    {
        if (source == null) { return; }
        for (U c : source) {
            if (isTrue(cardAccessor.apply(c))) { trueList.add(transformer.apply(c)); }
            else  { falseList.add(transformer.apply(c)); }
        }
    }

    // Unique
    public final <K> Iterable<T> uniqueByLast(final Iterable<T> source, final Lambda1<K, T> fnUniqueKey)
    {
        Map<K, T> uniques = new Hashtable<K, T>();
        for (T c : source) { if (isTrue(c)) { uniques.put(fnUniqueKey.apply(c), c); } }
        return uniques.values();
    }
    public final <K, U> Iterable<U> uniqueByLast(final Iterable<U> source, final Lambda1<K, U> fnUniqueKey, final Lambda1<T, U> accessor)
    { // this might be exotic
        Map<K, U> uniques = new Hashtable<K, U>();
        for (U c : source) { if (isTrue(accessor.apply(c))) { uniques.put(fnUniqueKey.apply(c), c); } }
        return uniques.values();
    }
    public final <K> Iterable<T> uniqueByFirst(final Iterable<T> source, final Lambda1<K, T> fnUniqueKey)
    {
        Map<K, T> uniques = new Hashtable<K, T>();
        for (T c : source) {
            K key = fnUniqueKey.apply(c);
            if (isTrue(c) && !uniques.containsKey(key)) { uniques.put(fnUniqueKey.apply(c), c); }
        }
        return uniques.values();
    }
    public final <K, U> Iterable<U> uniqueByFirst(final Iterable<U> source, final Lambda1<K, U> fnUniqueKey, final Lambda1<T, U> accessor)
    { // this might be exotic
        Map<K, U> uniques = new Hashtable<K, U>();
        for (U c : source) {
            K key = fnUniqueKey.apply(c);
            if (isTrue(accessor.apply(c)) && !uniques.containsKey(key)) { uniques.put(fnUniqueKey.apply(c), c); }
        }
        return uniques.values();
    }

    // Count
    public final int count(final Iterable<T> source) {
        int result = 0;
        if (source != null) { for (T c : source) { if (isTrue(c)) { result++; } } }
        return result;
    }
    public final <U> int count(final Iterable<U> source, final Lambda1<T, U> accessor) {
        int result = 0;
        if (source != null) { for (U c : source) { if (isTrue(accessor.apply(c))) { result++; } } }
        return result;
    }

    // Aggregates?
    public final <U> int aggregate(final Iterable<U> source, final Lambda1<T, U> accessor,
            final Lambda1<Integer, U> valueAccessor)
    {
        int result = 0;
        if (source != null) { for (U c : source) { if (isTrue(accessor.apply(c))) { result += valueAccessor.apply(c); } } }
        return result;
    }

    // Random - algorithm adapted from Braid's GeneratorFunctions
    public final T random(final Iterable<T> source) {
        int n = 0;
        T candidate = null;
        for (T item : source) {
            if (!isTrue(item)) { continue; }
            if (Math.random() * ++n < 1) { candidate = item; }
        }
        return candidate;
    }
    public final <U> U random(final Iterable<U> source, final Lambda1<T, U> accessor) {
        int n = 0;
        U candidate = null;
        for (U item : source) {
            if (!isTrue(accessor.apply(item))) { continue; }
            if (Math.random() * ++n < 1) { candidate = item; }
        }
        return candidate;
    }

    // Get several random values
    // should improve to make 1 pass over source and track N candidates at once
    public final List<T> random(final Iterable<T> source, final int count) {
        List<T> result = new ArrayList<T>();
        for (int i = 0; i < count; ++i) {
            T toAdd = random(source);
            if (toAdd == null) { break; }
            result.add(toAdd);
        }
        return result;
    }
    public final <U> List<U> random(final Iterable<U> source, final Lambda1<T, U> accessor, final int count) {
        List<U> result = new ArrayList<U>();
        for (int i = 0; i < count; ++i) {
            U toAdd = random(source, accessor);
            if (toAdd == null) { break; }
            result.add(toAdd);
        }
        return result;
    }
    public final <V> List<V> random(final Iterable<T> source, final int count, final Lambda1<V, T> transformer) {
        List<V> result = new ArrayList<V>();
        for (int i = 0; i < count; ++i) {
            T toAdd = random(source);
            if (toAdd == null) { break; }
            result.add(transformer.apply(toAdd));
        }
        return result;
    }    

    // Static builder methods - they choose concrete implementation by themselves
    public static <U, T> Predicate<U> brigde(final Predicate<T> predicate, final Lambda1<T, U> fnBridge) {
        return new Bridge<T, U>(predicate, fnBridge);
    }
    public static <U, T> Predicate<U> instanceOf(final Predicate<T> predicate, final Class<T> clsTarget) {
        return new BridgeToInstance<T, U>(predicate, clsTarget);
    }

    public static <T> Predicate<T> not(final Predicate<T> operand1) { return new Not<T>(operand1); }
    public static <T> Predicate<T> compose(final Predicate<T> operand1,
            final PredicatesOp operator, final Predicate<T> operand2)
    {
        return new Node<T>(operand1, operator, operand2);
    }
    // Predefined operators: and, or
    public static <T> Predicate<T> and(final Predicate<T> operand1, final Predicate<T> operand2) {
        if (operand1.is1()) { return operand2; }
        if (operand2 == null || operand2.is1()) { return operand1; }
        return new NodeAnd<T>(operand1, operand2);
        }
    public static <T> Predicate<T> and(final Iterable<Predicate<T>> operand) { return new MultiNodeAnd<T>(operand); }
    public static <T, U> Predicate<T> and(final Predicate<T> operand1, final Predicate<U> operand2, final Lambda1<U, T> bridge)
        { return new NodeAndBridged<T, U>(operand1, operand2, bridge); }
    public static <T> Predicate<T> or(final Predicate<T> operand1, final Predicate<T> operand2)
        { return new NodeOr<T>(operand1, operand2); }
    public static <T> Predicate<T> or(final Iterable<Predicate<T>> operand) { return new MultiNodeOr<T>(operand); }
    public static <T, U> Predicate<T> or(final Predicate<T> operand1, final Predicate<U> operand2, final Lambda1<U, T> bridge)
        { return new NodeOrBridged<T, U>(operand1, operand2, bridge); }

 
    
    public static <T> Predicate<T> getTrue(final Class<T> cls) { return new LeafConstant<T>(true); }
    public static <T> Predicate<T> getFalse(final Class<T> cls) { return new LeafConstant<T>(false); }
}

// Concrete implementations
// unary operators
final class Not<T> extends Predicate<T> {
    protected final Predicate<T> filter;
    public Not(final Predicate<T> operand) { filter = operand; }
    @Override public boolean isTrue(final T card) { return !filter.isTrue(card); }
}
final class Bridge<T, U> extends Predicate<U> {
    protected final Predicate<T> filter;
    protected final Lambda1<T, U> fnBridge;
    public Bridge(final Predicate<T> operand, final Lambda1<T, U> fnTfromU) { filter = operand; fnBridge = fnTfromU; }
    @Override public boolean isTrue(final U card) { return filter.isTrue(fnBridge.apply(card)); }
    @Override public boolean is1() { return filter.is1(); }
}

final class BridgeToInstance<T, U> extends Predicate<U> {
    protected final Predicate<T> filter;
    protected final Class<T> clsBridge;
    public BridgeToInstance(final Predicate<T> operand, final Class<T> clsT) { filter = operand; clsBridge = clsT; }
    @SuppressWarnings("unchecked") @Override
    public boolean isTrue(final U card) { return clsBridge.isInstance(card) && filter.isTrue((T) card); }
    @Override public boolean is1() { return filter.is1(); }
}

// binary operators
class Node<T> extends Predicate<T> {
    private final PredicatesOp operator;
    protected final Predicate<T> filter1;
    protected final Predicate<T> filter2;

    public Node(final Predicate<T> operand1, final PredicatesOp op, final Predicate<T> operand2)
    {
        operator = op;
        filter1 = operand1;
        filter2 = operand2;
    }

    @Override public boolean isTrue(final T card) {
        switch (operator) {
            case AND: return filter1.isTrue(card) && filter2.isTrue(card);
            case NAND: return !(filter1.isTrue(card) && filter2.isTrue(card));
            case OR: return filter1.isTrue(card) || filter2.isTrue(card);
            case NOR: return !(filter1.isTrue(card) || filter2.isTrue(card));
            case XOR: return filter1.isTrue(card) ^ filter2.isTrue(card);
            case EQ: return filter1.isTrue(card) == filter2.isTrue(card);
            default: return false;
        }
    }
}
final class NodeOr<T> extends Node<T> {
    public NodeOr(final Predicate<T> operand1, final Predicate<T> operand2) {
        super(operand1, PredicatesOp.OR, operand2);
    }
    @Override public boolean isTrue(final T card) { return filter1.isTrue(card) || filter2.isTrue(card); }
}
final class NodeAnd<T> extends Node<T> {
    public NodeAnd(final Predicate<T> operand1, final Predicate<T> operand2) {
        super(operand1, PredicatesOp.AND, operand2);
    }
    @Override public boolean isTrue(final T card) { return filter1.isTrue(card) && filter2.isTrue(card); }
}

// Bridged OR and AND
final class NodeOrBridged<T,U> extends Predicate<T> {
    private final Predicate<T> filter1;
    private final Predicate<U> filter2;
    private final Lambda1<U, T> bridge;
    public NodeOrBridged(final Predicate<T> operand1, final Predicate<U> operand2, final Lambda1<U, T> accessor) {
        filter1 = operand1;
        filter2 = operand2;
        bridge = accessor;
    }
    @Override public boolean isTrue(final T card) { return filter1.isTrue(card) || filter2.isTrue(bridge.apply(card)); }
}
final class NodeAndBridged<T,U> extends Predicate<T> {
    private final Predicate<T> filter1;
    private final Predicate<U> filter2;
    private final Lambda1<U, T> bridge;
    public NodeAndBridged(final Predicate<T> operand1, final Predicate<U> operand2, final Lambda1<U, T> accessor) {
        filter1 = operand1;
        filter2 = operand2;
        bridge = accessor;
    }
    @Override public boolean isTrue(final T card) { return filter1.isTrue(card) && filter2.isTrue(bridge.apply(card)); }
}

// multi-operand operators
abstract class MultiNode<T> extends Predicate<T> {
    protected final Iterable<Predicate<T>> operands;
    public MultiNode(Iterable<Predicate<T>> filters) { operands = filters; }
}
final class MultiNodeAnd<T> extends MultiNode<T> {
    public MultiNodeAnd(final Iterable<Predicate<T>> filters) { super(filters); }
    @Override public boolean isTrue(final T subject) {
        for (Predicate<T> p : operands) { if (!p.isTrue(subject)) { return false; } }
        return true;
    }
}
final class MultiNodeOr<T> extends MultiNode<T> {
    public MultiNodeOr(final Iterable<Predicate<T>> filters) { super(filters); }
    @Override public boolean isTrue(final T subject) {
        for (Predicate<T> p : operands) { if (p.isTrue(subject)) { return true; } }
        return false;
    }
}

class LeafConstant<T> extends Predicate<T> {
    private final boolean bValue;

    @Override public boolean is1() { return bValue; }
    @Override public boolean is0() { return !bValue; }
    @Override public boolean isTrue(final T card) { return bValue; }
    public LeafConstant(final boolean value) { bValue = value; }
}