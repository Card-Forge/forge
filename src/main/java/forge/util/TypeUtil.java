package forge.util;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TypeUtil {

    /**
     * Cast object to a given type if possible, returning null if not possible
     * @param obj
     * @param type
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeCast(Object obj, Class<T> type) {
        if (type.isInstance(obj)) {
            return (T) obj;
        }
        return null;
    }
    
    /**
     * Find all components of the given type at or under the given component
     * @param compType
     * @param searchComp
     */
    public static <T extends Component> ArrayList<T> findAllComponents(Class<T> compType, Component searchComp) {
        ArrayList<T> comps = new ArrayList<T>();
        searchForComponents(compType, searchComp, comps);
        return comps;
    }

    private static <T extends Component> void searchForComponents(Class<T> compType, Component searchComp, ArrayList<T> comps) {
        T comp = safeCast(searchComp, compType);
        if (comp != null) {
            comps.add(comp);
        }
        
        Container container = safeCast(searchComp, Container.class);
        if (container != null) {
            //search child components
            for (Component c : container.getComponents()) {
                searchForComponents(compType, c, comps);
            }
        }
    }
    
    public static <K, V extends Comparable<V>> Map<K, V> sortMap(Map<K, V> unsortedMap, final boolean ascending){
        List<Entry<K, V>> list = new LinkedList<Entry<K, V>>(unsortedMap.entrySet());

        //sort the list based on values
        Collections.sort(list, new Comparator<Entry<K, V>>() {
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                if (ascending) {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else {
                    return o2.getValue().compareTo(o1.getValue());
                }
            }
        });

        //maintain insertion order with the help of LinkedList
        Map<K, V> sortedMap = new LinkedHashMap<K, V>();
        for (Entry<K, V> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
