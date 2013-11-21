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

public class AwtUtil {

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
        T comp = ReflectionUtil.safeCast(searchComp, compType);
        if (comp != null) {
            comps.add(comp);
        }
        
        Container container = ReflectionUtil.safeCast(searchComp, Container.class);
        if (container != null) {
            //search child components
            for (Component c : container.getComponents()) {
                searchForComponents(compType, c, comps);
            }
        }
    }

}
