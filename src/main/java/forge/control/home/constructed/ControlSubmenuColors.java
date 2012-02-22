package forge.control.home.constructed;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JList;

import forge.Command;
import forge.control.home.IControlSubmenu;
import forge.view.home.constructed.ViewSubmenuColors;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum ControlSubmenuColors implements IControlSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private final Map<String, String> colorVals = new HashMap<String, String>();

    private ControlSubmenuColors() {
        colorVals.clear();
        colorVals.put("Random 1", "AI");
        colorVals.put("Random 2", "AI");
        colorVals.put("Random 3", "AI");
        colorVals.put("Random 4", "AI");
        colorVals.put("Black", "black");
        colorVals.put("Blue", "blue");
        colorVals.put("Green", "green");
        colorVals.put("Red", "red");
        colorVals.put("White", "white");
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        ViewSubmenuColors.SINGLETON_INSTANCE.populate();
        ControlSubmenuColors.SINGLETON_INSTANCE.update();
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        for (JList lst : ViewSubmenuColors.SINGLETON_INSTANCE.getLists()) {
            lst.setListData(new String[] {"Random 1", "Random 2", "Random 3",
                    "Random 4", "Black", "Blue", "Green", "Red", "White"});
        }
    }

    /** */
    public void randomSelect() {
        
    }
}
