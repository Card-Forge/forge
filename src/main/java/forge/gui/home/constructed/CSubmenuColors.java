package forge.gui.home.constructed;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.gui.home.ICSubmenu;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CSubmenuColors implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private final Map<String, String> colorVals = new HashMap<String, String>();

    private CSubmenuColors() {
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
        VSubmenuColors.SINGLETON_INSTANCE.populate();
        CSubmenuColors.SINGLETON_INSTANCE.update();

        for (final JList lst : VSubmenuColors.SINGLETON_INSTANCE.getLists()) {
            SubmenuConstructedUtil.randomSelect(lst);
        }

        VSubmenuColors.SINGLETON_INSTANCE.getBtnStart().addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseReleased(final MouseEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            SubmenuConstructedUtil.startGame(VSubmenuColors.SINGLETON_INSTANCE.getLists());
                        }
                    });
                }
            });
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
        for (final JList lst : VSubmenuColors.SINGLETON_INSTANCE.getLists()) {
            lst.setListData(new String[] {"Random 1", "Random 2", "Random 3",
                    "Random 4", "Black", "Blue", "Green", "Red", "White"});
        }
    }
}
