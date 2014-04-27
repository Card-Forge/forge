package forge.util;

import java.awt.*;
import java.util.ArrayList;

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

    @SuppressWarnings("unchecked")
    private static <T extends Component> void searchForComponents(Class<T> compType, Component searchComp, ArrayList<T> comps) {
        if (compType.isInstance(searchComp)) {
            comps.add((T) searchComp);
        }
        
        if (searchComp instanceof Container) {
            //search child components
            for (Component c : ((Container)searchComp).getComponents()) {
                searchForComponents(compType, c, comps);
            }
        }
    }

}
