package forge.util;

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
    public static <T> Predicate<T> or(Iterable<? extends Predicate<? super T>> components) {
        //TODO: Should be able to clean up the casting here.
        return x -> Iterables.any(components, (Predicate<Predicate<? super T>>) i -> i.test(x));
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
    public static <T> Predicate<T> equalTo(T target) {
        return x -> Objects.equals(target, x);
    }

    public static <T> Predicate<T> instanceOf(Class<?> clazz) {
        return clazz::isInstance;
    }
    public static <T> Predicate<T> in(Collection<? extends T> target) {
        return target::contains;
    }
}
