package forge.util;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;

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
}
