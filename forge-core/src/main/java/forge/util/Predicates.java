package forge.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class Predicates {
    private Predicates(){}

    //TODO: Migrate everything below.
    public static <T> Predicate<T> and(Iterable<? extends Predicate<? super T>> components) {
        //TODO: Should be able to clean up the casting here.
        return x -> Iterables.all(components, (Predicate<Predicate<? super T>>) i -> i.test(x));
    }
    public static <T> Predicate<T> and(Predicate<? super T>... components) {
        //TODO: Switch to iterables all once it stops being confused and the others are inlined.
        //Or just switch this to chained "and"s by hand.
        return x -> {
            for(Predicate<? super T> predicate : components) {
                if(!predicate.test(x))
                    return false;
            }
            return true;
        };
    }
    public static <T> Predicate<T> or(Iterable<? extends Predicate<? super T>> components) {
        //TODO: Should be able to clean up the casting here.
        return x -> Iterables.any(components, (Predicate<Predicate<? super T>>) i -> i.test(x));
    }
    public static <T> Predicate<T> or(Predicate<? super T>... components) {
        //TODO: Faster implementation. Or just do this one by hand.
        return or(Arrays.asList(components));
    }

    public static <A, B> Predicate<A> compose(Predicate<B> predicate, Function<A, ? extends B> function) {
        return x -> predicate.test(function.apply(x));
    }


    //TODO: Inline everything below.
    public static <T> Predicate<T> alwaysTrue() {
        return x -> true;
    }
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

    public static <T> Predicate<T> and(Predicate<? super T> first, Predicate<? super T> second) {
        //TODO: remove casting?
        return ((Predicate<T>) first).and(second);
    }

    public static <T> Predicate<T> or(Predicate<? super T> first, Predicate<? super T> second) {
        //TODO: remove casting?
        return ((Predicate<T>) first).or(second);
    }

    //TODO: Uncomment all when switching off Guava. Then Inline.

    //TODO: This one probably needs case by case; nullable targets need a safe test, whereas nonnull targets can be simplified further.
//    public static <T> Predicate<T> equalTo(T target) {
//        return x -> Objects.equals(target, x);
//    }

//    public static <T> Predicate<T> instanceOf(Class<?> clazz) {
//        return clazz::isInstance;
//    }
//    public static <T> Predicate<T> in(Collection<? extends T> target) {
//        return target::contains;
//    }



    //TODO: Delete everything below.
    public static <T> com.google.common.base.Predicate<T> not(com.google.common.base.Predicate<T> predicate) {
        return com.google.common.base.Predicates.not(predicate);
    }

    public static <T> com.google.common.base.Predicate<T> and(Iterable<? extends com.google.common.base.Predicate<? super T>> components) {
        return com.google.common.base.Predicates.and(components);
    }
    public static <T> com.google.common.base.Predicate<T> and(com.google.common.base.Predicate<? super T>... components) {
        return com.google.common.base.Predicates.and(components);
    }
    public static <T> com.google.common.base.Predicate<T> and(com.google.common.base.Predicate<? super T> first, com.google.common.base.Predicate<? super T> second) {
        return com.google.common.base.Predicates.and(first, second);
    }
    public static <T> com.google.common.base.Predicate<T> or(Iterable<? extends com.google.common.base.Predicate<? super T>> components) {
        return com.google.common.base.Predicates.or(components);
    }
    public static <T> com.google.common.base.Predicate<T> or(com.google.common.base.Predicate<? super T>... components) {
        return com.google.common.base.Predicates.or(components);
    }
    public static <T> com.google.common.base.Predicate<T> or(com.google.common.base.Predicate<? super T> first, com.google.common.base.Predicate<? super T> second) {
        return com.google.common.base.Predicates.or(first, second);
    }

    public static <T> com.google.common.base.Predicate<T> equalTo(T target) {
        return com.google.common.base.Predicates.equalTo(target);
    }
    public static <T> com.google.common.base.Predicate<T> instanceOf(Class<?> clazz) {
        return com.google.common.base.Predicates.instanceOf(clazz);
    }
    public static <T> com.google.common.base.Predicate<T> in(Collection<? extends T> target) {
        return com.google.common.base.Predicates.in(target);
    }

    public static <A, B> com.google.common.base.Predicate<A> compose(
            com.google.common.base.Predicate<B> predicate,
            com.google.common.base.Function<A, ? extends B> function) {
        return com.google.common.base.Predicates.compose(predicate, function);
    }
}
