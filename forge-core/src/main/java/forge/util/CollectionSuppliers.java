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
        return Lists::newArrayList;
    }

    public static <T> Supplier<Set<T>> hashSets() {
        return Sets::newHashSet;
    }

    public static <T extends Comparable<T>> Supplier<SortedSet<T>> treeSets() {
        return Sets::newTreeSet;
    }
}
