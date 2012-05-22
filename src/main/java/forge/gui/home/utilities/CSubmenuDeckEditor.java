package forge.gui.home.utilities;

import forge.Command;
import forge.control.FControl;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.framework.ICDoc;
import forge.gui.home.ICSubmenu;

/** 
 * Controls the deck editor submenu option in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuDeckEditor implements ICSubmenu, ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuDeckEditor.SINGLETON_INSTANCE.populate();

    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @SuppressWarnings("serial")
    @Override
    public Command getMenuCommand() {
        return new Command() { @Override
            public void execute() { showDeckEditor(); } };
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() { }

    /**
     * Shows constructed mode editor.
     */
    private void showDeckEditor() {
        CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(new CEditorConstructed());
        FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_CONSTRUCTED);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
