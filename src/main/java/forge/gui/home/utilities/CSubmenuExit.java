package forge.gui.home.utilities;

import forge.Command;
import forge.gui.home.ICSubmenu;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CSubmenuExit implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() { }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @SuppressWarnings("serial")
    @Override
    public Command getMenuCommand() {
        return new Command() { @Override
            public void execute() { System.exit(0); } };
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() { }
}
