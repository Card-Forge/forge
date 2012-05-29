package forge.gui.home.utilities;

import forge.Command;
import forge.control.FControl;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.framework.ICDoc;

/** 
 * Controls the deck editor submenu option in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuDeckEditor implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
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
    @SuppressWarnings("serial")
    @Override
    public Command getCommandOnSelect() {
        return new Command() { @Override
            public void execute() { showDeckEditor(); } };
    }
}
