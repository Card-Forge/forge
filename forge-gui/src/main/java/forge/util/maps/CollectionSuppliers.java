package forge.util.maps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import com.google.common.base.Supplier;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class CollectionSuppliers {


    public static <T> Supplier<ArrayList<T>> arrayLists() {
        return new Supplier<ArrayList<T>>() {
            @Override
            public ArrayList<T> get() {
                return new ArrayList<T>();
            }
        };
    }

    public static <T> Supplier<HashSet<T>> hashSets() {
        return new Supplier<HashSet<T>>() {
            @Override
            public HashSet<T> get() {
                return new HashSet<T>();
            }
        };
    }
    
    public static <T extends Comparable<T>> Supplier<TreeSet<T>> treeSets() {
        return new Supplier<TreeSet<T>>() {
            @Override
            public TreeSet<T> get() {
                return new TreeSet<T>();
            }
        };
    }
}
