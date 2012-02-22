package forge.control.home.constructed;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;

import forge.Command;
import forge.Singletons;
import forge.control.FControl;
import forge.control.home.IControlSubmenu;
import forge.deck.Deck;
import forge.util.IFolderMap;
import forge.view.home.constructed.ViewSubmenuCustom;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum ControlSubmenuCustom implements IControlSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getMenuCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @Override
    public void initialize() {
        ViewSubmenuCustom.SINGLETON_INSTANCE.populate();
        ControlSubmenuCustom.SINGLETON_INSTANCE.update();
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final List<String> customNames = new ArrayList<String>();
        final IFolderMap<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        for (JList lst : ViewSubmenuCustom.SINGLETON_INSTANCE.getLists()) {
            lst.setListData(FControl.oa2sa(customNames.toArray()));
        }
    }

    /** */
    public void randomSelect() {
        
    }
}
