package forge.gui.home.utilities;

import forge.Command;
import forge.gui.framework.ICDoc;
import forge.gui.home.ICSubmenu;

/** 
 * Controls the exit submenu option in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuExit implements ICSubmenu, ICDoc {
    /** */
    SINGLETON_INSTANCE;

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

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return getMenuCommand();
    }
}
