package forge.util;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public final class CollectionSuppliers {

    /**
     * Private constructor to prevent instantiation.
     */
    private CollectionSuppliers() {
    }

    public static <T> Supplier<List<T>> arrayLists() {
        return new Supplier<List<T>>() {
            @Override public List<T> get() {
                return Lists.newArrayList();
            }
        };
    }

    public static <T> Supplier<Set<T>> hashSets() {
        return new Supplier<Set<T>>() {
            @Override public Set<T> get() {
                return Sets.newHashSet();
            }
        };
    }

    public static <T extends Comparable<T>> Supplier<SortedSet<T>> treeSets() {
        return new Supplier<SortedSet<T>>() {
            @Override public SortedSet<T> get() {
                return Sets.newTreeSet();
            }
        };
    }
}
